import type { RouteRecordRaw } from "vue-router";

const route = (
  path: string,
  title: string,
  component: NonNullable<RouteRecordRaw["component"]>,
): RouteRecordRaw => ({
  path: "/app/patient" + path,
  component,
  meta: { role: "PATIENT", title },
});
const routes: RouteRecordRaw[] = [
  route(
    "/dashboard",
    "我的就医",
    () => import("@/views/patient/Dashboard.vue"),
  ),
  route(
    "/appointments/new",
    "预约挂号",
    () => import("@/views/patient/AppointmentWizard.vue"),
  ),
  route(
    "/appointments/new/step2",
    "选择医生",
    () => import("@/views/patient/AppointmentWizard.vue"),
  ),
  route(
    "/appointments/new/step3",
    "选择时段",
    () => import("@/views/patient/AppointmentWizard.vue"),
  ),
  route(
    "/appointments/new/step4",
    "确认预约",
    () => import("@/views/patient/AppointmentWizard.vue"),
  ),
  route(
    "/appointments",
    "我的挂号",
    () => import("@/views/patient/Appointments.vue"),
  ),
  route(
    "/appointments/:id/record",
    "就诊病历",
    () => import("@/views/patient/MedicalRecord.vue"),
  ),
  route(
    "/prescriptions",
    "处方与取药",
    () => import("@/views/patient/Prescriptions.vue"),
  ),
  route(
    "/prescriptions/:id",
    "处方详情",
    () => import("@/views/patient/PrescriptionDetail.vue"),
  ),
  route("/reports", "检查报告", () => import("@/views/patient/Reports.vue")),
  route(
    "/reports/:id",
    "报告详情",
    () => import("@/views/patient/ReportDetail.vue"),
  ),
  route(
    "/reports/:id/preview",
    "报告预览",
    () => import("@/views/patient/ReportDetail.vue"),
  ),
  route(
    "/hospitalizations",
    "住院服务",
    () => import("@/views/patient/Hospitalizations.vue"),
  ),
  route(
    "/hospitalizations/request",
    "住院办理",
    () => import("@/views/patient/Hospitalizations.vue"),
  ),
  route(
    "/hospitalizations/:id/detail",
    "住院详情",
    () => import("@/views/patient/HospitalizationDetail.vue"),
  ),
  route(
    "/payments/history",
    "缴费记录",
    () => import("@/views/patient/Payments.vue"),
  ),
  route(
    "/payments/:type/:id/alipay",
    "费用支付",
    () => import("@/views/patient/Payment.vue"),
  ),
  route("/profile", "就诊人资料", () => import("@/views/patient/Profile.vue")),
];
export default routes;
