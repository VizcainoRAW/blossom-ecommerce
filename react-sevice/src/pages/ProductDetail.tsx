import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { api, type Product } from '../api'
import { useAuth } from '../auth'
import { useCart } from '../cartContext'
import { inputClass } from '../components/Field'
import { money } from '../format'

const unique = (xs: string[]) => [...new Set(xs)]

export default function ProductDetail() {
  const { id } = useParams()
  const { accountType } = useAuth()
  const cart = useCart()
  const [product, setProduct] = useState<Product | null>(null)
  const [error, setError] = useState('')
  const [image, setImage] = useState(0)
  const [color, setColor] = useState('')
  const [size, setSize] = useState('')
  const [qty, setQty] = useState(1)
  const [added, setAdded] = useState(false)

  useEffect(() => {
    api<Product>(`/products/${id}`)
      .then(p => {
        setProduct(p)
        const first = p.variants.find(v => v.stock > 0) ?? p.variants[0]
        if (first) { setColor(first.color); setSize(first.size) }
      })
      .catch(e => setError(e.message))
  }, [id])

  if (error) {
    return (
      <div className="mx-auto max-w-6xl px-4 py-10">
        <p role="alert" className="rounded-lg bg-red-950/50 p-4 text-red-300">{error}</p>
        <Link to="/products" className="mt-4 inline-block text-pink-400 hover:text-pink-300">← Back to shop</Link>
      </div>
    )
  }
  if (!product) return <p className="mx-auto max-w-6xl px-4 py-10 text-neutral-500">Loading product…</p>

  const colors = unique(product.variants.map(v => v.color))
  const sizes = unique(product.variants.filter(v => v.color === color).map(v => v.size))
  const variant = product.variants.find(v => v.color === color && v.size === size)
  const inCart = cart.items.find(i => i.variantId === variant?.id)?.quantity ?? 0
  const available = variant ? variant.stock - inCart : 0

  function pickColor(c: string) {
    setColor(c)
    // Keep the size if this color has it, otherwise jump to the first size it does have.
    const options = product!.variants.filter(v => v.color === c)
    if (!options.some(v => v.size === size)) setSize(options[0].size)
    setQty(1)
    setAdded(false)
  }

  function addToCart() {
    if (!variant) return
    cart.add({
      variantId: variant.id, productId: product!.id, name: product!.name, image: product!.imageUrls[0],
      color: variant.color, size: variant.size, sku: variant.sku, price: variant.price, stock: variant.stock,
    }, qty)
    setQty(1)
    setAdded(true)
  }

  const chip = (active: boolean) =>
    `rounded-lg border px-3 py-1.5 text-sm transition ${active ? 'border-pink-500 bg-pink-500/10 text-white' : 'border-neutral-700 text-neutral-300 hover:border-neutral-500'}`

  return (
    <div className="mx-auto max-w-6xl px-4 py-10">
      <Link to="/products" className="text-sm text-neutral-400 hover:text-white">← Back to shop</Link>
      <div className="mt-4 grid gap-10 md:grid-cols-2">
        <div className="space-y-3">
          {product.imageUrls[image]
            ? <img src={product.imageUrls[image]} alt={product.name} className="aspect-square w-full rounded-2xl object-cover" />
            : <div className="aspect-square rounded-2xl bg-gradient-to-br from-neutral-800 to-neutral-900" />}
          {product.imageUrls.length > 1 && (
            <div className="flex gap-2">
              {product.imageUrls.map((url, i) => (
                <button key={url} onClick={() => setImage(i)} aria-label={`Show image ${i + 1}`}
                  className={`h-16 w-16 overflow-hidden rounded-lg border-2 ${i === image ? 'border-pink-500' : 'border-transparent'}`}>
                  <img src={url} alt="" className="h-full w-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="space-y-6">
          <div>
            <h1 className="text-3xl font-semibold">{product.name}</h1>
            {variant && <p className="mt-2 text-2xl text-pink-400">{money(variant.price)}</p>}
          </div>
          {product.description && <p className="whitespace-pre-line text-neutral-400">{product.description}</p>}

          {product.variants.length === 0 ? (
            <p className="text-neutral-500">This product is not available right now.</p>
          ) : (
            <>
              <div>
                <h2 className="mb-2 text-sm font-medium text-neutral-300">Color</h2>
                <div className="flex flex-wrap gap-2">
                  {colors.map(c => <button key={c} onClick={() => pickColor(c)} className={chip(c === color)}>{c}</button>)}
                </div>
              </div>
              <div>
                <h2 className="mb-2 text-sm font-medium text-neutral-300">Size</h2>
                <div className="flex flex-wrap gap-2">
                  {sizes.map(s => (
                    <button key={s} onClick={() => { setSize(s); setQty(1); setAdded(false) }} className={chip(s === size)}>{s}</button>
                  ))}
                </div>
              </div>

              <p className="text-sm text-neutral-400">
                {!variant || variant.stock === 0 ? 'Out of stock'
                  : available === 0 ? 'You already have all available units in your cart'
                  : `${variant.stock} in stock`}
              </p>

              {accountType === 'EMPLOYEE' ? (
                <p className="text-sm text-neutral-500">Employee accounts cannot place orders.</p>
              ) : (
                <div className="flex gap-3">
                  <input type="number" aria-label="Quantity" min={1} max={Math.max(available, 1)} value={qty}
                    disabled={available <= 0} className={`${inputClass} w-24`}
                    onChange={e => setQty(Math.min(Math.max(1, Math.floor(Number(e.target.value)) || 1), Math.max(available, 1)))} />
                  <button onClick={addToCart} disabled={available <= 0}
                    className="flex-1 rounded-lg bg-pink-600 py-2 font-medium text-white hover:bg-pink-500 disabled:opacity-40">
                    Add to cart
                  </button>
                </div>
              )}
              {added && (
                <p role="status" className="text-sm text-emerald-400">
                  Added to cart. <Link to="/cart" className="underline">View cart</Link>
                </p>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  )
}
