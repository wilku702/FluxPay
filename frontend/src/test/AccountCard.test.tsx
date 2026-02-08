import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import AccountCard from '../components/AccountCard';
import { describe, it, expect } from 'vitest';
import type { AccountResponse } from '../types/api';

const mockAccount: AccountResponse = {
  id: 1,
  userId: 1,
  accountName: 'Checking',
  balance: 1234.56,
  currency: 'USD',
  status: 'ACTIVE',
  createdAt: '2024-01-01T00:00:00',
};

describe('AccountCard', () => {
  it('renders account name and balance', () => {
    render(
      <BrowserRouter>
        <AccountCard account={mockAccount} />
      </BrowserRouter>
    );
    expect(screen.getByText('Checking')).toBeInTheDocument();
    expect(screen.getByText('$1,234.56')).toBeInTheDocument();
  });

  it('renders account status badge', () => {
    render(
      <BrowserRouter>
        <AccountCard account={mockAccount} />
      </BrowserRouter>
    );
    expect(screen.getByText('ACTIVE')).toBeInTheDocument();
  });

  it('links to account detail', () => {
    render(
      <BrowserRouter>
        <AccountCard account={mockAccount} />
      </BrowserRouter>
    );
    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', '/accounts/1');
  });
});
