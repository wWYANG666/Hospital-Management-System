<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/stores/auth'
import { hospitalProfile } from '@/lib/hospital'
import { notify } from '@/lib/notifications'
import UiIcon from '@/components/UiIcon.vue'
import '@/styles/patient-portal.css'
import '@/styles/patient-navigation.css'

const auth = useAuth()
const route = useRoute()
const router = useRouter()
const menuOpen = ref(false)
const accountOpen = ref(false)
const logoutBusy = ref(false)
const account = ref<HTMLElement | null>(null)
const accountButton = ref<HTMLButtonElement | null>(null)
const signedIn = computed(() => auth.user.value?.role === 'PATIENT')
const inServiceArea = computed(() => route.meta.patientServices === true)
function closeMenus() {
  menuOpen.value = false
  accountOpen.value = false
}
watch(() => route.fullPath, closeMenus)
async function toggleAccount() {
  accountOpen.value = !accountOpen.value
  if (accountOpen.value) {
    await nextTick()
    account.value?.querySelector<HTMLElement>('[role=menuitem]')?.focus()
  }
}
function closeAccount() { accountOpen.value = false; accountButton.value?.focus() }
function accountKeys(event: KeyboardEvent) {
  if (!accountOpen.value || !['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) return
  const items = [...(account.value?.querySelectorAll<HTMLElement>('[role=menuitem]') || [])]
  if (!items.length) return
  event.preventDefault()
  const index = items.indexOf(document.activeElement as HTMLElement)
  const next = event.key === 'Home' ? 0 : event.key === 'End' ? items.length - 1 : (index + (event.key === 'ArrowDown' ? 1 : -1) + items.length) % items.length
  items[next]?.focus()
}
function outside(event: PointerEvent) {
  if (!account.value?.contains(event.target as Node)) accountOpen.value = false
}
onMounted(() => document.addEventListener('pointerdown', outside))
onUnmounted(() => document.removeEventListener('pointerdown', outside))
async function logout() {
  if (logoutBusy.value) return
  logoutBusy.value = true
  try { await auth.logout(); closeMenus(); await router.replace('/app/login') }
  catch (cause) { notify(cause instanceof Error ? cause.message : '退出失败，请重试', 'error') }
  finally { logoutBusy.value = false }
}
</script>

<template>
  <div class="public-app patient-public-app" @keydown.esc="closeMenus">
    <header class="public-header">
      <div class="public-header-inner">
        <RouterLink class="hospital-brand" to="/app" aria-label="医院首页"><span class="brand-cross"><UiIcon name="hospital" :size="25" /></span><span><strong>{{ hospitalProfile.name }}</strong><small>{{ hospitalProfile.englishName }}</small></span></RouterLink>
        <button class="icon-button mobile-menu-toggle" type="button" @click="menuOpen = !menuOpen" :aria-label="menuOpen ? '关闭导航' : '打开导航'" :aria-expanded="menuOpen" aria-controls="public-navigation"><UiIcon name="menu" /></button>
        <nav id="public-navigation" :class="{ open: menuOpen }" aria-label="医院导航">
          <RouterLink to="/app" :class="{ active: route.path === '/app' && !route.hash }" :aria-current="route.path === '/app' && !route.hash ? 'page' : undefined" @click="closeMenus">首页</RouterLink>
          <RouterLink to="/app#hospital-overview" :class="{ active: route.path === '/app' && route.hash === '#hospital-overview' }" :aria-current="route.path === '/app' && route.hash === '#hospital-overview' ? 'location' : undefined" @click="closeMenus">医院概况</RouterLink>
          <RouterLink to="/app#hospital-departments" :class="{ active: route.path === '/app' && route.hash === '#hospital-departments' }" :aria-current="route.path === '/app' && route.hash === '#hospital-departments' ? 'location' : undefined" @click="closeMenus">特色科室</RouterLink>
          <RouterLink to="/app/guide" :class="{ active: route.path === '/app/guide' }" :aria-current="route.path === '/app/guide' ? 'page' : undefined" @click="closeMenus">就医指南</RouterLink>
        </nav>
        <RouterLink to="/app/booking" class="button button-secondary patient-service-entry" :class="{ active: inServiceArea }" :aria-current="inServiceArea ? 'page' : undefined"><UiIcon name="calendar" :size="16" />就医服务</RouterLink>
        <div v-if="signedIn" ref="account" class="patient-account" @keydown="accountKeys" @keydown.esc.stop="closeAccount">
          <button ref="accountButton" type="button" class="patient-account-toggle" aria-label="账号菜单" aria-haspopup="menu" :aria-expanded="accountOpen" aria-controls="patient-account-menu" @click="toggleAccount"><span class="patient-account-avatar">{{ auth.user.value?.realName?.slice(0, 1) || '患' }}</span><span class="patient-account-name">{{ auth.user.value?.realName || auth.user.value?.username }}</span><UiIcon name="chevron-down" :size="13" /></button>
          <div v-if="accountOpen" id="patient-account-menu" class="patient-account-menu" role="menu"><RouterLink role="menuitem" to="/app/patient/profile"><UiIcon name="user" :size="16" />个人资料</RouterLink><button role="menuitem" type="button" :disabled="logoutBusy" @click="logout"><UiIcon name="logout" :size="16" />{{ logoutBusy ? '正在退出…' : '退出登录' }}</button></div>
        </div>
        <RouterLink v-else-if="route.path !== '/app/login'" to="/app/login" class="button button-secondary header-account"><UiIcon name="user" :size="16" />登录</RouterLink>
      </div>
    </header>
    <RouterView />
    <footer class="public-footer patient-public-footer"><div class="footer-brand"><UiIcon name="hospital" :size="18" /><strong>{{ hospitalProfile.name }}</strong></div><span class="hospital-footer-purpose">{{ hospitalProfile.tagline }}</span><div v-if="hospitalProfile.address || hospitalProfile.phone" class="hospital-footer-contact"><span v-if="hospitalProfile.address">{{ hospitalProfile.address }}</span><a v-if="hospitalProfile.phone" :href="'tel:' + hospitalProfile.phone">{{ hospitalProfile.phone }}</a></div></footer>
  </div>
</template>
