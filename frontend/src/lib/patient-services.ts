export interface PatientServiceLink {
  id: string;
  label: string;
  path: string;
  icon: string;
}

export const patientServiceGroups: { label: string; links: PatientServiceLink[] }[] = [
  {
    label: "门诊服务",
    links: [
      { id: "booking", label: "预约挂号", path: "/app/booking", icon: "calendar" },
      { id: "directory", label: "科室与医生", path: "/app/departments", icon: "stethoscope" },
      { id: "appointments", label: "我的挂号", path: "/app/patient/appointments", icon: "clipboard" },
      { id: "prescriptions", label: "处方与缴费", path: "/app/patient/prescriptions", icon: "pill" },
      { id: "reports", label: "检查报告", path: "/app/patient/reports", icon: "file" },
    ],
  },
  {
    label: "住院服务",
    links: [{ id: "hospitalizations", label: "住院服务", path: "/app/patient/hospitalizations", icon: "bed" }],
  },
  {
    label: "就医记录",
    links: [
      { id: "dashboard", label: "我的就医", path: "/app/patient/dashboard", icon: "heart" },
      { id: "payments", label: "账单记录", path: "/app/patient/payments/history", icon: "credit-card" },
    ],
  },
];

export function patientServiceId(path: string, paymentType = "") {
  if (path === "/app/booking" || path.startsWith("/app/patient/appointments/new")) return "booking";
  if (path === "/app/departments" || path === "/app/doctors") return "directory";
  if (path.startsWith("/app/patient/appointments")) return "appointments";
  if (path.startsWith("/app/patient/prescriptions")) return "prescriptions";
  if (path.startsWith("/app/patient/reports")) return "reports";
  if (path.startsWith("/app/patient/hospitalizations")) return "hospitalizations";
  if (path === "/app/patient/dashboard" || path === "/app/patient/profile") return "dashboard";
  if (path.startsWith("/app/patient/payments/") && paymentType) {
    const type = paymentType.toLowerCase();
    if (type === "prescription") return "prescriptions";
    if (["report", "examreport", "exam_report", "examination"].includes(type)) return "reports";
    if (type === "hospitalization") return "hospitalizations";
  }
  return path.startsWith("/app/patient/payments/") ? "payments" : "";
}
