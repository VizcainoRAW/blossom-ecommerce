import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, type Order, type OrderStatus, type Page } from '../api'
import { useAuth } from '../auth'
import { inputClass } from '../components/Field'
import StatusBadge from '../components/StatusBadge'
import { formatDate, money } from '../format'

const PAGE_SIZE = 10
const statuses: OrderStatus[] = ['PENDING', 'PAID', 'SHIPPED', 'CANCELLED']

/** Customers see their own orders; the backend returns every order to employees. */
export default function Orders() {
  const { accountType } = useAuth()
  const isEmployee = accountType === 'EMPLOYEE'
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState<Page<Order> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) })
    if (status) params.set('status', status)
    api<Page<Order>>(`/orders?${params}`)
      .then(d => { if (!cancelled) setData(d) })
      .catch(e => { if (!cancelled) setError(e.message) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [status, page])

  // Loading/error reset happens here, in the event, rather than inside the effect.
  function show(next: { status?: string; page: number }) {
    setLoading(true)
    setError('')
    if (next.status !== undefined) setStatus(next.status)
    setPage(next.page)
  }

  const totalPages = data ? Math.max(1, Math.ceil(data.totalItems / PAGE_SIZE)) : 1

  return (
    <div className="mx-auto max-w-4xl space-y-6 px-4 py-10">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-3xl font-semibold">{isEmployee ? 'All orders' : 'My orders'}</h1>
        <select aria-label="Filter by status" className={`${inputClass} w-44`} value={status}
          onChange={e => show({ status: e.target.value, page: 0 })}>
          <option value="">All statuses</option>
          {statuses.map(s => <option key={s} value={s}>{s.charAt(0) + s.slice(1).toLowerCase()}</option>)}
        </select>
      </div>

      {error ? (
        <p role="alert" className="rounded-lg bg-red-950/50 p-4 text-red-300">{error}</p>
      ) : loading && !data ? (
        <p className="text-neutral-500">Loading orders…</p>
      ) : data?.items.length === 0 ? (
        <div className="py-10 text-center text-neutral-500">
          <p>No orders yet.</p>
          {!isEmployee && <Link to="/products" className="mt-2 inline-block text-pink-400 hover:text-pink-300">Start shopping</Link>}
        </div>
      ) : (
        <ul className={`divide-y divide-neutral-800 rounded-2xl border border-neutral-800 bg-neutral-900 ${loading ? 'opacity-50' : ''}`}>
          {data?.items.map(o => (
            <li key={o.id}>
              <Link to={`/orders/${o.id}`} className="flex flex-wrap items-center justify-between gap-4 p-4 hover:bg-neutral-800/50">
                <div>
                  <p className="font-medium">Order #{o.id}</p>
                  <p className="text-sm text-neutral-400">
                    {formatDate(o.createdAt)} · {o.items.reduce((n, i) => n + i.quantity, 0)} items
                  </p>
                </div>
                <div className="flex items-center gap-4">
                  <StatusBadge status={o.status} />
                  <span className="w-24 text-right font-medium">{money(o.total)}</span>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {data && totalPages > 1 && (
        <div className="flex items-center justify-end gap-3 text-sm">
          <button disabled={page === 0} onClick={() => show({ page: page - 1 })}
            className="rounded-lg border border-neutral-700 px-3 py-1 disabled:opacity-40">Previous</button>
          <span className="text-neutral-400">Page {page + 1} of {totalPages}</span>
          <button disabled={page + 1 >= totalPages} onClick={() => show({ page: page + 1 })}
            className="rounded-lg border border-neutral-700 px-3 py-1 disabled:opacity-40">Next</button>
        </div>
      )}
    </div>
  )
}
