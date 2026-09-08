package com.qiao.yunpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {
    private static final long serialVersionUID = -1005112927627014475L;
    private String userAccount;
    private String userPassword;
}
