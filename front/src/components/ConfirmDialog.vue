<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, useId, watch } from 'vue'
const props = withDefaults(defineProps<{ open: boolean; title: string; description: string; confirmLabel?: string }>(), { confirmLabel: '确认删除' })
const emit = defineEmits<{ 'update:open': [open: boolean]; confirm: [] }>()
const dialog = ref<HTMLDialogElement | null>(null)
const id = useId()
watch(() => props.open, async (open) => {
  await nextTick()
  if (open && !dialog.value?.open) dialog.value?.showModal()
  else if (!open && dialog.value?.open) dialog.value.close()
}, { immediate: true })
function confirm() { emit('update:open', false); emit('confirm') }
onBeforeUnmount(() => dialog.value?.close())
</script>

<template>
  <dialog ref="dialog" class="confirm-dialog" :aria-labelledby="`${id}-title`" :aria-describedby="`${id}-description`" @cancel.prevent="emit('update:open', false)" @close="emit('update:open', false)">
    <h2 :id="`${id}-title`">{{ title }}</h2>
    <p :id="`${id}-description`">{{ description }}</p>
    <div class="dialog-actions"><button class="secondary-button" type="button" autofocus @click="emit('update:open', false)">保留并返回</button><button class="danger-button" type="button" @click="confirm">{{ confirmLabel }}</button></div>
  </dialog>
</template>
