import { useState, useEffect } from 'react'
import { releases as releasesApi, type Release, type TaskRequest } from '../api/client'
import { useAuth } from '../context/AuthContext'
import './Releases.css'

export default function Releases() {
  const [releases, setReleases] = useState<Release[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [showAddTask, setShowAddTask] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [taskTitle, setTaskTitle] = useState('')
  const [taskDesc, setTaskDesc] = useState('')
  const [taskDev, setTaskDev] = useState('')
  const { user } = useAuth()

  const load = async () => {
    try {
      const data = await releasesApi.list()
      setReleases(data)
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await releasesApi.create({ name, description })
      setName('')
      setDescription('')
      setShowForm(false)
      load()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed')
    }
  }

  const handleAddTask = async (e: React.FormEvent, releaseId: string) => {
    e.preventDefault()
    const release = releases.find((r) => r.id === releaseId)
    if (!release) return
    try {
      await releasesApi.addTask(releaseId, {
        title: taskTitle,
        description: taskDesc,
        assignedDeveloperId: taskDev || user?.username || '',
        orderIndex: release.tasks?.length || 0,
      })
      setTaskTitle('')
      setTaskDesc('')
      setTaskDev('')
      setShowAddTask(null)
      load()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed')
    }
  }

  const handleComplete = async (id: string) => {
    if (!confirm('Mark this release as completed?')) return
    try {
      await releasesApi.complete(id)
      load()
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed')
    }
  }

  if (loading) return <div className="loading">Loading releases...</div>

  return (
    <div className="releases-page">
      <div className="page-header">
        <h1>Releases</h1>
        <button onClick={() => setShowForm(!showForm)} className="btn-primary">
          {showForm ? 'Cancel' : '+ New Release'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleCreate} className="release-form card">
          <h3>Create Release</h3>
          <div className="form-group">
            <label>Name</label>
            <input value={name} onChange={(e) => setName(e.target.value)} required />
          </div>
          <div className="form-group">
            <label>Description</label>
            <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} />
          </div>
          <button type="submit" className="btn-primary">Create</button>
        </form>
      )}

      <div className="releases-list">
        {releases.map((r) => (
          <div key={r.id} className="release-card card">
            <div className="release-header">
              <div>
                <h3>{r.name}</h3>
                <span className={`status status-${r.status?.toLowerCase()}`}>{r.status}</span>
              </div>
              {r.status !== 'COMPLETED' && (
                <button onClick={() => handleComplete(r.id)} className="btn-small">Complete</button>
              )}
            </div>
            {r.description && <p className="release-desc">{r.description}</p>}
            <div className="tasks-section">
              <h4>Tasks ({r.tasks?.length || 0})</h4>
              {r.tasks?.map((t, i) => (
                <div key={t.id} className="task-row">
                  <span className="task-index">{i + 1}.</span>
                  <span className="task-title">{t.title}</span>
                  <span className={`task-status status-${t.status?.toLowerCase()}`}>{t.status}</span>
                  <span className="task-assignee">{t.assignedDeveloperId}</span>
                </div>
              ))}
              {r.status !== 'COMPLETED' && (
                <>
                  {showAddTask === r.id ? (
                    <form onSubmit={(e) => handleAddTask(e, r.id)} className="add-task-form">
                      <input
                        placeholder="Task title"
                        value={taskTitle}
                        onChange={(e) => setTaskTitle(e.target.value)}
                        required
                      />
                      <input
                        placeholder="Description"
                        value={taskDesc}
                        onChange={(e) => setTaskDesc(e.target.value)}
                      />
                      <input
                        placeholder="Assigned to (username)"
                        value={taskDev}
                        onChange={(e) => setTaskDev(e.target.value)}
                      />
                      <div className="form-actions">
                        <button type="submit" className="btn-primary btn-small">Add</button>
                        <button type="button" onClick={() => setShowAddTask(null)}>Cancel</button>
                      </div>
                    </form>
                  ) : (
                    <button onClick={() => setShowAddTask(r.id)} className="btn-add-task">+ Add Task</button>
                  )}
                </>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
