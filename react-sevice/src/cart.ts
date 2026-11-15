// Pure cart logic, kept apart from React so it can be tested with plain node.

export type CartItem = {
  variantId: number; productId: number; name: string; image?: string
  color: string; size: string; sku: string
  price: number // snapshot at add time; the backend charges the current price
  stock: number // snapshot at add time; the backend re-checks stock when the order is placed
  quantity: number
}

export function addItem(items: CartItem[], item: Omit<CartItem, 'quantity'>, qty: number): CartItem[] {
  const existing = items.find(i => i.variantId === item.variantId)
  const quantity = Math.min((existing?.quantity ?? 0) + qty, item.stock)
  if (quantity <= 0) return items
  return existing
    ? items.map(i => (i.variantId === item.variantId ? { ...item, quantity } : i))
    : [...items, { ...item, quantity }]
}

export function setQuantity(items: CartItem[], variantId: number, qty: number): CartItem[] {
  if (qty <= 0) return removeItem(items, variantId)
  return items.map(i => (i.variantId === variantId ? { ...i, quantity: Math.min(qty, i.stock) } : i))
}

export const removeItem = (items: CartItem[], variantId: number) => items.filter(i => i.variantId !== variantId)

// Display only: float math is fine here because the backend computes the real total.
export const cartTotal = (items: CartItem[]) => items.reduce((sum, i) => sum + i.price * i.quantity, 0)
export const cartCount = (items: CartItem[]) => items.reduce((sum, i) => sum + i.quantity, 0)

/** Restores a saved cart, dropping anything that is not a well-formed item. */
export function loadCart(raw: string | null): CartItem[] {
  try {
    const parsed = JSON.parse(raw ?? '[]')
    if (!Array.isArray(parsed)) return []
    return parsed.filter(i =>
      Number.isInteger(i?.variantId) && Number.isInteger(i?.quantity) && i.quantity > 0 && typeof i.price === 'number')
  } catch {
    return []
  }
}
