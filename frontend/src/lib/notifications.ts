import { reactive } from "vue";

export interface Notice {
  id: number;
  text: string;
  type: "success" | "error" | "info";
}
export const notices = reactive<Notice[]>([]);
let sequence = 0;
export function notify(text: string, type: Notice["type"] = "info") {
  const id = ++sequence;
  notices.push({ id, text, type });
  window.setTimeout(() => dismiss(id), type === "error" ? 7000 : 4000);
}
export function dismiss(id: number) {
  const index = notices.findIndex((item) => item.id === id);
  if (index !== -1) notices.splice(index, 1);
}
