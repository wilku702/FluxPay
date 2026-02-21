import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { motion } from 'motion/react';
import { toast } from 'sonner';
import { transfer } from '../api/transactions';
import type { AccountResponse } from '../types/api';
import { formatBalance } from '../utils/currency';
import { inputClass } from '../utils/styles';
import Spinner from './ui/Spinner';

interface Props {
  accounts: AccountResponse[];
}

export default function QuickTransfer({ accounts }: Props) {
  const [sourceId, setSourceId] = useState('');
  const [destId, setDestId] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
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
      toast.success('Transfer completed!');
      setAmount('');
      setDescription('');
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Transfer failed';
      toast.error(msg);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (sourceId === destId) {
      toast.error('Source and destination must be different');
      return;
    }
    mutation.mutate();
  };

  const activeAccounts = accounts.filter(a => a.status === 'ACTIVE');

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
        <motion.button whileTap={{ scale: 0.97 }} type="submit" disabled={mutation.isPending}
          className="w-full bg-accent hover:bg-accent-hover text-white py-2.5 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2">
          {mutation.isPending && <Spinner />}
          {mutation.isPending ? 'Processing...' : 'Transfer'}
        </motion.button>
      </form>
    </div>
  );
}
