import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { transfer } from '../api/transactions';
import type { AccountResponse } from '../types/api';

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

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">Quick Transfer</h3>
      <form onSubmit={handleSubmit} className="space-y-3">
        <select value={sourceId} onChange={(e) => setSourceId(e.target.value)}
          className="w-full border rounded px-3 py-2 text-sm" required>
          <option value="">From account...</option>
          {activeAccounts.map(a => (
            <option key={a.id} value={a.id}>{a.accountName} (${a.balance.toFixed(2)})</option>
          ))}
        </select>
        <select value={destId} onChange={(e) => setDestId(e.target.value)}
          className="w-full border rounded px-3 py-2 text-sm" required>
          <option value="">To account...</option>
          {activeAccounts.map(a => (
            <option key={a.id} value={a.id}>{a.accountName} (${a.balance.toFixed(2)})</option>
          ))}
        </select>
        <input type="number" value={amount} onChange={(e) => setAmount(e.target.value)}
          placeholder="Amount" step="0.01" min="0.01"
          className="w-full border rounded px-3 py-2 text-sm" required />
        <input type="text" value={description} onChange={(e) => setDescription(e.target.value)}
          placeholder="Description (optional)"
          className="w-full border rounded px-3 py-2 text-sm" />
        <button type="submit" disabled={mutation.isPending}
          className="w-full bg-indigo-600 text-white py-2 rounded text-sm hover:bg-indigo-700 disabled:opacity-50">
          {mutation.isPending ? 'Processing...' : 'Transfer'}
        </button>
      </form>
      {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
      {success && <p className="mt-2 text-sm text-green-600">{success}</p>}
    </div>
  );
}
