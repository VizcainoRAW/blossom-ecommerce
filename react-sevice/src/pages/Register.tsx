import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { useAuth } from '../auth'
import Field, { inputClass } from '../components/Field'
import { emptyRegisterForm, validateRegister, type RegisterErrors, type RegisterForm } from '../validation'

// Mirrors the backend DocumentType enum.
const documentTypes = [
  { value: 'CC', label: 'Cédula de ciudadanía' },
  { value: 'TI', label: 'Tarjeta de identidad' },
  { value: 'CE', label: 'Cédula de extranjería' },
  { value: 'NIT', label: 'NIT' },
  { value: 'PASSPORT', label: 'Passport' },
]

export default function Register() {
  const { token, register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(emptyRegisterForm)
  const [errors, setErrors] = useState<RegisterErrors>({})
  const [serverError, setServerError] = useState('')
  const [loading, setLoading] = useState(false)

  if (token) return <Navigate to="/" replace />

  const bind = (key: keyof RegisterForm) => ({
    value: form[key],
    onChange: (e: { target: { value: string } }) => setForm(f => ({ ...f, [key]: e.target.value })),
    error: errors[key],
  })

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    const next = validateRegister(form)
    setErrors(next)
    if (Object.keys(next).length) return

    setLoading(true)
    setServerError('')
    try {
      await register({
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        identifier: form.identifier.trim(),
        password: form.password,
        documentType: form.documentType || null,
        documentNumber: form.documentNumber.trim() || null,
      })
      navigate('/')
    } catch (err) {
      setServerError((err as Error).message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={onSubmit} noValidate
      className="mx-auto my-16 max-w-lg space-y-4 rounded-2xl border border-neutral-800 bg-neutral-900 p-8">
      <h1 className="text-2xl font-semibold">Create your account</h1>
      {serverError && <p role="alert" className="rounded-lg bg-red-950/50 p-2 text-sm text-red-300">{serverError}</p>}

      <div className="grid gap-4 sm:grid-cols-2">
        <Field id="firstName" label="First name" autoComplete="given-name" {...bind('firstName')} />
        <Field id="lastName" label="Last name" autoComplete="family-name" {...bind('lastName')} />
      </div>
      <Field id="identifier" label="Email" type="email" autoComplete="email" {...bind('identifier')} />
      <div className="grid gap-4 sm:grid-cols-2">
        <Field id="password" label="Password" type="password" autoComplete="new-password" {...bind('password')} />
        <Field id="confirmPassword" label="Confirm password" type="password" autoComplete="new-password"
          {...bind('confirmPassword')} />
      </div>

      <fieldset className="grid gap-4 sm:grid-cols-2">
        <legend className="mb-2 text-sm text-neutral-500">Identity document (optional)</legend>
        <div>
          <label htmlFor="documentType" className="mb-1 block text-sm font-medium text-neutral-300">Document type</label>
          <select id="documentType" className={inputClass} value={form.documentType}
            aria-invalid={!!errors.documentType}
            onChange={e => setForm(f => ({ ...f, documentType: e.target.value }))}>
            <option value="">None</option>
            {documentTypes.map(d => <option key={d.value} value={d.value}>{d.label}</option>)}
          </select>
          {errors.documentType && <p className="mt-1 text-sm text-red-400">{errors.documentType}</p>}
        </div>
        <Field id="documentNumber" label="Document number" {...bind('documentNumber')} />
      </fieldset>

      <button disabled={loading}
        className="w-full rounded-lg bg-pink-600 py-2 font-medium text-white hover:bg-pink-500 disabled:opacity-50">
        {loading ? 'Creating account…' : 'Sign up'}
      </button>
      <p className="text-center text-sm text-neutral-400">
        Already have an account? <Link to="/login" className="text-pink-400 hover:text-pink-300">Log in</Link>
      </p>
    </form>
  )
}
