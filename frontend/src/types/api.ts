export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: number;
  email: string;
  fullName: string;
}

export interface AccountResponse {
  id: number;
  userId: number;
  accountName: string;
  balance: number;
  currency: string;
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED';
  createdAt: string;
}

export interface TransactionResponse {
  id: number;
  accountId: number;
  type: 'CREDIT' | 'DEBIT';
  amount: number;
  description: string;
  correlationId: string | null;
  status: 'PENDING' | 'COMPLETED' | 'FAILED' | 'REVERSED';
  balanceAfter: number;
  createdAt: string;
}

export interface TransferResponse {
  correlationId: string;
  debit: TransactionResponse;
  credit: TransactionResponse;
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface UserProfileResponse {
  id: number;
  email: string;
  fullName: string;
}

export interface ErrorResponse {
  status: number;
  message: string;
  fieldErrors?: Record<string, string>;
}
