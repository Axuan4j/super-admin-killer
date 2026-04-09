package com.sak.service.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileUpdateVO {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称长度不能超过64位")
    private String nickName;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128位")
    private String email;

    @Size(max = 64, message = "WxPusher UID 长度不能超过64位")
    private String wxPusherUid;

    @Size(max = 32, message = "手机号长度不能超过32位")
    private String phone;

    @Size(max = 255, message = "头像地址长度不能超过255位")
    private String avatar;
}
