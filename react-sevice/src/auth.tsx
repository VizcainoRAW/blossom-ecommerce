import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { api } from './api'
import { parseToken, type AccountType } from './jwt'

export type RegisterData = {
  firstName: string
  lastName: string
  identifier: string
  password: string
  documentType: string | null
  documentNumber: string | null
}

type Session = { token: string; accountType: AccountType }

type Auth = {
  token: string | null
  accountType: AccountType | null
  login: (identifier: string, password: string) => Promise<void>
  register: (data: RegisterData) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<Auth | null>(null)

function toSession(token: string | null): Session | null {
  const parsed = parseToken(token)
  if (!parsed) localStorage.removeItem('token') // drop expired/garbage tokens on load
  return parsed && { token: token!, accountType: parsed.accountType }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState(() => toSession(localStorage.getItem('token')))

  // api() fires this when the backend rejects our token (expired or revoked).
  useEffect(() => {
    const onExpired = () => setSession(null)
    window.addEventListener('auth:expired', onExpired)
    return () => window.removeEventListener('auth:expired', onExpired)
  }, [])

  // Login and register both answer with a JWT, so both end here.
  async function authenticate(path: string, body: object) {
    const res = await api<{ accessToken: string }>(path, { method: 'POST', body: JSON.stringify(body) })
    localStorage.setItem('token', res.accessToken)
    setSession(toSession(res.accessToken))
  }

  const login = (identifier: string, password: string) => authenticate('/auth/login', { identifier, password })
  const register = (data: RegisterData) => authenticate('/auth/register', data)

  async function logout() {
    // Revoke server-side, but always clear locally even if the token already expired.
    await api('/auth/logout', { method: 'POST' }).catch(() => {})
    localStorage.removeItem('token')
    setSession(null)
  }

  return (
    <AuthContext value={{
      token: session?.token ?? null, accountType: session?.accountType ?? null, login, register, logout,
    }}>
      {children}
    </AuthContext>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
