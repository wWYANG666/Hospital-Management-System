<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import UiIcon from '@/components/UiIcon.vue'
import EmptyState from '@/components/EmptyState.vue'
import AdminFeedback from './AdminFeedback.vue'
import AdminPagination from './AdminPagination.vue'
import { useAdminPage, useAdminPagination, items, entry } from './useAdminPage'
import { statusText } from '@/lib/format'
const route = useRoute(), router = useRouter(), keyword = ref(String(route.query.keyword || ''))
const {data,loading,error,load} = useAdminPage(() => '/admin/patients')
const patients = computed(() => items(data.value,'patients'))
const { page, totalPages, pageRows } = useAdminPagination(patients)
async function search() {
  const query = keyword.value.trim() ? { keyword: keyword.value.trim() } : {}
  if (String(route.query.keyword || '') === keyword.value.trim()) await load()
  else await router.replace({ path: '/app/admin/patients', query })
}
</script>
<template><div class="admin-view"><PageHeader title="患者档案中心" description="检索患者身份资料，统一查看挂号记录、检查报告、处方与住院记录。"><button class="button button-secondary" :disabled="loading" @click="load">刷新档案</button></PageHeader><AdminFeedback :loading="loading" :error="error" @retry="load" /><section v-if="!loading" class="panel"><div class="admin-panel-heading"><div><h2>患者列表 <span class="admin-count">{{ patients.length }}</span></h2><p>按姓名或身份证检索，进入详情处理就诊业务</p></div></div><form class="admin-toolbar" @submit.prevent="search"><label class="admin-search"><UiIcon name="search" :size="17" /><input v-model="keyword" type="search" placeholder="输入患者姓名或身份证号" aria-label="姓名或身份证" /></label><button class="button button-primary" type="submit">查询患者</button><button v-if="keyword || route.query.keyword" class="button button-secondary" type="button" @click="keyword='';search()">清空筛选</button><span class="admin-result-meta">{{ route.query.keyword ? '查询结果' : '全部档案' }} · {{ patients.length }} 人</span></form><EmptyState v-if="!patients.length" title="未找到患者档案" description="请调整检索条件，新的患者注册后会显示在此列表。" /><div v-else class="table-scroll"><table class="admin-table"><thead><tr><th>患者 / 编号</th><th>性别</th><th>身份证号</th><th>联系手机</th><th>登录账号</th><th>操作</th></tr></thead><tbody><tr v-for="p in pageRows" :key="p.id"><td><strong>{{ entry(data,'userMap',p.id).realName || '姓名待登记' }}</strong><small>{{ p.patientCode || `患者 #${p.id}` }}</small></td><td>{{ statusText(p.gender) }}</td><td class="admin-id">{{ p.idCard || '未登记' }}</td><td>{{ entry(data,'userMap',p.id).phone || '未登记' }}</td><td>{{ entry(data,'userMap',p.id).username || '未关联' }}</td><td><RouterLink class="admin-link" :to="`/app/admin/patients/${p.id}`">查看就诊档案 <UiIcon name="arrow-right" :size="15" /></RouterLink></td></tr></tbody></table></div><AdminPagination v-model:page="page" :total-pages="totalPages" :total="patients.length" /></section></div></template>
