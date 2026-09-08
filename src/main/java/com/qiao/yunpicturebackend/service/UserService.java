package com.qiao.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiao.yunpicturebackend.model.dto.user.UserQueryRequest;
import com.qiao.yunpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qiao.yunpicturebackend.model.vo.user.UserLoginVO;
import com.qiao.yunpicturebackend.model.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
* @author qiaoj
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2026-08-27 09:58:03
*/
public interface UserService extends IService<User> {
    Long userRegister(String userAccount, String userPassword, String checkPassword);

    UserLoginVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    User getLoginUser(HttpServletRequest request);

    UserLoginVO getSensitiveInfo(User user);

    boolean userLogout(HttpServletRequest request);

    String getEncryptPassword(String userPassword);

    UserVO getUserVO(User user);

    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    List<UserVO> getUserVOList(List<User> userList);
}
