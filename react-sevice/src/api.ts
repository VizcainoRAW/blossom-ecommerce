export type Category = { id: number; name: string }
export type Variant = { id: number; color: string; size: string; sku: string; price: number; stock: number }
export type Product = {
  id: number; categoryId: number; name: string; description: string | null
  imageUrls: string[]; variants: Variant[]
}
export type Page<T> = { items: T[]; page: number; size: number; totalItems: number }

export type OrderStatus = 'PENDING' | 'PAID' | 'SHIPPED' | 'CANCELLED'
export type OrderItem = {
  variantId: number; productName: string; sku: string; color: string; size: string
  quantity: number; unitPrice: number; subtotal: number
}
export type Order = {
  id: number; customerId: string; status: OrderStatus; total: number
  shippingAddress: string; createdAt: string; items: OrderItem[]
}
// Mirrors OrderStatus.canTransitionTo on the backend.
export const nextStatuses: Record<OrderStatus, OrderStatus[]> = {
  PENDING: ['PAID', 'CANCELLED'],
  PAID: ['SHIPPED', 'CANCELLED'],
  SHIPPED: [],
  CANCELLED: [],
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('token')
  const res = await fetch(`/api${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token && { Authorization: `Bearer ${token}` }),
      ...init.headers,
    },
  })
  if (res.status === 401 && token) {
    localStorage.removeItem('token')
    window.dispatchEvent(new Event('auth:expired'))
    throw new Error('Your session has expired. Please log in again.')
  }
  if (!res.ok) {
    // Backend returns ProblemDetail: { title, detail, ... }
    const body = await res.json().catch(() => null)
    throw new Error(body?.detail || body?.title || `Request failed (${res.status})`)
  }
  return res.status === 204 ? (undefined as T) : res.json()
}
