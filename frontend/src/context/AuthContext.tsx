import { useState, useCallback, type ReactNode } from 'react';
import { setTokens, clearTokens } from '../api/client';
import type { AuthResponse } from '../types/api';
import { AuthContext } from './authContextValue';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState({
    userId: null as number | null,
    email: null as string | null,
    fullName: null as string | null,
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
