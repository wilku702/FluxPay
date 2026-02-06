import { createContext, useContext, useState, useCallback, type ReactNode } from 'react';
import { setTokens, clearTokens } from '../api/client';
import type { AuthResponse } from '../types/api';

interface AuthState {
  userId: number | null;
  email: string | null;
  fullName: string | null;
  isAuthenticated: boolean;
}

interface AuthContextType extends AuthState {
  handleAuth: (data: AuthResponse) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthState>({
    userId: null,
    email: null,
    fullName: null,
    isAuthenticated: false,
  });

  const handleAuth = useCallback((data: AuthResponse) => {
    setTokens(data.accessToken, data.refreshToken);
    setAuth({
      userId: data.userId,
      email: data.email,
      fullName: data.fullName,
      isAuthenticated: true,
    });
  }, []);

  const logout = useCallback(() => {
    clearTokens();
    setAuth({ userId: null, email: null, fullName: null, isAuthenticated: false });
  }, []);

  return (
    <AuthContext.Provider value={{ ...auth, handleAuth, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
