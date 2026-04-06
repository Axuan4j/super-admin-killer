package com.sak.service.service;

import com.sak.service.dto.CaptchaResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaService {

    private static final String CAPTCHA_PREFIX = "auth:captcha:";
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CAPTCHA_LENGTH = 4;
    private static final int CAPTCHA_WIDTH = 130;
    private static final int CAPTCHA_HEIGHT = 44;
    private static final long CAPTCHA_EXPIRE_MINUTES = 5L;

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public CaptchaService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public CaptchaResponse generateCaptcha() {
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String captchaCode = createCaptchaCode();

        redisTemplate.opsForValue().set(
                CAPTCHA_PREFIX + captchaId,
                captchaCode,
                CAPTCHA_EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );

        return new CaptchaResponse(captchaId, createCaptchaImage(captchaCode));
    }

    public boolean validateCaptcha(String captchaId, String captchaCode) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captchaCode)) {
            return false;
        }

        String redisKey = CAPTCHA_PREFIX + captchaId;
        Object cachedCaptcha = redisTemplate.opsForValue().get(redisKey);
        redisTemplate.delete(redisKey);

        if (cachedCaptcha == null) {
            return false;
        }

        return captchaCode.trim().equalsIgnoreCase(String.valueOf(cachedCaptcha));
    }

    private String createCaptchaCode() {
        StringBuilder builder = new StringBuilder(CAPTCHA_LENGTH);
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            builder.append(CAPTCHA_CHARS.charAt(secureRandom.nextInt(CAPTCHA_CHARS.length())));
        }
        return builder.toString();
    }

    private String createCaptchaImage(String captchaCode) {
        BufferedImage image = new BufferedImage(CAPTCHA_WIDTH, CAPTCHA_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(245, 247, 250));
            graphics.fillRect(0, 0, CAPTCHA_WIDTH, CAPTCHA_HEIGHT);

            drawNoiseLines(graphics);
            drawNoiseDots(graphics);
            drawCaptchaText(graphics, captchaCode);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException exception) {
            throw new IllegalStateException("生成验证码图片失败", exception);
        } finally {
            graphics.dispose();
        }
    }

    private void drawNoiseLines(Graphics2D graphics) {
        for (int i = 0; i < 8; i++) {
            graphics.setColor(randomColor(160, 220));
            int x1 = secureRandom.nextInt(CAPTCHA_WIDTH);
            int y1 = secureRandom.nextInt(CAPTCHA_HEIGHT);
            int x2 = secureRandom.nextInt(CAPTCHA_WIDTH);
            int y2 = secureRandom.nextInt(CAPTCHA_HEIGHT);
            graphics.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawNoiseDots(Graphics2D graphics) {
        for (int i = 0; i < 40; i++) {
            graphics.setColor(randomColor(150, 230));
            int x = secureRandom.nextInt(CAPTCHA_WIDTH);
            int y = secureRandom.nextInt(CAPTCHA_HEIGHT);
            graphics.fillOval(x, y, 2, 2);
        }
    }

    private void drawCaptchaText(Graphics2D graphics, String captchaCode) {
        graphics.setFont(new Font("Arial", Font.BOLD, 28));
        FontMetrics fontMetrics = graphics.getFontMetrics();
        int baseY = ((CAPTCHA_HEIGHT - fontMetrics.getHeight()) / 2) + fontMetrics.getAscent();
        int charGap = CAPTCHA_WIDTH / (captchaCode.length() + 1);

        for (int i = 0; i < captchaCode.length(); i++) {
            int x = charGap * i + 18;
            int y = baseY + secureRandom.nextInt(7) - 3;
            AffineTransform originalTransform = graphics.getTransform();
            graphics.rotate(Math.toRadians(secureRandom.nextInt(31) - 15), x, y);
            graphics.setColor(randomColor(30, 130));
            graphics.drawString(String.valueOf(captchaCode.charAt(i)), x, y);
            graphics.setTransform(originalTransform);
        }
    }

    private Color randomColor(int min, int max) {
        int boundedMax = Math.max(min, max);
        int red = min + secureRandom.nextInt(boundedMax - min + 1);
        int green = min + secureRandom.nextInt(boundedMax - min + 1);
        int blue = min + secureRandom.nextInt(boundedMax - min + 1);
        return new Color(red, green, blue);
    }
}
