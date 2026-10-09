<script setup lang="ts">
import LoginForm from "@/components/LoginForm.vue";
import UiIcon from "@/components/UiIcon.vue";
</script>

<template>
  <main id="staff-main" class="staff-main staff-login" tabindex="-1">
    <div class="staff-login-surface">
      <aside class="staff-login-scene" aria-hidden="true">
        <div class="staff-scene-lines"></div>
        <div class="staff-scene-symbol"><UiIcon name="hospital" :size="136" /></div>
        <div class="staff-scene-caption">
          <span>院内工作空间</span>
          <p>门诊诊疗与医院运营</p>
        </div>
      </aside>
      <section class="staff-login-panel auth-card" aria-label="医务账号登录">
        <LoginForm title="医务账号登录" description="请使用医院开通的医生或管理员账号。" />
        <p class="staff-login-help"><UiIcon name="shield" :size="16" /><span>账号无法登录？请联系医院管理员。</span></p>
        <p class="auth-register">尚未开通医生账号？<RouterLink to="/app/register">提交申请</RouterLink></p>
      </section>
    </div>
  </main>
</template>
