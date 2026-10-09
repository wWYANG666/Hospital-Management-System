<script setup lang="ts">
import { useRoute } from "vue-router";
import LoginForm from "@/components/LoginForm.vue";
import UiIcon from "@/components/UiIcon.vue";

const route = useRoute();
</script>

<template>
  <main class="patient-login-page">
    <section class="patient-login-intro">
      <span class="patient-eyebrow"><span></span>患者服务中心</span>
      <h1>你的就诊安排，<br />在这里查看</h1>
      <p>登录后即可预约门诊、查看挂号记录，<br class="desktop-break" />跟进处方缴费与检查报告。</p>
      <div class="patient-login-reminder">
        <UiIcon name="calendar" :size="22" />
        <div><strong>已有预约？</strong><span>登录后进入“我的就医”，查看就诊日期与时段。</span></div>
      </div>
    </section>
    <section class="auth-card patient-login-card">
      <div v-if="route.query.registered === '1'" class="patient-register-success" role="status">
        <UiIcon name="check" :size="17" />患者账号已创建，请使用新账号登录。
      </div>
      <LoginForm title="登录患者账号" description="使用本人账号，管理预约与就诊信息。" />
      <div class="auth-register">还没有账号？<RouterLink to="/app/register">注册患者账号</RouterLink></div>
      <p class="auth-security"><UiIcon name="shield" :size="14" />请妥善保护账号与个人就诊资料</p>
    </section>
  </main>
</template>
