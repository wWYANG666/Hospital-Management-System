<script setup lang="ts">
import {computed,ref} from 'vue'
import {useRoute} from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import UiIcon from '@/components/UiIcon.vue'
import AdminFeedback from './AdminFeedback.vue'
import {useAdminPage,appPath} from './useAdminPage'
const route=useRoute(),password=ref(''),confirmation=ref('')
const {data,loading,saving,error,notice,load,action}=useAdminPage(()=>`/admin/users/${route.params.id}/change-password`)
const back=computed(()=>data.value.returnTo || (data.value.departmentId?`/admin/departments/${data.value.departmentId}/edit`:'/admin/departments'))
async function save(){if(password.value!==confirmation.value){error.value='两次输入的密码不一致';return}await action(`/admin/users/${route.params.id}/change-password`,{newPassword:password.value,departmentId:data.value.departmentId,returnTo:data.value.returnTo},back.value)}
</script>
<template><div class="admin-view"><PageHeader title="修改账户密码" description="为指定用户设置新的登录密码。"><RouterLink class="button button-secondary" :to="appPath(back)"><UiIcon name="chevron-left" :size="17" />返回</RouterLink></PageHeader><AdminFeedback :loading="loading" :error="error" :notice="notice" @retry="load" /><form v-if="!loading" class="panel admin-profile-card" @submit.prevent="save"><div class="admin-panel-heading"><div><h2>{{ data.user?.realName || data.user?.username || '用户账户' }}</h2><p>登录账号：{{ data.user?.username || '未关联' }}</p></div><UiIcon name="shield" :size="24" /></div><section class="admin-form-section"><div class="admin-form-grid"><label class="admin-field full"><span>新密码 *</span><input v-model="password" type="password" autocomplete="new-password" minlength="6" required placeholder="输入新密码，至少 6 位" /></label><label class="admin-field full"><span>确认新密码 *</span><input v-model="confirmation" type="password" autocomplete="new-password" minlength="6" required placeholder="再次输入新密码" /></label></div></section><div class="admin-form-footer"><button class="button button-primary" type="submit" :disabled="saving">{{ saving?'更新中…':'更新密码' }}</button><RouterLink class="button button-secondary" :to="appPath(back)">取消</RouterLink></div></form></div></template>
