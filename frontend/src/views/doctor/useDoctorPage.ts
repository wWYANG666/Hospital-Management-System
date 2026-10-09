import { computed, onBeforeUnmount, onMounted, ref, watch, type Ref } from "vue";
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from "vue-router";
import { loadPage, submitAction } from "@/lib/api";
import { confirmAction } from "@/lib/confirm";

export type DoctorData = Record<string, any>;

export function useDoctorPage(
  path: () => string,
  init?: (value: DoctorData) => void,
  draft?: () => unknown,
  acceptWholeDraft = true,
) {
  const route = useRoute();
  const router = useRouter();
  const data = ref<DoctorData>({});
  const loading = ref(true);
  const saving = ref(false);
  const error = ref("");
  const notice = ref("");
  const loadFailed = ref(false);
  const savedDraft = ref("");
  const dirty = computed(() => Boolean(draft && savedDraft.value && JSON.stringify(draft()) !== savedDraft.value));
  const acceptDraft = () => { if (draft) savedDraft.value = JSON.stringify(draft()); };
  let requestVersion = 0;
  onBeforeUnmount(() => { requestVersion++; });

  if (draft) {
    const confirmLeave = async () => {
      if (saving.value) return false;
      if (!dirty.value) return true;
      return confirmAction("当前填写内容尚未保存，离开后将丢失。确认离开此页面？", "离开未保存的表单");
    };
    onBeforeRouteLeave(confirmLeave);
    onBeforeRouteUpdate(confirmLeave);
    const protectDraft = (event: BeforeUnloadEvent) => {
      if (!dirty.value) return;
      event.preventDefault();
      event.returnValue = "";
    };
    onMounted(() => window.addEventListener("beforeunload", protectDraft));
    onBeforeUnmount(() => window.removeEventListener("beforeunload", protectDraft));
  }

  async function load() {
    const version = ++requestVersion;
    loading.value = true;
    loadFailed.value = false;
    error.value = "";
    try {
      const value = await loadPage<DoctorData>(path(), route.query);
      if (version !== requestVersion) return;
      if (value.__redirect) {
        await router.replace(appPath(value.__redirect));
        return;
      }
      data.value = value;
      if (value.error) {
        error.value = String(value.error);
      }
      init?.(value);
      if (acceptWholeDraft || !savedDraft.value) acceptDraft();
    } catch (cause) {
      if (version !== requestVersion) return;
      error.value = cause instanceof Error ? cause.message : "加载失败，请重试";
      loadFailed.value = true;
    } finally {
      if (version === requestVersion) loading.value = false;
    }
  }

  async function action(
    actionPath: string,
    fields: Record<string, unknown>,
    target?: string,
  ) {
    if (saving.value) return false;
    saving.value = true;
    error.value = "";
    notice.value = "";
    try {
      const value = await submitAction<DoctorData>(actionPath, fields);
      if (value.flash?.error || value.error) {
        error.value = String(value.flash?.error || value.error);
        return false;
      }
      notice.value = String(
        value.flash?.success || value.__message || "操作已完成",
      );
      if (acceptWholeDraft) acceptDraft();
      if (target) {
        saving.value = false;
        await router.push(target);
      }
      else await load();
      return true;
    } catch (cause) {
      error.value = cause instanceof Error ? cause.message : "操作失败，请重试";
      return false;
    } finally {
      saving.value = false;
    }
  }

  watch(() => route.fullPath, load, { immediate: true, flush: "post" });
  return { data, loading, saving, error, notice, loadFailed, dirty, acceptDraft, load, action };
}

export function useDoctorPagination(rows: Ref<DoctorData[]>, pageSize = 10) {
  const page = ref(1);
  const pages = computed(() => Math.max(1, Math.ceil(rows.value.length / pageSize)));
  const visible = computed(() => rows.value.slice((page.value - 1) * pageSize, page.value * pageSize));
  watch(rows, () => { page.value = 1; });
  return { page, pages, visible };
}

export function appPath(value: string) {
  const path = value.replace(/^redirect:/, "");
  return path.startsWith("/app/") ? path : `/app${path}`;
}

export function list(data: DoctorData, key: string): DoctorData[] {
  return Array.isArray(data[key]) ? data[key] : [];
}

export function mappedName(map: DoctorData | undefined, id: unknown) {
  const value = map?.[String(id)];
  return value?.realName || value?.name || "患者信息待补充";
}

export function useFilter(
  source: Ref<DoctorData>,
  key: string,
  search: Ref<string>,
  matcher: (row: DoctorData) => string,
) {
  return computed(() =>
    list(source.value, key).filter((row) =>
      matcher(row).toLowerCase().includes(search.value.trim().toLowerCase()),
    ),
  );
}
