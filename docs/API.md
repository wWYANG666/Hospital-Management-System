# API 使用说明

## 入口与角色

| 入口 | 本地地址 | 登录角色 |
| --- | --- | --- |
| 患者门户 | `http://localhost:8082` | `PATIENT` |
| 医务门户 | `http://localhost:8083` | `DOCTOR`、`ADMIN` |

前端通过同源 `/api/**` 调用后端。患者端和医务端分别使用独立 Session、Cookie 和 CSRF Cookie。

## 响应格式

响应为 JSON，消息字段为 `msg`，不是 `message`：

```json
{
  "code": 200,
  "msg": "success",
  "data": {},
  "timestamp": 0
}
```

成功返回 `code=200`；失败同时返回相应 HTTP 状态码和业务错误码。数据中的密码字段会被过滤。

## 登录流程

1. GET `/api/auth/session`，取得 `data.csrfToken` 并保留响应 Cookie。
2. POST `/api/auth/login`，提交 URL 编码的 `username` 和 `password` 表单参数。
3. 后续请求携带同一端的 Session Cookie；POST 请求通过 `X-XSRF-TOKEN` 请求头发送 CSRF Token。

以下示例用于浏览器同源调用；账号与密码由调用方输入，不包含真实凭据：

```javascript
async function login(username, password) {
  const sessionResponse = await fetch('/api/auth/session', {
    credentials: 'same-origin',
  });
  const session = await sessionResponse.json();
  if (!sessionResponse.ok || session.code !== 200) {
    throw new Error(session.msg || 'Cannot obtain session');
  }
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'X-XSRF-TOKEN': session.data.csrfToken,
    },
    body: new URLSearchParams({ username, password }),
  });
  const result = await response.json();
  if (!response.ok || result.code !== 200) {
    throw new Error(result.msg || 'Login failed');
  }
  return result.data;
}
```

登录接口成功后 `data` 为过滤后的用户对象。未登录时 Session 查询的 `data.user` 为 `null`。注销通过 POST `/api/auth/logout`，同样要求 CSRF 请求头。

## 接口模块

| 路径 | 内容 |
| --- | --- |
| `/api/auth/**` | Session、登录、注销、患者注册与医生申请 |
| `/api/public/**` | 患者端的公开科室、医生和预约目录 |
| `/api/patient/**` | 患者预约、报告、处方、模拟缴费、住院和资料 |
| `/api/doctor/**` | 医生诊疗、报告、排班和住院管理 |
| `/api/admin/**` | 管理员资源、账号、排班与药房管理 |

医生申请使用 POST `/api/auth/register/doctor`，患者注册使用 POST `/api/auth/register/patient`，均为 URL 编码表单。字段以 `AuthApiController` 的方法参数为准。其余端点可在 `src/main/java/xmu/edu/yiyuan/controller/api/` 中查阅，现有前端封装位于 `frontend/src/lib/api.ts`。

## English summary

The patient portal runs on port 8082; staff runs on 8083. Responses use `code`, `msg`, `data`, and `timestamp`. Obtain the CSRF token from GET `/api/auth/session`, preserve the session cookie, and submit login credentials as `application/x-www-form-urlencoded` with an `X-XSRF-TOKEN` header. POST requests require the token. The examples contain no real accounts or credentials.
