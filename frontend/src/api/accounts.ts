import client from './client';
import type { AccountResponse } from '../types/api';

export async function getAccounts(): Promise<AccountResponse[]> {
  const res = await client.get<AccountResponse[]>('/accounts');
  return res.data;
}

export async function getAccount(id: number): Promise<AccountResponse> {
  const res = await client.get<AccountResponse>(`/accounts/${id}`);
  return res.data;
}

export async function createAccount(accountName: string, currency: string = 'USD'): Promise<AccountResponse> {
  const res = await client.post<AccountResponse>('/accounts', { accountName, currency });
  return res.data;
}

export async function updateAccountStatus(id: number, status: string): Promise<AccountResponse> {
  const res = await client.patch<AccountResponse>(`/accounts/${id}/status`, { status });
  return res.data;
}
