import type { InputHTMLAttributes } from 'react'

export const inputClass =
  'w-full rounded-lg border border-neutral-700 bg-neutral-800 px-3 py-2 text-neutral-100 placeholder-neutral-500 focus:border-pink-500 focus:outline-none focus:ring-1 focus:ring-pink-500'

type Props = InputHTMLAttributes<HTMLInputElement> & { id: string; label: string; error?: string }

export default function Field({ id, label, error, ...input }: Props) {
  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-medium text-neutral-300">{label}</label>
      <input id={id} className={inputClass} aria-invalid={!!error}
        aria-describedby={error ? `${id}-error` : undefined} {...input} />
      {error && <p id={`${id}-error`} className="mt-1 text-sm text-red-400">{error}</p>}
    </div>
  )
}
