<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
import StatusBadge from "@/components/StatusBadge.vue";
import EmptyState from "@/components/EmptyState.vue";
import { money, formatDate, statusText } from "@/lib/format";
import DoctorFeedback from "./DoctorFeedback.vue";
import { useDoctorPage, list } from "./useDoctorPage";
import "@/styles/doctor.css";
const route = useRoute();
const tab = ref("orders");
const form = reactive({
  orderType: "MEDICATION",
  orderContent: "",
  medicineId: "",
  medicineQuantity: 1,
  examinationId: "",
  dosage: "",
  frequency: "",
});
const condition = ref("");
const { data, loading, saving, error, notice, load, action } = useDoctorPage(
  () => `/doctor/hospitalizations/${route.params.id}/manage`,
  undefined,
  () => ({
    orderContent: form.orderContent,
    medicineId: form.orderType === "MEDICATION" ? form.medicineId : "",
    examinationId: form.orderType === "EXAMINATION" ? form.examinationId : "",
    dosage: form.orderType === "MEDICATION" ? form.dosage : "",
    frequency: form.orderType === "MEDICATION" ? form.frequency : "",
    medicineQuantity: form.orderType === "MEDICATION" && form.medicineId ? form.medicineQuantity : 1,
    condition: condition.value,
  }),
  false,
);
const discharged = computed(
  () => data.value.hospitalization?.status === "DISCHARGED",
);
async function addOrder() {
  if (
    await action(`/doctor/hospitalizations/${route.params.id}/orders`, {
      ...form,
    })
  ) {
    form.orderContent = "";
    form.medicineId = "";
    form.examinationId = "";
    form.dosage = "";
    form.frequency = "";
    form.medicineQuantity = 1;
  }
}
async function updateCondition() {
  if (
    await action(
      `/doctor/hospitalizations/${route.params.id}/update-condition`,
      { conditionUpdate: condition.value },
    )
  ) {
    condition.value = "";
  }
}
</script>
<template>
  <div class="doctor-view">
    <PageHeader
      title="住院诊疗管理"
      description="维护住院医嘱、记录病情变化与病程。"
      ><RouterLink
        to="/app/doctor/hospitalizations"
        class="button button-secondary"
        >返回住院患者</RouterLink
      ><RouterLink
        v-if="data.hospitalization && !discharged"
        :to="`/app/doctor/hospitalizations/${route.params.id}/discharge`"
        class="button button-primary"
        >办理出院</RouterLink
      ></PageHeader
    ><DoctorFeedback
      :loading="loading"
      :error="error"
      :notice="notice"
      :reload-available="!data.hospitalization"
      @retry="load"
    /><template v-if="data.hospitalization"
      ><section class="panel doctor-patient-banner">
        <span class="doctor-patient-avatar"
          ><UiIcon name="bed" :size="26"
        /></span>
        <div class="doctor-patient-banner-main">
          <div>
            <h2>{{ data.patientUser?.realName || "患者" }}</h2>
            <StatusBadge :status="data.hospitalization.status" />
          </div>
          <p>
            {{ data.bed?.bedNumber || "床位未分配" }} ·
            {{ data.manageDepartmentName || "科室未指定" }} ·
            {{ formatDate(data.hospitalization.admissionDate) }} 入院
          </p>
        </div>
        <div class="doctor-chief-complaint">
          <span>当前诊断</span>
          <p class="read-scroll read-scroll--compact" tabindex="0" role="region" aria-label="住院诊断完整内容">{{ data.displayDiagnosis || "暂无诊断" }}</p>
        </div>
      </section>
      <div class="doctor-inline-metrics">
        <span
          >已住院<strong
            >{{ data.stayDays || 1 }}<small>天</small></strong
          ></span
        ><span
          >用药与检查累计<strong
            >¥{{ money(data.hospitalization.totalCost) }}</strong
          ></span
        ><span
          >医嘱记录<strong>{{ list(data, "orders").length }}</strong></span
        ><span class="doctor-muted">出院结算时另计床位费</span>
      </div>
      <nav class="doctor-tabs" aria-label="住院诊疗内容">
        <button
          v-for="item in [
            { id: 'orders', label: '医嘱管理' },
            { id: 'condition', label: '病情更新' },
            { id: 'records', label: '病历记录' },
          ]"
          :key="item.id"
          :class="{ active: tab === item.id }"
          @click="tab = item.id"
        >
          {{ item.label }}
        </button>
      </nav>
      <div v-if="tab === 'orders'" class="doctor-inpatient-grid">
        <section v-if="!discharged" class="panel">
          <div class="doctor-panel-heading">
            <div>
              <h2>开立医嘱</h2>
              <p>当前收治科室可用项目</p>
            </div>
          </div>
          <form @submit.prevent="addOrder">
            <fieldset class="doctor-form-lock" :disabled="saving">
            <label class="doctor-form-field"
              ><span>医嘱类型 <b>*</b></span
              ><select v-model="form.orderType">
                <option value="MEDICATION">用药医嘱</option>
                <option value="EXAMINATION">检查医嘱</option>
                <option value="NURSING">护理医嘱</option>
                <option value="OTHER">其他医嘱</option>
              </select></label
            ><label class="doctor-form-field"
              ><span>医嘱内容 <b>*</b></span
              ><textarea
                v-model="form.orderContent"
                rows="4"
                required
                placeholder="记录医嘱内容及补充说明"
              ></textarea></label
            ><template v-if="form.orderType === 'MEDICATION'"
              ><label class="doctor-form-field"
                ><span>选择药品</span
                ><select v-model="form.medicineId" required>
                  <option value="">请选择药品</option>
                  <option
                    v-for="med in list(data, 'medicines')"
                    :key="med.id"
                    :value="String(med.id)"
                  >
                    {{ med.name }} · ¥{{ money(med.price) }}
                  </option></select
                ><small v-if="!list(data, 'medicines').length"
                  >当前科室暂无可用药品。</small
                ></label
              >
              <div class="form-grid">
                <label class="doctor-form-field"
                  ><span>数量</span
                  ><input
                    v-model.number="form.medicineQuantity"
                    type="number"
                    min="1"
                    max="9999"
                    step="1"
                    required /></label
                ><label class="doctor-form-field"
                  ><span>频次</span
                  ><input v-model="form.frequency" placeholder="如：每日 3 次"
                /></label>
              </div>
              <label class="doctor-form-field"
                ><span>用法用量</span
                ><input
                  v-model="form.dosage"
                  placeholder="如：每次 2 片，饭后口服" /></label></template
            ><label
              v-if="form.orderType === 'EXAMINATION'"
              class="doctor-form-field"
              ><span>检查项目</span
              ><select v-model="form.examinationId" required>
                <option value="">请选择检查项目</option>
                <option
                  v-for="exam in list(data, 'examinations')"
                  :key="exam.id"
                  :value="String(exam.id)"
                >
                  {{ exam.name }} · ¥{{ money(exam.price) }}
                </option></select
              ><small v-if="!list(data, 'examinations').length"
                >当前科室暂无检查项目。</small
              ></label
            ><button
              class="button button-primary doctor-full-width"
              :disabled="saving"
            >
              {{ saving ? "提交中…" : "添加医嘱" }}
            </button>
            </fieldset>
          </form>
        </section>
        <section class="panel" :class="{ 'doctor-full-span': discharged }">
          <div class="doctor-panel-heading">
            <div>
              <h2>医嘱记录</h2>
              <p>
                {{
                  discharged ? "已出院患者的历史医嘱" : "当前住院期间已开立医嘱"
                }}
              </p>
            </div>
            <span class="doctor-count">{{ list(data, "orders").length }}</span>
          </div>
          <EmptyState
            v-if="!list(data, 'orders').length"
            title="暂无医嘱记录"
          />
          <article
            v-for="order in list(data, 'orders')"
            :key="order.id"
            class="doctor-order-record"
          >
            <header>
              <span class="doctor-tag">{{
                order.orderType === "NURSING"
                  ? "护理"
                  : statusText(order.orderType)
              }}</span
              ><StatusBadge :status="order.status" /><small>{{
                formatDate(order.createdAt, true)
              }}</small>
            </header>
            <p class="read-scroll read-scroll--compact" tabindex="0" role="region" aria-label="医嘱完整内容">{{ order.orderContent }}</p>
            <div
              v-if="order.dosage || order.frequency || order.quantity"
              class="doctor-order-meta"
            >
              <span v-if="order.quantity">数量 {{ order.quantity }}</span
              ><span v-if="order.dosage">{{ order.dosage }}</span
              ><span v-if="order.frequency">{{ order.frequency }}</span>
            </div>
          </article>
        </section>
      </div>
      <section v-if="tab === 'condition'" class="panel doctor-form-panel">
        <div class="doctor-panel-heading">
          <div>
            <h2>记录病情变化</h2>
            <p>记录患者病情、观察所见与处理建议</p>
          </div>
        </div>
        <p v-if="discharged" class="doctor-muted">
          该患者已出院，请在病历记录中查阅病程。
        </p>
        <form v-else @submit.prevent="updateCondition">
          <fieldset class="doctor-form-lock" :disabled="saving">
          <label class="doctor-form-field"
            ><span>病情更新 <b>*</b></span
            ><textarea
              v-model="condition"
              required
              rows="7"
              placeholder="记录病情变化、生命体征、治疗反应及下一步安排"
            ></textarea>
          </label>
          <div class="doctor-form-actions">
            <button class="button button-primary" :disabled="saving">
              {{ saving ? "保存中…" : "保存病情记录" }}
            </button>
          </div>
          </fieldset>
        </form>
      </section>
      <section v-if="tab === 'records'" class="panel">
        <div class="doctor-panel-heading">
          <div>
            <h2>住院病程与病历</h2>
            <p>关联患者诊疗记录</p>
          </div>
        </div>
        <EmptyState v-if="!list(data, 'records').length" title="暂无病历记录" />
        <article
          v-for="entry in list(data, 'records')"
          :key="entry.id"
          class="doctor-timeline-record"
        >
          <span class="doctor-timeline-marker"></span>
          <header>
            <strong>{{
              entry.conditionUpdate ? "病情更新" : "诊疗记录"
            }}</strong
            ><time>{{ formatDate(entry.createdAt, true) }}</time>
          </header>
          <p v-if="entry.conditionUpdate" class="doctor-preformatted read-scroll" tabindex="0" role="region" aria-label="病情更新完整内容">
            {{ entry.conditionUpdate }}
          </p>
          <p v-if="entry.diagnosis" class="read-scroll read-scroll--compact" tabindex="0" role="region" aria-label="住院诊断完整内容"><b>诊断：</b>{{ entry.diagnosis }}</p>
          <p v-if="entry.medicalRecordContent" class="doctor-preformatted read-scroll" tabindex="0" role="region" aria-label="住院病历完整内容">
            {{ entry.medicalRecordContent }}
          </p>
        </article>
      </section></template
    >
  </div>
</template>
