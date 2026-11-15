import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { addItem, cartCount, cartTotal, loadCart, removeItem, setQuantity, type CartItem } from './cart'

type Cart = {
  items: CartItem[]
  count: number
  total: number
  add: (item: Omit<CartItem, 'quantity'>, qty: number) => void
  setQty: (variantId: number, qty: number) => void
  remove: (variantId: number) => void
  clear: () => void
}

const CartContext = createContext<Cart | null>(null)

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState(() => loadCart(localStorage.getItem('cart')))

  useEffect(() => localStorage.setItem('cart', JSON.stringify(items)), [items])

  return (
    <CartContext value={{
      items,
      count: cartCount(items),
      total: cartTotal(items),
      add: (item, qty) => setItems(i => addItem(i, item, qty)),
      setQty: (variantId, qty) => setItems(i => setQuantity(i, variantId, qty)),
      remove: variantId => setItems(i => removeItem(i, variantId)),
      clear: () => setItems([]),
    }}>
      {children}
    </CartContext>
  )
}

export function useCart() {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used inside CartProvider')
  return ctx
}
