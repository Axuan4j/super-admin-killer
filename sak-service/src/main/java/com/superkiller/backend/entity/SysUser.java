package com.superkiller.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickName;
    private String email;
    private String phone;
    private String sex;
    private String avatar;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String remark;
}
