import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router'
import { api, nextStatuses, type Order, type OrderStatus } from '../api'
import { useAuth } from '../auth'
import StatusBadge from '../components/StatusBadge'
import { formatDate, money } from '../format'

const actionLabel: Record<OrderStatus, string> = {
  PENDING: 'Mark pending', PAID: 'Mark as paid', SHIPPED: 'Mark as shipped', CANCELLED: 'Cancel order',
}

export default function OrderDetail() {
  const { id } = useParams()
  const { accountType } = useAuth()
  const isEmployee = accountType === 'EMPLOYEE'
  const justPlaced = (useLocation().state as { placed?: boolean } | null)?.placed
  const [order, setOrder] = useState<Order | null>(null)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api<Order>(`/orders/${id}`).then(setOrder).catch(e => setError(e.message))
  }, [id])

  async function run(next: OrderStatus) {
    if (next === 'CANCELLED' && !confirm('Cancel this order? Items go back to stock.')) return
    setBusy(true)
    setActionError('')
    try {
      // Customers can only cancel; status changes are employee-only on the backend.
      const updated = isEmployee
        ? await api<Order>(`/orders/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status: next }) })
        : await api<Order>(`/orders/${id}/cancel`, { method: 'POST' })
      setOrder(updated)
    } catch (err) {
      setActionError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  if (error) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10">
        <p role="alert" className="rounded-lg bg-red-950/50 p-4 text-red-300">{error}</p>
        <Link to="/orders" className="mt-4 inline-block text-pink-400 hover:text-pink-300">← Back to orders</Link>
      </div>
    )
  }
  if (!order) return <p className="mx-auto max-w-3xl px-4 py-10 text-neutral-500">Loading order…</p>

  const actions: OrderStatus[] = isEmployee ? nextStatuses[order.status]
    : order.status === 'PENDING' ? ['CANCELLED'] : []

  return (
    <div className="mx-auto max-w-3xl space-y-6 px-4 py-10">
      <Link to="/orders" className="text-sm text-neutral-400 hover:text-white">← Back to orders</Link>
      {justPlaced && (
        <div role="status" className="rounded-2xl border border-emerald-800 bg-emerald-950/40 p-4 text-emerald-300">
          Thank you! Your order was placed. We will confirm the payment shortly.
        </div>
      )}

      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold">Order #{order.id}</h1>
          <p className="text-sm text-neutral-400">{formatDate(order.createdAt)}</p>
        </div>
        <StatusBadge status={order.status} />
      </div>

      <ul className="divide-y divide-neutral-800 rounded-2xl border border-neutral-800 bg-neutral-900">
        {order.items.map(i => (
          <li key={i.variantId} className="flex justify-between gap-4 p-4">
            <div>
              <p className="font-medium">{i.productName}</p>
              <p className="text-sm text-neutral-400">
                {i.color} · {i.size} · {i.quantity} × {money(i.unitPrice)}
              </p>
            </div>
            <p className="font-medium">{money(i.subtotal)}</p>
          </li>
        ))}
        <li className="flex justify-between p-4 text-lg font-semibold">
          <span>Total</span><span>{money(order.total)}</span>
        </li>
      </ul>

      <section className="rounded-2xl border border-neutral-800 bg-neutral-900 p-4">
        <h2 className="mb-1 text-sm font-medium text-neutral-400">Shipping address</h2>
        <p className="whitespace-pre-line">{order.shippingAddress}</p>
      </section>

      {actionError && <p role="alert" className="rounded-lg bg-red-950/50 p-3 text-sm text-red-300">{actionError}</p>}
      {actions.length > 0 && (
        <div className="flex flex-wrap justify-end gap-3">
          {actions.map(s => (
            <button key={s} disabled={busy} onClick={() => run(s)}
              className={s === 'CANCELLED'
                ? 'rounded-lg border border-red-900 px-4 py-2 text-red-300 hover:bg-red-950/50 disabled:opacity-50'
                : 'rounded-lg bg-pink-600 px-4 py-2 font-medium text-white hover:bg-pink-500 disabled:opacity-50'}>
              {actionLabel[s]}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
