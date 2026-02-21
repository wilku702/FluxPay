import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getAccounts } from '../api/accounts';
import { deposit, withdraw } from '../api/transactions';
import { getApiErrorMessage } from '../utils/errors';
import { formatBalance } from '../utils/currency';
import { useState } from 'react';

const schema = z.object({
  accountId: z.string().min(1, 'Account is required'),
  amount: z.string()
    .min(1, 'Amount is required')
    .refine((v) => !isNaN(parseFloat(v)) && parseFloat(v) > 0, 'Amount must be positive')
    .refine((v) => {
      const parts = v.split('.');
      return !parts[1] || parts[1].length <= 2;
    }, 'Maximum 2 decimal places'),
  description: z.string().optional(),
});

type FormData = z.infer<typeof schema>;

type Tab = 'deposit' | 'withdraw';

export default function DepositWithdrawPage() {
  const [tab, setTab] = useState<Tab>('deposit');
  const [success, setSuccess] = useState('');
  const queryClient = useQueryClient();

  const { data: accounts } = useQuery({
    queryKey: ['accounts'],
    queryFn: getAccounts,
  });

  const { register, handleSubmit, reset, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const mutation = useMutation({
    mutationFn: (data: FormData) => {
      const accountId = parseInt(data.accountId);
      const amount = parseFloat(data.amount);
      const description = data.description || (tab === 'deposit' ? 'Deposit' : 'Withdrawal');
      const idempotencyKey = crypto.randomUUID();
      return tab === 'deposit'
        ? deposit(accountId, amount, description, idempotencyKey)
        : withdraw(accountId, amount, description, idempotencyKey);
    },
    onSuccess: () => {
      setSuccess(`${tab === 'deposit' ? 'Deposit' : 'Withdrawal'} completed successfully!`);
      reset();
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
    },
  });

  const onSubmit = (data: FormData) => {
    setSuccess('');
    mutation.mutate(data);
  };

  const handleTabSwitch = (newTab: Tab) => {
    setTab(newTab);
    setSuccess('');
    mutation.reset();
  };

  const activeAccounts = accounts?.filter(a => a.status === 'ACTIVE') || [];
  const inputClass = "w-full bg-surface-secondary border border-border-primary text-text-primary rounded-lg px-4 py-3 text-sm placeholder:text-text-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors";

  return (
    <div className="max-w-lg mx-auto">
      <h1 className="text-2xl font-semibold text-text-primary mb-1">Deposit & Withdraw</h1>
      <p className="text-sm text-text-secondary mb-6">Add or remove funds from your accounts</p>

      {/* Tab switcher */}
      <div className="flex bg-surface-secondary border border-border-primary rounded-lg p-1 mb-6">
        <button
          type="button"
          onClick={() => handleTabSwitch('deposit')}
          className={`flex-1 py-2 text-sm font-medium rounded-md transition-colors ${
            tab === 'deposit'
              ? 'bg-accent text-white'
              : 'text-text-secondary hover:text-text-primary'
          }`}
        >
          Deposit
        </button>
        <button
          type="button"
          onClick={() => handleTabSwitch('withdraw')}
          className={`flex-1 py-2 text-sm font-medium rounded-md transition-colors ${
            tab === 'withdraw'
              ? 'bg-accent text-white'
              : 'text-text-secondary hover:text-text-primary'
          }`}
        >
          Withdraw
        </button>
      </div>

      <div className="bg-surface-elevated border border-border-primary rounded-xl p-6">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1.5">Account</label>
            <select {...register('accountId')} className={inputClass}>
              <option value="">Select account</option>
              {activeAccounts.map(a => (
                <option key={a.id} value={a.id}>
                  {a.accountName} ({formatBalance(a.balance, a.currency)})
                </option>
              ))}
            </select>
            {errors.accountId && (
              <p className="mt-1.5 text-sm text-danger">{errors.accountId.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1.5">Amount</label>
            <input type="text" {...register('amount')}
              placeholder="0.00"
              className={inputClass} />
            {errors.amount && (
              <p className="mt-1.5 text-sm text-danger">{errors.amount.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1.5">Description (optional)</label>
            <input type="text" {...register('description')}
              placeholder="What's this for?"
              className={inputClass} />
          </div>

          {mutation.isError && (
            <div className="bg-danger-muted text-danger rounded-lg p-3 text-sm flex items-start gap-2">
              <svg className="w-4 h-4 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
              </svg>
              {getApiErrorMessage(mutation.error, `${tab === 'deposit' ? 'Deposit' : 'Withdrawal'} failed`)}
            </div>
          )}

          {success && (
            <div className="bg-accent-muted text-accent rounded-lg p-3 text-sm flex items-center gap-2">
              <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              {success}
            </div>
          )}

          <button type="submit" disabled={mutation.isPending}
            className="w-full bg-accent hover:bg-accent-hover text-white py-3 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
            {mutation.isPending ? (
              <span className="flex items-center justify-center gap-2">
                <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
                Processing...
              </span>
            ) : (
              tab === 'deposit' ? 'Deposit' : 'Withdraw'
            )}
          </button>
        </form>
      </div>
    </div>
  );
}
