<script setup lang="ts">
import { onBeforeUnmount } from 'vue'
import { CircleCheck, X } from 'lucide'
import { useToast } from '../composables/useToast'
import UiIcon from './UiIcon.vue'

const { messages, dismiss, pause, resume, clear } = useToast()

function handleFocusOut(id: number, event: FocusEvent) {
  if (!(event.currentTarget as HTMLElement).contains(event.relatedTarget as Node | null)) resume(id, 'focus')
}

onBeforeUnmount(clear)
</script>

<template>
  <Teleport to="body">
    <div class="toast-viewport" aria-live="polite" aria-relevant="additions text" aria-label="操作提示">
      <TransitionGroup name="toast">
        <div
          v-for="toast in messages"
          :key="toast.id"
          class="toast-message"
          @mouseenter="pause(toast.id, 'hover')"
          @mouseleave="resume(toast.id, 'hover')"
          @focusin="pause(toast.id, 'focus')"
          @focusout="handleFocusOut(toast.id, $event)"
        >
          <UiIcon :icon="CircleCheck" :size="22" />
          <p>{{ toast.message }}</p>
          <button
            type="button"
            class="toast-close"
            :aria-label="`关闭提示：${toast.message}`"
            @click="dismiss(toast.id)"
          >
            <UiIcon :icon="X" :size="18" />
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-viewport {
  position: fixed;
  top: max(80px, env(safe-area-inset-top));
  right: max(24px, env(safe-area-inset-right));
  z-index: 30;
  display: grid;
  gap: 12px;
  width: min(400px, calc(100vw - 32px));
  pointer-events: none;
}
.toast-message {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 8px 8px 16px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--surface);
  color: var(--ink);
  box-shadow: 0 8px 24px rgb(29 43 64 / 12%);
  pointer-events: auto;
}
.toast-message > .ui-icon {
  flex-shrink: 0;
  color: var(--green);
}
.toast-message p {
  flex: 1;
  min-width: 0;
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  overflow-wrap: anywhere;
}
.toast-close {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 44px;
  height: 44px;
  padding: 0;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
}
.toast-close:hover {
  background: var(--blue-soft);
  color: var(--ink);
}
.toast-enter-active,
.toast-leave-active,
.toast-move {
  transition:
    opacity 160ms ease,
    transform 160ms ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
@media (max-width: 620px) {
  .toast-viewport {
    top: max(16px, env(safe-area-inset-top));
    right: max(16px, env(safe-area-inset-right));
  }
}
</style>
