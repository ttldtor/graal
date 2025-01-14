/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.compiler.replacements.amd64;

import java.util.EnumSet;

import org.graalvm.compiler.asm.amd64.AVXKind;
import org.graalvm.compiler.core.common.Stride;
import org.graalvm.compiler.core.common.StrideUtil;
import org.graalvm.compiler.core.common.spi.ForeignCallDescriptor;
import org.graalvm.compiler.debug.GraalError;
import org.graalvm.compiler.lir.amd64.AMD64ArrayEqualsOp;
import org.graalvm.compiler.lir.gen.LIRGenerator;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.replacements.NodeStrideUtil;
import org.graalvm.compiler.replacements.nodes.ForeignCalls;
import org.graalvm.word.Pointer;

import jdk.vm.ci.amd64.AMD64;
import jdk.vm.ci.meta.JavaKind;

public final class AMD64ArrayEqualsWithMaskForeignCalls {
    private static final ForeignCallDescriptor STUB_REGION_EQUALS_S1_S2_S1 = foreignCallDescriptor("arrayRegionEqualsS1S2S1");
    private static final ForeignCallDescriptor STUB_REGION_EQUALS_S2_S2_S1 = foreignCallDescriptor("arrayRegionEqualsS2S2S1");
    private static final ForeignCallDescriptor STUB_REGION_EQUALS_DYNAMIC_STRIDES = ForeignCalls.pureFunctionForeignCallDescriptor(
                    "arrayRegionEqualsDynamicStrides", boolean.class, Object.class, long.class, Object.class, long.class, Pointer.class, int.class, int.class);
    /**
     * CAUTION: the ordering here is important: entries 0-9 must match the indices generated by
     * {@link NodeStrideUtil#getDirectStubCallIndex(ValueNode, Stride, Stride)}.
     *
     * @see #getStub(AMD64ArrayRegionEqualsWithMaskNode, LIRGenerator)
     */
    public static final ForeignCallDescriptor[] STUBS = {
                    foreignCallDescriptor("arrayRegionEqualsS1S1"),
                    foreignCallDescriptor("arrayRegionEqualsS1S2"),
                    foreignCallDescriptor("arrayRegionEqualsS1S4"),
                    foreignCallDescriptor("arrayRegionEqualsS2S1"),
                    foreignCallDescriptor("arrayRegionEqualsS2S2"),
                    foreignCallDescriptor("arrayRegionEqualsS2S4"),
                    foreignCallDescriptor("arrayRegionEqualsS4S1"),
                    foreignCallDescriptor("arrayRegionEqualsS4S2"),
                    foreignCallDescriptor("arrayRegionEqualsS4S4"),

                    STUB_REGION_EQUALS_DYNAMIC_STRIDES,

                    STUB_REGION_EQUALS_S1_S2_S1,
                    STUB_REGION_EQUALS_S2_S2_S1,
    };

    private static ForeignCallDescriptor foreignCallDescriptor(String name) {
        return ForeignCalls.pureFunctionForeignCallDescriptor(name, boolean.class, Object.class, long.class, Object.class, long.class, Pointer.class, int.class);
    }

    @SuppressWarnings("unchecked")
    public static ForeignCallDescriptor getStub(AMD64ArrayRegionEqualsWithMaskNode node, LIRGenerator gen) {
        Stride strideA = node.getStrideA();
        Stride strideB = node.getStrideB();
        Stride strideM = node.getStrideMask();
        ValueNode length = node.getLength();
        int directStubCallIndex = node.getDirectStubCallIndex();
        GraalError.guarantee(-1 <= directStubCallIndex && directStubCallIndex < 9, "invalid direct stub call index");
        if (directStubCallIndex >= 0 && length.isJavaConstant() &&
                        AMD64ArrayEqualsOp.canGenerateConstantLengthCompare(gen.target(),
                                        (EnumSet<AMD64.CPUFeature>) node.getRuntimeCheckedCPUFeatures(),
                                        JavaKind.Byte,
                                        StrideUtil.getConstantStrideA(directStubCallIndex),
                                        StrideUtil.getConstantStrideB(directStubCallIndex),
                                        length.asJavaConstant().asInt(), (AVXKind.AVXSize) gen.getMaxVectorSize(node.getRuntimeCheckedCPUFeatures()))) {
            // Yield constant-length arrays comparison assembly
            return null;
        }
        if (strideB != null && strideB != strideM) {
            if (strideA == Stride.S1) {
                GraalError.guarantee(strideB == Stride.S2 && strideM == Stride.S1, "unsupported strides");
                return STUB_REGION_EQUALS_S1_S2_S1;
            } else {
                GraalError.guarantee(strideA == Stride.S2 && strideB == Stride.S2 && strideM == Stride.S1, "unsupported strides");
                return STUB_REGION_EQUALS_S2_S2_S1;
            }
        }
        GraalError.guarantee(strideB == strideM, "unsupported strides");
        return directStubCallIndex < 0 ? STUB_REGION_EQUALS_DYNAMIC_STRIDES : STUBS[directStubCallIndex];
    }
}
