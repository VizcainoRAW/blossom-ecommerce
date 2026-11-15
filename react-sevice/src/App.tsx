import { Link, NavLink, Route, Routes } from 'react-router'
import { useAuth } from './auth'
import RequireEmployee from './components/RequireEmployee'
import Home from './pages/Home'
import Login from './pages/Login'
import ManageProducts from './pages/ManageProducts'
import ProductForm from './pages/ProductForm'
import Products from './pages/Products'
import Register from './pages/Register'

const navLink = ({ isActive }: { isActive: boolean }) =>
  `text-sm transition hover:text-white ${isActive ? 'text-white' : 'text-neutral-400'}`

const year = new Date().getFullYear()

export default function App() {
  const { token, accountType, logout } = useAuth()
  return (
    <div className="flex min-h-screen flex-col bg-neutral-950 text-neutral-100">
      <header className="sticky top-0 z-10 border-b border-neutral-800 bg-neutral-950/80 backdrop-blur">
        <nav className="mx-auto flex max-w-6xl items-center justify-between p-4">
          <Link to="/" className="text-xl font-bold tracking-tight text-pink-500">Blossom</Link>
          <div className="flex items-center gap-6">
            <NavLink to="/products" className={navLink}>Shop</NavLink>
            {accountType === 'EMPLOYEE' && <NavLink to="/manage" className={navLink}>Manage</NavLink>}
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
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/manage" element={<RequireEmployee><ManageProducts /></RequireEmployee>} />
          <Route path="/manage/new" element={<RequireEmployee><ProductForm /></RequireEmployee>} />
          <Route path="/manage/:id" element={<RequireEmployee><ProductForm /></RequireEmployee>} />
        </Routes>
      </main>

      <footer className="border-t border-neutral-800 py-6 text-center text-sm text-neutral-500">
        © {year} Blossom
      </footer>
    </div>
  )
}
