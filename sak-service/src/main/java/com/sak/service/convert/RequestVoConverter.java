package com.sak.service.convert;

import com.sak.service.dto.MenuSaveRequest;
import com.sak.service.dto.MfaCodeRequest;
import com.sak.service.dto.MfaLoginVerifyRequest;
import com.sak.service.dto.NotificationSendRequest;
import com.sak.service.dto.OperLogExportRequest;
import com.sak.service.dto.RoleSaveRequest;
import com.sak.service.dto.ScheduledTaskSaveRequest;
import com.sak.service.dto.UserPasswordUpdateRequest;
import com.sak.service.dto.UserProfileUpdateRequest;
import com.sak.service.dto.UserSaveRequest;
import com.sak.service.vo.MenuSaveVO;
import com.sak.service.vo.MfaCodeVO;
import com.sak.service.vo.MfaLoginVerifyVO;
import com.sak.service.vo.NotificationSendVO;
import com.sak.service.vo.OperLogExportVO;
import com.sak.service.vo.RoleSaveVO;
import com.sak.service.vo.ScheduledTaskSaveVO;
import com.sak.service.vo.UserPasswordUpdateVO;
import com.sak.service.vo.UserProfileUpdateVO;
import com.sak.service.vo.UserSaveVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RequestVoConverter {

    UserSaveRequest toUserSaveRequest(UserSaveVO source);

    RoleSaveRequest toRoleSaveRequest(RoleSaveVO source);

    MenuSaveRequest toMenuSaveRequest(MenuSaveVO source);

    MfaCodeRequest toMfaCodeRequest(MfaCodeVO source);

    MfaLoginVerifyRequest toMfaLoginVerifyRequest(MfaLoginVerifyVO source);

    NotificationSendRequest toNotificationSendRequest(NotificationSendVO source);

    ScheduledTaskSaveRequest toScheduledTaskSaveRequest(ScheduledTaskSaveVO source);

    OperLogExportRequest toOperLogExportRequest(OperLogExportVO source);

    UserProfileUpdateRequest toUserProfileUpdateRequest(UserProfileUpdateVO source);

    UserPasswordUpdateRequest toUserPasswordUpdateRequest(UserPasswordUpdateVO source);
}
