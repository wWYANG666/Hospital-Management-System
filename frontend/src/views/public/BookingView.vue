<script setup lang="ts">
import AppointmentFinder from "@/components/AppointmentFinder.vue";
import UiIcon from "@/components/UiIcon.vue";
</script>

<template>
  <main class="patient-portal-home patient-booking-page">
    <header class="patient-home-heading">
      <span class="patient-eyebrow"><span></span>就医服务</span>
      <h1>预约挂号</h1>
      <p>按就诊需求选择科室或医生，查看出诊日期与可预约号源。</p>
    </header>
    <section class="patient-home-booking" aria-labelledby="booking-finder-title">
      <div class="patient-home-section-heading"><h2 id="booking-finder-title">查找门诊</h2><RouterLink to="/app/departments" class="patient-home-inline-link">科室与医生介绍<UiIcon name="chevron-right" :size="16" /></RouterLink></div>
      <AppointmentFinder />
    </section>
    <details class="patient-home-before-visit">
      <summary><UiIcon name="info" :size="18" /><span>首次预约须知</span><UiIcon name="chevron-down" :size="16" /></summary>
      <p>选择科室或医生后，登录患者账号继续选择出诊时段并核对就诊信息。预约成功后，请按预约日期及时段携带本人有效证件到院；需要调整时，可在“我的挂号”查看和取消预约。</p>
    </details>
  </main>
</template>
