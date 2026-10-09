import type { RouteRecordRaw } from "vue-router";
import doctorRoutes from "../doctor";
import adminRoutes from "../admin";

const routes: RouteRecordRaw[] = [
  {
    path: "/app",
    component: () => import("@/layouts/StaffLayout.vue"),
    children: [
      {
        path: "",
        component: () => import("@/views/staff/StaffLogin.vue"),
        meta: { title: "医务账号登录" },
      },
      {
        path: "login",
        component: () => import("@/views/staff/StaffLogin.vue"),
        meta: { title: "医务账号登录" },
      },
      {
        path: "register",
        component: () => import("@/views/staff/StaffRegister.vue"),
        meta: { title: "医生账号申请" },
      },
    ],
  },
  {
    path: "/app/workspace",
    component: () => import("@/layouts/ClinicLayout.vue"),
    children: [...doctorRoutes, ...adminRoutes],
  },
];
export default routes;
