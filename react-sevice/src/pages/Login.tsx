import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation } from 'react-router'
import { useAuth } from '../auth'
import Field from '../components/Field'

export default function Login() {
  const { token, login } = useAuth()
  // Set by RequireAuth so we return to the page that asked for login.
  const from = (useLocation().state as { from?: string } | null)?.from ?? '/'
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<{ identifier?: string; password?: string }>({})
  const [serverError, setServerError] = useState('')
  const [loading, setLoading] = useState(false)

  // Also performs the redirect right after a successful login.
  if (token) return <Navigate to={from} replace />

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    const next = {
      identifier: identifier.trim() ? undefined : 'Email or username is required',
      password: password ? undefined : 'Password is required',
    }
    setErrors(next)
    if (next.identifier || next.password) return

    setLoading(true)
    setServerError('')
    try {
      await login(identifier.trim(), password)
    } catch (err) {
      setServerError((err as Error).message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={onSubmit} noValidate
      className="mx-auto my-16 max-w-sm space-y-4 rounded-2xl border border-neutral-800 bg-neutral-900 p-8">
      <h1 className="text-2xl font-semibold">Welcome back</h1>
      {serverError && <p role="alert" className="rounded-lg bg-red-950/50 p-2 text-sm text-red-300">{serverError}</p>}

      <Field id="identifier" label="Email or username" autoComplete="username"
        value={identifier} onChange={e => setIdentifier(e.target.value)} error={errors.identifier} />
      <Field id="password" label="Password" type="password" autoComplete="current-password"
        value={password} onChange={e => setPassword(e.target.value)} error={errors.password} />

      <button disabled={loading}
        className="w-full rounded-lg bg-pink-600 py-2 font-medium text-white hover:bg-pink-500 disabled:opacity-50">
        {loading ? 'Logging in…' : 'Log in'}
      </button>
      <p className="text-center text-sm text-neutral-400">
        No account? <Link to="/register" className="text-pink-400 hover:text-pink-300">Sign up</Link>
      </p>
    </form>
  )
}
