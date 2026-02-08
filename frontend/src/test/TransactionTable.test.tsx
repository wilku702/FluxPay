import { render, screen } from '@testing-library/react';
import TransactionTable from '../components/TransactionTable';
import { describe, it, expect } from 'vitest';
import type { TransactionResponse } from '../types/api';

const mockTransactions: TransactionResponse[] = [
  {
    id: 1,
    accountId: 1,
    type: 'CREDIT',
    amount: 500,
    description: 'Deposit',
    correlationId: null,
    status: 'COMPLETED',
    balanceAfter: 1500,
    createdAt: '2024-01-15T10:00:00',
  },
  {
    id: 2,
    accountId: 1,
    type: 'DEBIT',
    amount: 100,
    description: 'Transfer out',
    correlationId: 'abc-123',
    status: 'COMPLETED',
    balanceAfter: 1400,
    createdAt: '2024-01-16T10:00:00',
  },
];

describe('TransactionTable', () => {
  it('renders transactions', () => {
    render(<TransactionTable transactions={mockTransactions} />);
    expect(screen.getByText('Deposit')).toBeInTheDocument();
    expect(screen.getByText('Transfer out')).toBeInTheDocument();
  });

  it('shows credit/debit styling', () => {
    render(<TransactionTable transactions={mockTransactions} />);
    expect(screen.getByText('+$500.00')).toBeInTheDocument();
    expect(screen.getByText('-$100.00')).toBeInTheDocument();
  });

  it('shows empty state', () => {
    render(<TransactionTable transactions={[]} />);
    expect(screen.getByText(/no transactions found/i)).toBeInTheDocument();
  });
});
