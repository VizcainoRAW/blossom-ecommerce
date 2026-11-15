import { Link, NavLink, Route, Routes } from 'react-router'
import { useAuth } from './auth'
import { useCart } from './cartContext'
import RequireAuth from './components/RequireAuth'
import Cart from './pages/Cart'
import Checkout from './pages/Checkout'
import Home from './pages/Home'
import Login from './pages/Login'
import ManageProducts from './pages/ManageProducts'
import OrderDetail from './pages/OrderDetail'
import Orders from './pages/Orders'
import ProductDetail from './pages/ProductDetail'
import ProductForm from './pages/ProductForm'
import Products from './pages/Products'
import Register from './pages/Register'

const navLink = ({ isActive }: { isActive: boolean }) =>
  `text-sm transition hover:text-white ${isActive ? 'text-white' : 'text-neutral-400'}`

const year = new Date().getFullYear()

export default function App() {
  const { token, accountType, logout } = useAuth()
  const { count } = useCart()
  const isEmployee = accountType === 'EMPLOYEE'
  return (
    <div className="flex min-h-screen flex-col bg-neutral-950 text-neutral-100">
      <header className="sticky top-0 z-10 border-b border-neutral-800 bg-neutral-950/80 backdrop-blur">
        <nav className="mx-auto flex max-w-6xl items-center justify-between p-4">
          <Link to="/" className="text-xl font-bold tracking-tight text-pink-500">Blossom</Link>
          <div className="flex items-center gap-6">
            <NavLink to="/products" end className={navLink}>Shop</NavLink>
            {isEmployee && <NavLink to="/manage" className={navLink}>Manage</NavLink>}
            {token && <NavLink to="/orders" className={navLink}>Orders</NavLink>}
            {!isEmployee && (
              <NavLink to="/cart" className={navLink} aria-label={`Cart, ${count} items`}>
                Cart{count > 0 && (
                  <span className="ml-1.5 rounded-full bg-pink-600 px-1.5 py-0.5 text-xs font-medium text-white">{count}</span>
                )}
              </NavLink>
            )}
            {token ? (
              <button onClick={logout} className="text-sm text-neutral-400 hover:text-white">Log out</button>
            ) : (
              <>
                <NavLink to="/login" className={navLink}>Log in</NavLink>
                <Link to="/register"
                  className="rounded-full bg-pink-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-pink-500">
                  Sign up
                </Link>
              </>
            )}
          </div>
        </nav>
      </header>

      <main className="flex-1">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/products" element={<Products />} />
          <Route path="/products/:id" element={<ProductDetail />} />
          <Route path="/cart" element={<Cart />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/checkout" element={<RequireAuth><Checkout /></RequireAuth>} />
          <Route path="/orders" element={<RequireAuth><Orders /></RequireAuth>} />
          <Route path="/orders/:id" element={<RequireAuth><OrderDetail /></RequireAuth>} />
          <Route path="/manage" element={<RequireAuth employee><ManageProducts /></RequireAuth>} />
          <Route path="/manage/new" element={<RequireAuth employee><ProductForm /></RequireAuth>} />
          <Route path="/manage/:id" element={<RequireAuth employee><ProductForm /></RequireAuth>} />
        </Routes>
      </main>

      <footer className="border-t border-neutral-800 py-6 text-center text-sm text-neutral-500">
        © {year} Blossom
      </footer>
    </div>
  )
}
