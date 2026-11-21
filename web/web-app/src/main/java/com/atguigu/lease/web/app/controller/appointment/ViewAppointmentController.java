package com.atguigu.lease.web.app.controller.appointment;

import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.model.entity.ViewAppointment;
import com.atguigu.lease.model.enums.AppointmentStatus;
import com.atguigu.lease.web.app.service.*;
import com.atguigu.lease.web.app.vo.appointment.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/app/appointment")
@RequiredArgsConstructor
public class ViewAppointmentController {
    private final LoginService login;
    private final ViewAppointmentService visits;
    private final ApartmentInfoService apartments;

    @PostMapping("/saveOrUpdate")
    public Result<Long> saveOrUpdate(@RequestHeader(value="access-token", required=false) String token,
                                     @RequestBody ViewAppointment form) {
        Long owner = login.requireUser(token).getId();
        if (form.getId() != null && form.getAppointmentStatus() == AppointmentStatus.CANCELED) {
            boolean changed = visits.lambdaUpdate().eq(ViewAppointment::getId, form.getId())
                    .eq(ViewAppointment::getUserId, owner).eq(ViewAppointment::getAppointmentStatus, AppointmentStatus.WAITING)
                    .set(ViewAppointment::getAppointmentStatus, AppointmentStatus.CANCELED).update();
            return changed ? Result.ok(form.getId()) : Result.fail(409, "仅可取消自己的待看房预约");
        }
        if (form.getApartmentId() == null || apartments.selectApartmentItemVoById(form.getApartmentId()) == null
                || form.getName() == null || form.getName().isBlank() || form.getName().length() > 80
                || form.getPhone() == null || !form.getPhone().matches("1[0-9]{10}")
                || form.getAppointmentTime() == null || !form.getAppointmentTime().after(new Date())) {
            return Result.fail(202, "请填写已发布公寓、姓名、手机号和未来的看房时间");
        }
        if (form.getId() == null) {
            var visit = new ViewAppointment();
            visit.setUserId(owner);
            visit.setName(form.getName());
            visit.setPhone(form.getPhone());
            visit.setApartmentId(form.getApartmentId());
            visit.setAppointmentTime(form.getAppointmentTime());
            visit.setAdditionalInfo(form.getAdditionalInfo());
            visit.setAppointmentStatus(AppointmentStatus.WAITING);
            visits.save(visit);
            return Result.ok(visit.getId());
        }
        boolean changed = visits.lambdaUpdate().eq(ViewAppointment::getId, form.getId())
                .eq(ViewAppointment::getUserId, owner).eq(ViewAppointment::getAppointmentStatus, AppointmentStatus.WAITING)
                .set(ViewAppointment::getName, form.getName()).set(ViewAppointment::getPhone, form.getPhone())
                .set(ViewAppointment::getApartmentId, form.getApartmentId()).set(ViewAppointment::getAppointmentTime, form.getAppointmentTime())
                .set(ViewAppointment::getAdditionalInfo, form.getAdditionalInfo()).update();
        return changed ? Result.ok(form.getId()) : Result.fail(409, "预约已结束或不存在");
    }

    @GetMapping("/listItem")
    public Result<List<AppointmentItemVo>> list(@RequestHeader(value="access-token", required=false) String token) {
        var records = visits.lambdaQuery().eq(ViewAppointment::getUserId, login.requireUser(token).getId())
                .orderByDesc(ViewAppointment::getId).list();
        return Result.ok(records.stream().map(visit -> {
            var item = new AppointmentItemVo();
            BeanUtils.copyProperties(visit, item);
            var apartment = apartments.selectApartmentItemVoById(visit.getApartmentId());
            if (apartment != null) {
                item.setApartmentName(apartment.getName());
                item.setGraphVoList(apartment.getGraphVoList());
            }
            return item;
        }).toList());
    }

    @GetMapping("/getDetailById")
    public Result<AppointmentDetailVo> detail(@RequestHeader(value="access-token", required=false) String token,
                                             @RequestParam Long id) {
        var visit = visits.lambdaQuery().eq(ViewAppointment::getId, id)
                .eq(ViewAppointment::getUserId, login.requireUser(token).getId()).one();
        if (visit == null) return Result.fail(404, "预约不存在");
        var detail = new AppointmentDetailVo();
        BeanUtils.copyProperties(visit, detail);
        detail.setApartmentItemVo(apartments.selectApartmentItemVoById(visit.getApartmentId()));
        return Result.ok(detail);
    }
}
