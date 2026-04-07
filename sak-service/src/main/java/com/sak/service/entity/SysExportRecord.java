package com.sak.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_export_record")
public class SysExportRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizType;
    private String fileName;
    private String queryCondition;
    private String status;
    private String filePath;
    private Long fileSize;
    private Long totalCount;
    private String operator;
    private String errMsg;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
