<script setup lang="ts">
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuth } from "@/stores/auth";
import { acceptsRole, isPatientPortal } from "@/lib/portal";
import UiIcon from "./UiIcon.vue";

defineProps<{ title: string; description?: string }>();
const route = useRoute();
const router = useRouter();
const auth = useAuth();
const username = ref("");
const password = ref("");
const visible = ref(false);
const busy = ref(false);
const error = ref(
  route.query.reason === "expired" ? "登录已过期，请重新登录后继续。" : "",
);

async function login() {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    const user = await auth.login(username.value.trim(), password.value);
    if (!user || !acceptsRole(user.role))
      throw new Error("该账号不能登录当前服务端");
    const prefix = "/app/" + user.role.toLowerCase() + "/";
    const target = String(route.query.redirect || "");
    await router.replace(
      target.startsWith(prefix) ? target : prefix + "dashboard",
    );
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : "登录失败，请重试";
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <div class="login-form">
    <h2>{{ title }}</h2>
    <p v-if="description" class="login-description">{{ description }}</p>
    <div
      v-if="!isPatientPortal && route.query.registered === '1'"
      class="info-banner"
      role="status"
    >
      医生账号申请已提交，管理员审核通过后方可登录。
    </div>
    <form @submit.prevent="login">
      <fieldset class="login-fields" :disabled="busy">
        <label class="field"
          ><span>账号</span>
          <div class="input-with-icon">
            <UiIcon name="user" :size="18" /><input
              v-model="username"
              name="username"
              autocomplete="username"
              required
              placeholder="请输入账号"
            /></div
        ></label>
        <label class="field"
          ><span>密码</span>
          <div class="input-with-icon">
            <UiIcon name="shield" :size="18" /><input
              v-model="password"
              name="password"
              :type="visible ? 'text' : 'password'"
              autocomplete="current-password"
              required
              placeholder="请输入密码"
            /><button
              type="button"
              @click="visible = !visible"
              :aria-label="visible ? '隐藏密码' : '显示密码'"
            >
              {{ visible ? "隐藏" : "显示" }}
            </button>
          </div></label
        >
      </fieldset>
      <p v-if="error" class="inline-error" role="alert">{{ error }}</p>
      <button
        class="button button-primary auth-submit"
        type="submit"
        :disabled="busy"
      >
        {{ busy ? "正在登录…" : "登录"
        }}<UiIcon name="arrow-right" :size="17" />
      </button>
    </form>
  </div>
</template>
<style scoped>
.login-description {
  color: #8294ab;
  font-size: 13px;
  margin-bottom: 24px;
  line-height: 1.8;
}
.login-fields {
  margin: 0;
  padding: 0;
  border: 0;
  min-width: 0;
}
.login-fields .field {
  margin-bottom: 18px;
}
</style>
