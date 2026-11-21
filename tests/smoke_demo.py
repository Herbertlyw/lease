"""Exercise admin_token disposable, freshly seeded Compose demo on loopback ports."""

import json
import os
import subprocess
import urllib.request as request
import uuid
from datetime import date, timedelta


def call(port, path, body=None, token=None, method=None, expected_code=200):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["access-token"] = token
    req = request.Request(
        f"http://127.0.0.1:{port}" + path,
        data=json.dumps(body).encode() if body is not None else None,
        headers=headers,
        method=method,
    )
    data = json.load(request.urlopen(req, timeout=15))
    assert data["code"] == expected_code, (path, data)
    return data.get("data")


def main():
    admin = int(os.environ.get("ADMIN_PORT", 8080))
    app = int(os.environ.get("APP_PORT", 8081))
    assert call(app, "/app/region/province/list")[0]["name"] == "浙江省"
    rooms = call(
        app, "/app/room/pageItem?current=1&size=10&orderType=desc&paymentTypeId=1"
    )
    assert rooms["total"] == 2 and rooms["records"][0]["roomNumber"] == "102", rooms
    assert call(app, "/app/room/pageItem?current=1&size=10&maxRent=2000")["total"] == 1
    call(app, "/app/room/getDetailById?id=3", expected_code=404)
    detail = call(app, "/app/room/getDetailById?id=1")
    assert (
        detail["apartmentItemVo"]["name"] == "青禾公寓"
        and detail["labelInfoList"][0]["name"] == "朝南"
    )
    assert call(app, "/app/apartment/getDetailById?id=1")["minRent"] == 1800
    print("Catalog, release filters, details and relations passed", flush=True)
    captcha = call(admin, "/admin/login/captcha")
    assert captcha["image"].startswith("data:image")

    def fixture_value(key):
        return subprocess.check_output(
            [
                "docker",
                "compose",
                "exec",
                "-T",
                "redis",
                "redis-cli",
                "--raw",
                "GET",
                key,
            ],
            text=True,
        ).strip()

    admin_token = call(
        admin,
        "/admin/login",
        {
            "username": "demo",
            "password": "Lease-Demo-2026",
            "captchaKey": captcha["key"],
            "captchaCode": fixture_value(captcha["key"]),
        },
    )
    assert call(admin, "/admin/info", token=admin_token)["name"] == "演示管理员"
    assert (
        call(
            admin,
            "/admin/room/pageItem?current=1&size=10&provinceId=1&apartmentId=1",
            token=admin_token,
        )["total"]
        >= 3
    )
    call(
        admin,
        "/admin/room/updateReleaseStatusById?id=2&status=0",
        token=admin_token,
        method="POST",
    )
    call(app, "/app/room/getDetailById?id=2", expected_code=404)
    call(
        admin,
        "/admin/room/updateReleaseStatusById?id=2&status=1",
        token=admin_token,
        method="POST",
    )
    assert call(app, "/app/room/getDetailById?id=2")["roomNumber"] == "102"

    def tenant(phone):
        call(app, "/app/login/getCode?phone=" + phone)
        code = fixture_value("lease:tenant:code:" + phone)
        return call(app, "/app/login", {"phone": phone, "code": code})

    phone = "199" + str(uuid.uuid4().int)[-8:]
    tenant_token = tenant(phone)
    other_token = tenant("199" + str(uuid.uuid4().int)[-8:])
    assert call(app, "/app/info", token=tenant_token)["nickname"]
    call(app, "/app/room/getDetailById?id=1", token=tenant_token)
    row = call(app, "/app/history/pageItem?current=1&size=2", token=tenant_token)[
        "records"
    ][0]
    assert row["roomId"] == 1 and row["userId"]
    visit = call(
        app,
        "/app/appointment/saveOrUpdate",
        {
            "apartmentId": 1,
            "name": "示例租客",
            "phone": phone,
            "appointmentTime": "2099-01-10 14:00:00",
        },
        tenant_token,
    )
    assert call(app, "/app/appointment/listItem", token=tenant_token)[0]["id"] == visit
    call(
        app,
        f"/app/appointment/getDetailById?id={visit}",
        token=other_token,
        expected_code=404,
    )
    call(
        admin,
        f"/admin/appointment/updateStatusById?id={visit}&status=3",
        token=admin_token,
        method="POST",
    )
    assert (
        call(app, f"/app/appointment/getDetailById?id={visit}", token=tenant_token)[
            "appointmentStatus"
        ]
        == 3
    )
    print(
        "Login, history, cross-account isolation, appointment and admin confirmation passed",
        flush=True,
    )
    agreement = {
        "phone": phone,
        "name": "示例租客",
        "apartmentId": 1,
        "roomId": 1,
        "leaseStartDate": date.today().isoformat(),
        "leaseEndDate": (date.today() + timedelta(days=365)).isoformat(),
        "leaseTermId": 1,
        "paymentTypeId": 1,
        "rent": 1800,
        "deposit": 1800,
        "status": 1,
        "sourceType": 1,
    }
    call(admin, "/admin/agreement/saveOrUpdate", agreement, admin_token)
    contract = call(app, "/app/agreement/listItem", token=tenant_token)[0]["id"]
    assert (
        call(app, f"/app/agreement/getDetailById?id={contract}", token=tenant_token)[
            "rent"
        ]
        == 1800
    )
    call(
        app,
        f"/app/agreement/getDetailById?id={contract}",
        token=other_token,
        expected_code=404,
    )
    call(
        app,
        f"/app/agreement/updateStatusById?id={contract}&leaseStatus=2",
        token=tenant_token,
        method="POST",
    )
    call(app, "/app/agreement/saveOrUpdate", {"id": contract}, tenant_token)
    assert (
        call(admin, f"/admin/agreement/getById?id={contract}", token=admin_token)[
            "status"
        ]
        == 7
    )
    print(
        "Admin contract, tenant confirmation, renewal request and admin reread passed",
        flush=True,
    )


if __name__ == "__main__":
    main()
