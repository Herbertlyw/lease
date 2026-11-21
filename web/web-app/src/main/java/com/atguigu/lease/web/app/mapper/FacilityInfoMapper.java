package com.atguigu.lease.web.app.mapper;

import org.apache.ibatis.annotations.Select;
import com.atguigu.lease.model.entity.FacilityInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author liubo
* @description 针对表【facility_info(配套信息表)】的数据库操作Mapper
* @createDate 2023-07-26 11:12:39
* @Entity FacilityInfo
*/
public interface FacilityInfoMapper extends BaseMapper<FacilityInfo> {

    @Select("SELECT f.* FROM facility_info f JOIN apartment_facility a ON a.facility_id=f.id WHERE a.apartment_id=#{id} AND a.is_deleted=0 AND f.is_deleted=0")
    List<FacilityInfo> selectListByApartmentId(Long id);

    List<FacilityInfo> selectListByRoomId(Long id);
}




