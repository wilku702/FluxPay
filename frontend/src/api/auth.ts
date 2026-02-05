import client from './client';
import type { AuthResponse } from '../types/api';

export async function register(email: string, password: string, fullName: string): Promise<AuthResponse> {
  const res = await client.post<AuthResponse>('/auth/register', { email, password, fullName });
  return res.data;
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  const res = await client.post<AuthResponse>('/auth/login', { email, password });
  return res.data;
}
