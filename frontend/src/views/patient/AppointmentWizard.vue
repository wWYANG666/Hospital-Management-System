<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import EmptyState from "@/components/EmptyState.vue";
import PatientPageState from "./PatientPageState.vue";
import { enumName, list, usePatientPage } from "./usePatientPage";
import { submitAction } from "@/lib/api";
import { formatDate, text } from "@/lib/format";

const route = useRoute();
const router = useRouter();
const step = computed(() => Number(route.path.match(/step([2-4])$/)?.[1] || 1));
const query = () =>
  Object.fromEntries(
    Object.entries(route.query).map(([key, value]) => [
      key,
      Array.isArray(value) ? value[0] : value,
    ]),
  );
const { data, loading, error, message, submitting, reload, action } =
  usePatientPage(
    () =>
      "/patient/appointments/new" +
      (step.value === 1 ? "" : "/step" + step.value),
    query,
  );
const labels = ["选择科室", "选择医生", "选择时段", "确认预约"];
const descriptions = [
  "按就诊需求选择科室，也可以使用导诊助手获取参考建议。",
  "查看医生职称、专长与简介，选择适合您的门诊。",
  "查看真实排班与剩余号源，选择方便的就诊日期。",
  "核对预约信息与门诊类型，确认后提交预约。",
];
const search = ref("");
const departments = computed(() =>
  list(data.value, "departments").filter((dept) =>
    (String(dept.name) + String(dept.description || "")).includes(
      search.value.trim(),
    ),
  ),
);
const doctors = computed(() => list(data.value, "doctors"));
const groups = [
  { value: "MORNING", label: "上午门诊", range: "09:00 — 12:00" },
  { value: "AFTERNOON", label: "下午门诊", range: "14:00 — 17:00" },
  { value: "EVENING", label: "晚间门诊", range: "18:30 — 21:30" },
];
const slots = (group: string) =>
  list(data.value, "timeSlots").filter(
    (slot) => enumName(slot.workTime) === group && slot.hasSchedule,
  );
