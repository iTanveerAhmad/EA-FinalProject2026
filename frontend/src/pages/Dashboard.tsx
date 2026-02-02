import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import './Dashboard.css'

export default function Dashboard() {
  const { user } = useAuth()

  return (
    <div className="dashboard">
      <h1>Welcome, {user?.username}</h1>
      <p className="dashboard-sub">Release Management System</p>
      <div className="dashboard-cards">
        {user?.role === 'ADMIN' && (
          <Link to="/releases" className="card">
            <span className="card-icon">📦</span>
            <h3>Manage Releases</h3>
            <p>Create releases, add tasks, track progress</p>
          </Link>
        )}
        <Link to="/tasks" className="card">
          <span className="card-icon">✓</span>
          <h3>My Tasks</h3>
          <p>View and work on assigned tasks</p>
        </Link>
        <Link to="/activity" className="card">
          <span className="card-icon">⚡</span>
          <h3>Activity Feed</h3>
          <p>Real-time updates and events</p>
        </Link>
        <Link to="/chat" className="card">
          <span className="card-icon">💬</span>
          <h3>AI Assistant</h3>
          <p>Chat with contextual AI support</p>
        </Link>
      </div>
    </div>
  )
}
