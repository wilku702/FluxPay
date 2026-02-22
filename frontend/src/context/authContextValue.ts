import { createContext } from 'react';
import type { AuthResponse } from '../types/api';

interface AuthState {
  userId: number | null;
  email: string | null;
  fullName: string | null;
  isAuthenticated: boolean;
}

export interface AuthContextType extends AuthState {
  handleAuth: (data: AuthResponse) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);
