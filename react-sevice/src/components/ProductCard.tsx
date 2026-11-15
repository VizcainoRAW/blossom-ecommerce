import type { Product } from '../api'

const fmt = new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' })

export default function ProductCard({ product }: { product: Product }) {
  const prices = product.variants.map(v => v.price)
  return (
    <article className="group overflow-hidden rounded-xl border border-neutral-800 bg-neutral-900 transition hover:border-pink-500/50">
      {product.imageUrls[0]
        ? <img src={product.imageUrls[0]} alt={product.name}
            className="h-56 w-full object-cover transition duration-300 group-hover:scale-105" />
        : <div className="h-56 bg-gradient-to-br from-neutral-800 to-neutral-900" />}
      <div className="p-4">
        <h3 className="font-medium text-neutral-100">{product.name}</h3>
        {prices.length > 0 && <p className="mt-1 text-pink-400">From {fmt.format(Math.min(...prices))}</p>}
      </div>
    </article>
  )
}
