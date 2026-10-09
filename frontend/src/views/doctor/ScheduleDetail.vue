<script setup lang="ts">
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import EmptyState from "@/components/EmptyState.vue";
import DoctorFeedback from "./DoctorFeedback.vue";
import { useDoctorPage, list, mappedName } from "./useDoctorPage";
import "@/styles/doctor.css";
const route = useRoute();
const queueNumber = (value: string | number) => Number(value) + 1;
const { data, loading, error, loadFailed, load } = useDoctorPage(
  () => "/doctor/schedule-detail",
);
</script>
<template>
  <div class="doctor-view">
    <PageHeader
      :title="`${data.workTimeName || '班次'}预约详情`"
      description="今日该时段号源配置与预约患者。"
    >
      <RouterLink class="button button-secondary" to="/app/doctor/schedules"
        >返回排班管理</RouterLink
      >
    </PageHeader>
    <DoctorFeedback :loading="loading" :error="error" @retry="load" />
    <div class="doctor-tabs">
      <RouterLink
        v-for="time in [
          { id: 'MORNING', label: '上午' },
          { id: 'AFTERNOON', label: '下午' },
          { id: 'EVENING', label: '晚间' },
        ]"
        :key="time.id"
        :to="`/app/doctor/schedule-detail?workTime=${time.id}`"
        :class="{ active: route.query.workTime === time.id }"
        >{{ time.label }}</RouterLink
      >
    </div>
    <EmptyState
      v-if="!loading && !loadFailed && !list(data, 'scheduleDetails').length"
      title="该时段暂无排班"
    />
    <section
      v-for="(detail, index) in list(data, 'scheduleDetails')"
      :key="index"
      class="panel"
    >
      <div class="doctor-panel-heading">
        <div>
          <h2>{{ data.workTimeName }} · 时段 {{ index + 1 }}</h2>
          <p>
            已预约 {{ detail.usedAppointments }} /
            {{ detail.maxAppointments }} · 剩余
            {{ detail.availableAppointments }} 号源
          </p>
        </div>
      </div>
      <EmptyState v-if="!detail.appointments?.length" title="此时段暂无预约" />
      <div v-else class="doctor-patient-queue">
        <article
          v-for="(apt, n) in detail.appointments"
          :key="apt.id"
          class="doctor-queue-row"
        >
          <span class="doctor-queue-number">{{ queueNumber(n) }}</span>
          <div class="doctor-queue-person">
            <strong>{{ mappedName(data.userMap, apt.patientId) }}</strong>
            <p>{{ apt.symptoms || "未填写主诉" }}</p>
          </div>
          <div class="doctor-queue-time">
            <strong>{{ apt.appointmentTime || "时间待定" }}</strong
            ><StatusBadge :status="apt.status" />
          </div>
          <RouterLink
            v-if="['PENDING', 'CONFIRMED'].includes(apt.status)"
            class="button button-primary"
            :to="`/app/doctor/diagnose/${apt.id}`"
            >接诊</RouterLink
          >
        </article>
      </div>
    </section>
  </div>
</template>
