<script setup lang="ts">
import PageHeader from '@/components/PageHeader.vue'
import UiIcon from '@/components/UiIcon.vue'
import EmptyState from '@/components/EmptyState.vue'
import AdminFeedback from './AdminFeedback.vue'
import {useAdminPage} from './useAdminPage'
const {data,loading,error,load}=useAdminPage(()=>'/admin/profile')
</script>
<template><div class="admin-view"><PageHeader title="管理员账户" description="查看当前账号资料与管理身份。"><RouterLink v-if="data.admin?.id" class="button button-primary" :to="{path:`/app/admin/users/${data.admin.id}/change-password`,query:{returnTo:'/admin/profile'}}"><UiIcon name="shield" :size="17" />修改登录密码</RouterLink></PageHeader><AdminFeedback :loading="loading" :error="error" @retry="load" /><section v-if="!loading" class="panel admin-profile-card"><template v-if="data.admin"><div class="admin-person-summary"><span class="admin-person-avatar">{{ String(data.admin.realName || data.admin.username || '管').slice(0,1) }}</span><div><h2>{{ data.admin.realName || data.admin.username }}</h2><p><UiIcon name="shield" :size="14" />医院系统管理员</p></div></div><dl class="admin-info-grid"><div><dt>登录账号</dt><dd>{{ data.admin.username }}</dd></div><div><dt>姓名</dt><dd>{{ data.admin.realName || '未登记' }}</dd></div><div><dt>手机号</dt><dd>{{ data.admin.phone || '未登记' }}</dd></div><div><dt>邮箱</dt><dd>{{ data.admin.email || '未登记' }}</dd></div><div><dt>账号角色</dt><dd>管理员</dd></div><div><dt>账号状态</dt><dd>{{ data.admin.status===1?'已启用':'未启用' }}</dd></div></dl></template><EmptyState v-else title="未能获取账户资料" description="请刷新页面或重新登录。" /></section></div></template>
