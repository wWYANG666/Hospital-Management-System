<script setup lang="ts">
import PageHeader from "@/components/PageHeader.vue";
import UiIcon from "@/components/UiIcon.vue";
</script>
<template>
  <main class="public-inner">
    <PageHeader
      title="就医指南"
      description="从预约到就诊，了解每一步需要完成的事项。"
    />
    <div class="guide-layout">
      <section class="panel">
        <h2>预约与就诊流程</h2>
        <div
          class="guide-step"
          v-for="(item, index) in [
            {
              title: '登录患者账号',
              desc: '使用本人账号登录并核对个人资料；没有账号时可先注册。',
            },
            {
              title: '选择科室与医生',
              desc: '按就诊需求选择科室，查看医生介绍及已发布的出诊日期。',
            },
            {
              title: '选择时段并确认',
              desc: '选择普通或专家号源，填写症状说明并确认挂号。',
            },
            {
              title: '按时到院就诊',
              desc: '按预约日期及时段到院，医生完成接诊后可查询病历与处方。',
            },
            {
              title: '缴费并查询结果',
              desc: '处方与检查缴费完成后，按页面进度确认取药并查询报告。',
            },
          ]"
          :key="item.title"
        >
          <span>{{ index + 1 }}</span>
          <div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.desc }}</p>
          </div>
        </div>
        <RouterLink
          class="button button-primary"
          to="/app/patient/appointments/new"
          >开始预约<UiIcon name="arrow-right" :size="16"
        /></RouterLink>
      </section>
      <aside>
        <section class="panel">
          <h2>预约注意事项</h2>
          <ul class="notice-list">
            <li>请使用准确的本人姓名及联系方式。</li>
            <li>医生出诊日期与余号以系统展示为准。</li>
            <li>无法按时到院时，可查看“我的挂号”中允许取消的记录。</li>
            <li>急症请直接就近急诊，线上导诊建议仅供参考。</li>
          </ul>
        </section>
        <section class="panel">
          <h2>其他服务</h2>
          <RouterLink to="/app/patient/reports" class="guide-link"
            ><UiIcon name="file" />检查报告查询<UiIcon
              name="chevron-right" /></RouterLink
          ><RouterLink to="/app/patient/hospitalizations" class="guide-link"
            ><UiIcon name="bed" />住院信息查询<UiIcon
              name="chevron-right" /></RouterLink
          ><RouterLink to="/app/patient/payments/history" class="guide-link"
            ><UiIcon name="credit-card" />账单与缴费记录<UiIcon
              name="chevron-right"
          /></RouterLink>
        </section>
      </aside>
    </div>
  </main>
</template>
