package com.sak.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.sak.service.entity.SysSiteMessage;
import com.sak.service.entity.SysUser;
import com.sak.service.mapper.SysSiteMessageMapper;
import com.sak.service.mapper.SysUserMapper;
import com.sak.service.service.SiteMessagePushService;
import com.sak.service.service.SysSiteMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysSiteMessageServiceImpl implements SysSiteMessageService {

    private final SysSiteMessageMapper sysSiteMessageMapper;
    private final SysUserMapper sysUserMapper;
    private final SiteMessagePushService siteMessagePushService;

    @Override
    public List<SysSiteMessage> getCurrentUserMessages(String username) {
        Long userId = getUserId(username);
        if (userId == null) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<SysSiteMessage> queryWrapper = new LambdaQueryWrapper<SysSiteMessage>()
                .eq(SysSiteMessage::getUserId, userId)
                .orderByAsc(SysSiteMessage::getReadStatus)
                .orderByDesc(SysSiteMessage::getCreateTime)
                .orderByDesc(SysSiteMessage::getId);

        return sysSiteMessageMapper.selectList(queryWrapper);
    }

    @Override
    public long getCurrentUserUnreadCount(String username) {
        Long userId = getUserId(username);
        if (userId == null) {
            return 0L;
        }

        return sysSiteMessageMapper.selectCount(new LambdaQueryWrapper<SysSiteMessage>()
                .eq(SysSiteMessage::getUserId, userId)
                .eq(SysSiteMessage::getReadStatus, 0));
    }

    @Override
    public int markCurrentUserMessagesRead(String username) {
        Long userId = getUserId(username);
        if (userId == null) {
            return 0;
        }

        int updatedCount = sysSiteMessageMapper.update(null, new LambdaUpdateWrapper<SysSiteMessage>()
                .eq(SysSiteMessage::getUserId, userId)
                .eq(SysSiteMessage::getReadStatus, 0)
                .set(SysSiteMessage::getReadStatus, 1)
                .set(SysSiteMessage::getReadTime, LocalDateTime.now()));
        syncUnreadCount(username);
        return updatedCount;
    }

    @Override
    public SysSiteMessage createMessage(Long userId, String title, String content, String senderName) {
        SysSiteMessage message = new SysSiteMessage();
        LocalDateTime now = LocalDateTime.now();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setSenderName(senderName);
        message.setReadStatus(0);
        message.setCreateTime(now);
        message.setUpdateTime(now);
        sysSiteMessageMapper.insert(message);
        return message;
    }

    @Override
    public void sendMessageToUser(Long userId, String title, String content, String senderName) {
        if (userId == null) {
            return;
        }
        SysSiteMessage message = createMessage(userId, title, content, senderName);
        String username = getUsername(userId);
        if (username == null) {
            return;
        }
        long unreadCount = getCurrentUserUnreadCount(username);
        siteMessagePushService.pushNewMessage(username, unreadCount, message);
    }

    @Override
    public void syncUnreadCount(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        siteMessagePushService.pushUnreadCount(username, getCurrentUserUnreadCount(username));
    }

    private Long getUserId(String username) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("limit 1"));
        return user == null ? null : user.getId();
    }

    private String getUsername(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        return user == null ? null : user.getUsername();
    }
}
