import { useState, useEffect } from 'react'
import { tasks as tasksApi, forum, type Task, type Comment } from '../api/client'
import { useAuth } from '../context/AuthContext'
import './MyTasks.css'

export default function MyTasks() {
  const [taskList, setTaskList] = useState<Task[]>([])
  const [loading, setLoading] = useState(true)
  const [selectedTask, setSelectedTask] = useState<Task | null>(null)
  const [comments, setComments] = useState<Comment[]>([])
  const [newComment, setNewComment] = useState('')
  const { user } = useAuth()

  const loadTasks = async (silent = false) => {
    if (!silent) setLoading(true)
    try {
      const data = await tasksApi.my()
      setTaskList(data)
    } catch (err) {
      console.error(err)
    } finally {
      if (!silent) setLoading(false)
    }
  }

  useEffect(() => {
    loadTasks()
    const interval = setInterval(() => loadTasks(true), 10000)
    return () => clearInterval(interval)
  }, [])

  const loadComments = async (taskId: string) => {
    try {
      const data = await forum.getComments(taskId)
      setComments(data)
    } catch (err) {
      console.error(err)
    }
  }

  useEffect(() => {
    if (selectedTask) loadComments(selectedTask.id)
  }, [selectedTask?.id])

  const handleStart = async (id: string) => {
    try {
      await tasksApi.start(id)
      loadTasks()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed')
    }
  }

  const handleComplete = async (id: string) => {
    try {
      await tasksApi.complete(id)
      loadTasks()
      if (selectedTask?.id === id) setSelectedTask(null)
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed')
    }
  }

  const handleAddComment = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedTask || !newComment.trim()) return
    try {
      await forum.addComment(selectedTask.id, newComment.trim())
      setNewComment('')
      loadComments(selectedTask.id)
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed')
    }
  }

  if (loading) return <div className="loading">Loading tasks...</div>

  return (
    <div className="mytasks-page">
      <h1>My Tasks</h1>
      <div className="mytasks-layout">
        <div className="task-list">
          {taskList.length === 0 ? (
            <p className="empty">No tasks assigned to you.</p>
          ) : (
            taskList.map((t) => (
              <div
                key={t.id}
                className={`task-item card ${selectedTask?.id === t.id ? 'selected' : ''}`}
                onClick={() => setSelectedTask(t)}
              >
                <div className="task-item-header">
                  <span className={`status status-${t.status?.toLowerCase()}`}>{t.status}</span>
                  <span className="task-item-title">{t.title}</span>
                </div>
                <p className="task-item-desc">{t.description}</p>
                <div className="task-actions">
                  {t.status === 'TODO' && (
                    <button onClick={(e) => { e.stopPropagation(); handleStart(t.id); }} className="btn-primary btn-small">
                      Start
                    </button>
                  )}
                  {t.status === 'IN_PROCESS' && (
                    <button onClick={(e) => { e.stopPropagation(); handleComplete(t.id); }} className="btn-primary btn-small">
                      Complete
                    </button>
                  )}
                </div>
              </div>
            ))
          )}
        </div>
        <div className="task-detail">
          {selectedTask ? (
            <>
              <div className="card task-detail-card">
                <h3>{selectedTask.title}</h3>
                <span className={`status status-${selectedTask.status?.toLowerCase()}`}>{selectedTask.status}</span>
                <p>{selectedTask.description}</p>
              </div>
              <div className="card comments-card">
                <h4>Discussion</h4>
                <div className="comments-list">
                  {comments.map((c) => (
                    <CommentNode key={c.id} comment={c} />
                  ))}
                </div>
                <form onSubmit={handleAddComment} className="comment-form">
                  <textarea
                    placeholder="Add a comment..."
                    value={newComment}
                    onChange={(e) => setNewComment(e.target.value)}
                    rows={2}
                  />
                  <button type="submit" className="btn-primary btn-small">Post</button>
                </form>
              </div>
            </>
          ) : (
            <p className="empty">Select a task to view details and discussion.</p>
          )}
        </div>
      </div>
    </div>
  )
}

function CommentNode({ comment }: { comment: Comment }) {
  return (
    <div className="comment">
      <div className="comment-header">
        <span className="comment-author">{comment.authorId}</span>
      </div>
      <p className="comment-content">{comment.content}</p>
      {comment.replies?.length > 0 && (
        <div className="comment-replies">
          {comment.replies.map((r) => (
            <CommentNode key={r.id} comment={r} />
          ))}
        </div>
      )}
    </div>
  )
}
