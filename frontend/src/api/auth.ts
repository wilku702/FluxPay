import client from './client';
import type { AuthResponse, UserProfileResponse } from '../types/api';

export async function register(email: string, password: string, fullName: string): Promise<AuthResponse> {
  const res = await client.post<AuthResponse>('/auth/register', { email, password, fullName });
  return res.data;
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  const res = await client.post<AuthResponse>('/auth/login', { email, password });
  return res.data;
}

export async function getMe(): Promise<UserProfileResponse> {
  const res = await client.get<UserProfileResponse>('/auth/me');
  return res.data;
}
