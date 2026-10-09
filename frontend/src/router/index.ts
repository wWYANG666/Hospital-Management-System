import { createRouter, createWebHistory } from "vue-router";
import { clearSession, useAuth } from "@/stores/auth";
import { notify } from "@/lib/notifications";
import { acceptsRole, isPatientPortal, portalTitle } from "@/lib/portal";
import type { Role } from "@/types";
import portalRoutes from "@portal-routes";

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, _from, savedPosition) {
    if (savedPosition) return savedPosition;
    if (to.hash) return { el: to.hash, top: 100 };
    return { top: 0 };
  },
  routes: [
    { path: "/", redirect: "/app" },
    ...portalRoutes,
    {
      path: "/app/:pathMatch(.*)*",
      component: () => import("@/views/public/NotFoundView.vue"),
      meta: { title: "页面未找到" },
    },
  ],
});

router.beforeEach(async (to) => {
  const auth = useAuth();
  try {
    if (!auth.initialized.value) await auth.refresh();
  } catch {
    if (to.meta.role)
      return { path: "/app/login", query: { redirect: to.fullPath } };
  }
  if (auth.user.value && acceptsRole(auth.user.value.role) &&
      (to.path === '/app/login' || (!isPatientPortal && to.path === '/app'))) {
    return isPatientPortal ? '/app' : '/app/' + auth.user.value.role.toLowerCase() + '/dashboard';
  }
  const role = to.meta.role as Role | undefined;
  if (role) {
    if (!auth.user.value)
      return { path: "/app/login", query: { redirect: to.fullPath } };
    if (!acceptsRole(auth.user.value.role)) {
      clearSession();
      return "/app/login";
    }
    if (auth.user.value.role !== role) {
      notify("当前账号不能进入该角色工作台", "error");
      return "/app/" + auth.user.value.role.toLowerCase() + "/dashboard";
    }
  }
  document.title = (to.meta.title || "服务首页") + " · " + portalTitle;
});
window.addEventListener("session-expired", () => {
  clearSession();
  const current = router.currentRoute.value;
  if (current.meta.role)
    void router.replace({
      path: "/app/login",
      query: { redirect: current.fullPath, reason: "expired" },
    });
});
export default router;
