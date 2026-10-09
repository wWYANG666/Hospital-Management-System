import type { Role } from "@/types";
import { hospitalProfile } from "./hospital";

declare const __PORTAL_MODE__: "patient" | "staff";
export const portalMode = __PORTAL_MODE__;
export const isPatientPortal = portalMode === "patient";
export const portalTitle = isPatientPortal
  ? hospitalProfile.name
  : "医务管理工作台";
export function acceptsRole(role?: Role | null) {
  return isPatientPortal
    ? role === "PATIENT"
    : role === "DOCTOR" || role === "ADMIN";
}
