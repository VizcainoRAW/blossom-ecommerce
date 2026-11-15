import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { api, type Page, type Product } from '../api'
import ProductCard from '../components/ProductCard'

const perks = [
  { title: 'Free shipping', text: 'On every order over $50.' },
  { title: 'Easy returns', text: '30 days to change your mind.' },
  { title: 'Secure checkout', text: 'Your data stays protected.' },
]

export default function Home() {
  const [featured, setFeatured] = useState<Product[] | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    // shortcut: "featured" = first 4 products, until the backend has a featured flag.
    api<Page<Product>>('/products?size=4')
      .then(d => setFeatured(d.items))
      .catch(e => setError(e.message))
  }, [])

  return (
    <>
      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_at_top,rgba(236,72,153,0.25),transparent_60%)]" />
        <div className="relative mx-auto max-w-6xl px-4 py-28 text-center">
          <h1 className="text-4xl font-bold tracking-tight sm:text-6xl">
            Style that <span className="text-pink-500">blossoms</span> with you
          </h1>
          <p className="mx-auto mt-6 max-w-xl text-lg text-neutral-400">
            Discover pieces made to last, picked for every season and every occasion.
          </p>
          <div className="mt-10 flex justify-center gap-4">
            <Link to="/products" className="rounded-full bg-pink-600 px-6 py-3 font-medium text-white hover:bg-pink-500">
              Shop now
            </Link>
            <Link to="/register"
              className="rounded-full border border-neutral-700 px-6 py-3 font-medium text-neutral-200 hover:border-neutral-500">
              Create account
            </Link>
          </div>
        </div>
      </section>

      {/* Featured products */}
      <section className="mx-auto max-w-6xl px-4 py-16">
        <div className="mb-8 flex items-end justify-between">
          <h2 className="text-2xl font-semibold">Featured products</h2>
          <Link to="/products" className="text-sm text-pink-400 hover:text-pink-300">View all →</Link>
        </div>
        {error ? (
          <p role="alert" className="rounded-lg bg-red-950/50 p-4 text-red-300">{error}</p>
        ) : !featured ? (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4" aria-label="Loading products">
            {Array.from({ length: 4 }, (_, i) => <div key={i} className="h-72 animate-pulse rounded-xl bg-neutral-900" />)}
          </div>
        ) : featured.length === 0 ? (
          <p className="text-neutral-500">New products are coming soon.</p>
        ) : (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {featured.map(p => <ProductCard key={p.id} product={p} />)}
          </div>
        )}
      </section>

      {/* Perks */}
      <section className="mx-auto grid max-w-6xl gap-6 px-4 pb-20 sm:grid-cols-3">
        {perks.map(p => (
          <div key={p.title} className="rounded-xl border border-neutral-800 bg-neutral-900 p-6">
            <h3 className="font-semibold text-pink-400">{p.title}</h3>
            <p className="mt-2 text-sm text-neutral-400">{p.text}</p>
          </div>
        ))}
      </section>
    </>
  )
}
