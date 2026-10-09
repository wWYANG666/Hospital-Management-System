<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from "vue";
import { onBeforeRouteLeave } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import PatientPageState from "./PatientPageState.vue";
import { usePatientPage } from "./usePatientPage";
import { useAuth } from "@/stores/auth";
import { formatDate, statusText, text } from "@/lib/format";
import { confirmAction } from "@/lib/confirm";
const auth = useAuth();
const { data, loading, error, message, submitting, reload, action } =
  usePatientPage(() => "/patient/profile");
const form = reactive<Record<string, string>>({
  realName: "",
  email: "",
  phone: "",
  idCard: "",
  gender: "",
  birthday: "",
  address: "",
  emergencyContact: "",
  emergencyPhone: "",
});
const saved = ref("");
const dirty = computed(() => !!saved.value && JSON.stringify(form) !== saved.value);
const today = new Date();
const latestBirthday = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
function resetForm() {
  for (const key of ["realName", "email", "phone"])
    form[key] = String(data.value.user?.[key] || "");
  for (const key of [
    "idCard",
    "gender",
    "birthday",
    "address",
    "emergencyContact",
    "emergencyPhone",
  ])
    form[key] = String(data.value.patient?.[key] || "");
  saved.value = JSON.stringify(form);
}
watch(data, () => { if (data.value.user) resetForm(); });
onBeforeRouteLeave(async () => !dirty.value || await confirmAction("您修改的就诊人资料尚未保存，确认放弃修改并离开？", "资料尚未保存"));
function beforeUnload(event: BeforeUnloadEvent) {
  if (!dirty.value) return;
  event.preventDefault();
  event.returnValue = "";
}
window.addEventListener("beforeunload", beforeUnload);
onBeforeUnmount(() => window.removeEventListener("beforeunload", beforeUnload));
async function retryProfile() {
  if (dirty.value && !await confirmAction("重新加载会恢复已保存的资料，当前修改将被放弃。", "重新加载资料？")) return;
  await reload();
}
const initial = computed(() =>
  String(data.value.user?.realName || data.value.user?.username || "患").slice(
    0,
    1,
  ),
);
async function save() {
  if (!dirty.value || loading.value) return;
  form.realName = form.realName.trim();
  if (await action("/patient/profile", form, "就诊人资料已更新")) {
    saved.value = JSON.stringify(form);
    await auth.refresh();
  }
}
</script>
<template>
  <div class="patient-experience">
    <PageHeader
      title="就诊人资料"
      description="维护患者档案与联系方式，为预约及诊疗提供准确的信息。"
    /><PatientPageState
      :loading="loading"
      :error="error"
      :message="message"
      @retry="retryProfile"
    />
    <div v-if="data.user" class="patient-profile-layout">
      <aside class="panel patient-profile-summary">
        <div class="patient-profile-avatar">{{ initial }}</div>
        <h2>{{ data.user?.realName || data.user?.username || "就诊人" }}</h2>
        <p>{{ text(data.user?.username) }}</p>
        <span class="patient-type-label">患者账号</span>
        <dl class="patient-facts">
          <div>
            <dt>联系电话</dt>
            <dd>{{ text(data.user?.phone) }}</dd>
          </div>
          <div>
            <dt>性别</dt>
            <dd>{{ statusText(data.patient?.gender) }}</dd>
          </div>
          <div>
            <dt>出生日期</dt>
            <dd>{{ formatDate(data.patient?.birthday) }}</dd>
          </div>
        </dl>
        <div class="patient-fine-note">
          <UiIcon name="shield" :size="18" /><span
            >个人资料用于本系统内的医疗服务与联系。</span
          >
        </div>
      </aside>
      <section class="panel">
        <form @submit.prevent="save">
          <fieldset class="patient-form-fields" :disabled="submitting || loading">
          <div class="patient-section-heading">
            <div>
              <h2>基本信息</h2>
              <p>姓名与手机号用于确认就诊身份及联系</p>
            </div>
          </div>
          <div class="form-grid">
            <label class="field"
              ><span>用户名</span
              ><input
                :value="data.user?.username"
                readonly
                aria-readonly="true" /></label
            ><label class="field"
              ><span>真实姓名 <b>*</b></span
              ><input
                v-model="form.realName"
                name="realName"
                required
                maxlength="50"
                autocomplete="name" /></label
            ><label class="field"
              ><span>手机号</span
              ><input
                v-model="form.phone"
                name="phone"
                type="tel"
                maxlength="20"
                minlength="7"
                autocomplete="tel" /></label
            ><label class="field"
              ><span>邮箱</span
              ><input
                v-model="form.email"
                name="email"
                type="email"
                autocomplete="email"
            /></label>
          </div>
          <h3 class="patient-detail-subtitle">患者档案</h3>
          <div class="form-grid">
            <label class="field"
              ><span>身份证号</span
              ><input
                v-model="form.idCard"
                name="idCard"
                maxlength="18" /></label
            ><label class="field"
              ><span>性别</span
              ><select v-model="form.gender" name="gender">
                <option value="">请选择</option>
                <option value="MALE">男</option>
                <option value="FEMALE">女</option>
              </select></label
            ><label class="field"
              ><span>出生日期</span
              ><input
                v-model="form.birthday"
                name="birthday"
                type="date"
                :max="latestBirthday" /></label
            ><label class="field"
              ><span>联系地址</span
              ><input
                v-model="form.address"
                name="address"
                autocomplete="street-address"
            /></label>
          </div>
          <h3 class="patient-detail-subtitle">紧急联系人</h3>
          <p class="patient-muted">便于需要时与您的家属或联系人沟通。</p>
          <div class="form-grid">
            <label class="field"
              ><span>联系人姓名</span
              ><input
                v-model="form.emergencyContact"
                name="emergencyContact" /></label
            ><label class="field"
              ><span>联系人电话</span
              ><input
                v-model="form.emergencyPhone"
                name="emergencyPhone"
                type="tel"
                maxlength="20"
                minlength="7"
            /></label>
          </div>
          </fieldset>
          <div class="patient-form-footer">
            <span v-if="dirty" class="patient-unsaved" role="status">有未保存的修改</span>
            <button v-if="dirty" type="button" class="button button-secondary" :disabled="submitting || loading" @click="resetForm">撤销修改</button>
            <button
              class="button button-primary"
              type="submit"
              :disabled="submitting || loading || !dirty"
            >
              <UiIcon name="check" :size="18" />{{
                submitting ? "正在保存…" : "保存资料"
              }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>
