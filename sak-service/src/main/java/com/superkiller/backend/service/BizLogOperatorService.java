package com.superkiller.backend.service;

import com.mzt.logapi.beans.Operator;
import com.mzt.logapi.service.IOperatorGetService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class BizLogOperatorService implements IOperatorGetService {

    @Override
    public Operator getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Operator operator = new Operator();
        operator.setOperatorId(authentication == null ? "anonymous" : authentication.getName());
        return operator;
    }
}
