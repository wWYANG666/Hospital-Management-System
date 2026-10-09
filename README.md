# 医院管理系统

![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen?logo=springboot)
![Vue](https://img.shields.io/badge/Vue-3-42b883?logo=vuedotjs)
![TypeScript](https://img.shields.io/badge/TypeScript-5.x-3178c6?logo=typescript)

面向门诊与住院业务的全栈医院管理系统示例，包含患者门户、医务工作台和管理员后台。

如果这个项目对你的学习、毕业设计或全栈开发实践有帮助，欢迎点亮 **Star**、提交 Issue 或分享给更多开发者。

## 项目介绍

![医院门户使用的生成插画](src/main/resources/static/images/hospital-outpatient.png)

患者端与医务端分别运行在独立端口，共用后端业务代码和数据库。上图是门户使用的生成插画。支付流程为模拟实现，未连接真实支付服务。本分享版包含整理后的源码和建表脚本，不包含真实用户、病历、数据库备份或本地开发历史；未配置 AI Key 时使用本地规则。

## 功能

### 患者端

- 公开医院首页、科室与医生目录
- 在线预约挂号与预约记录
- 报告、处方、缴费和住院信息
- 患者资料与就医记录

### 医务端

- 医生登录、诊疗记录与患者队列
- 住院申请、排班和病床管理
- 检查、处方、药品与药房业务
- AI 导诊建议与本地规则回退

### 管理员端

- 医生、患者和账号管理
- 科室、排班、病床和药品维护
- 权限控制、审计日志与业务数据管理

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot、Spring Security、Spring JDBC |
| 前端 | Vue 3、TypeScript、Vue Router、Vite |
| 数据库 | MySQL 8.0+ |
| 页面兼容 | Thymeleaf 旧模板与 Vue SPA |
| 构建 | Maven Wrapper、npm |

## 项目结构

```text
src/main/java/                  Spring Boot 后端
src/main/resources/             模板、静态资源和应用配置
frontend/src/                   Vue 3 + TypeScript 前端
frontend/package.json           前端脚本与依赖
src/test/                       后端测试
config/                         外置本地配置的脱敏模板
database/init.sql               统一建表入口
database/migrations/            旧数据库的可选迁移
docs/                           API、数据库和发布说明
scripts/check-public-release.mjs 发布内容检查
Start-HospitalPortals.ps1       Windows 双端启动脚本
Stop-HospitalPortals.ps1        Windows 双端停止脚本
```

## 快速开始

### 环境要求

- Java 17+
- Node.js 22.12+（Vite 8）
- MySQL 8.0+（本分享版以 MySQL 8 为验证目标）
- Git

### 1. 克隆项目

```bash
git clone https://github.com/wWYANG666/Hospital-Management-System.git
cd Hospital-Management-System
```

### 2. 创建数据库

在项目根目录打开 MySQL 客户端（`-p` 会提示输入密码）：

```bash
mysql --default-character-set=utf8mb4 -u root -p
```

在 MySQL 提示符执行：

```sql
SOURCE database/init.sql;
```

脚本创建 `yiyuan` 数据库与基础业务表，不导入用户或病历。先启动医务端，启动时会执行其余可重复的业务字段迁移。可选执行 `SOURCE database/seed-department-items.sql;` 添加演示科室、药品和检查项目，也可登录后通过管理员页面自行维护。已有数据库迁移见 [数据库说明](docs/DATABASE.md)。

### 3. 配置环境变量

仓库提供脱敏模板 `config/application-local.example.properties`。可以复制为被 Git 忽略的 `config/application-local.properties` 并仅在本地填写配置；该外置文件从启动工作目录读取，不会打入 JAR。以下环境变量方式无需复制模板。

PowerShell：

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = Read-Host "MySQL 密码"
$env:HOSPITAL_ADMIN_USERNAME = "admin"
$env:HOSPITAL_ADMIN_PASSWORD = Read-Host "设置初始管理员密码"
# 可选：使用自定义数据库地址
# $env:DB_URL = "jdbc:mysql://localhost:3306/yiyuan?..."
```

macOS / Linux：

```bash
export DB_USERNAME=root
read -r -s -p 'MySQL password: ' DB_PASSWORD; echo
export DB_PASSWORD
export HOSPITAL_ADMIN_USERNAME=admin
read -r -s -p 'Initial admin password: ' HOSPITAL_ADMIN_PASSWORD; echo
export HOSPITAL_ADMIN_PASSWORD
```

未设置 `HOSPITAL_ADMIN_PASSWORD` 时，医务端会跳过管理员初始化；已有管理员时不会覆盖其密码。患者在患者端注册，医生在医务端申请并由管理员审核。外部 AI 默认关闭，如需接入通义千问，另设 `AI_QWEN_ENABLED=true` 和 `DASHSCOPE_API_KEY`。

### 4. 构建并启动

Windows：

```powershell
.\mvnw.cmd -Pfrontend -DskipTests package
.\Start-HospitalPortals.ps1
```

macOS / Linux：先构建，再在两个终端分别启动医务端与患者端。

```bash
./mvnw -Pfrontend -DskipTests package
# 终端 1：医务端
java -jar target/YIYUAN-0.0.1-SNAPSHOT.jar --spring.profiles.active=staff --server.port=8083
```

```bash
# 终端 2：患者端（也需要设置数据库环境变量）
java -jar target/YIYUAN-0.0.1-SNAPSHOT.jar --spring.profiles.active=patient --server.port=8082
```

访问地址：

- 患者门户：<http://localhost:8082/app>
- 医务门户：<http://localhost:8083/app>
- 预约服务：<http://localhost:8082/app/booking>

### 前端开发模式

```bash
cd frontend
npm ci
npm run dev:patient
# 另一个终端
npm run dev:staff
```

开发模式仍需分别启动 `staff` 和 `patient` 后端。前端开发服务器将 `/api/**` 代理到对应实例；患者前端为 5173，医务前端为 5174。详细步骤见 [前端说明](frontend/README.md)。

## API 与安全

- `/api/auth/**`：登录、注册和会话
- `/api/public/**`：公开目录与预约入口
- `/api/patient/**`：患者业务
- `/api/doctor/**`：医生业务
- `/api/admin/**`：管理员业务
- 已启用 Session 认证、CSRF 防护、角色边界、敏感字段过滤和预约并发保护。

请求编码、响应字段和认证示例见 [API 说明](docs/API.md)。

生产部署前请替换本地数据库凭据，配置 HTTPS，并接入日志、监控、备份和权限审计方案。

## 测试

无需导入业务数据即可运行现有测试：

```powershell
.\mvnw.cmd test
```

前端类型检查与构建：

```bash
cd frontend
npm run typecheck
npm run build
```

提交前可以运行发布内容检查：

```bash
node scripts/check-public-release.mjs
```

## 贡献

欢迎提交 Issue、改进文档、补充测试或发起 Pull Request。请在提交前确认没有上传密码、API Key、数据库导出文件、日志或真实用户数据。


---

# Hospital Management System

![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen?logo=springboot)
![Vue](https://img.shields.io/badge/Vue-3-42b883?logo=vuedotjs)
![TypeScript](https://img.shields.io/badge/TypeScript-5.x-3178c6?logo=typescript)

A full-stack hospital management system for outpatient and inpatient workflows, with patient, staff, and administrator portals.

If this project helps your learning, graduation project, or full-stack practice, please consider giving it a **Star**, opening an Issue, or sharing it with other developers.

## Overview

![Generated artwork used by the hospital portal](src/main/resources/static/images/hospital-outpatient.png)

The two portals run on separate ports and share backend business code and a database. The image above is generated portal artwork. Payments are simulated. This distribution includes organized source and schema scripts, with no user records, clinical records, database backups, or local development history. Local rules are available without an AI API key.

## Features

### Patient portal

- Public hospital home page, department directory, and doctor directory
- Appointment booking and appointment history
- Reports, prescriptions, payments, and hospitalization records
- Patient profile and personal care history

### Staff portal

- Doctor login, diagnosis records, and patient queue
- Hospitalization requests, schedules, and bed management
- Examination, prescription, medicine, and pharmacy workflows
- AI-assisted suggestions with a local rule fallback

### Admin portal

- Doctor, patient, and account management
- Department, schedule, bed, and medicine administration
- Access control, audit logs, and operational data management

## Tech stack

| Layer | Technology |
| --- | --- |
| Backend | Java 17, Spring Boot, Spring Security, Spring JDBC |
| Frontend | Vue 3, TypeScript, Vue Router, Vite |
| Database | MySQL 8.0+ |
| Compatibility | Thymeleaf legacy templates and Vue SPA |
| Build | Maven Wrapper, npm |

## Repository layout

```text
src/main/java/                  Spring Boot backend
src/main/resources/             templates, static resources, and application configuration
frontend/src/                   Vue 3 + TypeScript frontend
frontend/package.json           frontend scripts and dependencies
src/test/                       backend tests
config/                         redacted external configuration template
database/init.sql               unified database initialization entry point
database/migrations/            optional migrations for existing databases
docs/                           API, database, and publication instructions
scripts/check-public-release.mjs publication checks
Start-HospitalPortals.ps1       start both portals on Windows
Stop-HospitalPortals.ps1        stop both portals on Windows
```

## Quick start

### Requirements

- Java 17+
- Node.js 22.12+ for Vite 8
- MySQL 8.0+ (the verification target for this distribution)
- Git

### 1. Clone

```bash
git clone https://github.com/wWYANG666/Hospital-Management-System.git
cd Hospital-Management-System
```

### 2. Create the database

Start the MySQL client from the repository root. `-p` prompts for your password:

```bash
mysql --default-character-set=utf8mb4 -u root -p
```

At the MySQL prompt:

```sql
SOURCE database/init.sql;
```

This creates the `yiyuan` database and core tables without importing users or clinical records. Start the staff portal first so it can apply the remaining repeatable migrations. Optionally run `SOURCE database/seed-department-items.sql;` for demonstration departments, medicines, and examinations, or create them through the administrator interface. See [database instructions](docs/DATABASE.md) for existing databases.

### 3. Configure environment variables

The redacted template is `config/application-local.example.properties`. Copy it to the Git-ignored `config/application-local.properties` and fill it locally if needed. This external file is loaded relative to the working directory and is not bundled in the JAR. Alternatively use the environment variables below.

PowerShell:

```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = Read-Host "MySQL password"
$env:HOSPITAL_ADMIN_USERNAME = "admin"
$env:HOSPITAL_ADMIN_PASSWORD = Read-Host "Initial admin password"
# Optional custom database URL:
# $env:DB_URL = "jdbc:mysql://localhost:3306/yiyuan?..."
```

macOS / Linux:

```bash
export DB_USERNAME=root
read -r -s -p 'MySQL password: ' DB_PASSWORD; echo
export DB_PASSWORD
export HOSPITAL_ADMIN_USERNAME=admin
read -r -s -p 'Initial admin password: ' HOSPITAL_ADMIN_PASSWORD; echo
export HOSPITAL_ADMIN_PASSWORD
```

When `HOSPITAL_ADMIN_PASSWORD` is empty, the staff portal skips administrator creation. Existing administrators and their passwords are preserved. Patients register in the patient portal; doctors apply in the staff portal and await administrator approval. External AI calls are disabled by default. To enable Qwen, set both `AI_QWEN_ENABLED=true` and `DASHSCOPE_API_KEY`.

### 4. Build and run

Windows:

```powershell
.\mvnw.cmd -Pfrontend -DskipTests package
.\Start-HospitalPortals.ps1
```

macOS / Linux: build first, then start each portal in a separate terminal.

```bash
./mvnw -Pfrontend -DskipTests package
# Terminal 1: staff
java -jar target/YIYUAN-0.0.1-SNAPSHOT.jar --spring.profiles.active=staff --server.port=8083
```

```bash
# Terminal 2: patient (set database environment variables here as well)
java -jar target/YIYUAN-0.0.1-SNAPSHOT.jar --spring.profiles.active=patient --server.port=8082
```

URLs:

- Patient portal: <http://localhost:8082/app>
- Staff portal: <http://localhost:8083/app>
- Booking: <http://localhost:8082/app/booking>

### Frontend development mode

```bash
cd frontend
npm ci
npm run dev:patient
# another terminal
npm run dev:staff
```

Run the `staff` and `patient` backends for development as well. Vite proxies `/api/**` to the matching instance; the patient frontend uses port 5173 and staff uses 5174. See [frontend instructions](frontend/README.md).

## API and security

- `/api/auth/**`: login, registration, and sessions
- `/api/public/**`: public directory and booking entry points
- `/api/patient/**`: patient workflows
- `/api/doctor/**`: doctor workflows
- `/api/admin/**`: administrator workflows
- Session authentication, CSRF protection, role boundaries, sensitive-field filtering, and booking concurrency protection are enabled.

See [API instructions](docs/API.md) for request encoding, response fields, and authentication examples.

Before production deployment, replace local credentials, configure HTTPS, and add production-grade logging, monitoring, backups, and access auditing.

## Testing

Existing tests do not require imported business data:

```powershell
.\mvnw.cmd test
```

Frontend type checking and build:

```bash
cd frontend
npm run typecheck
npm run build
```

## Contributing

Before publication, run `node scripts/check-public-release.mjs` to check files for prohibited artifacts and common sensitive strings.

Issues, documentation improvements, tests, and pull requests are welcome. Before submitting, make sure no passwords, API keys, database dumps, logs, or real user data are included.
