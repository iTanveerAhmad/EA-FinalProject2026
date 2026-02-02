import { useState, useEffect, useRef } from 'react'
import { chat as chatApi } from '../api/client'
import { useAuth } from '../context/AuthContext'
import './Chat.css'

const STORAGE_KEY = 'chat_session_id'

export default function Chat() {
  const [sessionId, setSessionId] = useState<string | null>(null)
  const [messages, setMessages] = useState<{ role: string; content: string }[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [restoring, setRestoring] = useState(true)
  const { user, token } = useAuth()
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const storageKey = user ? `${STORAGE_KEY}_${user.username}` : null

  useEffect(() => {
    if (!storageKey || !token) {
      setRestoring(false)
      return
    }
    const saved = localStorage.getItem(storageKey)
    if (!saved) {
      setRestoring(false)
      return
    }
    chatApi.getHistory(saved)
      .then((history) => {
        setSessionId(saved)
        setMessages(history.map((m) => ({ role: m.role, content: m.content })))
      })
      .catch(() => localStorage.removeItem(storageKey))
      .finally(() => setRestoring(false))
  }, [storageKey, token])

  const startSession = async () => {
    try {
      const session = await chatApi.createSession()
      if (storageKey) localStorage.setItem(storageKey, session.id)
      setSessionId(session.id)
      setMessages([])
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to start chat')
    }
  }

  const sendMessage = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!sessionId || !input.trim()) return
    const userMsg = input.trim()
    setInput('')
    setMessages((prev) => [...prev, { role: 'user', content: userMsg }])
    setLoading(true)
    try {
      const res = await chatApi.sendMessage(sessionId, userMsg)
      setMessages((prev) => [...prev, { role: res.role || 'assistant', content: res.content }])
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: err instanceof Error ? err.message : 'Failed to get response' },
      ])
    } finally {
      setLoading(false)
    }
  }

  const loadHistory = async () => {
    if (!sessionId) return
    try {
      const history = await chatApi.getHistory(sessionId)
      setMessages(
        history.map((m) => ({ role: m.role, content: m.content }))
      )
    } catch (err) {
      console.error(err)
    }
  }

  // Load history when sessionId is set from startSession (messages start empty)
  // Restore flow sets messages directly, so skip load for that case
  useEffect(() => {
    if (sessionId && messages.length === 0) loadHistory()
  }, [sessionId])

  if (restoring) {
    return (
      <div className="chat-page">
        <h1>AI Assistant</h1>
        <p className="chat-sub">Loading chat...</p>
      </div>
    )
  }

  if (!sessionId) {
    return (
      <div className="chat-page">
        <h1>AI Assistant</h1>
        <p className="chat-sub">Chat with contextual AI support for your tasks</p>
        <div className="chat-start card">
          <p>Start a new chat session to ask questions about releases, tasks, or workflow.</p>
          <button onClick={startSession} className="btn-primary">
            Start Chat
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="chat-page">
      <h1>AI Assistant</h1>
      <div className="chat-container card">
        <div className="chat-messages">
          {messages.map((m, i) => (
            <div key={i} className={`chat-msg chat-msg-${m.role}`}>
              <span className="chat-msg-role">{m.role === 'user' ? 'You' : 'AI'}</span>
              <p>{m.content}</p>
            </div>
          ))}
          {loading && (
            <div className="chat-msg chat-msg-assistant">
              <span className="chat-msg-role">AI</span>
              <p className="typing">Thinking...</p>
            </div>
          )}
          <div ref={bottomRef} />
        </div>
        <form onSubmit={sendMessage} className="chat-form">
          <input
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ask about your release or tasks..."
            disabled={loading}
          />
          <button type="submit" className="btn-primary" disabled={loading}>
            Send
          </button>
        </form>
      </div>
    </div>
  )
}
