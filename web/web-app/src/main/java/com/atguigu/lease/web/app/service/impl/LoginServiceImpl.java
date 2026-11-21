package com.atguigu.lease.web.app.service.impl;

import lombok.RequiredArgsConstructor;
import com.atguigu.lease.web.app.service.SmsService;
import com.atguigu.lease.common.exception.LeaseException;
import com.atguigu.lease.common.result.ResultCodeEnum;
import com.atguigu.lease.model.entity.UserInfo;
import com.atguigu.lease.model.enums.BaseStatus;
import com.atguigu.lease.web.app.mapper.UserInfoMapper;
import com.atguigu.lease.web.app.service.LoginService;
import com.atguigu.lease.web.app.vo.user.LoginVo;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {
    private static final SecureRandom random = new SecureRandom();
    private static final String CODE = "lease:tenant:code:";
    private static final String SESSION = "lease:tenant:session:";
    private static final DefaultRedisScript<Long> REMOVE_IF_MATCH = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL',KEYS[1]) end return 0", Long.class);
    private final StringRedisTemplate redis;
    private final UserInfoMapper users;
    private final ObjectProvider<SmsService> senders;

    public void sendCode(String phone) {
        checkPhone(phone);
        SmsService sender = senders.getIfAvailable();
        if (sender == null) throw new IllegalStateException("请配置短信服务，或在本机启用 demo profile");
        String code = String.format("%06d", random.nextInt(1_000_000));
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(CODE + phone, code, Duration.ofMinutes(2)))) {
            throw new LeaseException(ResultCodeEnum.APP_SEND_SMS_TOO_OFTEN);
        }
        try {
            sender.sendCode(phone, code);
        } catch (Exception e) {
            redis.execute(REMOVE_IF_MATCH, List.of(CODE + phone), code);
            throw new IllegalStateException("短信发送失败");
        }
    }

    private static void checkPhone(String phone) {
        if (phone == null || !phone.matches("1[0-9]{10}")) throw new LeaseException(ResultCodeEnum.PARAM_ERROR);
    }

    public String login(LoginVo login) {
        String phone = login.getPhone();
        checkPhone(phone);
        String expected = redis.opsForValue().getAndDelete(CODE + phone);
        if (expected == null || !expected.equals(login.getCode())) throw new LeaseException(ResultCodeEnum.APP_LOGIN_CODE_ERROR);
        var query = Wrappers.<UserInfo>lambdaQuery().eq(UserInfo::getPhone, phone);
        UserInfo user = users.selectOne(query);
        if (user == null) {
            user = new UserInfo();
            user.setPhone(phone);
            user.setNickname("租客" + phone.substring(7));
            user.setStatus(BaseStatus.ENABLE);
            try { users.insert(user); }
            catch (DuplicateKeyException e) { user = users.selectOne(query); }
        }
        if (user == null || user.getStatus() != BaseStatus.ENABLE) throw new LeaseException(ResultCodeEnum.APP_ACCOUNT_DISABLED_ERROR);
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(SESSION + token, user.getId().toString(), Duration.ofHours(8));
        return token;
    }

    public UserInfo requireUser(String token) {
        String id = token == null ? null : redis.opsForValue().get(SESSION + token);
        UserInfo user = id == null ? null : users.selectById(Long.parseLong(id));
        if (user == null || user.getStatus() != BaseStatus.ENABLE) throw new LeaseException(ResultCodeEnum.APP_LOGIN_AUTH);
        return user;
    }
}
