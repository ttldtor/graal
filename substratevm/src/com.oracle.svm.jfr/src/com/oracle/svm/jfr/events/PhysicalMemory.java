/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.svm.jfr.events;

import jdk.jfr.Category;
import jdk.jfr.DataAmount;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.Period;
import jdk.jfr.StackTrace;
import jdk.jfr.internal.Type;

@Label("Physical Memory")
@Description("OS Physical Memory")
@Category("Operating System, Memory")
@StackTrace(false)
@Name(Type.EVENT_NAME_PREFIX + "PhysicalMemory")
@Period(value = "everyChunk")
public class PhysicalMemory extends Event {

    @Label("Total Size") @Description("Total amount of physical memory available to OS") @DataAmount long totalSize;

    @Label("Used Size") @Description("Total amount of physical memory in use") @DataAmount long usedSize;

    public static void emitPhysicalMemory() {
        PhysicalMemory pmInfo = new PhysicalMemory();

        pmInfo.totalSize = com.oracle.svm.core.heap.PhysicalMemory.size().rawValue();
        // usedSize is not implemented at the moment.
        pmInfo.commit();
    }
}
