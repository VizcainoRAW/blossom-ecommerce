import { useEffect, useState, type ChangeEvent } from 'react'
import { api, type Category, type Page, type Product } from '../api'
import { inputClass } from '../components/Field'
import ProductCard from '../components/ProductCard'

export default function Products() {
  const [categories, setCategories] = useState<Category[]>([])
  const [filters, setFilters] = useState({ name: '', categoryId: '', minPrice: '', maxPrice: '' })
  const [data, setData] = useState<Page<Product> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const badRange = !!filters.minPrice && !!filters.maxPrice && Number(filters.minPrice) > Number(filters.maxPrice)

  useEffect(() => {
    api<Category[]>('/categories').then(setCategories).catch(() => {}) // product list still works without it
  }, [])

  useEffect(() => {
    if (badRange) return
    let cancelled = false
    // Debounce so typing in the name box doesn't fire a request per keystroke.
    const t = setTimeout(() => {
      const params = new URLSearchParams(Object.entries(filters).filter(([, v]) => v.trim()))
      setLoading(true)
      setError('')
      api<Page<Product>>(`/products?${params}`)
        .then(d => { if (!cancelled) setData(d) })
        .catch(e => { if (!cancelled) setError(e.message) })
        .finally(() => { if (!cancelled) setLoading(false) })
    }, 300)
    return () => { cancelled = true; clearTimeout(t) }
  }, [filters, badRange])

  const set = (key: keyof typeof filters) =>
    (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setFilters(f => ({ ...f, [key]: e.target.value }))

  return (
    <div className="mx-auto max-w-6xl space-y-6 px-4 py-10">
      <h1 className="text-3xl font-semibold">Shop</h1>
      <div className="grid gap-3 sm:grid-cols-4">
        <input aria-label="Search by name" placeholder="Search products…" className={inputClass}
          value={filters.name} onChange={set('name')} />
        <select aria-label="Category" className={inputClass} value={filters.categoryId} onChange={set('categoryId')}>
          <option value="">All categories</option>
          {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <input aria-label="Min price" type="number" min="0" placeholder="Min price" className={inputClass}
          value={filters.minPrice} onChange={set('minPrice')} />
        <input aria-label="Max price" type="number" min="0" placeholder="Max price" className={inputClass}
          value={filters.maxPrice} onChange={set('maxPrice')} />
      </div>
      {badRange && <p className="text-sm text-red-400">Min price cannot be greater than max price.</p>}

      {error ? (
        <p role="alert" className="rounded-lg bg-red-950/50 p-4 text-red-300">{error}</p>
      ) : loading && !data ? (
        <p className="text-neutral-500">Loading products…</p>
      ) : data?.items.length === 0 ? (
        <p className="text-neutral-500">No products match your filters.</p>
      ) : (
        <div className={`grid gap-6 sm:grid-cols-2 lg:grid-cols-4 ${loading ? 'opacity-50' : ''}`}>
          {data?.items.map(p => <ProductCard key={p.id} product={p} />)}
        </div>
      )}
    </div>
  )
}
