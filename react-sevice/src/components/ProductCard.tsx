import { Link } from 'react-router'
import type { Product } from '../api'
import { money } from '../format'

export default function ProductCard({ product }: { product: Product }) {
  const prices = product.variants.map(v => v.price)
  const inStock = product.variants.some(v => v.stock > 0)
  return (
    <Link to={`/products/${product.id}`}
      className="group block overflow-hidden rounded-xl border border-neutral-800 bg-neutral-900 transition hover:border-pink-500/50">
      {product.imageUrls[0]
        ? <img src={product.imageUrls[0]} alt={product.name}
            className="h-56 w-full object-cover transition duration-300 group-hover:scale-105" />
        : <div className="h-56 bg-gradient-to-br from-neutral-800 to-neutral-900" />}
      <div className="p-4">
        <h3 className="font-medium text-neutral-100">{product.name}</h3>
        <div className="mt-1 flex items-center justify-between">
          {prices.length > 0 && <p className="text-pink-400">From {money(Math.min(...prices))}</p>}
          {!inStock && <span className="text-xs text-neutral-500">Sold out</span>}
        </div>
      </div>
    </Link>
  )
}
