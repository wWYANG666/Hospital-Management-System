import type { ApiEnvelope, PageData } from "@/types";
import { notify } from "./notifications";
export type { PageData } from "@/types";

export class ApiError extends Error {
  constructor(
    public code: number,
    message: string,
  ) {
    super(message);
  }
}
let csrfToken = "";
let expiryReported = false;
export function setCsrfToken(token: string) {
  csrfToken = token;
  if (token) expiryReported = false;
}
function handleExpiredSession(error: ApiError, path: string) {
  if (error.code === 401 && !path.startsWith('/auth/') && !path.startsWith('/api/auth/') && !expiryReported) {
    expiryReported = true;
    window.dispatchEvent(new CustomEvent('session-expired'));
  }
}
function pathOf(path: string) {
  return path.startsWith("/api/") ? path : "/api" + path;
}

async function decode<T>(response: Response): Promise<T> {
  if (!response.headers.get("content-type")?.includes("json"))
    throw new ApiError(response.status, "服务返回了无效响应，请刷新页面");
  const result = (await response.json()) as ApiEnvelope<T>;
  if (!response.ok || result.code !== 200)
    throw new ApiError(
      result.code || response.status,
      result.msg || "请求失败",
    );
  return result.data;
}

export async function loadPage<T = PageData>(
  path: string,
  query?: Record<string, unknown>,
): Promise<T> {
  const url = new URL(pathOf(path), window.location.origin);
  for (const [key, value] of Object.entries(query || {})) {
    if (value !== undefined && value !== null && value !== "")
      url.searchParams.set(key, String(value));
  }
  try {
    return await decode<T>(
      await fetch(url, {
        credentials: "same-origin",
        headers: { Accept: "application/json" },
      }),
    );
  } catch (error) {
    if (error instanceof ApiError) { handleExpiredSession(error, path); throw error; }
    throw new ApiError(0, "无法连接服务，请检查网络后重试");
  }
}

export async function submitAction<T = PageData>(
  path: string,
  form?: Record<string, unknown>,
): Promise<T> {
  if (!csrfToken) {
    const session = await loadPage<{ csrfToken: string }>("/auth/session");
    csrfToken = session.csrfToken;
  }
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(form || {})) {
    if (value === undefined || value === null) continue;
    for (const item of Array.isArray(value) ? value : [value])
      params.append(key, String(item));
  }
  try {
    const response = await fetch(pathOf(path), {
      method: "POST",
      credentials: "same-origin",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Accept: "application/json",
        "X-XSRF-TOKEN": csrfToken,
      },
      body: params,
    });
    const result = (await response.clone().json()) as ApiEnvelope<T>;
    const data = await decode<T>(response);
    if (result.msg && result.msg !== "success") notify(result.msg, "success");
    return data;
  } catch (error) {
    const resolved =
      error instanceof ApiError
        ? error
        : new ApiError(0, "无法连接服务，请稍后重试");
    handleExpiredSession(resolved, path);
    throw resolved;
  }
}
