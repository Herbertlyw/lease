package com.atguigu.lease.web.app.controller.history;

import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.web.app.mapper.BrowsingHistoryMapper;
import com.atguigu.lease.web.app.service.LoginService;
import com.atguigu.lease.web.app.vo.history.HistoryItemVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/app/history")
@RequiredArgsConstructor
public class BrowsingHistoryController {
    private final LoginService login;
    private final BrowsingHistoryMapper history;

    @GetMapping("/pageItem")
    public Result<IPage<HistoryItemVo>> page(@RequestHeader(value="access-token", required=false) String token,
                                            @RequestParam long current, @RequestParam long size) {
        Long userId = login.requireUser(token).getId();
        if (current < 1 || size < 1 || size > 100) return Result.fail(202, "分页参数无效");
        return Result.ok(history.forUser(new Page<>(current, size), userId));
    }
}
