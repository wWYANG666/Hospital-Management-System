<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import UiIcon from "@/components/UiIcon.vue";
import { useAuth } from "@/stores/auth";
import { patientServiceGroups, patientServiceId } from "@/lib/patient-services";
import "@/styles/patient-service.css";

const route = useRoute();
const { user } = useAuth();
const activeService = computed(() => patientServiceId(route.path, String(route.params.type || "")));
</script>

<template>
  <div class="patient-service-shell">
    <aside class="patient-service-sidebar">
      <header class="patient-service-brand"><UiIcon name="heart" :size="25" /><div><strong>就医服务</strong><small>门诊 · 住院 · 就医记录</small></div></header>
      <nav class="patient-service-navigation" aria-label="就医服务导航">
        <section v-for="group in patientServiceGroups" :key="group.label" class="patient-service-nav-group">
          <h2>{{ group.label }}</h2>
          <div class="patient-service-nav-links">
            <RouterLink v-for="item in group.links" :key="item.id" :to="item.path" class="patient-service-nav-link" :class="{ active: activeService === item.id }" :aria-current="activeService === item.id ? 'page' : undefined"><UiIcon :name="item.icon" :size="18" /><span>{{ item.label }}</span></RouterLink>
          </div>
        </section>
      </nav>
      <p v-if="!user" class="patient-service-sidebar-note"><UiIcon name="shield" :size="16" />个人就医信息需登录后查看</p>
    </aside>
    <main id="patient-service-main" class="patient-service-content">
      <RouterView v-slot="{ Component }"><component :is="Component" :key="route.path" /></RouterView>
    </main>
  </div>
</template>
