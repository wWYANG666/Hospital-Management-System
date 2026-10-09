<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { loadPage, submitAction } from "@/lib/api";
import UiIcon from "@/components/UiIcon.vue";

interface DepartmentOption { id?: number; name: string }
const router = useRouter();
const departments = ref<DepartmentOption[]>([]);
const optionsLoading = ref(true);
const optionsError = ref("");
const busy = ref(false);
const error = ref("");
const confirmPassword = ref("");
const form = reactive({
  username: "",
  password: "",
  realName: "",
  department: "",
  title: "",
  specialty: "",
  introduction: "",
  email: "",
  phone: "",
});

async function loadOptions() {
  optionsLoading.value = true;
  optionsError.value = "";
  try {
    const data = await loadPage<{ departments: DepartmentOption[] }>("/auth/registration-options");
    departments.value = data.departments || [];
  } catch (cause) {
    optionsError.value = cause instanceof Error ? cause.message : "科室信息加载失败，请重试。";
  } finally {
    optionsLoading.value = false;
  }
}

async function save() {
  if (busy.value || optionsLoading.value || !departments.value.length) return;
  error.value = "";
  if (form.password !== confirmPassword.value) {
    error.value = "两次密码输入不一致，请重新确认。";
    return;
  }
  busy.value = true;
  try {
    await submitAction("/auth/register/doctor", form);
    await router.push({ path: "/app/login", query: { registered: "1" } });
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "申请提交失败，请稍后重试。";
  } finally {
    busy.value = false;
  }
}
onMounted(loadOptions);
</script>

<template>
  <main id="staff-main" class="staff-main staff-registration" tabindex="-1">
    <section class="panel registration-card">
      <div class="registration-heading">
        <RouterLink to="/app/login"><UiIcon name="chevron-left" :size="16" />返回医务登录</RouterLink>
        <h1>医生账号申请</h1>
        <p>填写本人账号与执业信息，管理员审核通过后方可登录。</p>
      </div>
      <form @submit.prevent="save">
        <fieldset class="staff-register-fields" :disabled="busy">
          <h2 class="form-section-title">账号信息</h2>
          <div class="form-grid">
            <label class="field"><span>用户名 <b>*</b></span><input v-model="form.username" required minlength="3" maxlength="50" autocomplete="username" placeholder="设置登录用户名" /></label>
            <label class="field"><span>姓名 <b>*</b></span><input v-model="form.realName" required maxlength="50" autocomplete="name" placeholder="请输入本人姓名" /></label>
            <label class="field"><span>密码 <b>*</b></span><input v-model="form.password" type="password" required minlength="6" maxlength="72" autocomplete="new-password" placeholder="至少 6 位密码" /></label>
            <label class="field"><span>确认密码 <b>*</b></span><input v-model="confirmPassword" type="password" required minlength="6" maxlength="72" autocomplete="new-password" placeholder="请再次输入密码" /></label>
            <label class="field"><span>手机号码</span><input v-model="form.phone" type="tel" maxlength="20" autocomplete="tel" placeholder="用于院内联系" /></label>
            <label class="field"><span>邮箱</span><input v-model="form.email" type="email" maxlength="100" autocomplete="email" placeholder="请输入联系邮箱" /></label>
          </div>
          <h2 class="form-section-title">执业信息</h2>
          <div v-if="optionsError" class="inline-error" role="alert">
            {{ optionsError }} <button type="button" class="button button-small button-secondary" @click="loadOptions">重新加载</button>
          </div>
          <p v-else-if="!optionsLoading && !departments.length" class="inline-error" role="status">医院暂未配置科室，请联系管理员后再提交申请。</p>
          <div class="form-grid">
            <label class="field"><span>所属科室 <b>*</b></span><select v-model="form.department" required :disabled="optionsLoading || !departments.length">
              <option value="">{{ optionsLoading ? '正在加载科室…' : '请选择所属科室' }}</option>
              <option v-for="department in departments" :key="department.id || department.name" :value="department.name">{{ department.name }}</option>
            </select></label>
            <label class="field"><span>职称</span><input v-model="form.title" maxlength="50" placeholder="如：主治医师" /></label>
            <label class="field field-full"><span>专业特长</span><input v-model="form.specialty" maxlength="500" placeholder="填写主要诊疗方向与专业特长" /></label>
            <label class="field field-full"><span>个人简介</span><textarea v-model="form.introduction" maxlength="2000" placeholder="简要说明执业经历与专业背景"></textarea></label>
          </div>
        </fieldset>
        <div v-if="error" class="inline-error" role="alert">{{ error }}</div>
        <div class="form-actions">
          <RouterLink class="button button-secondary" to="/app/login">返回登录</RouterLink>
          <button type="submit" class="button button-primary" :disabled="busy || optionsLoading || !departments.length">{{ busy ? '正在提交…' : '提交申请' }}<UiIcon name="arrow-right" :size="16" /></button>
        </div>
      </form>
    </section>
  </main>
</template>

<style scoped>
.staff-register-fields { min-width: 0; border: 0; padding: 0; margin: 0; }
.staff-register-fields .form-section-title:first-child { margin-top: 0; }
</style>
