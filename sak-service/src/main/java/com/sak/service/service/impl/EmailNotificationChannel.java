package com.sak.service.service.impl;

import com.sak.service.dto.NotificationChannelSendResult;
import com.sak.service.dto.NotificationDispatchMessage;
import com.sak.service.entity.SysUser;
import com.sak.service.service.NotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EmailNotificationChannel implements NotificationChannel {

    public static final String CHANNEL_CODE = "EMAIL";

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    private final MailProperties mailProperties;

    @Override
    public String getChannelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public void validateGlobalConfig() {
        if (mailSenderProvider.getIfAvailable() == null) {
            throw new IllegalArgumentException("邮件通道未启用，请先配置 spring.mail.*");
        }
        if (!StringUtils.hasText(mailProperties.getHost())) {
            throw new IllegalArgumentException("邮件通道未配置 host");
        }
        if (!StringUtils.hasText(mailProperties.getUsername())) {
            throw new IllegalArgumentException("邮件通道未配置发件人账号");
        }
    }

    @Override
    public NotificationChannelSendResult send(NotificationDispatchMessage message, SysUser recipient) {
        if (!StringUtils.hasText(recipient.getEmail())) {
            return NotificationChannelSendResult.skipped("用户未配置邮箱");
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new IllegalArgumentException("邮件通道未启用");
        }

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(mailProperties.getUsername());
        mailMessage.setTo(recipient.getEmail().trim());
        mailMessage.setSubject(message.getTitle());
        mailMessage.setText(message.getContent());
        mailSender.send(mailMessage);
        return NotificationChannelSendResult.success();
    }
}
