export type Category = { id: number; name: string }
export type Variant = { id: number; color: string; size: string; sku: string; price: number; stock: number }
export type Product = {
  id: number; categoryId: number; name: string; description: string | null
  imageUrls: string[]; variants: Variant[]
}
export type Page<T> = { items: T[]; page: number; size: number; totalItems: number }

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
