package com.atguigu.lease.web.app.controller.region;


import com.atguigu.lease.web.app.mapper.CityInfoMapper;
import com.atguigu.lease.web.app.mapper.DistrictInfoMapper;
import com.atguigu.lease.web.app.mapper.ProvinceInfoMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.model.entity.CityInfo;
import com.atguigu.lease.model.entity.DistrictInfo;
import com.atguigu.lease.model.entity.ProvinceInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "地区信息")
@RestController
@RequestMapping("/app/region")
@RequiredArgsConstructor
public class RegionController {
    private final ProvinceInfoMapper provinceInfo;
    private final CityInfoMapper cityInfo;
    private final DistrictInfoMapper districtInfo;


    @Operation(summary = "查询省份信息列表")
    @GetMapping("province/list")
    public Result<List<ProvinceInfo>> listProvince() {
        return Result.ok(provinceInfo.selectList(null));
    }

    @Operation(summary = "根据省份id查询城市信息列表")
    @GetMapping("city/listByProvinceId")
    public Result<List<CityInfo>> listCityInfoByProvinceId(@RequestParam Long id) {
        return Result.ok(cityInfo.selectList(Wrappers.<CityInfo>lambdaQuery().eq(CityInfo::getProvinceId, id)));
    }

    @GetMapping("district/listByCityId")
    @Operation(summary = "根据城市id查询区县信息")
    public Result<List<DistrictInfo>> listDistrictInfoByCityId(@RequestParam Long id) {
        return Result.ok(districtInfo.selectList(Wrappers.<DistrictInfo>lambdaQuery().eq(DistrictInfo::getCityId, id)));
    }
}
