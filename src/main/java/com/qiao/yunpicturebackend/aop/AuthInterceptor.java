package com.qiao.yunpicturebackend.aop;

import com.qiao.yunpicturebackend.annotation.AuthCheck;
import com.qiao.yunpicturebackend.exception.ErrorCode;
import com.qiao.yunpicturebackend.exception.ThrowUtils;
import com.qiao.yunpicturebackend.model.entity.User;
import com.qiao.yunpicturebackend.model.enums.UserRoleEnum;
import com.qiao.yunpicturebackend.model.vo.user.UserLoginVO;
import com.qiao.yunpicturebackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
@RequiredArgsConstructor
public class AuthInterceptor {
    private final UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 获取需要的权限
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        if (mustRoleEnum == null) {
            // 放行
            return joinPoint.proceed();
        }
        // 判断用户权限
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        ThrowUtils.throwIf(userRoleEnum == null || userRoleEnum.getValue().compareTo(mustRoleEnum.getValue()) < 0, ErrorCode.NOT_AUTH_ERROR);
        return joinPoint.proceed();
    }
}
