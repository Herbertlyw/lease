package com.atguigu.lease.web.app.controller.room;


import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.model.entity.BrowsingHistory;
import com.atguigu.lease.web.app.service.LoginService;
import com.atguigu.lease.web.app.service.BrowsingHistoryService;
import org.springframework.web.bind.annotation.RequestHeader;
import lombok.RequiredArgsConstructor;
import com.atguigu.lease.web.app.service.RoomInfoService;
import com.atguigu.lease.web.app.vo.room.RoomDetailVo;
import com.atguigu.lease.web.app.vo.room.RoomItemVo;
import com.atguigu.lease.web.app.vo.room.RoomQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "房间信息")
@RestController
@RequestMapping("/app/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomInfoService service;
    private final LoginService login;
    private final BrowsingHistoryService history;

    @Operation(summary = "分页查询房间列表")
    @GetMapping("pageItem")
    public Result<IPage<RoomItemVo>> pageItem(@RequestParam long current, @RequestParam long size, RoomQueryVo queryVo) {
        return Result.ok(service.pageItems(current, size, queryVo, null));
    }

    @Operation(summary = "根据id获取房间的详细信息")
    @GetMapping("getDetailById")
    public Result<RoomDetailVo> getDetailById(@RequestParam Long id, @RequestHeader(value="access-token", required=false) String token) {
        RoomDetailVo roomInfo = service.getDetailById(id);
        if (roomInfo == null) return Result.fail(404, "房源不存在");
        if (token != null) {
            BrowsingHistory visit = new BrowsingHistory();
            visit.setUserId(login.requireUser(token).getId());
            visit.setRoomId(id);
            visit.setBrowseTime(new java.util.Date());
            history.save(visit);
        }
        return Result.ok(roomInfo);
    }
    @Operation(summary = "根据公寓id分页查询房间列表")
    @GetMapping("pageItemByApartmentId")
    public Result<IPage<RoomItemVo>> pageItemByApartmentId(@RequestParam long current, @RequestParam long size, @RequestParam Long id) {
        return Result.ok(service.pageItems(current, size, new RoomQueryVo(), id));
    }
}
