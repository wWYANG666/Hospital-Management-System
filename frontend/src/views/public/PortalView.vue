<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import UiIcon from "@/components/UiIcon.vue";
import { loadPage } from "@/lib/api";
import { hospitalProfile } from "@/lib/hospital";
import "@/styles/hospital-home.css";

interface Department {
  id: number;
  name: string;
  description?: string | null;
}
interface Directory {
  departments: Department[];
}

const departments = ref<Department[]>([]);
const loading = ref(true);
const error = ref("");
const featuredDepartments = computed(() => departments.value.slice(0, 4));

async function loadDepartments() {
  loading.value = true;
  error.value = "";
  try {
    const directory = await loadPage<Directory>("/public/directory");
    departments.value = directory.departments.filter((department) => department.name?.trim());
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "科室介绍暂时无法加载，请稍后重试。";
  } finally {
    loading.value = false;
  }
}

function departmentDescription(department: Department) {
  return department.description?.trim() || "为患者提供本科室相关诊疗服务，具体诊疗范围请以科室介绍为准。";
}

onMounted(loadDepartments);
</script>

<template>
  <main class="hospital-home">
    <section class="hospital-home-hero" aria-labelledby="hospital-home-title">
      <img class="hospital-home-hero-image" src="/images/hospital-outpatient.png"
        alt="医院门诊建筑形象" fetchpriority="high" width="1536" height="1024" />
      <div class="hospital-home-hero-shade" aria-hidden="true"></div>
      <div class="hospital-home-hero-content">
        <span class="hospital-home-eyebrow">{{ hospitalProfile.englishName }}</span>
        <h1 id="hospital-home-title">{{ hospitalProfile.name }}</h1>
        <p class="hospital-home-tagline">{{ hospitalProfile.tagline }}</p>
        <p class="hospital-home-hero-description">关注健康，更关心每一位患者的感受。</p>
        <div class="hospital-home-hero-principles" aria-label="医院理念">
          <span>专业诊疗</span><span>人文关怀</span><span>用心服务</span>
        </div>
      </div>
      <span class="hospital-home-image-caption">医院建筑形象</span>
    </section>

    <section id="hospital-overview" class="hospital-home-overview hospital-home-container"
      tabindex="-1" aria-labelledby="hospital-overview-title">
      <div class="hospital-home-section-heading">
        <span class="hospital-home-eyebrow">ABOUT OUR HOSPITAL</span>
        <h2 id="hospital-overview-title">了解{{ hospitalProfile.name }}</h2>
        <p>以医疗为本，以关怀相伴。</p>
      </div>
      <div class="hospital-home-overview-content">
        <p class="hospital-home-introduction">{{ hospitalProfile.introduction }}</p>
        <div class="hospital-home-overview-note">
          <UiIcon name="heart" :size="27" />
          <div><h3>把患者的需要放在心上</h3><p>尊重每一位患者，认真倾听、耐心沟通，让医疗服务更贴近人的需要。</p></div>
        </div>
      </div>
    </section>

    <section id="hospital-departments" class="hospital-home-departments hospital-home-container"
      tabindex="-1" aria-labelledby="hospital-departments-title" :aria-busy="loading">
      <div class="hospital-home-section-heading">
        <span class="hospital-home-eyebrow">OUR DEPARTMENTS</span>
        <h2 id="hospital-departments-title">科室介绍</h2>
        <p>了解医院的诊疗科室与服务方向。</p>
      </div>
      <p v-if="loading" class="hospital-home-department-state" role="status">正在加载科室介绍…</p>
      <div v-else-if="error" class="hospital-home-department-state hospital-home-department-error" role="alert">
        <UiIcon name="alert" :size="21" /><p>{{ error }}</p>
        <button class="button button-secondary" type="button" @click="loadDepartments"><UiIcon name="refresh" :size="16" />重新加载</button>
      </div>
      <div v-else-if="featuredDepartments.length" class="hospital-home-department-list">
        <article v-for="department in featuredDepartments" :key="department.id" class="hospital-home-department">
          <span class="hospital-home-department-symbol" aria-hidden="true"><UiIcon name="stethoscope" :size="24" /></span>
          <h3>{{ department.name }}</h3>
          <p>{{ departmentDescription(department) }}</p>
        </article>
      </div>
      <p v-else class="hospital-home-department-state" role="status">暂无科室介绍。</p>
    </section>

    <section class="hospital-home-care" aria-labelledby="hospital-care-title">
      <div class="hospital-home-container hospital-home-care-inner">
        <div class="hospital-home-section-heading">
          <span class="hospital-home-eyebrow">CARE BEYOND TREATMENT</span>
          <h2 id="hospital-care-title">让诊疗更有温度</h2>
          <p>从认真倾听开始，关注每一次就医体验。</p>
        </div>
        <div class="hospital-home-care-list">
          <article><UiIcon name="users" :size="25" /><h3>尊重与倾听</h3><p>关注患者的感受与需求，以尊重和理解建立信任。</p></article>
          <article><UiIcon name="heart" :size="25" /><h3>沟通与关怀</h3><p>重视清晰的诊疗沟通，让患者更好地理解自己的健康状况。</p></article>
          <article><UiIcon name="building" :size="25" /><h3>环境与秩序</h3><p>关注诊疗环境与服务细节，为舒适、有序的就医体验持续努力。</p></article>
        </div>
      </div>
    </section>
  </main>
</template>
