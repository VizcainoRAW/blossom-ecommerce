import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router'
import { useAuth } from '../auth'

/** Sends guests to login (and back afterwards); with `employee`, also keeps customers out. */
export default function RequireAuth({ children, employee = false }: { children: ReactNode; employee?: boolean }) {
  const { token, accountType } = useAuth()
  const location = useLocation()
  if (!token) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (employee && accountType !== 'EMPLOYEE') return <Navigate to="/" replace />
  return children
}
