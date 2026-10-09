import type { RouteRecordRaw } from 'vue-router'

const list = () => import('@/views/admin/ResourceList.vue')
const form = () => import('@/views/admin/ResourceForm.vue')
const baseMeta = { role: 'ADMIN' }
const routes: RouteRecordRaw[] = [
  {path:'/app/admin/dashboard',component:()=>import('@/views/admin/Dashboard.vue'),meta:{...baseMeta,title:'医院运营工作台'}},
  {path:'/app/admin/departments',component:list,meta:{...baseMeta,title:'科室与医生',resource:'departments'}},
  {path:'/app/admin/departments/new',component:()=>import('@/views/admin/DepartmentDetail.vue'),meta:{...baseMeta,title:'新增科室'}},
  {path:'/app/admin/departments/:id/edit',component:()=>import('@/views/admin/DepartmentDetail.vue'),meta:{...baseMeta,title:'科室管理'}},
  {path:'/app/admin/departments/:departmentId/doctors/:doctorId/schedules',component:()=>import('@/views/admin/DoctorSchedules.vue'),meta:{...baseMeta,title:'医生出诊排班'}},
  {path:'/app/admin/doctors',redirect:'/app/admin/departments',meta:{...baseMeta,title:'医生管理'}},
  {path:'/app/admin/doctors/new',redirect:'/app/admin/departments',meta:{...baseMeta,title:'新增医生'}},
  {path:'/app/admin/patients',component:()=>import('@/views/admin/Patients.vue'),meta:{...baseMeta,title:'患者档案中心'}},
  {path:'/app/admin/patients/:id',component:()=>import('@/views/admin/PatientDetail.vue'),meta:{...baseMeta,title:'患者就诊档案'}},
  {path:'/app/admin/appointments',component:()=>import('@/views/admin/ResourceList.vue'),meta:{...baseMeta,title:'全院挂号管理',resource:'appointments'}},
  {path:'/app/admin/appointments/:id/reports',component:()=>import('@/views/admin/AppointmentDetail.vue'),meta:{...baseMeta,title:'挂号检查报告',kind:'reports'}},
  {path:'/app/admin/appointments/:id/prescriptions',component:()=>import('@/views/admin/AppointmentDetail.vue'),meta:{...baseMeta,title:'挂号处方',kind:'prescriptions'}},
  {path:'/app/admin/medicines',component:list,meta:{...baseMeta,title:'药品库存',resource:'medicines'}},
  {path:'/app/admin/medicines/new',component:form,meta:{...baseMeta,title:'新增药品',resource:'medicines'}},
  {path:'/app/admin/medicines/:id/edit',component:form,meta:{...baseMeta,title:'编辑药品',resource:'medicines'}},
  {path:'/app/admin/medicines/:id/inbound',component:form,meta:{...baseMeta,title:'药品入库',resource:'inbound'}},
  {path:'/app/admin/medicines/inbounds',component:list,meta:{...baseMeta,title:'入库记录',resource:'inbounds'}},
  {path:'/app/admin/examinations',component:list,meta:{...baseMeta,title:'检查项目',resource:'examinations'}},
  {path:'/app/admin/examinations/new',component:form,meta:{...baseMeta,title:'新增检查项目',resource:'examinations'}},
  {path:'/app/admin/examinations/:id/edit',component:form,meta:{...baseMeta,title:'编辑检查项目',resource:'examinations'}},
  {path:'/app/admin/beds',component:()=>import('@/views/admin/Beds.vue'),meta:{...baseMeta,title:'住院床位看板'}},
  {path:'/app/admin/beds/new',component:form,meta:{...baseMeta,title:'新增床位',resource:'beds'}},
  {path:'/app/admin/beds/:id/edit',component:form,meta:{...baseMeta,title:'编辑床位',resource:'beds'}},
  {path:'/app/admin/hospitalizations',component:list,meta:{...baseMeta,title:'住院管理',resource:'hospitalizations'}},
  {path:'/app/admin/hospitalizations/:id/settle',component:()=>import('@/views/admin/Settlement.vue'),meta:{...baseMeta,title:'出院结算'}},
  {path:'/app/admin/schedules',component:list,meta:{...baseMeta,title:'排班与号源',resource:'schedules'}},
  {path:'/app/admin/schedules/new',component:form,meta:{...baseMeta,title:'新增门诊排班',resource:'schedules'}},
  {path:'/app/admin/reports',component:list,meta:{...baseMeta,title:'检查报告中心',resource:'reports'}},
  {path:'/app/admin/reports/:id/preview',component:()=>import('@/views/admin/ReportDetail.vue'),meta:{...baseMeta,title:'报告单详情'}},
  {path:'/app/admin/pharmacy',redirect:'/app/admin/patients',meta:{...baseMeta,title:'药房处理'}},
  {path:'/app/admin/pharmacy/:id',component:()=>import('@/views/admin/PrescriptionDetail.vue'),meta:{...baseMeta,title:'处方核对与发药'}},
  {path:'/app/admin/prescriptions',redirect:'/app/admin/patients',meta:{...baseMeta,title:'处方查询'}},
  {path:'/app/admin/ai-logs',component:()=>import('@/views/admin/AiLogs.vue'),meta:{...baseMeta,title:'AI 生成审计'}},
  {path:'/app/admin/profile',component:()=>import('@/views/admin/Profile.vue'),meta:{...baseMeta,title:'管理员账户'}},
  {path:'/app/admin/users/:id/change-password',component:()=>import('@/views/admin/Password.vue'),meta:{...baseMeta,title:'修改账户密码'}},
  {path:'/app/admin',redirect:'/app/admin/dashboard',meta:{...baseMeta,title:'医院运营工作台'}}
]
export default routes
