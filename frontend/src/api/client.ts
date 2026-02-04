const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

function getToken(): string | null {
  return localStorage.getItem('token');
}

export async function api<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };
  if (token) {
    (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(`${API_URL}${path}`, { ...options, headers });
  if (res.status === 401) {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login';
    throw new Error('Unauthorized');
  }
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    throw new Error(err.message || `Request failed: ${res.status}`);
  }
  const text = await res.text();
  if (res.status === 204 || !text || text.trim() === '') return undefined as T;
  return JSON.parse(text) as T;
}

// Auth
export const auth = {
  login: (username: string, password: string) =>
    api<{ token: string }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),
  register: (username: string, email: string, password: string, role: 'ADMIN' | 'DEVELOPER') =>
    api<{ token: string }>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ username, email, password, role }),
    }),
  me: () => api<{ username: string; role: string }>('/auth/me'),
};

// Releases (Admin)
export const releases = {
  list: () => api<Release[]>('/releases'),
  create: (data: { name: string; description: string }) =>
    api<Release>('/releases', { method: 'POST', body: JSON.stringify(data) }),
  addTask: (id: string, data: TaskRequest) =>
    api<Release>(`/releases/${id}/tasks`, {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  complete: (id: string) =>
    api<void>(`/releases/${id}/complete`, { method: 'PATCH' }),
};

// Tasks
export const tasks = {
  my: () => api<Task[]>('/tasks/my'),
  start: (id: string) => api<void>(`/tasks/${id}/start`, { method: 'PATCH' }),
  complete: (id: string) =>
    api<void>(`/tasks/${id}/complete`, { method: 'PATCH' }),
};

// Forum
export const forum = {
  addComment: (taskId: string, content: string) =>
    api<void>(`/tasks/${taskId}/comments`, {
      method: 'POST',
      body: JSON.stringify({ content }),
    }),
  getComments: (taskId: string) =>
    api<Comment[]>(`/tasks/${taskId}/comments`),
  reply: (commentId: string, content: string) =>
    api<void>(`/comments/${commentId}/reply`, {
      method: 'POST',
      body: JSON.stringify({ content }),
    }),
};

// Chat
export const chat = {
  createSession: () => api<ChatSession>('/chat/session', { method: 'POST' }),
  sendMessage: (sessionId: string, message: string) =>
    api<ChatMessage>(`/chat/${sessionId}/message`, {
      method: 'POST',
      body: JSON.stringify({ message }),
    }),
  getHistory: (sessionId: string) =>
    api<ChatMessage[]>(`/chat/${sessionId}/history`),
};

export interface Release {
  id: string;
  name: string;
  description: string;
  status: string;
  tasks: Task[];
}

export interface Task {
  id: string;
  title: string;
  description: string;
  status: string;
  assignedDeveloperId: string;
  orderIndex: number;
  comments: Comment[];
}

export interface TaskRequest {
  title: string;
  description: string;
  assignedDeveloperId: string;
  orderIndex: number;
}

export interface Comment {
  id: string;
  authorId: string;
  content: string;
  timestamp: string;
  replies: Comment[];
}

export interface ChatSession {
  id: string;
  developerId: string;
}

export interface ChatMessage {
  role: string;
  content: string;
  timestamp: string;
}
