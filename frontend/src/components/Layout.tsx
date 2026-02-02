import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import './Layout.css'

export default function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="layout">
      <header className="header">
        <div className="header-brand">
          <span className="logo">▸</span>
          <h1>Release System</h1>
        </div>
        <nav className="nav">
          <NavLink to="/" end>Dashboard</NavLink>
          {user?.role === 'ADMIN' && <NavLink to="/releases">Releases</NavLink>}
          <NavLink to="/tasks">My Tasks</NavLink>
          <NavLink to="/activity">Activity</NavLink>
          <NavLink to="/chat">AI Chat</NavLink>
        </nav>
        <div className="header-user">
          <span className="user-badge">{user?.username}</span>
          <span className="role-badge">{user?.role}</span>
          <button onClick={handleLogout} className="btn-logout">Logout</button>
        </div>
      </header>
      <main className="main">
        <Outlet />
      </main>
    </div>
  )
}
