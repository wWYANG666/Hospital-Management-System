import type { RouteRecordRaw } from "vue-router";
import patientRoutes from "../patient";

const routes: RouteRecordRaw[] = [
  {
    path: "/app",
    component: () => import("@/layouts/PublicLayout.vue"),
    children: [
      {
        path: "",
        component: () => import("@/views/public/PortalView.vue"),
        meta: { title: "医院首页" },
      },
      {
        path: "guide",
        component: () => import("@/views/public/GuideView.vue"),
        meta: { title: "就医指南" },
      },
      {
        path: "login",
        component: () => import("@/views/public/PatientLogin.vue"),
        meta: { title: "患者登录" },
      },
      {
        path: "register",
        component: () => import("@/views/public/RegisterView.vue"),
        meta: { title: "患者注册" },
      },
      {
        path: "services",
        component: () => import("@/layouts/PatientServiceLayout.vue"),
        meta: { patientServices: true },
        children: [
          { path: "", redirect: "/app/booking" },
          {
            path: "/app/booking",
            component: () => import("@/views/public/BookingView.vue"),
            meta: { title: "预约挂号" },
          },
          {
            path: "/app/departments",
            component: () => import("@/views/public/DirectoryView.vue"),
            meta: { title: "科室导航" },
          },
          {
            path: "/app/doctors",
            component: () => import("@/views/public/DirectoryView.vue"),
            meta: { title: "医生排班" },
          },
          ...patientRoutes,
        ],
      },
    ],
  },
  {
    path: "/app/workspace",
    redirect: "/app/patient/dashboard",
  },
];
export default routes;
