import { Link } from 'react-router'
import { useCart } from '../cartContext'
import { money } from '../format'

export default function Cart() {
  const { items, total, setQty, remove } = useCart()

  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-20 text-center">
        <h1 className="text-3xl font-semibold">Your cart is empty</h1>
        <Link to="/products" className="mt-6 inline-block rounded-full bg-pink-600 px-6 py-3 font-medium text-white hover:bg-pink-500">
          Start shopping
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-10">
      <h1 className="mb-6 text-3xl font-semibold">Your cart</h1>
      <div className="grid gap-8 lg:grid-cols-[1fr_320px]">
        <ul className="divide-y divide-neutral-800 rounded-2xl border border-neutral-800 bg-neutral-900">
          {items.map(i => (
            <li key={i.variantId} className="flex gap-4 p-4">
              <Link to={`/products/${i.productId}`} className="shrink-0">
                {i.image
                  ? <img src={i.image} alt="" className="h-24 w-24 rounded-lg object-cover" />
                  : <div className="h-24 w-24 rounded-lg bg-neutral-800" />}
              </Link>
              <div className="flex flex-1 flex-col justify-between">
                <div className="flex justify-between gap-4">
                  <div>
                    <Link to={`/products/${i.productId}`} className="font-medium hover:text-pink-400">{i.name}</Link>
                    <p className="text-sm text-neutral-400">{i.color} · {i.size}</p>
                  </div>
                  <p className="font-medium">{money(i.price * i.quantity)}</p>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center rounded-lg border border-neutral-700">
                    <button onClick={() => setQty(i.variantId, i.quantity - 1)} aria-label={`Decrease ${i.name}`}
                      className="px-3 py-1 hover:bg-neutral-800">−</button>
                    <span className="w-8 text-center text-sm" aria-label="Quantity">{i.quantity}</span>
                    <button onClick={() => setQty(i.variantId, i.quantity + 1)} aria-label={`Increase ${i.name}`}
                      disabled={i.quantity >= i.stock} className="px-3 py-1 hover:bg-neutral-800 disabled:opacity-40">+</button>
                  </div>
                  <button onClick={() => remove(i.variantId)} className="text-sm text-neutral-400 hover:text-red-300">
                    Remove
                  </button>
                </div>
              </div>
            </li>
          ))}
        </ul>

        <aside className="h-fit space-y-4 rounded-2xl border border-neutral-800 bg-neutral-900 p-6">
          <h2 className="text-lg font-semibold">Summary</h2>
          <div className="flex justify-between text-neutral-300">
            <span>Subtotal</span><span>{money(total)}</span>
          </div>
          <p className="text-xs text-neutral-500">Final prices and stock are confirmed when you place the order.</p>
          <Link to="/checkout" className="block rounded-lg bg-pink-600 py-2 text-center font-medium text-white hover:bg-pink-500">
            Checkout
          </Link>
        </aside>
      </div>
    </div>
  )
}
