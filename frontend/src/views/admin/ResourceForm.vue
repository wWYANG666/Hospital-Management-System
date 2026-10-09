<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import UiIcon from '@/components/UiIcon.vue'
import AdminFeedback from './AdminFeedback.vue'
import { useAdminPage, items, type AdminData } from './useAdminPage'
import { money } from '@/lib/format'
import { confirmAction } from '@/lib/confirm'

type Option = { value: any; label: string }
type Field = { key: string; label: string; type?: string; required?: boolean; full?: boolean; min?: number; step?: string; hint?: string; options?: Option[]; source?: string }
const route = useRoute(), form = reactive<AdminData>({})
const original = ref('')
const dirty = computed(() => original.value !== '' && JSON.stringify(form) !== original.value)
const today = new Intl.DateTimeFormat('en-CA').format(new Date())
const resource = computed(() => String(route.meta.resource))
const isEdit = computed(() => !!route.params.id && resource.value !== 'inbound')
const settings: Record<string, { singular: string; key: string; back: string; sub: string; sections: { title: string; fields: Field[] }[]; defaults: AdminData }> = {
  medicines: { singular: '药品', key: 'medicine', back: '/admin/medicines', sub: '维护完整药品目录信息；库存增加请通过批次入库办理。', defaults: { status: 1, stock: 0 }, sections: [
    { title: '药品基础信息', fields: [ { key: 'name', label: '药品名称', required: true }, { key: 'code', label: '药品编码' }, { key: 'type', label: '药品分类', required: true, type: 'select', options: ['西药', '中成药', '中药饮片'].map(v => ({value:v,label:v})) }, { key: 'specification', label: '规格', required: true, hint: '例如：0.25g × 24片 / 盒' }, { key: 'manufacturer', label: '生产厂家' }, { key: 'approvalNumber', label: '批准文号' } ] },
    { title: '收费与使用范围', fields: [ { key: 'unit', label: '计价单位', required: true, type: 'select', options: ['盒', '瓶', '支', '片', '粒', '袋'].map(v => ({value:v,label:v})) }, { key: 'price', label: '零售价（元）', type: 'number', required: true, min: 0, step: '0.01' }, { key: 'insuranceCategory', label: '医保类别', type: 'select', options: ['甲类', '乙类', '自费'].map(v => ({value:v,label:v})) }, { key: 'departmentId', label: '适用科室', type: 'select', source: 'departments', hint: '不选科室表示全院通用' }, { key: 'description', label: '药品说明', type: 'textarea', full: true } ] }
  ] },
  examinations: { singular: '检查项目', key: 'examination', back: '/admin/examinations', sub: '维护检查项目所属科室、类型及收费标准。', defaults: { status: 1 }, sections: [
    { title: '项目基础信息', fields: [ { key: 'name', label: '项目名称', required: true }, { key: 'departmentId', label: '归属科室', type: 'select', required: true, source: 'departments' }, { key: 'type', label: '项目类型', type: 'select', required: true, options: ['检查', '检验'].map(v => ({value:v,label:v})) }, { key: 'price', label: '收费标准（元）', type: 'number', min: 0, step: '0.01' }, { key: 'status', label: '启用状态', type: 'select', options: [{value:1,label:'启用'},{value:0,label:'停用'}] }, { key: 'description', label: '项目说明', type: 'textarea', full: true } ] }
  ] },
  beds: { singular: '床位', key: 'bed', back: '/admin/beds', sub: '维护病区床位信息与每日收费标准。', defaults: { status: 'AVAILABLE', bedType: 'GENERAL' }, sections: [
    { title: '床位归属', fields: [ { key: 'bedNumber', label: '床位号', required: true }, { key: 'roomNumber', label: '房间号' }, { key: 'departmentId', label: '归属科室', type: 'select', source: 'departments' }, { key: 'ward', label: '病区' } ] },
    { title: '类型与收费', fields: [ { key: 'bedType', label: '床位类型', type: 'select', options: [{value:'GENERAL',label:'普通床位'},{value:'VIP',label:'VIP床位'},{value:'ICU',label:'ICU床位'}] }, { key: 'status', label: '床位状态', type: 'select', options: [{value:'AVAILABLE',label:'空闲'},{value:'OCCUPIED',label:'占用'},{value:'MAINTENANCE',label:'维修'}] }, { key: 'pricePerDay', label: '床位费（元 / 天）', type: 'number', min: 0, step: '0.01' } ] }
  ] },
  inbound: { singular: '药品入库', key: 'inbound', back: '/admin/medicines', sub: '登记药品批次、有效期与采购数量，提交后自动审核并更新库存。', defaults: { location: '药房', quantity: 1 }, sections: [
    { title: '药品批次', fields: [ { key: 'batchNumber', label: '生产批次号', required: true }, { key: 'location', label: '存放位置', required: true }, { key: 'productionDate', label: '生产日期', type: 'date' }, { key: 'expiryDate', label: '有效期至', required: true, type: 'date' } ] },
    { title: '数量与采购', fields: [ { key: 'quantity', label: '入库数量', type: 'number', min: 1, step: '1', required: true }, { key: 'purchasePrice', label: '采购单价（元）', type: 'number', min: 0, step: '0.01', required: true }, { key: 'operator', label: '经办人' } ] }
  ] },
  schedules: { singular: '门诊排班', key: 'schedule', back: '/admin/schedules', sub: '按日期区间批量建立普通与专家门诊号源。', defaults: { workTime: 'MORNING', maxAppointments: 20, expertMaxAppointments: 0 }, sections: [
    { title: '出诊医生与科室', fields: [ { key: 'doctorId', label: '医生', required: true, type: 'select', source: 'doctors' }, { key: 'departmentId', label: '科室', required: true, type: 'select', source: 'departments' } ] },
    { title: '出诊日期与班次', fields: [ { key: 'startDate', label: '开始日期', type: 'date', required: true }, { key: 'endDate', label: '结束日期', type: 'date', required: true }, { key: 'workTime', label: '出诊班次', type: 'select', required: true, options: [{value:'MORNING',label:'上午'},{value:'AFTERNOON',label:'下午'},{value:'EVENING',label:'晚上'}] } ] },
    { title: '号源容量', fields: [ { key: 'maxAppointments', label: '普通门诊号源 / 日', type: 'number', min: 1, step: '1', required: true }, { key: 'expertMaxAppointments', label: '专家门诊号源 / 日', type: 'number', min: 0, step: '1', hint: '填写 0 表示不创建专家门诊排班' } ] }
  ] }
}
const config = computed(() => settings[resource.value] || settings.medicines)
const path = () => resource.value === 'inbound' ? `/admin/medicines/${route.params.id}/inbound` : `/admin/${resource.value}/${isEdit.value ? `${route.params.id}/edit` : 'new'}`
const { data, loading, saving, error, notice, load, action } = useAdminPage(path, value => {
  Object.keys(form).forEach(key => delete form[key])
  Object.assign(form, config.value.defaults, value[config.value.key] || {})
  if (value.selectedDoctorId) form.doctorId = value.selectedDoctorId
  if (value.selectedDepartmentId) form.departmentId = value.selectedDepartmentId
  original.value = JSON.stringify(form)
})
const title = computed(() => resource.value === 'inbound' ? '办理药品入库' : `${isEdit.value ? '编辑' : '新增'}${config.value.singular}`)
function options(field: Field) { return field.source ? items(data.value, field.source).map(r => ({ value:r.id, label: field.source === 'doctors' ? `${data.value.doctorUserMap?.[r.id]?.realName || r.doctorCode || `医生 #${r.id}`} · ${r.department || '科室待登记'}${r.title ? ` · ${r.title}` : ''}` : r.name })) : field.options || [] }
async function save() {
  if (resource.value === 'schedules' && form.endDate < form.startDate) { error.value = '结束日期不能早于开始日期'; return }
  if (resource.value === 'inbound' && form.productionDate && form.productionDate > form.expiryDate) { error.value = '有效期必须晚于生产日期'; return }
  if (resource.value === 'inbound' && form.expiryDate < today) { error.value = '不能入库已过期的药品'; return }
  const postPath = resource.value === 'inbound' ? `/admin/medicines/${route.params.id}/inbound` : `/admin/${resource.value}${isEdit.value ? `/${route.params.id}` : ''}`
  const back = resource.value === 'schedules' ? `/admin/departments/${form.departmentId}/doctors/${form.doctorId}/schedules` : config.value.back
  await action(postPath, { ...form }, back)
}
async function canLeave() {
  return !dirty.value || saving.value || await confirmAction('填写内容尚未保存，确定离开当前页面吗？', '离开编辑页面')
}
async function reload() { if (await canLeave()) await load() }
function beforeUnload(event: BeforeUnloadEvent) {
  if (dirty.value && !saving.value) { event.preventDefault(); event.returnValue = '' }
}
onBeforeRouteLeave(canLeave)
onBeforeRouteUpdate(canLeave)
onMounted(() => window.addEventListener('beforeunload', beforeUnload))
onBeforeUnmount(() => window.removeEventListener('beforeunload', beforeUnload))
function minimum(field: Field) {
  if (field.key === 'endDate') return form.startDate
  if (field.key === 'expiryDate') return form.productionDate && form.productionDate > today ? form.productionDate : today
  return field.min
}
</script>
<template>
  <div class="admin-view">
    <PageHeader :title="title" :description="config.sub"><RouterLink class="button button-secondary" :to="`/app${config.back}`"><UiIcon name="chevron-left" :size="17" />返回列表</RouterLink></PageHeader>
    <AdminFeedback :loading="loading" :error="error" :notice="notice" @retry="reload" />
    <div v-if="!loading" class="admin-form-layout"><form class="panel" @submit.prevent="save">
      <div v-if="resource === 'inbound'" class="admin-form-section"><div class="admin-person-summary"><span class="admin-person-avatar"><UiIcon name="pill" :size="26" /></span><div><h2>{{ data.medicine?.name }}</h2><p>{{ data.medicine?.specification }} · 当前库存 {{ data.medicine?.stock ?? 0 }} {{ data.medicine?.unit }} · 零售价 {{ money(data.medicine?.price) }}</p></div></div></div>
      <fieldset :disabled="saving" class="admin-form-fields"><section v-for="section in config.sections" :key="section.title" class="admin-form-section"><h2>{{ section.title }}</h2><div class="admin-form-grid"><label v-for="field in section.fields" :key="field.key" class="admin-field" :class="{full: field.full}"><span>{{ field.label }} <span v-if="field.required" class="admin-required">*</span></span><select v-if="field.type === 'select'" v-model="form[field.key]" :required="field.required"><option value="">{{ field.required ? '请选择' : field.key === 'departmentId' ? '未设置 / 全院通用' : '未设置' }}</option><option v-for="option in options(field)" :key="String(option.value)" :value="option.value">{{ option.label }}</option></select><textarea v-else-if="field.type === 'textarea'" v-model="form[field.key]" :required="field.required" rows="4"></textarea><input v-else v-model="form[field.key]" :type="field.type || 'text'" :required="field.required" :min="minimum(field)" :step="field.step" :max="field.key === 'productionDate' ? form.expiryDate : undefined" /><small v-if="field.hint">{{ field.hint }}</small></label></div></section></fieldset>
      <div class="admin-form-footer"><button type="submit" class="button button-primary" :disabled="saving"><UiIcon name="check" :size="17" />{{ saving ? '提交中…' : resource === 'inbound' ? '确认入库' : '保存信息' }}</button><RouterLink class="button button-secondary" :to="`/app${config.back}`">取消</RouterLink></div>
    </form><aside class="panel admin-help-panel"><h2><UiIcon name="clipboard" :size="18" />填写说明</h2><p>带 * 的字段为必填项，请核对后保存。</p><ul><li v-if="resource === 'medicines'">规格、单位及零售价用于处方开立与费用计算。</li><li v-if="resource === 'inbound'">提交入库后会更新药品库存，批次记录可在入库记录中追溯。</li><li v-if="resource === 'schedules'">日期区间内每日生成指定班次的普通门诊排班；专家号源大于 0 时额外生成专家排班。</li><li v-if="resource === 'beds'">床位状态请与实际住院分配记录保持一致。</li><li v-if="resource === 'examinations'">科室归属用于医生开单时筛选检查项目。</li></ul><p>保存成功后返回业务列表。</p></aside></div>
  </div>
</template>
