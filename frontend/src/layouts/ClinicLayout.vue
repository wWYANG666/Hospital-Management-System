<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuth } from "@/stores/auth";
import UiIcon from "@/components/UiIcon.vue";
import { notify } from "@/lib/notifications";
import { isPatientPortal, portalTitle } from "@/lib/portal";
const auth = useAuth();
const route = useRoute();
const router = useRouter();
const open = ref(false);
const busy = ref(false);
const sidebar = ref<HTMLElement | null>(null);
const toggle = ref<HTMLButtonElement | null>(null);
const role = computed(() => auth.user.value?.role || "PATIENT");
const labels = {
  PATIENT: "患者服务中心",
  DOCTOR: "医生诊疗工作台",
  ADMIN: "医院运营管理",
};
const menu = computed(() =>
  role.value === "PATIENT"
    ? [
        ["服务首页", "dashboard", "hospital"],
        ["预约挂号", "appointments/new", "calendar"],
        ["我的挂号", "appointments", "clipboard"],
        ["处方与缴费", "prescriptions", "pill"],
        ["检查报告", "reports", "file"],
        ["住院服务", "hospitalizations", "bed"],
        ["账单记录", "payments/history", "credit-card"],
        ["个人资料", "profile", "user"],
      ]
    : role.value === "DOCTOR"
      ? [
          ["工作台", "dashboard", "activity"],
          ["门诊接诊", "diagnose", "stethoscope"],
          ["历史挂号", "appointments", "clipboard"],
          ["检查报告", "reports", "file"],
          ["住院管理", "hospitalizations", "bed"],
          ["我的排班", "schedules", "calendar"],
          ["执业资料", "profile", "user"],
        ]
      : [
          ["运营总览", "dashboard", "activity"],
          ["科室与医生", "departments", "building"],
          ["患者管理", "patients", "users"],
          ["挂号管理", "appointments", "clipboard"],
          ["全院排班", "schedules", "calendar"],
          ["药品库存", "medicines", "pill"],
          ["药品入库", "medicines/inbounds", "download"],
          ["检查项目", "examinations", "stethoscope"],
          ["床位管理", "beds", "bed"],
          ["住院管理", "hospitalizations", "hospital"],
          ["报告管理", "reports", "file"],
          ["AI 生成审计", "ai-logs", "shield"],
          ["管理员资料", "profile", "user"],
        ],
);
const prefix = computed(() => "/app/" + role.value.toLowerCase() + "/");
const groups = computed(() =>
  role.value === "ADMIN"
    ? [
        {
          title: "门诊业务",
          items: menu.value.slice(0, 5).concat([menu.value[10]!]),
        },
        { title: "资源维护", items: menu.value.slice(5, 8) },
        { title: "住院业务", items: menu.value.slice(8, 10) },
        { title: "系统管理", items: menu.value.slice(11) },
      ]
    : role.value === "DOCTOR"
      ? [
          { title: "门诊诊疗", items: menu.value.slice(0, 4) },
          { title: "住院与排班", items: menu.value.slice(4) },
        ]
      : [
          { title: "门诊服务", items: menu.value.slice(0, 5) },
          { title: "住院与资料", items: menu.value.slice(5) },
        ],
);
const activeItem = computed(
  () =>
    menu.value
      .filter(
        (item) =>
          route.path === prefix.value + item[1] ||
          route.path.startsWith(prefix.value + item[1] + "/"),
      )
      .sort((a, b) => b[1]!.length - a[1]!.length)[0]?.[1],
);
const date = new Intl.DateTimeFormat("zh-CN", {
  month: "long",
  day: "numeric",
  weekday: "long",
  timeZone: "Asia/Shanghai",
}).format(new Date());
function active(path: string) {
  return activeItem.value === path;
}
async function logout() {
  busy.value = true;
  try {
    await auth.logout();
    await router.replace("/app/login");
  } catch (error) {
    notify((error as Error).message, "error");
  } finally {
    busy.value = false;
  }
}
watch(
  () => route.fullPath,
  () => {
    open.value = false;
  },
);
watch(open, async (value) => {
  await nextTick();
  if (value)
    sidebar.value?.querySelector<HTMLElement>("nav a.active, nav a")?.focus();
  else if (window.innerWidth <= 760) toggle.value?.focus();
});
function closeDesktop() {
  if (window.innerWidth > 760) open.value = false;
}
onMounted(() => window.addEventListener("resize", closeDesktop));
onUnmounted(() => window.removeEventListener("resize", closeDesktop));
function trapSidebar(event: KeyboardEvent) {
  if (!open.value || event.key !== "Tab") return;
  const controls = [
    ...(sidebar.value?.querySelectorAll<HTMLElement>("a, button") || []),
  ];
  const first = controls[0],
    last = controls.at(-1);
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last?.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first?.focus();
  }
}
</script>
<template>
  <div
    class="clinic-app"
    :class="['clinic-' + role.toLowerCase(), { 'nav-open': open }]"
    @keydown.esc="open = false"
  >
    <button
      v-if="open"
      class="nav-backdrop"
      @click="open = false"
      aria-label="关闭导航"
    ></button>
    <aside
      ref="sidebar"
      id="workspace-navigation"
      class="clinic-sidebar"
      @keydown="trapSidebar"
    >
      <RouterLink to="/app" class="sidebar-brand"
        ><span class="brand-cross"><UiIcon name="hospital" :size="24" /></span
        ><span
          ><strong>{{
            isPatientPortal ? "患者门诊服务" : "医务管理工作台"
          }}</strong
          ><small>{{ labels[role] }}</small></span
        ></RouterLink
      >
      <button
        v-if="open"
        class="icon-button sidebar-close"
        @click="open = false"
        aria-label="关闭工作台导航"
      >
        <UiIcon name="close" :size="17" />
      </button>
      <nav aria-label="工作台导航">
        <section
          v-for="group in groups"
          :key="group.title"
          class="sidebar-nav-group"
        >
          <div class="sidebar-role">{{ group.title }}</div>
          <RouterLink
            v-for="item in group.items"
            :key="item[1]"
            :to="prefix + item[1]"
            :class="{ active: active(item[1]!) }"
            :aria-current="active(item[1]!) ? 'page' : undefined"
            ><UiIcon :name="item[2]" :size="19" /><span>{{ item[0] }}</span
            ><UiIcon v-if="active(item[1]!)" name="chevron-right" :size="14"
          /></RouterLink>
        </section>
      </nav>
      <div class="sidebar-bottom">
        <RouterLink to="/app"
          ><UiIcon name="hospital" :size="16" />{{
            isPatientPortal ? "返回预约首页" : "工作台首页"
          }}</RouterLink
        ><span>{{
          isPatientPortal ? "预约 · 报告 · 就医" : "门诊 · 住院 · 运营"
        }}</span>
      </div>
    </aside>
    <div class="clinic-content">
      <header class="clinic-topbar">
        <div class="topbar-breadcrumb">
          <button
            ref="toggle"
            class="icon-button mobile-menu-toggle"
            @click="open = !open"
            :aria-label="open ? '关闭工作台导航' : '打开工作台导航'"
            :aria-expanded="open"
            aria-controls="workspace-navigation"
          >
            <UiIcon name="menu" /></button
          ><span>{{ labels[role] }}</span
          ><UiIcon name="chevron-right" :size="14" /><strong>{{
            route.meta.title || "业务详情"
          }}</strong>
        </div>
        <div class="topbar-account">
          <span class="topbar-date">{{ date }}</span
          ><RouterLink :to="prefix + 'profile'" class="account-pill"
            ><span>{{ auth.user.value?.realName?.slice(0, 1) || "医" }}</span
            ><strong>{{ auth.user.value?.realName }}</strong></RouterLink
          ><button
            class="icon-button"
            :disabled="busy"
            @click="logout"
            aria-label="退出登录"
            title="退出登录"
          >
            <UiIcon name="logout" :size="18" />
          </button>
        </div>
      </header>
      <main class="workspace-main">
        <RouterView v-slot="{ Component }"
          ><component :is="Component" :key="route.path"
        /></RouterView>
      </main>
      <footer class="workspace-footer">
        <span>{{ portalTitle }}</span
        ><span>{{ isPatientPortal ? '请妥善保护个人就诊资料，使用后及时退出' : '请保护患者资料，完成工作后及时退出账号' }}</span>
      </footer>
    </div>
  </div>
</template>
