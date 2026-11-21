package com.atguigu.lease.web.app.controller.apartment;

import com.atguigu.lease.web.app.mapper.FacilityInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.web.app.service.ApartmentInfoService;
import com.atguigu.lease.web.app.vo.apartment.ApartmentDetailVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "公寓信息")
@RequestMapping("/app/apartment")
@RequiredArgsConstructor
public class ApartmentController {
    private final ApartmentInfoService apartments;
    private final FacilityInfoMapper facilities;


    @Operation(summary = "根据id获取公寓信息")
    @GetMapping("getDetailById")
    public Result<ApartmentDetailVo> getDetailById(@RequestParam Long id) {
        var item = apartments.selectApartmentItemVoById(id);
        if (item == null) return Result.fail(404, "公寓不存在");
        var detail = new ApartmentDetailVo();
        BeanUtils.copyProperties(item, detail);
        detail.setFacilityInfoList(facilities.selectListByApartmentId(id));
        return Result.ok(detail);
    }
}
