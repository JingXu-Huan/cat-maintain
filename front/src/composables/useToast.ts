import { readonly, shallowRef } from 'vue'

const duration = 4000
const maxVisible = 3
const messages = shallowRef<{ id: number; message: string }[]>([])
const lifetimes = new Map<
  number,
  {
    timer?: ReturnType<typeof setTimeout>
    remaining: number
    startedAt: number
    pauses: Set<'hover' | 'focus'>
  }
>()
let nextId = 0

function dismiss(id: number) {
  clearTimeout(lifetimes.get(id)?.timer)
  lifetimes.delete(id)
  messages.value = messages.value.filter((toast) => toast.id !== id)
}

function schedule(id: number) {
  const lifetime = lifetimes.get(id)
  if (!lifetime || lifetime.pauses.size) return
  lifetime.startedAt = Date.now()
  lifetime.timer = setTimeout(() => dismiss(id), lifetime.remaining)
}

export function toastSuccess(message: string) {
  if (!message.trim()) return
  const existing = messages.value.find((toast) => toast.message === message)
  if (existing) {
    const lifetime = lifetimes.get(existing.id)!
    clearTimeout(lifetime.timer)
    lifetime.remaining = duration
    schedule(existing.id)
    return
  }
  if (messages.value.length >= maxVisible) dismiss(messages.value[0]!.id)
  const id = ++nextId
  lifetimes.set(id, { remaining: duration, startedAt: 0, pauses: new Set() })
  messages.value = [...messages.value, { id, message }]
  schedule(id)
}

function pause(id: number, reason: 'hover' | 'focus') {
  const lifetime = lifetimes.get(id)
  if (!lifetime || lifetime.pauses.has(reason)) return
  if (!lifetime.pauses.size) {
    clearTimeout(lifetime.timer)
    lifetime.remaining = Math.max(0, lifetime.remaining - (Date.now() - lifetime.startedAt))
  }
  lifetime.pauses.add(reason)
}

function resume(id: number, reason: 'hover' | 'focus') {
  const lifetime = lifetimes.get(id)
  if (lifetime?.pauses.delete(reason)) schedule(id)
}

function clear() {
  for (const toast of messages.value) dismiss(toast.id)
}

export function useToast() {
  return { messages: readonly(messages), dismiss, pause, resume, clear }
}
