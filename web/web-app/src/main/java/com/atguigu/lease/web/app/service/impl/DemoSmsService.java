package com.atguigu.lease.web.app.service.impl;

import com.atguigu.lease.web.app.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("demo")
@Slf4j
public class DemoSmsService implements SmsService {
    public void sendCode(String phone, String code) {
        log.info("本机演示验证码（尾号 {}）：{}", phone.substring(7), code);
    }
}
