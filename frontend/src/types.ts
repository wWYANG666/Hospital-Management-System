export type Role = "PATIENT" | "DOCTOR" | "ADMIN";
export interface SessionUser {
  id: number;
  username: string;
  realName: string;
  role: Role;
  email?: string;
  phone?: string;
  status: number;
}
export type PageData = Record<string, any>;
export interface ApiEnvelope<T> {
  code: number;
  msg: string;
  data: T;
  timestamp?: number;
}
