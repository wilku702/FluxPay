import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getAccounts } from '../api/accounts';
import { transfer } from '../api/transactions';
import { getApiErrorMessage } from '../utils/errors';
import { formatBalance } from '../utils/currency';
import { useState } from 'react';

const transferSchema = z.object({
  sourceAccountId: z.string().min(1, 'Source account is required'),
  destinationAccountId: z.string().min(1, 'Destination account is required'),
  amount: z.string()
    .min(1, 'Amount is required')
    .refine((v) => !isNaN(parseFloat(v)) && parseFloat(v) > 0, 'Amount must be positive')
    .refine((v) => {
      const parts = v.split('.');
      return !parts[1] || parts[1].length <= 2;
    }, 'Maximum 2 decimal places'),
  description: z.string().optional(),
}).refine(
  (data) => data.sourceAccountId !== data.destinationAccountId,
  { message: 'Source and destination must be different', path: ['destinationAccountId'] }
);

type TransferFormData = z.infer<typeof transferSchema>;

export default function TransferPage() {
  const [success, setSuccess] = useState('');
  const queryClient = useQueryClient();

  const { data: accounts } = useQuery({
    queryKey: ['accounts'],
    queryFn: getAccounts,
  });

  const { register, handleSubmit, reset, formState: { errors } } = useForm<TransferFormData>({
    resolver: zodResolver(transferSchema),
  });

  const mutation = useMutation({
    mutationFn: (data: TransferFormData) => transfer(
      parseInt(data.sourceAccountId),
      parseInt(data.destinationAccountId),
      parseFloat(data.amount),
      data.description || 'Transfer',
      crypto.randomUUID()
    ),
    onSuccess: () => {
      setSuccess('Transfer completed successfully!');
      reset();
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
    },
  });

  const onSubmit = (data: TransferFormData) => {
    setSuccess('');
    mutation.mutate(data);
  };

  const activeAccounts = accounts?.filter(a => a.status === 'ACTIVE') || [];
  const inputClass = "w-full bg-surface-secondary border border-border-primary text-text-primary rounded-lg px-4 py-3 text-sm placeholder:text-text-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors";

  return (
    <div className="max-w-lg mx-auto">
      <h1 className="text-2xl font-semibold text-text-primary mb-1">Transfer Funds</h1>
      <p className="text-sm text-text-secondary mb-6">Move money between your accounts</p>

      <div className="bg-surface-elevated border border-border-primary rounded-xl p-6">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1.5">From Account</label>
            <select {...register('sourceAccountId')} className={inputClass}>
              <option value="">Select source account</option>
              {activeAccounts.map(a => (
                <option key={a.id} value={a.id}>
                  {a.accountName} ({formatBalance(a.balance, a.currency)})
                </option>
              ))}
            </select>
            {errors.sourceAccountId && (
              <p className="mt-1.5 text-sm text-danger">{errors.sourceAccountId.message}</p>
            )}
          </div>

          {/* Direction indicator */}
          <div className="flex items-center justify-center">
            <div className="w-8 h-8 rounded-full bg-surface-secondary border border-border-primary flex items-center justify-center">
              <svg className="w-4 h-4 text-text-muted" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 13.5L12 21m0 0l-7.5-7.5M12 21V3" />
              </svg>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1.5">To Account</label>
            <select {...register('destinationAccountId')} className={inputClass}>
              <option value="">Select destination account</option>
              {activeAccounts.map(a => (
                <option key={a.id} value={a.id}>
                  {a.accountName} ({formatBalance(a.balance, a.currency)})
                </option>
              ))}
            </select>
            {errors.destinationAccountId && (
              <p className="mt-1.5 text-sm text-danger">{errors.destinationAccountId.message}</p>
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
              {getApiErrorMessage(mutation.error, 'Transfer failed')}
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
              'Transfer'
            )}
          </button>
        </form>
      </div>
    </div>
  );
}
