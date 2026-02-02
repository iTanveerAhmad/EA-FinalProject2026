import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import './ActivityFeed.css'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export default function ActivityFeed() {
  const [events, setEvents] = useState<{ type: string; data: unknown }[]>([])
  const [status, setStatus] = useState<'connecting' | 'connected' | 'error'>('connecting')
  const { token } = useAuth()

  useEffect(() => {
    if (!token) return
    const controller = new AbortController()
    fetch(`${API_URL}/activity/stream`, {
      headers: { Authorization: `Bearer ${token}` },
      signal: controller.signal,
    })
      .then((res) => {
        if (!res.ok) {
          setStatus('error')
          return
        }
        setStatus('connected')
        if (!res.body) return
        const reader = res.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        let eventType = ''
        let data = ''
        const processLine = (line: string) => {
          if (line.startsWith('event:')) eventType = line.slice(6).trim()
          else if (line.startsWith('event: ')) eventType = line.slice(7).trim()
          else if (line.startsWith('data:')) data = line.slice(5).trim()
          else if (line.startsWith('data: ')) data = line.slice(6).trim()
          else if (line === '' || line === '\r') {
            if (data !== '' || eventType !== '') {
              try {
                const parsed = data ? (data.startsWith('{') ? JSON.parse(data) : data) : {}
                setEvents((prev) => [{ type: eventType || 'message', data: parsed }, ...prev].slice(0, 50))
              } catch {
                setEvents((prev) => [{ type: eventType || 'message', data }, ...prev].slice(0, 50))
              }
              data = ''
              eventType = ''
            }
          }
        }
        const read = () => {
          reader.read().then(({ done, value }) => {
            if (done) return
            buffer += decoder.decode(value, { stream: true })
            const lines = buffer.split(/\r?\n/)
            buffer = lines.pop() || ''
            for (const line of lines) processLine(line)
            read()
          })
        }
        read()
      })
      .catch(() => setStatus('error'))

    return () => controller.abort()
  }, [token])

  return (
    <div className="activity-page">
      <h1>Activity Feed</h1>
      <p className="activity-sub">Real-time updates from the release system</p>
      {status === 'connecting' && <p className="activity-status">Connecting...</p>}
      {status === 'error' && <p className="activity-status error">Failed to connect. Check that you are logged in.</p>}
      {status === 'connected' && (
        <p className="activity-hint">Open <strong>My Tasks</strong> in another tab and start/complete a task or add a comment to see events here.</p>
      )}
      <div className="activity-list">
        {events.length === 0 ? (
          <p className="empty">Waiting for activity...</p>
        ) : (
          events.map((e, i) => (
            <div key={i} className="activity-item card">
              <span className="activity-type">{e.type}</span>
              <pre className="activity-data">{JSON.stringify(e.data, null, 2)}</pre>
            </div>
          ))
        )}
      </div>
    </div>
  )
}
