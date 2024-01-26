package com.sjy.gulimall.authServer.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserLoginVo {
    private String loginacct;    //登录账号名
    private String password;
}
 