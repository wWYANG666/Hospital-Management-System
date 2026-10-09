import { computed, ref, watch, type ComputedRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { loadPage, submitAction } from '@/lib/api'
import '@/styles/admin.css'

export type AdminData = Record<string, any>
export function appPath(path: string) { return path.startsWith('/app/') ? path : `/app${path.replace(/^redirect:/, '')}` }
export function items(data: AdminData, key: string): AdminData[] { return Array.isArray(data[key]) ? data[key] : [] }
export function entry(data: AdminData, key: string, id: unknown): AdminData { return data[key]?.[String(id)] || {} }
export function name(data: AdminData, key: string, id: unknown) { const item = entry(data, key, id); return item.realName || item.name || '未登记' }
export { scheduleLabel as scheduleStatus } from '@/lib/schedule'
export function useAdminPagination<T>(rows: ComputedRef<T[]>, pageSize = 20) {
  const page = ref(1)
  const totalPages = computed(() => Math.max(1, Math.ceil(rows.value.length / pageSize)))
  const pageRows = computed(() => rows.value.slice((page.value - 1) * pageSize, page.value * pageSize))
  watch(rows, () => { page.value = 1 })
  return { page, totalPages, pageRows }
}
export function useAdminPage(path: () => string, init?: (value: AdminData) => void) {
  const route = useRoute(), router = useRouter()
  const data = ref<AdminData>({}), loading = ref(true), saving = ref(false), error = ref(''), notice = ref('')
  let sequence = 0
  async function load() {
    const current = ++sequence
    loading.value = true; error.value = ''
    try {
      const value = await loadPage<AdminData>(path(), route.query)
      if (current !== sequence) return
      if (value.__redirect) { await router.replace(appPath(value.__redirect)); return }
      data.value = value; init?.(value)
    } catch (cause) { if (current === sequence) error.value = cause instanceof Error ? cause.message : '加载失败，请重试' }
    finally { if (current === sequence) loading.value = false }
  }
  async function action(path: string, form: Record<string, unknown> = {}, target?: string) {
    if (saving.value) return false
    saving.value = true; error.value = ''; notice.value = ''
    try {
      const value = await submitAction<AdminData>(path, form)
      if (value.error || value.flash?.error) { error.value = String(value.error || value.flash.error); return false }
      notice.value = value.flash?.success || value.__message || '操作已完成'
      if (target) await router.push(appPath(target)); else await load()
      return true
    } catch (cause) { error.value = cause instanceof Error ? cause.message : '操作失败，请重试'; return false }
    finally { saving.value = false }
  }
  watch(() => route.fullPath, load, { immediate: true })
  return { data, loading, saving, error, notice, load, action }
}
