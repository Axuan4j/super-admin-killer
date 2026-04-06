package com.sak.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizNo;
    private String logType;
    private String subType;
    private String operator;
    private String action;
    private String extra;
    private String ip;
    private String method;
    private String requestUrl;
    private String requestMethod;
    private Integer executionTime;
    private Integer success;
    private String errMsg;
    private LocalDateTime createTime;
}
