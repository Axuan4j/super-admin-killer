package com.sak.service.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.time.Instant;

public final class TotpUtil {

    private static final long TIME_STEP_SECONDS = 30L;
    private static final int CODE_DIGITS = 6;
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    private TotpUtil() {
    }

    public static boolean verifyCode(String secret, String code, int window) {
        if (secret == null || code == null || !code.matches("^\\d{6}$")) {
            return false;
        }
        long counter = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        for (int i = -window; i <= window; i++) {
            if (generateCode(secret, counter + i).equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static String generateCode(String secret, long counter) {
        try {
            byte[] key = Base32Util.decode(secret);
            byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("生成动态验证码失败", e);
        }
    }
}
