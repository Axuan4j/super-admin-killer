package com.sak.service.util;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;

public final class Base32Util {

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final int[] LOOKUP = new int[128];
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static {
        for (int i = 0; i < LOOKUP.length; i++) {
            LOOKUP[i] = -1;
        }
        for (int i = 0; i < ALPHABET.length; i++) {
            LOOKUP[ALPHABET[i]] = i;
        }
    }

    private Base32Util() {
    }

    public static String generateSecret(int byteLength) {
        byte[] buffer = new byte[byteLength];
        SECURE_RANDOM.nextBytes(buffer);
        return encode(buffer);
    }

    public static String encode(byte[] data) {
        StringBuilder builder = new StringBuilder((data.length * 8 + 4) / 5);
        int current = 0;
        int bits = 0;
        for (byte datum : data) {
            current = (current << 8) | (datum & 0xFF);
            bits += 8;
            while (bits >= 5) {
                builder.append(ALPHABET[(current >> (bits - 5)) & 0x1F]);
                bits -= 5;
            }
        }
        if (bits > 0) {
            builder.append(ALPHABET[(current << (5 - bits)) & 0x1F]);
        }
        return builder.toString();
    }

    public static byte[] decode(String value) {
        if (value == null) {
            return new byte[0];
        }
        String normalized = value.replace("=", "").replace(" ", "").toUpperCase();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int current = 0;
        int bits = 0;
        for (int i = 0; i < normalized.length(); i++) {
            char currentChar = normalized.charAt(i);
            if (currentChar >= LOOKUP.length || LOOKUP[currentChar] < 0) {
                throw new IllegalArgumentException("无效的Base32字符");
            }
            current = (current << 5) | LOOKUP[currentChar];
            bits += 5;
            if (bits >= 8) {
                output.write((current >> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return output.toByteArray();
    }
}
