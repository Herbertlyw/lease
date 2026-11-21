package com.atguigu.lease.web.app.controller.agreement;

import com.atguigu.lease.common.result.Result;
import com.atguigu.lease.model.entity.LeaseAgreement;
import com.atguigu.lease.model.enums.LeaseStatus;
import com.atguigu.lease.web.app.mapper.LeaseAgreementMapper;
import com.atguigu.lease.web.app.service.LeaseAgreementService;
import com.atguigu.lease.web.app.service.LoginService;
import com.atguigu.lease.web.app.vo.agreement.AgreementDetailVo;
import com.atguigu.lease.web.app.vo.agreement.AgreementItemVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/app/agreement")
@RequiredArgsConstructor
public class LeaseAgreementController {
    private final LoginService login;
    private final LeaseAgreementService agreements;
    private final LeaseAgreementMapper mapper;

    @GetMapping("/listItem")
    public Result<List<AgreementItemVo>> list(@RequestHeader(value="access-token", required=false) String token) {
        return Result.ok(mapper.listForTenant(login.requireUser(token).getPhone()));
    }

    @GetMapping("/getDetailById")
    public Result<AgreementDetailVo> detail(@RequestHeader(value="access-token", required=false) String token,
                                            @RequestParam Long id) {
        var detail = mapper.detailForTenant(id, login.requireUser(token).getPhone());
        return detail == null ? Result.fail(404, "租约不存在") : Result.ok(detail);
    }

    @PostMapping("/updateStatusById")
    public Result<Void> update(@RequestHeader(value="access-token", required=false) String token,
                               @RequestParam Long id, @RequestParam int leaseStatus) {
        String phone = login.requireUser(token).getPhone();
        return switch (leaseStatus) {
            case 2 -> transition(id, phone, LeaseStatus.SIGNING, LeaseStatus.SIGNED);
            case 3 -> transition(id, phone, LeaseStatus.SIGNING, LeaseStatus.CANCELED);
            case 5 -> transition(id, phone, LeaseStatus.SIGNED, LeaseStatus.WITHDRAWING);
            default -> Result.fail(202, "不支持的租客操作");
        };
    }

    @PostMapping("/saveOrUpdate")
    public Result<Void> renew(@RequestHeader(value="access-token", required=false) String token,
                              @RequestBody LeaseAgreement request) {
        String phone = login.requireUser(token).getPhone();
        if (request.getId() == null) return Result.fail(202, "请选择要续租的原租约");
        return transition(request.getId(), phone, LeaseStatus.SIGNED, LeaseStatus.RENEWING);
    }

    private Result<Void> transition(Long id, String phone, LeaseStatus current, LeaseStatus target) {
        boolean updated = agreements.lambdaUpdate().eq(LeaseAgreement::getId, id).eq(LeaseAgreement::getPhone, phone)
                .eq(LeaseAgreement::getStatus, current).set(LeaseAgreement::getStatus, target).update();
        return updated ? Result.ok() : Result.fail(409, "租约不存在或状态已变化");
    }
}
