package com.sak.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_file_record")
public class SysFileRecord implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String bizType;
    private String fileName;
    private String storageName;
    private String filePath;
    private String contentType;
    private String fileExt;
    private Long fileSize;
    private String operator;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
