/**
 * Copyright 2014-2020 [fisco-dev]
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fisco.bcos.sdk.v3.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import org.fisco.bcos.sdk.v3.utils.exceptions.MessageDecodingException;
import org.fisco.bcos.sdk.v3.utils.exceptions.MessageEncodingException;

/** Message codec functions. */
public final class Numeric {

    private static final String HEX_PREFIX = "0x";

    private Numeric() {}

    public static String encodeQuantity(BigInteger value) {
        if (value.signum() != -1) {
            return HEX_PREFIX + value.toString(16);
        } else {
            throw new MessageEncodingException("Negative values are not supported");
        }
    }

    public static BigInteger decodeQuantity(String value) {
        if (!isValidHexQuantity(value)) {
            try {
                if (value == null) return BigInteger.ZERO;
                return new BigInteger(value);
            } catch (NumberFormatException e) {
                throw new MessageDecodingException("Negative ", e);
            }
        } else {
            try {
                return new BigInteger(value.substring(2), 16);
            } catch (NumberFormatException e) {
                throw new MessageDecodingException("value is not a hex number or a decimal number");
            }
        }
    }

    private static boolean isValidHexQuantity(String value) {
        if (value == null) {
            return false;
        }

        if (value.length() < 3) {
            return false;
        }
        return (value.startsWith(HEX_PREFIX));
    }

    public static String cleanHexPrefix(String input) {
        if (containsHexPrefix(input)) {
            return input.substring(2);
        } else {
            return input;
        }
    }

    public static String prependHexPrefix(String input) {
        if (!containsHexPrefix(input)) {
            return HEX_PREFIX + input;
        } else {
            return input;
        }
    }

    public static boolean containsHexPrefix(String input) {
        return !StringUtils.isEmpty(input)
                && input.length() > 1
                && input.charAt(0) == '0'
                && input.charAt(1) == 'x';
    }

    public static BigInteger toBigInt(byte[] value, int offset, int length) {
        return toBigInt((Arrays.copyOfRange(value, offset, offset + length)));
    }

    public static BigInteger toBigInt(byte[] value) {
        return new BigInteger(1, value);
    }

    public static BigInteger toBigInt(String hexValue) {
        String cleanValue = cleanHexPrefix(hexValue);
        return toBigIntNoPrefix(cleanValue);
    }

    public static BigInteger toBigIntNoPrefix(String hexValue) {
        return new BigInteger(hexValue, 16);
    }

    public static String toHexStringWithPrefix(BigInteger value) {
        return HEX_PREFIX + value.toString(16);
    }

    public static String toHexStringNoPrefix(byte[] input) {
        return Hex.toHexString(input);
    }

    public static String toHexStringNoPrefix(BigInteger value) {
        return value.toString(16);
    }

    public static String toHexStringWithPrefixZeroPadded(BigInteger value, int size) {
        return toHexStringZeroPadded(value, size, true);
    }

    public static String toHexStringWithPrefixSafe(BigInteger value) {
        String result = toHexStringNoPrefix(value);
        if (result.length() < 2) {
            result = StringUtils.zeros(1) + result;
        }
        return HEX_PREFIX + result;
    }

    public static String toHexStringNoPrefixZeroPadded(BigInteger value, int size) {
        return toHexStringZeroPadded(value, size, false);
    }

    private static String toHexStringZeroPadded(BigInteger value, int size, boolean withPrefix) {
        String result = toHexStringNoPrefix(value);

        int length = result.length();
        if (length > size) {
            throw new UnsupportedOperationException(
                    "Value " + result + "is larger then length " + size);
        } else if (value.signum() < 0) {
            throw new UnsupportedOperationException("Value cannot be negative");
        }

        if (length < size) {
            result = StringUtils.zeros(size - length) + result;
        }

        if (withPrefix) {
            return HEX_PREFIX + result;
        } else {
            return result;
        }
    }

    public static byte[] toBytesPadded(BigInteger value, int length) {
        byte[] result = new byte[length];
        byte[] bytes = value.toByteArray();

        int bytesLength;
        int srcOffset;
        if (bytes[0] == 0) {
            bytesLength = bytes.length - 1;
            srcOffset = 1;
        } else {
            bytesLength = bytes.length;
            srcOffset = 0;
        }

        if (bytesLength > length) {
            throw new RuntimeException("Input is too large to put in byte array of size " + length);
        }

        int destOffset = length - bytesLength;
        System.arraycopy(bytes, srcOffset, result, destOffset, bytesLength);
        return result;
    }

    public static byte[] hexStringToByteArray(String input) {
        String cleanInput = cleanHexPrefix(input);

        int len = cleanInput.length();

        if (len == 0) {
            return new byte[] {};
        }

        byte[] data;
        int startIdx;
        if (len % 2 != 0) {
            data = new byte[(len / 2) + 1];
            data[0] = (byte) Character.digit(cleanInput.charAt(0), 16);
            startIdx = 1;
        } else {
            data = new byte[len / 2];
            startIdx = 0;
        }

        for (int i = startIdx; i < len; i += 2) {
            data[(i + 1) / 2] =
                    (byte)
                            ((Character.digit(cleanInput.charAt(i), 16) << 4)
                                    + Character.digit(cleanInput.charAt(i + 1), 16));
        }
        return data;
    }

    public static String toHexString(byte[] input, int offset, int length, boolean withPrefix) {
        return withPrefix
                ? toHexString(Arrays.copyOfRange(input, offset, offset + length))
                : toHexStringNoPrefix(Arrays.copyOfRange(input, offset, offset + length));
    }

    public static String toHexString(byte[] input) {
        return "0x" + Hex.toHexString(input);
    }

    public static byte asByte(int m, int n) {
        return (byte) ((m << 4) | n);
    }

    public static boolean isIntegerValue(BigDecimal value) {
        return value.signum() == 0 || value.scale() <= 0 || value.stripTrailingZeros().scale() <= 0;
    }

    public static String getKeyNoPrefix(String prefix, String keyStr, int keyLengthInHex) {
        String keyNoPrefix = Numeric.cleanHexPrefix(keyStr);
        if (keyNoPrefix.startsWith(prefix)
                && keyNoPrefix.length() == keyLengthInHex + prefix.length()) {
            keyNoPrefix = keyNoPrefix.substring(prefix.length());
        }
        // Hexadecimal public key length is less than 128, add 0 in front
        if (keyNoPrefix.length() < keyLengthInHex) {
            keyNoPrefix = StringUtils.zeros(keyLengthInHex - keyNoPrefix.length()) + keyNoPrefix;
        }
        return keyNoPrefix;
    }

    public static String getHexKeyWithPrefix(
            String hexPublicKey, String requiredPrefix, int requiredKeyLengthInHex) {
        String keyWithPrefix = Numeric.cleanHexPrefix(hexPublicKey);
        if (keyWithPrefix.length() < requiredKeyLengthInHex) {
            keyWithPrefix =
                    StringUtils.zeros(requiredKeyLengthInHex - keyWithPrefix.length())
                            + keyWithPrefix;
        }
        if (!keyWithPrefix.startsWith(requiredPrefix)) {
            keyWithPrefix = requiredPrefix + keyWithPrefix;
        }
        return keyWithPrefix;
    }
}
