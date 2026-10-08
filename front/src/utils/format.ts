const labels: Record<string, string> = {
  PENDING_APPROVAL: '待平台审核', APPROVED: '待配送', REJECTED: '已拒绝', DELIVERED: '已到店',
  VERIFIED: '已核销', COMPLETED: '已完成', CANCELLED: '已取消', PENDING: '待门店确认',
  CONFIRMED: '已确认', IN_PROGRESS: '保养中',
}

export function statusLabel(status: string) { return labels[status] ?? status }
export function formatTime(value: string | null) { return value ? value.replace('T', ' ').slice(0, 16) : '—' }
export function toCents(value: string | number) { return Math.round(Number(value) * 100) }
export function money(cents: number) { return (cents / 100).toFixed(2) }
