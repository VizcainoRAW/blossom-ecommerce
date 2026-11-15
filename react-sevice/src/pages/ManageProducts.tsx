import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, type Category, type Page, type Product } from '../api'
import { inputClass } from '../components/Field'

const PAGE_SIZE = 20
import { money } from '../format'

export default function ManageProducts() {
  const [categories, setCategories] = useState<Category[]>([])
  const [name, setName] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState<Page<Product> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    api<Category[]>('/categories').then(setCategories).catch(() => {})
  }, [])

  useEffect(() => {
    let cancelled = false
    const t = setTimeout(() => {
      const params = new URLSearchParams({ page: String(page), size: String(PAGE_SIZE) })
      if (name.trim()) params.set('name', name.trim())
      setLoading(true)
      setError('')
      api<Page<Product>>(`/products?${params}`)
        .then(d => { if (!cancelled) setData(d) })
        .catch(e => { if (!cancelled) setError(e.message) })
        .finally(() => { if (!cancelled) setLoading(false) })
    }, 300)
    return () => { cancelled = true; clearTimeout(t) }
  }, [name, page])

  const categoryName = (id: number) => categories.find(c => c.id === id)?.name ?? '—'
  const totalPages = data ? Math.max(1, Math.ceil(data.totalItems / PAGE_SIZE)) : 1

  return (
    <div className="mx-auto max-w-6xl space-y-6 px-4 py-10">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-3xl font-semibold">Manage products</h1>
        <Link to="/manage/new" className="rounded-lg bg-pink-600 px-4 py-2 font-medium text-white hover:bg-pink-500">
          + New product
        </Link>
      </div>

      <input aria-label="Search by name" placeholder="Search products…" className={`${inputClass} max-w-sm`}
        value={name} onChange={e => { setName(e.target.value); setPage(0) }} />

      {error ? (
        <p role="alert" className="rounded-lg bg-red-950/50 p-4 text-red-300">{error}</p>
      ) : loading && !data ? (
        <p className="text-neutral-500">Loading products…</p>
      ) : data?.items.length === 0 ? (
        <p className="text-neutral-500">No products found.</p>
      ) : (
        <div className={`overflow-x-auto rounded-xl border border-neutral-800 ${loading ? 'opacity-50' : ''}`}>
          <table className="w-full text-left text-sm">
            <thead className="bg-neutral-900 text-neutral-400">
              <tr>
                <th className="p-3 font-medium">Product</th>
                <th className="p-3 font-medium">Category</th>
                <th className="p-3 font-medium">Variants</th>
                <th className="p-3 font-medium">Stock</th>
                <th className="p-3 font-medium">Price</th>
                <th className="p-3"><span className="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-neutral-800">
              {data?.items.map(p => {
                const prices = p.variants.map(v => v.price)
                return (
                  <tr key={p.id} className="hover:bg-neutral-900/60">
                    <td className="flex items-center gap-3 p-3">
                      {p.imageUrls[0]
                        ? <img src={p.imageUrls[0]} alt="" className="h-10 w-10 rounded object-cover" />
                        : <div className="h-10 w-10 rounded bg-neutral-800" />}
                      <span className="font-medium">{p.name}</span>
                    </td>
                    <td className="p-3 text-neutral-400">{categoryName(p.categoryId)}</td>
                    <td className="p-3 text-neutral-400">{p.variants.length}</td>
                    <td className="p-3 text-neutral-400">{p.variants.reduce((s, v) => s + v.stock, 0)}</td>
                    <td className="p-3 text-neutral-400">
                      {prices.length ? money(Math.min(...prices)) : '—'}
                      {prices.length > 1 && Math.max(...prices) !== Math.min(...prices) && ` – ${money(Math.max(...prices))}`}
                    </td>
                    <td className="p-3 text-right">
                      <Link to={`/manage/${p.id}`} className="text-pink-400 hover:text-pink-300">Edit</Link>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {data && totalPages > 1 && (
        <div className="flex items-center justify-end gap-3 text-sm">
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)}
            className="rounded-lg border border-neutral-700 px-3 py-1 disabled:opacity-40">Previous</button>
          <span className="text-neutral-400">Page {page + 1} of {totalPages}</span>
          <button disabled={page + 1 >= totalPages} onClick={() => setPage(p => p + 1)}
            className="rounded-lg border border-neutral-700 px-3 py-1 disabled:opacity-40">Next</button>
        </div>
      )}
    </div>
  )
}
