# 公寓租赁服务

两个 Spring Boot 应用共用一套房源和租约数据。租客可以按地区、租金和付款方式找房，查看配套，预约看房并确认租约。管理员负责发布房源、处理预约和管理合同。两个服务都自带 Knife4j 接口页面。

## 启动本地演示

需要 JDK 17、Maven 3.8+、Docker 和带 `--wait` 的 Docker Compose v2。在仓库根目录执行：

```sh
mvn clean verify
docker compose up -d --wait
```

Compose 会启动 MySQL 8 和 Redis 6.2，在新数据卷中执行 `db/01-schema.sql` 和 `db/02-demo.sql`。种子数据包含杭州的「青禾公寓」、两间已发布的房间、一间草稿房和一个演示管理员。已有卷会沿用原数据；日常停止服务使用 `docker compose stop`。

如果 Docker Desktop 的虚拟磁盘已满，而宿主机仍有空间，可在首次初始化时将 MySQL 放在宿主机目录。请从仓库根目录运行，示例使用新的空目录：

```sh
mkdir -p .local/mysql
LEASE_MYSQL_DATA="$(pwd)/.local/mysql" docker compose up -d --wait
```

复用该数据时继续设置相同的 `LEASE_MYSQL_DATA`。在两个终端分别启动后台和租客端：

```sh
java -jar web/web-admin/target/web-admin-1.0-SNAPSHOT.jar
```

```sh
java -jar web/web-app/target/web-app-1.0-SNAPSHOT.jar --spring.profiles.active=demo
```

打开[后台接口文档](http://127.0.0.1:8080/doc.html)或[租客端接口文档](http://127.0.0.1:8081/doc.html)。端口被占用时可在对应的 JAR 命令末尾加 `--server.port=新端口`。

## 从找房到签约

1. **查房源。** 访问 `GET /app/room/pageItem?current=1&size=10&maxRent=2000`，初始数据应返回 101 室，月租 1800 元。`GET /app/room/getDetailById?id=1` 会返回公寓、标签、租期和付款方式。下架房源对租客不可见。
2. **租客登录。** 请求 `GET /app/login/getCode?phone=13900000001`。`demo` 配置将一次性验证码写到租客端终端，有效期两分钟。向 `POST /app/login` 提交 `{"phone":"13900000001","code":"终端中的六位数"}`。后续请求将返回的 `data` 放入 `access-token` 头。带令牌查看房间详情后，可在 `/app/history/pageItem?current=1&size=10` 查询记录。
3. **预约。** 向 `POST /app/appointment/saveOrUpdate` 提交下方 JSON，将时间换成未来的看房时间。返回值为预约 ID，个人列表在 `/app/appointment/listItem`。预约待处理时可修改，也可提交 `{"id":预约ID,"appointmentStatus":2}` 取消。
4. **后台确认。** 在后台请求 `GET /admin/login/captcha`，打开返回的 `image` 数据地址读取验证码。向 `POST /admin/login` 提交 `username`、`password`、`captchaKey`、`captchaCode`。初始演示账号为 `demo`，密码为 `Lease-Demo-2026`。后台使用自己的 `access-token`，从 `/admin/appointment/page?current=1&size=10` 找到预约后，调用 `POST /admin/appointment/updateStatusById?id=预约ID&status=3` 标为已看房。租客刷新详情即可看到新状态。
5. **签约。** 后台通过 `POST /admin/agreement/saveOrUpdate` 建立待确认租约，租客手机号应与登录号相同。租客从 `/app/agreement/listItem` 取得 ID，以 `POST /app/agreement/updateStatusById?id=租约ID&leaseStatus=2` 确认签约，随后可向 `POST /app/agreement/saveOrUpdate` 提交原租约 ID 申请续约。管理员可在租约详情看到状态变化。

预约请求示例：

```json
{"apartmentId":1,"name":"演示租客","phone":"13900000001","appointmentTime":"2099-01-10 14:00:00"}
```

待签约合同请求示例：

```json
{"phone":"13900000001","name":"演示租客","apartmentId":1,"roomId":1,"leaseTermId":1,"paymentTypeId":1,"rent":1800,"deposit":1800,"leaseStartDate":"2027-01-01","leaseEndDate":"2027-12-31","status":1,"sourceType":1}
```

接口沿用 `{code,message,data}` 返回格式，成功业务码为 200。个人预约、浏览记录和租约均按当前租客身份查询。

## 连接与可选服务

| 配置 | 用途 |
| --- | --- |
| `LEASE_DB_HOST`、`LEASE_DB_PORT`、`LEASE_DB_USER`、`LEASE_DB_PASSWORD` | Java 连接 MySQL，默认地址为 `127.0.0.1:13316`，库名为 `lease` |
| `LEASE_REDIS_HOST`、`LEASE_REDIS_PORT` | 租客会话和验证码使用 Redis，默认 `127.0.0.1:16389` |
| `LEASE_JWT_KEY` | 后台 JWT 签名，Base64 编码的 32 字节密钥。省略时会在启动时生成，重启后需重新登录 |
| `minio.endpoint`、`minio.access-key`、`minio.secret-key`、`minio.bucket-name` | 后台的可选图片上传服务，填写后开放上传入口 |
| `aliyun.sms.endpoint`、`access-key-id`、`access-key-secret`、`sign-name`、`template-code` | 真实短信配置，均使用 `aliyun.sms.` 前缀。需使用已开通的账号、签名、模板，并关闭 `demo` profile |

默认服务绑定本机，固定演示凭据仅用于本地数据。启用真实短信和对象存储需自行配置账号。公开的旧版 JWT 签名值已替换，现有部署升级后需重新登录。

## 模块与验证

`model` 对应 28 张表和业务枚举；`common` 集中管理返回值、持久层和可选基础设施；`web/web-admin` 与 `web/web-app` 分别承载管理员和租客 API。房源详情直接从数据库读取，后台下架后立即生效。验证码单次消费，租客的预约和租约更新均校验归属及原状态。

`mvn clean verify` 使用独立的内存数据库验证到期租约的状态更新，不需要启动演示库。端到端验收可按上面的五步使用 MySQL、Redis 和两个实际服务执行。

GitHub Actions 会在全新的 Compose 数据库上执行 `tests/smoke_demo.py`，覆盖上下架、预约、后台确认、签约、续约和跨账号隔离。该脚本会创建测试账号、预约和租约，适合运行在专用演示库。
