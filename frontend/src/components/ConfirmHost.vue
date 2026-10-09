<script setup lang="ts">
import { nextTick, ref, watch } from "vue";
import { confirmation, resolveConfirmation } from "@/lib/confirm";
import UiIcon from "./UiIcon.vue";
const cancel = ref<HTMLButtonElement | null>(null);
let previousFocus: HTMLElement | null = null;
watch(
  () => confirmation.open,
  async (open) => {
    if (open) {
      previousFocus = document.activeElement as HTMLElement;
      await nextTick();
      cancel.value?.focus();
    } else previousFocus?.focus();
  },
);
function trap(event: KeyboardEvent) {
  const container = event.currentTarget as HTMLElement;
  const buttons = [...container.querySelectorAll<HTMLButtonElement>("button")];
  if (event.key === "Tab" && buttons.length) {
    const first = buttons[0],
      last = buttons.at(-1);
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last?.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first?.focus();
    }
  }
}
</script>
<template>
  <Teleport to="body"
    ><div
      v-if="confirmation.open"
      class="confirmation-backdrop"
      @click.self="resolveConfirmation(false)"
      @keydown.esc="resolveConfirmation(false)"
    >
      <section
        class="confirmation-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirmation-title"
        aria-describedby="confirmation-message"
        @keydown="trap"
      >
        <span class="confirmation-icon"
          ><UiIcon name="alert" :size="25"
        /></span>
        <h2 id="confirmation-title">{{ confirmation.title }}</h2>
        <p id="confirmation-message">{{ confirmation.message }}</p>
        <div>
          <button
            ref="cancel"
            class="button button-secondary"
            @click="resolveConfirmation(false)"
          >
            取消</button
          ><button
            class="button button-danger"
            @click="resolveConfirmation(true)"
          >
            确认操作
          </button>
        </div>
      </section>
    </div></Teleport
  >
</template>
