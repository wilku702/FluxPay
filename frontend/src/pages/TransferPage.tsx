import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getAccounts } from '../api/accounts';
import { transfer } from '../api/transactions';
import { getApiErrorMessage } from '../utils/errors';
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

  return (
    <div className="max-w-lg mx-auto">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Transfer Funds</h1>

      <div className="bg-white rounded-lg shadow p-6">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700">From Account</label>
            <select {...register('sourceAccountId')}
              className="mt-1 w-full border rounded px-3 py-2">
              <option value="">Select source account</option>
              {activeAccounts.map(a => (
                <option key={a.id} value={a.id}>
                  {a.accountName} (${a.balance.toFixed(2)})
                </option>
              ))}
            </select>
            {errors.sourceAccountId && (
              <p className="mt-1 text-sm text-red-600">{errors.sourceAccountId.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700">To Account</label>
            <select {...register('destinationAccountId')}
              className="mt-1 w-full border rounded px-3 py-2">
              <option value="">Select destination account</option>
              {activeAccounts.map(a => (
                <option key={a.id} value={a.id}>
                  {a.accountName} (${a.balance.toFixed(2)})
                </option>
              ))}
            </select>
            {errors.destinationAccountId && (
              <p className="mt-1 text-sm text-red-600">{errors.destinationAccountId.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700">Amount</label>
            <input type="text" {...register('amount')}
              placeholder="0.00"
              className="mt-1 w-full border rounded px-3 py-2" />
            {errors.amount && (
              <p className="mt-1 text-sm text-red-600">{errors.amount.message}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700">Description (optional)</label>
            <input type="text" {...register('description')}
              placeholder="What's this for?"
              className="mt-1 w-full border rounded px-3 py-2" />
          </div>

          {mutation.isError && (
            <p className="text-sm text-red-600">
              {getApiErrorMessage(mutation.error, 'Transfer failed')}
            </p>
          )}
          {success && <p className="text-sm text-green-600">{success}</p>}

          <button type="submit" disabled={mutation.isPending}
            className="w-full bg-indigo-600 text-white py-2 rounded hover:bg-indigo-700 disabled:opacity-50">
            {mutation.isPending ? 'Processing...' : 'Transfer'}
          </button>
        </form>
      </div>
    </div>
  );
}
