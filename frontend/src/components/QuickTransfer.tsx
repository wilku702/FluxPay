import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { transfer } from '../api/transactions';
import type { AccountResponse } from '../types/api';
import { formatBalance } from '../utils/currency';

interface Props {
  accounts: AccountResponse[];
}

export default function QuickTransfer({ accounts }: Props) {
  const [sourceId, setSourceId] = useState('');
  const [destId, setDestId] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: () => transfer(
      parseInt(sourceId),
      parseInt(destId),
      parseFloat(amount),
      description || 'Quick transfer',
      crypto.randomUUID()
    ),
    onSuccess: () => {
      setSuccess('Transfer completed!');
      setError('');
      setAmount('');
      setDescription('');
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Transfer failed';
      setError(msg);
      setSuccess('');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (sourceId === destId) {
      setError('Source and destination must be different');
      return;
    }
    setError('');
    setSuccess('');
    mutation.mutate();
  };

  const activeAccounts = accounts.filter(a => a.status === 'ACTIVE');
  const inputClass = "w-full bg-surface-secondary border border-border-primary text-text-primary rounded-lg px-4 py-2.5 text-sm placeholder:text-text-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors";

  return (
    <div className="bg-surface-elevated border border-border-primary rounded-xl p-6">
      <h3 className="text-base font-semibold text-text-primary mb-5">Quick Transfer</h3>
      <form onSubmit={handleSubmit} className="space-y-3">
        <select value={sourceId} onChange={(e) => setSourceId(e.target.value)}
          className={inputClass} required>
          <option value="">From account...</option>
          {activeAccounts.map(a => (
            <option key={a.id} value={a.id}>{a.accountName} ({formatBalance(a.balance, a.currency)})</option>
          ))}
        </select>
        <select value={destId} onChange={(e) => setDestId(e.target.value)}
          className={inputClass} required>
          <option value="">To account...</option>
          {activeAccounts.map(a => (
            <option key={a.id} value={a.id}>{a.accountName} ({formatBalance(a.balance, a.currency)})</option>
          ))}
        </select>
        <input type="number" value={amount} onChange={(e) => setAmount(e.target.value)}
          placeholder="Amount" step="0.01" min="0.01"
          className={inputClass} required />
        <input type="text" value={description} onChange={(e) => setDescription(e.target.value)}
          placeholder="Description (optional)"
          className={inputClass} />
        <button type="submit" disabled={mutation.isPending}
          className="w-full bg-accent hover:bg-accent-hover text-white py-2.5 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
          {mutation.isPending ? 'Processing...' : 'Transfer'}
        </button>
      </form>
      {error && (
        <div className="mt-3 bg-danger-muted text-danger rounded-lg p-3 text-sm">{error}</div>
      )}
      {success && (
        <div className="mt-3 bg-accent-muted text-accent rounded-lg p-3 text-sm">{success}</div>
      )}
    </div>
  );
}
