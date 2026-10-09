<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { submitAction } from "@/lib/api";
import UiIcon from "@/components/UiIcon.vue";
const router = useRouter();
const busy = ref(false);
const error = ref("");
const confirmPassword = ref("");
const form = reactive({
  username: "",
  password: "",
  realName: "",
  phone: "",
  email: "",
  idCard: "",
  gender: "",
  birthday: "",
  address: "",
  emergencyContact: "",
  emergencyPhone: "",
});
async function save() {
  if (busy.value) return;
  error.value = "";
  if (form.password !== confirmPassword.value) {
    error.value = "两次密码输入不一致";
    return;
  }
  busy.value = true;
  try {
    await submitAction("/auth/register/patient", form);
    await router.push({ path: "/app/login", query: { registered: "1" } });
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <main class="public-inner registration-page">
    <section class="panel registration-card">
      <div class="registration-heading">
        <RouterLink to="/app/login"
          ><UiIcon name="chevron-left" :size="16" />返回登录</RouterLink
        >
        <h1>注册患者账号</h1>
        <p>创建账号后即可预约门诊、查看本人的就诊信息。</p>
      </div>
      <form @submit.prevent="save">
        <h3 class="form-section-title">账号信息</h3>
        <div class="form-grid">
          <label class="field"
            ><span>用户名 <b>*</b></span
            ><input
              v-model="form.username"
              required
              minlength="3"
              maxlength="50"
              autocomplete="username"
              placeholder="设置登录用户名" /></label
          ><label class="field"
            ><span>姓名 <b>*</b></span
            ><input
              v-model="form.realName"
              required
              maxlength="50"
              autocomplete="name"
              placeholder="请输入本人姓名" /></label
          ><label class="field"
            ><span>密码 <b>*</b></span
            ><input
              v-model="form.password"
              type="password"
              required
              minlength="6"
              maxlength="72"
              autocomplete="new-password"
              placeholder="至少 6 位密码" /></label
          ><label class="field"
            ><span>确认密码 <b>*</b></span
            ><input
              v-model="confirmPassword"
              type="password"
              required
              minlength="6"
              autocomplete="new-password"
              placeholder="请再次输入密码" /></label
          ><label class="field"
            ><span>手机号码</span
            ><input
              v-model="form.phone"
              type="tel"
              maxlength="20"
              autocomplete="tel" /></label
          ><label class="field"
            ><span>邮箱</span
            ><input
              v-model="form.email"
              type="email"
              maxlength="100"
              autocomplete="email"
          /></label>
        </div>
        <details class="patient-register-details">
          <summary>补充就诊人资料 <span>选填，也可登录后完善</span><UiIcon name="chevron-down" :size="16" /></summary>
          <div class="form-grid">
          <label class="field"
            ><span>证件号码</span
            ><input v-model="form.idCard" maxlength="30" /></label
          ><label class="field"
            ><span>性别</span
            ><select v-model="form.gender">
              <option value="">请选择</option>
              <option value="MALE">男</option>
              <option value="FEMALE">女</option>
            </select></label
          ><label class="field"
            ><span>出生日期</span
            ><input v-model="form.birthday" type="date" /></label
          ><label class="field"
            ><span>联系地址</span
            ><input
              v-model="form.address"
              autocomplete="street-address" /></label
          ><label class="field"
            ><span>紧急联系人</span
            ><input v-model="form.emergencyContact" /></label
          ><label class="field"
            ><span>紧急联系电话</span
            ><input v-model="form.emergencyPhone" type="tel"
          /></label>
        </div>
        </details>
        <div v-if="error" role="alert" class="inline-error">{{ error }}</div>
        <div class="form-actions">
          <RouterLink class="button button-secondary" to="/app/login"
            >返回登录</RouterLink
          ><button class="button button-primary" :disabled="busy">
            {{ busy ? "正在提交…" : "创建患者账号" }}<UiIcon name="arrow-right" :size="16" />
          </button>
        </div>
      </form>
    </section>
  </main>
</template>
