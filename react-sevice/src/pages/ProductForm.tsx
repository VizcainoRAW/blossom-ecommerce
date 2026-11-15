import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { api, type Category, type Product } from '../api'
import Field, { inputClass } from '../components/Field'
import { emptyVariant, validateProduct, type ProductErrors, type ProductForm as Form, type VariantForm } from '../validation'

const emptyForm: Form = { categoryId: '', name: '', description: '', imageUrls: '', variants: [{ ...emptyVariant }] }

const toForm = (p: Product): Form => ({
  categoryId: String(p.categoryId),
  name: p.name,
  description: p.description ?? '',
  imageUrls: p.imageUrls.join('\n'),
  variants: p.variants.map(v => ({ color: v.color, size: v.size, sku: v.sku, price: String(v.price), stock: String(v.stock) })),
})

const toRequest = (f: Form) => ({
  categoryId: Number(f.categoryId),
  name: f.name.trim(),
  description: f.description.trim() || null,
  imageUrls: f.imageUrls.split('\n').map(u => u.trim()).filter(Boolean),
  variants: f.variants.map(v => ({
    color: v.color.trim(), size: v.size.trim(), sku: v.sku.trim(), price: Number(v.price), stock: Number(v.stock),
  })),
})

const variantFields: { key: keyof VariantForm; label: string; inputMode?: 'decimal' | 'numeric' }[] = [
  { key: 'color', label: 'Color' },
  { key: 'size', label: 'Size' },
  { key: 'sku', label: 'SKU' },
  { key: 'price', label: 'Price', inputMode: 'decimal' },
  { key: 'stock', label: 'Stock', inputMode: 'numeric' },
]

