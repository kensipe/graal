/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.compiler.api.replacements.ClassSubstitution;
import org.graalvm.compiler.api.replacements.Fold;
import org.graalvm.compiler.api.replacements.Fold.InjectedParameter;
import org.graalvm.compiler.api.replacements.MethodSubstitution;
import org.graalvm.compiler.nodes.DeoptimizeNode;
import org.graalvm.compiler.replacements.nodes.ArrayCompareToNode;
import org.graalvm.compiler.word.Word;
import org.graalvm.word.Pointer;

import jdk.vm.ci.meta.DeoptimizationAction;
import jdk.vm.ci.meta.DeoptimizationReason;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.MetaAccessProvider;

// JaCoCo Exclude

/**
 * Substitutions for {@code java.lang.StringLatin1} methods.
 *
 * Since JDK 9.
 */
@ClassSubstitution(className = "java.lang.StringLatin1", optional = true)
public class AMD64StringLatin1Substitutions {

    @Fold
    static int byteArrayBaseOffset(@InjectedParameter MetaAccessProvider metaAccess) {
        return metaAccess.getArrayBaseOffset(JavaKind.Byte);
    }

    @Fold
    static int byteArrayIndexScale(@InjectedParameter MetaAccessProvider metaAccess) {
        return metaAccess.getArrayBaseOffset(JavaKind.Byte);
    }

    @Fold
    static int charArrayBaseOffset(@InjectedParameter MetaAccessProvider metaAccess) {
        return metaAccess.getArrayBaseOffset(JavaKind.Char);
    }

    @Fold
    static int charArrayIndexScale(@InjectedParameter MetaAccessProvider metaAccess) {
        return metaAccess.getArrayIndexScale(JavaKind.Char);
    }

    /** Marker value for the {@link InjectedParameter} injected parameter. */
    static final MetaAccessProvider INJECTED = null;

    /**
     * @param value is byte[]
     * @param other is byte[]
     */
    @MethodSubstitution
    public static int compareTo(byte[] value, byte[] other) {
        return ArrayCompareToNode.compareTo(value, other, value.length, other.length, JavaKind.Byte, JavaKind.Byte);
    }

    /**
     * @param value is byte[]
     * @param other is char[]
     */
    @MethodSubstitution
    public static int compareToUTF16(byte[] value, byte[] other) {
        return ArrayCompareToNode.compareTo(value, other, value.length, other.length, JavaKind.Byte, JavaKind.Char);
    }

    @MethodSubstitution(optional = true)
    public static int indexOf(byte[] value, int ch, int origFromIndex) {
        int fromIndex = origFromIndex;
        if (ch >>> 8 != 0) {
            // search value must be a byte value
            return -1;
        }
        int length = value.length;
        if (fromIndex < 0) {
            fromIndex = 0;
        } else if (fromIndex >= length) {
            // Note: fromIndex might be near -1>>>1.
            return -1;
        }
        Pointer sourcePointer = Word.objectToTrackedPointer(value).add(byteArrayBaseOffset(INJECTED)).add(fromIndex);
        int result = AMD64ArrayIndexOf.indexOf1Byte(sourcePointer, length - fromIndex, (byte) ch);
        if (result != -1) {
            return result + fromIndex;
        }
        return result;
    }

    /*-
     * java.lang.StringLatin1.inflate([BI[CII)V
     *
     * @HotSpotIntrinsicCandidate
     * public static void inflate(byte[] src, int src_indx, char[] dst, int dst_indx, int len)
     */
    @MethodSubstitution
    public static void inflate(byte[] src, int sndx, char[] dst, int dndx, int len) {
        if (len < 0 || sndx < 0 || (sndx + len > src.length) || dndx < 0 || (dndx + len > dst.length)) {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.BoundsCheckException);
        }

        // Offset calc. outside of the actual intrinsic.
        Pointer srcptr = Word.objectToTrackedPointer(src).add(byteArrayBaseOffset(INJECTED)).add(sndx * byteArrayIndexScale(INJECTED));
        Pointer dstptr = Word.objectToTrackedPointer(dst).add(charArrayBaseOffset(INJECTED)).add(dndx * charArrayIndexScale(INJECTED));
        AMD64StringLatin1InflateNode.inflate(srcptr, dstptr, len);
    }

    /*-
     * java.lang.StringLatin1.inflate([BI[BII)V
     *
     * @HotSpotIntrinsicCandidate
     * public static void inflate(byte[] src, int src_indx, byte[] dst, int dst_indx, int len)
     */
    @MethodSubstitution
    public static void inflate(byte[] src, int sndx, byte[] dst, int dndx, int len) {
        if (len < 0 || sndx < 0 || (sndx + len > src.length) || dndx < 0 || (dndx + len * 2 > dst.length)) {
            DeoptimizeNode.deopt(DeoptimizationAction.None, DeoptimizationReason.BoundsCheckException);
        }

        // Offset calc. outside of the actual intrinsic.
        Pointer srcptr = Word.objectToTrackedPointer(src).add(byteArrayBaseOffset(INJECTED)).add(sndx * byteArrayIndexScale(INJECTED));
        Pointer dstptr = Word.objectToTrackedPointer(dst).add(byteArrayBaseOffset(INJECTED)).add(dndx * 2 * byteArrayBaseOffset(INJECTED));
        AMD64StringLatin1InflateNode.inflate(srcptr, dstptr, len);
    }

}
