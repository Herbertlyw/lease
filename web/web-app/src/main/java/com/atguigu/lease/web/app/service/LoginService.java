package com.atguigu.lease.web.app.service;

import com.atguigu.lease.model.entity.UserInfo;
import com.atguigu.lease.web.app.vo.user.LoginVo;

public interface LoginService {
    void sendCode(String phone);
    String login(LoginVo login);
    UserInfo requireUser(String token);
}
