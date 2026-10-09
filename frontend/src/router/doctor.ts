import type { RouteRecordRaw } from "vue-router";

const routes: RouteRecordRaw[] = [
  { path: "/app/doctor", redirect: "/app/doctor/dashboard" },
  {
    path: "/app/doctor/dashboard",
    component: () => import("@/views/doctor/Dashboard.vue"),
    meta: { role: "DOCTOR", title: "医生工作台" },
  },
  {
    path: "/app/doctor/diagnose",
    component: () => import("@/views/doctor/AppointmentList.vue"),
    meta: { role: "DOCTOR", title: "门诊接诊" },
  },
  {
    path: "/app/doctor/diagnose/:id",
    component: () => import("@/views/doctor/Diagnosis.vue"),
    meta: { role: "DOCTOR", title: "门诊诊疗" },
  },
  {
    path: "/app/doctor/appointments",
    component: () => import("@/views/doctor/AppointmentList.vue"),
    meta: { role: "DOCTOR", title: "就诊档案" },
  },
  {
    path: "/app/doctor/appointments/:id/record",
    component: () => import("@/views/doctor/MedicalRecord.vue"),
    meta: { role: "DOCTOR", title: "电子病历" },
  },
  {
    path: "/app/doctor/schedules",
    component: () => import("@/views/doctor/Schedules.vue"),
    meta: { role: "DOCTOR", title: "排班管理" },
  },
  {
    path: "/app/doctor/schedule-detail",
    component: () => import("@/views/doctor/ScheduleDetail.vue"),
    meta: { role: "DOCTOR", title: "班次预约详情" },
  },
  {
    path: "/app/doctor/reports",
    component: () => import("@/views/doctor/Reports.vue"),
    meta: { role: "DOCTOR", title: "检查报告" },
  },
  {
    path: "/app/doctor/reports/:id/edit",
    component: () => import("@/views/doctor/ReportEditor.vue"),
    meta: { role: "DOCTOR", title: "报告审核" },
  },
  {
    path: "/app/doctor/reports/:id/preview",
    component: () => import("@/views/doctor/ReportPreview.vue"),
    meta: { role: "DOCTOR", title: "报告预览" },
  },
  {
    path: "/app/doctor/hospitalizations",
    component: () => import("@/views/doctor/Hospitalizations.vue"),
    meta: { role: "DOCTOR", title: "住院患者" },
  },
  {
    path: "/app/doctor/hospitalizations/request/:appointmentId",
    component: () => import("@/views/doctor/HospitalizationRequest.vue"),
    meta: { role: "DOCTOR", title: "开立住院申请" },
  },
  {
    path: "/app/doctor/hospitalizations/:id/assign-bed",
    component: () => import("@/views/doctor/AssignBed.vue"),
    meta: { role: "DOCTOR", title: "床位分配" },
  },
  {
    path: "/app/doctor/hospitalizations/:id/manage",
    component: () => import("@/views/doctor/HospitalizationManage.vue"),
    meta: { role: "DOCTOR", title: "住院医嘱" },
  },
  {
    path: "/app/doctor/hospitalizations/:id/discharge",
    component: () => import("@/views/doctor/Discharge.vue"),
    meta: { role: "DOCTOR", title: "办理出院" },
  },
  {
    path: "/app/doctor/profile",
    component: () => import("@/views/doctor/Profile.vue"),
    meta: { role: "DOCTOR", title: "执业档案" },
  },
];

export default routes;
