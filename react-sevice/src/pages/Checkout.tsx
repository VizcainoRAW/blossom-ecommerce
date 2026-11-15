import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { api, type Order } from '../api'
import { useAuth } from '../auth'
import { useCart } from '../cartContext'
import { inputClass } from '../components/Field'
import { money } from '../format'

export default function Checkout() {
  const { accountType } = useAuth()
  const { items, total, clear } = useCart()
  const navigate = useNavigate()
  const [address, setAddress] = useState('')
  const [addressError, setAddressError] = useState('')
  const [serverError, setServerError] = useState('')
  const [placing, setPlacing] = useState(false)

  if (accountType === 'EMPLOYEE') {
    return <p className="mx-auto max-w-3xl px-4 py-20 text-center text-neutral-400">Employee accounts cannot place orders.</p>
  }
  if (items.length === 0) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-20 text-center">
        <p className="text-neutral-400">Your cart is empty.</p>
        <Link to="/products" className="mt-4 inline-block text-pink-400 hover:text-pink-300">Go to the shop</Link>
      </div>
    )
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    const trimmed = address.trim()
    const error = !trimmed ? 'Shipping address is required'
      : trimmed.length < 10 ? 'Please enter the full address (street, city, details)' : ''
    setAddressError(error)
    if (error) return

    setPlacing(true)
    setServerError('')
    try {
      const order = await api<Order>('/orders', {
        method: 'POST',
        body: JSON.stringify({
          shippingAddress: trimmed,
          items: items.map(i => ({ variantId: i.variantId, quantity: i.quantity })),
        }),
      })
      clear()
      navigate(`/orders/${order.id}`, { state: { placed: true } })
    } catch (err) {
      // e.g. 409 "Not enough stock for SKU": the cart stays so the customer can adjust it.
      setServerError((err as Error).message)
    } finally {
      setPlacing(false)
    }
  }

  return (
    <form onSubmit={onSubmit} noValidate className="mx-auto max-w-5xl px-4 py-10">
      <h1 className="mb-6 text-3xl font-semibold">Checkout</h1>
      <div className="grid gap-8 lg:grid-cols-[1fr_360px]">
        <section className="space-y-4 rounded-2xl border border-neutral-800 bg-neutral-900 p-6">
          <h2 className="text-lg font-semibold">Shipping</h2>
          {serverError && (
            <p role="alert" className="rounded-lg bg-red-950/50 p-3 text-sm text-red-300">
              {serverError} <Link to="/cart" className="underline">Review cart</Link>
            </p>
          )}
          <div>
            <label htmlFor="address" className="mb-1 block text-sm font-medium text-neutral-300">Shipping address</label>
            <textarea id="address" rows={4} className={inputClass} value={address} autoComplete="street-address"
              placeholder="Street, number, apartment, city" aria-invalid={!!addressError}
              aria-describedby={addressError ? 'address-error' : undefined}
              onChange={e => setAddress(e.target.value)} />
            {addressError && <p id="address-error" className="mt-1 text-sm text-red-400">{addressError}</p>}
          </div>
          <p className="text-sm text-neutral-500">
            Your order will stay <strong>pending</strong> until our team confirms the payment.
          </p>
        </section>

        <aside className="h-fit space-y-4 rounded-2xl border border-neutral-800 bg-neutral-900 p-6">
          <h2 className="text-lg font-semibold">Order summary</h2>
          <ul className="space-y-3 text-sm">
            {items.map(i => (
              <li key={i.variantId} className="flex justify-between gap-4">
                <span className="text-neutral-300">
                  {i.quantity} × {i.name} <span className="text-neutral-500">({i.color} · {i.size})</span>
                </span>
                <span>{money(i.price * i.quantity)}</span>
              </li>
            ))}
          </ul>
          <div className="flex justify-between border-t border-neutral-800 pt-4 font-semibold">
            <span>Total</span><span>{money(total)}</span>
          </div>
          <button disabled={placing}
            className="w-full rounded-lg bg-pink-600 py-2 font-medium text-white hover:bg-pink-500 disabled:opacity-50">
            {placing ? 'Placing order…' : 'Place order'}
          </button>
        </aside>
      </div>
    </form>
  )
}
