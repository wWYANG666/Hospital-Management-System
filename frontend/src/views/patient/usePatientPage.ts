import { ref, watch } from "vue";
import { useRoute } from "vue-router";
import { loadPage, submitAction } from "@/lib/api";
import "@/styles/patient.css";

export type PatientData = Record<string, any>;
export const enumName = (value: any): string =>
  typeof value === "object" && value !== null
    ? String(value.name ?? value.value ?? "")
    : String(value ?? "");
export const list = (data: PatientData, key: string): PatientData[] =>
  Array.isArray(data[key]) ? data[key] : [];
export const currency = (value: unknown): string =>
  "¥" + (Number.isFinite(Number(value)) ? Number(value).toFixed(2) : "0.00");
export const appPath = (
  path: unknown,
  fallback = "/app/patient/dashboard",
): string =>
  typeof path === "string" && path.startsWith("/patient/")
    ? "/app" + path
    : fallback;

export function usePatientPage(
  path: () => string,
  query?: () => Record<string, unknown>,
) {
  const route = useRoute();
  const data = ref<PatientData>({});
  const loading = ref(false);
  const submitting = ref(false);
  const error = ref("");
  const message = ref("");
  let request = 0;
  async function reload() {
    const current = ++request;
    loading.value = true;
    error.value = "";
    try {
      const result = await loadPage<PatientData>(path(), query?.());
      if (current === request) {
        data.value = result ?? {};
        if (result?.error) error.value = String(result.error);
      }
    } catch (cause) {
      if (current === request)
        error.value =
          cause instanceof Error ? cause.message : "数据加载失败，请稍后重试。";
    } finally {
      if (current === request) loading.value = false;
    }
  }
  async function action(
    endpoint: string,
    form: Record<string, unknown> = {},
    success = "操作成功",
    refresh = true,
  ) {
    if (submitting.value) return false;
    submitting.value = true;
    error.value = "";
    message.value = "";
    try {
      await submitAction(endpoint, form);
      message.value = success;
      if (refresh) await reload();
      return true;
    } catch (cause) {
      error.value =
        cause instanceof Error ? cause.message : "操作失败，请稍后重试。";
      return false;
    } finally {
      submitting.value = false;
    }
  }
  watch(
    () => route.fullPath,
    (current, previous) => {
      message.value = "";
      if (current.split("?")[0] !== previous?.split("?")[0]) data.value = {};
      void reload();
    },
    { immediate: true },
  );
  return { data, loading, submitting, error, message, reload, action };
}
