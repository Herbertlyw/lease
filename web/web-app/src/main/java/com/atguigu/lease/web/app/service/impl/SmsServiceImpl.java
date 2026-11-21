package com.atguigu.lease.web.app.service.impl;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.atguigu.lease.web.app.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@Profile("!demo")
@ConditionalOnProperty(name="aliyun.sms.endpoint")
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {
    private final Client client;
    private final Environment environment;

    public void sendCode(String phone, String code) {
        var sms = new SendSmsRequest().setPhoneNumbers(phone)
                .setSignName(environment.getRequiredProperty("aliyun.sms.sign-name"))
                .setTemplateCode(environment.getRequiredProperty("aliyun.sms.template-code"))
                .setTemplateParam("{\"code\":\"" + code + "\"}");
        try {
            if (!"OK".equals(client.sendSms(sms).getBody().getCode())) throw new IllegalStateException("短信服务拒绝请求");
        } catch (Exception e) {
            throw new IllegalStateException("短信发送失败", e);
        }
    }
}
