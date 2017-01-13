/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

package org.bhavaya.util;

import com.sun.org.apache.xml.internal.utils.FastStringBuffer;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.2 $
 */
public class Base64 {
    private static final FastStringBufferThreadLocal bufferThreadLocal = new FastStringBufferThreadLocal();

    public Base64() {
    }

    public static String encode(byte abyte0[]) {
        return encode(abyte0, 0);
    }

    private static String encode(byte abyte0[], int i) {
        int j = 0;
        FastStringBuffer encoded = (FastStringBuffer) bufferThreadLocal.get();
        for (int k = 0; k < abyte0.length; k += 3) {
            char[] chars = encodeBlock(abyte0, k);
            encoded.append(chars, 0, chars.length);
            if (i > 0 && (j += 4) >= i) {
                encoded.append('\n');
                j = 0;
            }
        }

        if (i > 0) encoded.append('\n');
        return encoded.toString();
    }

    private static char[] encodeBlock(byte abyte0[], int i) {
        int j = 0;
        int k = abyte0.length - i - 1;
        int l = k < 2 ? k : 2;
        for (int i1 = 0; i1 <= l; i1++) {
            byte byte0 = abyte0[i + i1];
            int j1 = byte0 >= 0 ? ((int) (byte0)) : byte0 + 256;
            j += j1 << 8 * (2 - i1);
        }

        char ac[] = new char[4];
        for (int k1 = 0; k1 < 4; k1++) {
            int l1 = j >>> 6 * (3 - k1) & 63;
            ac[k1] = getChar(l1);
        }

        if (k < 1) ac[2] = '=';
        if (k < 2) ac[3] = '=';
        return ac;
    }

    private static char getChar(int i) {
        if (i >= 0 && i <= 25) return (char) (65 + i);
        if (i >= 26 && i <= 51) return (char) (97 + (i - 26));
        if (i >= 52 && i <= 61) return (char) (48 + (i - 52));
        if (i == 62) return '+';
        return i != 63 ? '?' : '/';
    }

    public static byte[] decode(String s) throws Exception {
        if (s == null || s.length() < 1) return null;
        int i = s.length();
        int j = 0;
        for (int k = i - 1; s.charAt(k) == '='; k--) {
            j++;
        }

        int l = (i * 6) / 8 - j;
        byte abyte0[] = new byte[l];
        int i1 = 0;
        int j1 = 0;
        int k1 = 0;
        int l1 = 0;
        int i2 = 0;
        while (l1 < i) {
            int j2 = getValue(s.charAt(l1));
            l1++;
            switch (j2) {
                case -3:
                    throw new Exception("Base64 decoding error: " + s.charAt(l1) + "," + l1);

                case -1:
                    i2++;
                    j2 = 0;
                    // fall through

                default:
                    j1 = (j1 << 6) + j2;
                    k1++;
                    break;

                case -2:
                    break;
            }
            if (k1 != 4) continue;
            k1 = 0;
            abyte0[i1++] = (byte) (j1 >> 16);
            if (i2 == 2) break;
            if (i1 < l) {
                abyte0[i1++] = (byte) (j1 >> 8 & 255);
                if (i2 == 1) break;
                if (i1 < l) abyte0[i1++] = (byte) (j1 & 255);
            }
            if (i2 > 0) break;
        }
        if (i1 == l) return abyte0;
        if (i2 > 2) {
            throw new Exception("Base64 decoding error too many padding characters: " + i2);
        } else {
            int k2 = i1;
            byte abyte1[] = new byte[k2];
            System.arraycopy(abyte0, 0, abyte1, 0, k2);
            return abyte1;
        }
    }

    private static int getValue(char c) {
        if (c >= 'A' && c <= 'Z') return c - 65;
        if (c >= 'a' && c <= 'z') return (c - 97) + 26;
        if (c >= '0' && c <= '9') return (c - 48) + 52;
        if (c == '+') return 62;
        if (c == '/') return 63;
        if (c == '=') return -1;
        return !Character.isWhitespace(c) ? -3 : -2;
    }
}