const scheduledGroups = computed(() =>
  groups.filter((group) => slots(group.value).length),
);
const dates = computed(() => {
  if (!data.value.minDate) return [];
  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date(
      String(data.value.minDate).slice(0, 10) + "T12:00:00",
    );
    date.setDate(date.getDate() + index);
    return {
      value: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`,
      title:
        index === 0
          ? "今天"
          : ["周日", "周一", "周二", "周三", "周四", "周五", "周六"][
              date.getDay()
            ],
      day: `${date.getMonth() + 1}/${date.getDate()}`,
    };
  });
});
const clinicType = ref("GENERAL");
const symptoms = ref("");
const generalRemaining = computed(() =>
  Math.max(
    0,
    Number(data.value.generalMax || 0) - Number(data.value.generalCurrent || 0),
  ),
);
const expertRemaining = computed(() =>
  Math.max(
    0,
    Number(data.value.expertMax || 0) - Number(data.value.expertCurrent || 0),
  ),
);
watch(data, () => {
  if (step.value === 4)
    clinicType.value = generalRemaining.value > 0 ? "GENERAL" : "EXPERT";
});
const previous = computed(() =>
  step.value === 2
    ? { path: "/app/patient/appointments/new" }
    : step.value === 3
      ? {
          path: data.value.department?.id
            ? "/app/patient/appointments/new/step2"
            : "/app/patient/appointments/new",
          query: { departmentId: data.value.department?.id },
        }
      : {
          path: "/app/patient/appointments/new/step3",
          query: { doctorId: route.query.doctorId, date: route.query.date },
        },
);
function changeDate(value: string) {
  if (!value || value < String(data.value.minDate || "") || loading.value) return;
  void router.replace({
    path: route.path,
    query: { doctorId: route.query.doctorId, date: value },
  });
}
async function confirm() {
  if (loading.value || !data.value.doctor?.id || !data.value.timeSlot) return;
  if (
    await action(
      "/patient/appointments",
      {
        doctorId: data.value.doctor?.id,
        date: data.value.date || data.value.schedule?.workDate,
        timeSlot: data.value.timeSlot,
        clinicType: clinicType.value,
        symptoms: symptoms.value,
      },
      "预约已提交",
      false,
    )
  ) {
    await router.push({
      path: "/app/patient/appointments",
      query: { booked: "1" },
    });
  }
}
const triageDescription = ref("");
const triageOpen = ref(false);
const triageBusy = ref(false);
const triageError = ref("");
const triage = ref<Record<string, any> | null>(null);
async function runTriage() {
  if (!triageDescription.value.trim() || triageBusy.value) return;
  triageBusy.value = true;
  triageError.value = "";
  triage.value = null;
  try {
    triage.value = await submitAction<Record<string, any>>(
      "/patient/ai/triage",
      { description: triageDescription.value },
    );
  } catch (cause) {
    triageError.value =
      cause instanceof Error ? cause.message : "导诊建议生成失败，请稍后重试。";
  } finally {
    triageBusy.value = false;
  }
}
</script>
<template>
  <div class="patient-experience patient-wizard">
    <PageHeader
      :title="labels[step - 1] || '预约挂号'"
      :description="descriptions[step - 1]"
    />
    <div class="patient-wizard-top">
      <RouterLink v-if="step > 1" :to="previous" class="patient-link"
        ><UiIcon name="chevron-left" :size="17" />返回上一步</RouterLink
      ><RouterLink to="/app/patient/appointments" class="patient-link"
        >查看我的挂号</RouterLink
      >
    </div>
    <ol class="patient-steps" aria-label="预约进度">
      <li
        v-for="(label, index) in labels"
        :key="label"
        :class="{ active: step === index + 1, done: step > index + 1 }"
        :aria-current="step === index + 1 ? 'step' : undefined"
      >
        <span class="patient-step-number"
          ><UiIcon v-if="step > index + 1" name="check" :size="17" /><template
            v-else
            >{{ index + 1 }}</template
          ></span
        ><span>{{ label }}</span>
      </li>
    </ol>
    <PatientPageState
      :loading="loading"
      :error="error"
      :message="message"
      @retry="reload"
    />
    <div v-if="step === 1 && data.departments" class="patient-booking-grid">
      <section class="panel">
        <div class="patient-section-heading">
          <div>
            <h2>门诊科室</h2>
            <p>请选择您需要就诊的科室</p>
          </div>
          <span class="patient-count"
            >{{ list(data, "departments").length }} 个科室</span
          >
        </div>
        <label class="patient-search"
          ><UiIcon name="search" :size="19" /><input
            v-model="search"
            type="search"
            placeholder="搜索科室名称或介绍"
            aria-label="搜索科室" /></label
        ><EmptyState
          v-if="!loading && !error && !departments.length"
          :title="search ? '未找到相关科室' : '暂无开放科室'"
          description="请调整搜索内容，或稍后再查看。"
        />
        <div class="patient-departments">
          <RouterLink
            v-for="dept in departments"
            :key="dept.id"
            :to="{
              path: '/app/patient/appointments/new/step2',
              query: { departmentId: dept.id },
            }"
            class="patient-department"
            ><span class="patient-department-icon"
              ><UiIcon name="stethoscope" :size="23"
            /></span>
            <div>
              <h3>{{ dept.name }}</h3>
              <p>
                {{ dept.description || "查看该科室的出诊医生与可预约时间。" }}
              </p>
            </div>
            <UiIcon name="chevron-right" :size="18"
          /></RouterLink>
        </div>
      </section>
      <details class="patient-triage panel" :open="triageOpen" @toggle="triageOpen = ($event.target as HTMLDetailsElement).open">
        <summary><UiIcon name="sparkles" :size="20" /><strong>不确定挂哪个科？</strong><span>{{ triageOpen ? "收起导诊" : "展开智能导诊" }}</span></summary>
        <div class="patient-triage-content">
        <p>不确定挂哪个科？描述您的不适，获取就诊科室建议。</p>
        <form @submit.prevent="runTriage">
          <label class="field"
            ><span>主要不适与持续时间</span
            ><textarea
              v-model="triageDescription"
              rows="5"
              placeholder="例如：咳嗽三天，伴有低热，想了解应该就诊的科室。"
              required
            ></textarea></label
          ><button
            type="submit"
            class="button button-primary"
            :disabled="triageBusy || !triageDescription.trim()"
          >
            {{ triageBusy ? "正在生成建议…" : "获取导诊建议" }}
          </button>
        </form>
        <div v-if="triageError" class="patient-notice patient-notice-error" role="alert">
          {{ triageError }}
        </div>
        <div v-if="triage" class="patient-triage-result" role="status">
          <strong>建议科室：{{ text(triage.department) }}</strong>
          <p>紧急程度：{{ text(triage.urgency) }}</p>
          <p class="read-scroll read-scroll--compact" tabindex="0" role="region" aria-label="导诊建议完整内容">{{ text(triage.advice) }}</p>
        </div>
        <div class="patient-fine-note">
          导诊建议仅供参考，具体诊疗请咨询医生。
        </div>
        </div>
      </details>
    </div>
    <section v-if="step === 2 && data.department" class="panel">
      <div class="patient-section-heading">
        <div>
          <h2>{{ text(data.department?.name) }} · 出诊医生</h2>
          <p>
            {{
              data.department?.description || "以下医生有今天或未来的有效排班。"
            }}
          </p>
        </div>
        <span class="patient-count">{{ doctors.length }} 位医生</span>
      </div>
      <EmptyState
        v-if="!loading && !error && !doctors.length"
        title="该科室暂无可预约医生"
        description="请返回选择其他科室，或稍后查看新的排班。"
      />
      <div class="patient-doctors">
        <RouterLink
          v-for="doctor in doctors"
          :key="doctor.id"
          :to="{
            path: '/app/patient/appointments/new/step3',
            query: { doctorId: doctor.id },
          }"
          class="patient-doctor"
          ><div class="patient-doctor-avatar">
            <UiIcon name="stethoscope" :size="32" />
          </div>
          <div class="patient-doctor-content">
            <div class="patient-doctor-heading">
              <h3>
                {{ data.userMap?.[doctor.id]?.realName || doctor.doctorCode }}
              </h3>
              <span>{{ text(doctor.title) }}</span>
            </div>
            <p class="patient-doctor-dept">
              {{ text(doctor.department)
              }}<span> · 医师编号 {{ doctor.doctorCode }}</span>
            </p>
            <div v-if="doctor.specialty" class="patient-doctor-specialty">
              <strong>擅长</strong> {{ doctor.specialty }}
            </div>
            <p v-if="doctor.introduction" class="patient-doctor-intro">
              {{ doctor.introduction }}
            </p>
          </div>
          <span class="button button-primary"
            >查看号源 <UiIcon name="chevron-right" :size="16" /></span
        ></RouterLink>
      </div>
    </section>
    <div v-if="step === 3 && data.doctor" class="patient-slot-layout">
      <section class="panel">
        <div class="patient-section-heading">
          <div>
            <h2>{{ data.doctorUser?.realName || data.doctor.doctorCode }} · 选择就诊日期</h2>
            <p>{{ text(data.doctor.department) }} · 点击日期查看出诊时段。</p>
          </div>
        </div>
        <div class="patient-dates">
          <button
            v-for="date in dates"
            :key="date.value"
            type="button"
            :class="{ selected: data.selectedDate === date.value }"
            :aria-pressed="data.selectedDate === date.value"
            :disabled="loading"
            @click="changeDate(date.value)"
          >
            <span>{{ date.title }}</span
            ><strong>{{ date.day }}</strong>
          </button>
        </div>
        <label class="field patient-date-field"
          ><span>其他日期</span
          ><input
            type="date"
            :value="data.selectedDate"
            :min="data.minDate"
            :disabled="loading"
            @change="changeDate(($event.target as HTMLInputElement).value)"
        /></label>
        <div v-if="data.info" class="patient-notice">{{ data.info }}</div>
        <EmptyState v-if="!loading && !scheduledGroups.length" title="当天暂无出诊安排" description="请选择其他日期，或返回选择其他医生。" />
        <div
          v-for="group in scheduledGroups"
          :key="group.value"
          class="patient-slot-group"
        >
          <div class="patient-section-heading">
            <h3>{{ group.label }}</h3>
            <span>{{ group.range }}</span>
          </div>
          <div class="patient-slots">
            <button
              v-for="slot in slots(group.value)"
              :key="slot.time"
              type="button"
              class="patient-slot"
              :class="{
                available: slot.hasSchedule && slot.available,
                unavailable: !slot.hasSchedule || !slot.available,
              }"
              :disabled="loading || !!error || !slot.available"
              @click="
                router.push({
                  path: '/app/patient/appointments/new/step4',
                  query: {
                    doctorId: data.doctorId,
                    date: data.selectedDate,
                    timeSlot: slot.time,
                  },
                })
              "
            >
              <strong>{{ slot.time }}</strong
              ><template v-if="slot.hasSchedule"
                ><span v-if="slot.bookedBySelf">您已预约该时段</span
                ><template v-else
                  ><span v-if="slot.generalRemaining != null"
                    >普通门诊 · 余 {{ slot.generalRemaining }} 号</span
                  ><span v-if="slot.expertRemaining != null"
                    >专家门诊 · 余 {{ slot.expertRemaining }} 号</span
                  ><span v-if="!slot.available">号源已满</span></template
                ></template
              ><span v-else>未安排门诊</span>
            </button>
          </div>
        </div>
      </section>
      <aside class="panel patient-doctor-summary">
        <div class="patient-doctor-avatar">
          <UiIcon name="stethoscope" :size="32" />
        </div>
        <h2>{{ data.doctorUser?.realName || data.doctor?.doctorCode }}</h2>
        <p>{{ text(data.doctor?.title) }}</p>
        <dl class="patient-facts">
          <div>
            <dt>就诊科室</dt>
            <dd>{{ text(data.doctor?.department) }}</dd>
          </div>
          <div v-if="data.doctor?.specialty">
            <dt>医生专长</dt>
            <dd>{{ data.doctor.specialty }}</dd>
          </div>
        </dl>
        <div class="patient-fine-note">
          剩余号源随预约实时变化，请在下一步核对并提交。
        </div>
      </aside>
    </div>
    <section v-if="step === 4 && data.doctor" class="panel patient-confirm-panel">
      <div class="patient-section-heading">
        <div>
          <h2>核对就诊信息</h2>
          <p>请确认以下信息，提交后可在我的挂号中查看状态。</p>
        </div>
        <span class="patient-task-icon"
          ><UiIcon name="clipboard" :size="24"
        /></span>
      </div>
      <div class="patient-appointment-ticket">
        <div>
          <span>就诊科室</span>
          <h3>{{ data.department?.name || data.doctor?.department }}</h3>
          <p>
            {{ data.doctorUser?.realName || data.doctor?.doctorCode }} ·
            {{ text(data.doctor?.title) }}
          </p>
        </div>
        <div>
          <span>预约日期与时段</span>
          <h3>{{ formatDate(data.date || data.schedule?.workDate) }}</h3>
          <p>{{ data.timeSlot }}</p>
        </div>
      </div>
      <form @submit.prevent="confirm">
        <fieldset class="patient-fieldset">
          <legend>选择门诊类型</legend>
          <div class="patient-clinic-options">
            <label
              v-if="generalRemaining > 0"
              :class="{ selected: clinicType === 'GENERAL' }"
              ><input
                v-model="clinicType"
                type="radio"
                value="GENERAL"
                name="clinicType"
              /><span
                ><strong>普通门诊</strong
                ><small>当前剩余 {{ generalRemaining }} 个号源</small></span
              ></label
            ><label
              v-if="expertRemaining > 0"
              :class="{ selected: clinicType === 'EXPERT' }"
              ><input
                v-model="clinicType"
                type="radio"
                value="EXPERT"
                name="clinicType"
              /><span
                ><strong>专家门诊</strong
                ><small>当前剩余 {{ expertRemaining }} 个号源</small></span
              ></label
            >
          </div>
          <div
            v-if="!generalRemaining && !expertRemaining"
            class="patient-notice patient-notice-error"
          >
            该时段号源已满，请返回上一步选择其他时段。
          </div>
        </fieldset>
        <label class="field"
          ><span>症状描述 <small>选填</small></span
          ><textarea
            v-model="symptoms"
            rows="4"
            name="symptoms"
            placeholder="请简要描述主要症状、持续时间或就诊诉求，帮助医生提前了解情况。"
          ></textarea>
        </label>
        <div class="patient-form-footer">
          <RouterLink :to="previous" class="button button-secondary"
            >重新选择时段</RouterLink
          ><button
            class="button button-primary"
            type="submit"
            :disabled="
              submitting || loading || (!generalRemaining && !expertRemaining)
            "
          >
            <UiIcon name="check" :size="18" />{{
              submitting ? "正在提交…" : "确认并提交预约"
            }}
          </button>
        </div>
      </form>
    </section>
  </div>
</template>
