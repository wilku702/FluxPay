import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '../context/AuthContext';
import TransferPage from '../pages/TransferPage';
import { describe, it, expect } from 'vitest';

function renderWithProviders(ui: React.ReactNode) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <AuthProvider>
        <BrowserRouter>{ui}</BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}

describe('TransferPage', () => {
  it('renders transfer form', () => {
    renderWithProviders(<TransferPage />);
    expect(screen.getByText(/transfer funds/i)).toBeInTheDocument();
    expect(screen.getByText(/from account/i)).toBeInTheDocument();
    expect(screen.getByText(/to account/i)).toBeInTheDocument();
    expect(screen.getByText(/amount/i)).toBeInTheDocument();
  });

  it('renders submit button', () => {
    renderWithProviders(<TransferPage />);
    expect(screen.getByRole('button', { name: /transfer/i })).toBeInTheDocument();
  });
});
