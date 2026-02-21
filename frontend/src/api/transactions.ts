import client from './client';
import type { TransactionResponse, TransferResponse, Page } from '../types/api';

export async function deposit(accountId: number, amount: number, description: string, idempotencyKey: string): Promise<TransactionResponse> {
  const res = await client.post<TransactionResponse>('/transactions/deposit', { accountId, amount, description, idempotencyKey });
  return res.data;
}

export async function withdraw(accountId: number, amount: number, description: string, idempotencyKey: string): Promise<TransactionResponse> {
  const res = await client.post<TransactionResponse>('/transactions/withdraw', { accountId, amount, description, idempotencyKey });
  return res.data;
}

export async function transfer(
  sourceAccountId: number,
  destinationAccountId: number,
  amount: number,
  description: string,
  idempotencyKey: string
): Promise<TransferResponse> {
  const res = await client.post<TransferResponse>('/transactions/transfer', {
    sourceAccountId, destinationAccountId, amount, description, idempotencyKey,
  });
  return res.data;
}

export interface TransactionFilters {
  accountId: number;
  type?: string;
  status?: string;
  from?: string;
  to?: string;
  minAmount?: number;
  maxAmount?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}

export async function exportTransactions(filters: Partial<TransactionFilters>): Promise<void> {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== '') {
      params.append(key, String(value));
    }
  });
  const res = await client.get(`/transactions/export?${params.toString()}`, {
    responseType: 'blob',
  });
  const url = window.URL.createObjectURL(new Blob([res.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', 'transactions.csv');
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

export async function getTransactions(filters: TransactionFilters): Promise<Page<TransactionResponse>> {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== '') {
      params.append(key, String(value));
    }
  });
  const res = await client.get<Page<TransactionResponse>>(`/transactions?${params.toString()}`);
  return res.data;
}
