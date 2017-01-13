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

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

/**
 * WARNING!: DO NOT USE LOGGING IN THIS CLASS.  IF THINK THIS IS A JOKE OR NO LONGER RELEVENT, YOU JUST HAVEN'T
 * UNDERSTOOD THE CHICKEN AND EGG PROBLEM YET.  GO AWAY AND COME BACK WHEN YOU DO.
 *
 * This class provides simple crypto functions.  The key is embedded in the class file, so any determined hacker
 * can break the encyrption.  Usage of this class should be considered only for primitive obsufication of text.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.3 $
 */
public class Crypto {
    private static final byte[] desKeyData = {(byte) 0x02, (byte) 0x04, (byte) 0x03, (byte) 0x06, (byte) 0x08, (byte) 0x01, (byte) 0x09, (byte) 0x08};
    private static final String cipherType = "DES";
    private static Cipher cipher;
    private static SecretKey key;

    static {
        try {
            DESKeySpec desKeySpec = new DESKeySpec(desKeyData);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(cipherType);
            key = keyFactory.generateSecret(desKeySpec);
            cipher = Cipher.getInstance(cipherType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String encrypt(String clearText) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] bytes = cipher.doFinal(clearText.getBytes());
            return Base64.encode(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String decrypt(String cipherText) {
        try {
            byte[] bytes = Base64.decode(cipherText);
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(bytes));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String clearText = args[i];
            String cipherText = encrypt(clearText);
            System.out.println("'" + decrypt(cipherText) + "' = '" + cipherText + "'");
        }
    }
}
