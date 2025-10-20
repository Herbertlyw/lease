package com.atguigu.lease.web.app.service;

import com.aliyun.dysmsapi20170525.Client;
import org.springframework.beans.factory.annotation.Autowired;

public interface SmsService {


    void sendCode(String phone, String code);
}
