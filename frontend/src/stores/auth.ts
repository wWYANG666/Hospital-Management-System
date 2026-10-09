import { computed, reactive } from "vue";
import { loadPage, setCsrfToken, submitAction } from "@/lib/api";
import type { SessionUser } from "@/types";
import { acceptsRole, portalMode } from "@/lib/portal";

const state = reactive<{ user: SessionUser | null; initialized: boolean }>({
  user: null,
  initialized: false,
});
let pending: Promise<void> | null = null;
async function refresh() {
  if (!pending)
    pending = (async () => {
      const response = await loadPage<{
        user: SessionUser | null;
        csrfToken: string;
        portalMode: string;
      }>("/auth/session");
      if (response.portalMode !== portalMode)
        throw new Error("当前页面与服务端配置不一致，请重新打开对应入口");
      state.user =
        response.user && acceptsRole(response.user.role) ? response.user : null;
      state.initialized = true;
      setCsrfToken(response.csrfToken);
    })().finally(() => {
      pending = null;
    });
  return pending;
}
async function login(username: string, password: string) {
  await submitAction("/auth/login", { username, password });
  await refresh();
  return state.user;
}
async function logout() {
  await submitAction("/auth/logout");
  state.user = null;
  setCsrfToken("");
  await refresh();
}
export function useAuth() {
  return {
    user: computed(() => state.user),
    initialized: computed(() => state.initialized),
    refresh,
    login,
    logout,
  };
}
export function clearSession() {
  state.user = null;
  state.initialized = true;
  setCsrfToken("");
}
