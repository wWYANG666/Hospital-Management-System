import type { PageData } from "@/types";

export function record(value: unknown): PageData {
  return value && typeof value === "object" && !Array.isArray(value)
    ? (value as PageData)
    : {};
}
export function rows(data: unknown, key?: string): PageData[] {
  const value = key ? record(data)[key] : data;
  return Array.isArray(value) ? value.map(record) : [];
}
export function text(value: unknown, fallback = "—"): string {
  if (value === undefined || value === null || value === "") return fallback;
  if (typeof value === "object")
    return String(
      record(value).chineseName ||
        record(value).name ||
        record(value).value ||
        fallback,
    );
  return String(value);
}
const statuses: Record<string, string> = {
  PENDING: "待处理",
  CONFIRMED: "已确认",
  COMPLETED: "已完成",
  CANCELLED: "已取消",
  ADMITTED: "住院中",
  DISCHARGED: "已出院",
  APPROVED: "已通过",
  REJECTED: "已拒绝",
  AVAILABLE: "空闲",
  OCCUPIED: "使用中",
  MAINTENANCE: "维护中",
  ACTIVE: "执行中",
  STOPPED: "已停止",
  DISPENSED: "已发药",
  GENERAL: "普通",
  EXPERT: "专家",
  VIP: "特需",
  ICU: "重症",
  MORNING: "上午",
  AFTERNOON: "下午",
  EVENING: "夜间",
  MALE: "男",
  FEMALE: "女",
  OTHER: "其他",
  PATIENT: "患者",
  DOCTOR: "医生",
  ADMIN: "管理员",
  MEDICATION: "用药",
  EXAMINATION: "检查",
  TREATMENT: "治疗",
};
export function statusText(value: unknown): string {
  const key = text(value);
  return statuses[key] || key;
}
export function formatDate(value: unknown, withTime = false): string {
  const source = text(value, "");
  if (!source) return "—";
  if (/^\d{4}-\d{2}-\d{2}/.test(source))
    return source.replace("T", " ").slice(0, withTime ? 16 : 10);
  return source;
}
export function money(value: unknown): string {
  const number = Number(value);
  return Number.isFinite(number) ? number.toFixed(2) : "0.00";
}
