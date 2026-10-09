# 医院双端前端

Vue 3 + TypeScript + Vue Router + Vite。一个源码工程分别构建患者包和医务包，各端只注册本端业务路由；页面通过同源 `/api/**` 调用后端，没有嵌入旧模板。

## 两端入口

| 平台 | 正式地址 | 可登录角色 |
| --- | --- | --- |
| 明禾医院 | http://localhost:8082/app | PATIENT |
| 医院医务管理平台 | http://localhost:8083/app | DOCTOR、ADMIN |

患者端 `/app` 是明禾医院介绍首页，登录前后均展示建筑形象、医院概况、科室介绍与诊疗关怀；联系方式配置后才展示。公开预约查找位于 `/app/booking`；个人就医概览位于 `/app/patient/dashboard`，展示真实的下一次门诊、待缴费和近期已完成报告提醒。患者页面共用顶部导航，导航提供医院介绍与就医服务入口。医务端未登录时 `/app` 直接显示账号登录表单；已登录时访问 `/app` 或 `/app/login`，按真实角色直接进入 `/app/doctor/dashboard` 或 `/app/admin/dashboard`。患者注册在患者端，医生账号申请在医务端；管理员不提供自助注册。

预约、科室与医生目录和全部 `/app/patient/**` 业务页面嵌入 `PatientServiceLayout.vue`，按门诊服务、住院服务、就医记录三组提供八项常驻入口。电脑端使用左侧导航，手机端使用顶部网格，进入详情后仍可直接切换服务；账号菜单仅保留个人资料和退出登录。私人记录登录后查看，登录成功返回原目标页面。旧 `/app/workspace` 地址兼容跳转到 `/app/patient/dashboard`。支付完成后可返回对应的处方、检查报告或住院详情，继续后续业务。

医院名称、简介与联系方式集中在 `src/lib/hospital.ts`，可按医院实际信息修改。

## 开发

先在项目根目录启动对应后端：

```powershell
# 两个终端分别执行
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=patient"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=staff"
```

再运行前端开发服务器：

```powershell
cd frontend
npm ci
npm run dev:patient
# 另一个终端
npm run dev:staff
```

患者开发地址为 http://127.0.0.1:5173/app，代理到 8082；医务开发地址为 http://127.0.0.1:5174/app，代理到 8083。

## 构建与启动

```powershell
# 项目根目录，一次构建两端前端和后端 JAR
.\mvnw.cmd -Pfrontend -DskipTests package

# 后台先启动医务端，再启动患者端
.\Start-HospitalPortals.ps1

# 停止本项目两端，重新构建并启动
.\Start-HospitalPortals.ps1 -Build

# 停止两端
.\Stop-HospitalPortals.ps1
```

启动脚本只会停止端口上命令行匹配当前 JAR 路径的程序；其他项目占用端口时会停止操作并报告。日志在 `target/portal-logs/`。

也可以在两个终端手动运行：

```powershell
java -jar target/YIYUAN-0.0.1-SNAPSHOT.jar --spring.profiles.active=staff
java -jar target/YIYUAN-0.0.1-SNAPSHOT.jar --spring.profiles.active=patient
```

前端 `npm run build` 顺序构建两包；`build:patient` 和 `build:staff` 可分别构建。生成资源为 `src/main/resources/static/spa/patient/` 和 `spa/staff/`，由 Maven 打入同一 JAR，无需提交生成文件。刷新 `/app/**` 时由对应端返回自己的 SPA 入口。

需 Node.js 22.12+（或满足 Vite 8 要求的版本）、Java 17+、MySQL。原库和账号继续沿用；新环境请先从项目根目录的 MySQL 客户端执行 `SOURCE database/init.sql;`，再启动医务端和患者端。数据库迁移说明见 [docs/DATABASE.md](../docs/DATABASE.md)。

## 会话与权限

- 后端配置 `hospital.portal.mode=patient|staff` 决定可用接口、页面和静态包。
- 患者端拒绝医生/管理员登录，医务端拒绝患者登录；拒绝登录不会替换原会话。
- 两端共用业务代码与数据库，分别持有独立的内存 Session。
- Cookie 分别为 `PATIENT_SESSION` / `STAFF_SESSION`，CSRF Cookie 为 `PATIENT_XSRF_TOKEN` / `STAFF_XSRF_TOKEN`。
- 每端 POST 发送自己的 `X-XSRF-TOKEN`，前端不跨端口调用 API。
- 旧 `POST /login` 已停用，登录统一使用 JSON API，避免绕过端级角色检查。
- 医务端承担初始化任务，患者端关闭重复 DDL、管理员初始化和空表序列维护。
- 端口分离与 Cookie 命名用于本地双端会话；正式部署可使用两个域名、HTTPS 和相同的后端权限边界。

## 源码结构

- `src/router/portals/patient.ts` / `staff.ts`：分端路由，Vite 按模式选择，避免把另一端页面打入当前包。
- `src/lib/portal.ts`：编译时端类型、名称和账号角色检查。
- `src/lib/hospital.ts`：医院名称、简介与联系信息。
- `src/views/public` / `layouts/PublicLayout.vue`：患者门户及公开目录。
- `src/layouts/PatientServiceLayout.vue` / `src/lib/patient-services.ts`：患者服务区布局、分组导航与当前业务高亮。
- `src/views/staff` / `layouts/StaffLayout.vue`：医务门户、统一登录和医生申请。
- `src/views/patient`、`doctor`、`admin`：已有领域工作台与业务流程。
- `src/components/LoginForm.vue`：共用账号密码表单，不提供角色切换。
- `src/lib/api.ts` / `stores/auth.ts`：JSON、CSRF、会话恢复和过期反馈。

支付仍为原项目模拟支付，不会真实扣款。AI 草稿需医生审阅，未接入真实 HIS 或支付服务。

患者首页建筑资源为 `src/main/resources/static/images/hospital-outpatient.png`，使用内置 image_gen 生成；提示为现代综合医院门诊楼、白石材与玻璃、蓝色医疗十字、安静绿化前庭与自然日光，无院名、文字和水印。
