package com.atguigu.lease.common.exception;

import com.atguigu.lease.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

//    @ExceptionHandler(Exception.class)
//    public Result handle(Exception e) {
//        e.getMessage();
//        e.printStackTrace();
//        return Result.fail();
//    }

    @ExceptionHandler(LeaseException.class)
    public Result handle(LeaseException e) {
        e.printStackTrace();
        log.error("捕获到 LeaseException: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(),e.getMessage());
    }

}