export default function ProductForm() {
  const { id } = useParams()
  const isEdit = id !== undefined
  const navigate = useNavigate()
  const [categories, setCategories] = useState<Category[] | null>(null)
  const [form, setForm] = useState<Form>(emptyForm)
  const [errors, setErrors] = useState<ProductErrors>({})
  const [loadError, setLoadError] = useState('')
  const [serverError, setServerError] = useState('')
  const [loadingProduct, setLoadingProduct] = useState(isEdit)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    api<Category[]>('/categories').then(setCategories).catch(e => setLoadError(e.message))
  }, [])

  useEffect(() => {
    if (!isEdit) return
    api<Product>(`/products/${id}`)
      .then(p => setForm(toForm(p)))
      .catch(e => setLoadError(e.message))
      .finally(() => setLoadingProduct(false))
  }, [id, isEdit])

  const setField = (key: Exclude<keyof Form, 'variants'>) =>
    (e: { target: { value: string } }) => setForm(f => ({ ...f, [key]: e.target.value }))

  const setVariant = (i: number, key: keyof VariantForm, value: string) =>
    setForm(f => ({ ...f, variants: f.variants.map((v, j) => (j === i ? { ...v, [key]: value } : v)) }))

  // Errors are keyed by row index, so they would point at the wrong row after a removal.
  const removeVariant = (i: number) => {
    setForm(f => ({ ...f, variants: f.variants.filter((_, j) => j !== i) }))
    setErrors({})
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    const next = validateProduct(form)
    setErrors(next)
    if (Object.keys(next).length) return

    setSaving(true)
    setServerError('')
    try {
      await api(isEdit ? `/products/${id}` : '/products', {
        method: isEdit ? 'PUT' : 'POST',
        body: JSON.stringify(toRequest(form)),
      })
      navigate('/manage')
    } catch (err) {
      setServerError((err as Error).message)
    } finally {
      setSaving(false)
    }
  }

  if (loadError) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-10">
        <p role="alert" className="rounded-lg bg-red-950/50 p-4 text-red-300">{loadError}</p>
        <Link to="/manage" className="mt-4 inline-block text-pink-400 hover:text-pink-300">← Back to products</Link>
      </div>
    )
  }
  if (loadingProduct || !categories) return <p className="mx-auto max-w-3xl px-4 py-10 text-neutral-500">Loading…</p>

  return (
    <form onSubmit={onSubmit} noValidate className="mx-auto max-w-3xl space-y-6 px-4 py-10">
      <div>
        <Link to="/manage" className="text-sm text-neutral-400 hover:text-white">← Back to products</Link>
        <h1 className="mt-2 text-3xl font-semibold">{isEdit ? 'Edit product' : 'New product'}</h1>
      </div>
      {serverError && <p role="alert" className="rounded-lg bg-red-950/50 p-3 text-sm text-red-300">{serverError}</p>}

      <section className="space-y-4 rounded-2xl border border-neutral-800 bg-neutral-900 p-6">
        <div className="grid gap-4 sm:grid-cols-2">
          <Field id="name" label="Name" value={form.name} onChange={setField('name')} error={errors.name} />
          <div>
            <label htmlFor="categoryId" className="mb-1 block text-sm font-medium text-neutral-300">Category</label>
            <select id="categoryId" className={inputClass} value={form.categoryId} onChange={setField('categoryId')}
              aria-invalid={!!errors.categoryId}>
              <option value="">Choose…</option>
              {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
            {errors.categoryId && <p className="mt-1 text-sm text-red-400">{errors.categoryId}</p>}
            {categories.length === 0 && <p className="mt-1 text-sm text-amber-400">No categories exist yet.</p>}
          </div>
        </div>
        <div>
          <label htmlFor="description" className="mb-1 block text-sm font-medium text-neutral-300">Description</label>
          <textarea id="description" rows={4} className={inputClass} value={form.description}
            onChange={setField('description')} />
        </div>
        <div>
          <label htmlFor="imageUrls" className="mb-1 block text-sm font-medium text-neutral-300">
            Image URLs <span className="text-neutral-500">(one per line)</span>
          </label>
          <textarea id="imageUrls" rows={3} className={inputClass} value={form.imageUrls}
            onChange={setField('imageUrls')} aria-invalid={!!errors.imageUrls}
            placeholder="https://example.com/image.jpg" />
          {errors.imageUrls && <p className="mt-1 text-sm text-red-400">{errors.imageUrls}</p>}
        </div>
      </section>

      <section className="space-y-4 rounded-2xl border border-neutral-800 bg-neutral-900 p-6">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Variants</h2>
          <button type="button" onClick={() => setForm(f => ({ ...f, variants: [...f.variants, { ...emptyVariant }] }))}
            className="rounded-lg border border-neutral-700 px-3 py-1 text-sm hover:border-neutral-500">
            + Add variant
          </button>
        </div>
        {errors.variants && <p className="text-sm text-red-400">{errors.variants}</p>}

        {form.variants.map((v, i) => (
          <fieldset key={i} className="grid items-start gap-3 border-t border-neutral-800 pt-4 sm:grid-cols-[repeat(5,1fr)_auto]">
            <legend className="sr-only">Variant {i + 1}</legend>
            {variantFields.map(f => (
              <Field key={f.key} id={`variant-${i}-${f.key}`} label={f.label} inputMode={f.inputMode}
                value={v[f.key]} onChange={e => setVariant(i, f.key, e.target.value)}
                error={errors[`variants.${i}.${f.key}`]} />
            ))}
            <button type="button" onClick={() => removeVariant(i)} aria-label={`Remove variant ${i + 1}`}
              className="mt-7 rounded-lg px-2 py-2 text-neutral-500 hover:bg-red-950/50 hover:text-red-300">✕</button>
          </fieldset>
        ))}
      </section>

      <div className="flex justify-end gap-3">
        <Link to="/manage" className="rounded-lg border border-neutral-700 px-4 py-2 hover:border-neutral-500">Cancel</Link>
        <button disabled={saving}
          className="rounded-lg bg-pink-600 px-6 py-2 font-medium text-white hover:bg-pink-500 disabled:opacity-50">
          {saving ? 'Saving…' : isEdit ? 'Save changes' : 'Create product'}
        </button>
      </div>
    </form>
  )
}
