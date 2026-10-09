import { reactive } from "vue";

export const confirmation = reactive({ open: false, title: "", message: "" });
let finish: ((accepted: boolean) => void) | null = null;
export function confirmAction(
  message: string,
  title = "确认操作",
): Promise<boolean> {
  if (finish) return Promise.resolve(false);
  Object.assign(confirmation, { open: true, title, message });
  return new Promise((resolve) => {
    finish = resolve;
  });
}
export function resolveConfirmation(accepted: boolean) {
  confirmation.open = false;
  const resolve = finish;
  finish = null;
  resolve?.(accepted);
}
