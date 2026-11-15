import type { OrderStatus } from '../api'

const styles: Record<OrderStatus, string> = {
  PENDING: 'bg-amber-500/15 text-amber-300',
  PAID: 'bg-sky-500/15 text-sky-300',
  SHIPPED: 'bg-emerald-500/15 text-emerald-300',
  CANCELLED: 'bg-neutral-700/50 text-neutral-400',
}

export default function StatusBadge({ status }: { status: OrderStatus }) {
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${styles[status]}`}>
      {status.charAt(0) + status.slice(1).toLowerCase()}
    </span>
  )
}
