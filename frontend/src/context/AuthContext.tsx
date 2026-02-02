import { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { auth } from '../api/client';

interface User {
  username: string;
  role: string;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, password: string, role: 'ADMIN' | 'DEVELOPER') => Promise<void>;
  logout: () => void;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const loadUser = async (t: string) => {
    try {
      localStorage.setItem('token', t);
      const me = await auth.me();
      setUser(me);
      setToken(t);
      localStorage.setItem('user', JSON.stringify(me));
    } catch {
      localStorage.removeItem('token');
      setUser(null);
      setToken(null);
    }
  };

  useEffect(() => {
    const t = localStorage.getItem('token');
    const u = localStorage.getItem('user');
    if (t && u) {
      setToken(t);
      setUser(JSON.parse(u));
      auth.me().then((me) => {
        setUser(me);
        localStorage.setItem('user', JSON.stringify(me));
      }).catch(() => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        setUser(null);
        setToken(null);
      });
    }
    setLoading(false);
  }, []);

  const login = async (username: string, password: string) => {
    const { token: t } = await auth.login(username, password);
    await loadUser(t);
  };

  const register = async (username: string, password: string, role: 'ADMIN' | 'DEVELOPER') => {
    const { token: t } = await auth.register(username, password, role);
    await loadUser(t);
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
    setToken(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
