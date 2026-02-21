import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { motion } from 'motion/react';
import { toast } from 'sonner';
import { createAccount } from '../api/accounts';
import { getApiErrorMessage } from '../utils/errors';
import { inputClass } from '../utils/styles';
import ModalWrapper from './animation/ModalWrapper';
import Spinner from './ui/Spinner';

interface Props {
  onClose: () => void;
}

const CURRENCY_OPTIONS = [
  { code: 'USD', label: 'USD — US Dollar' },
  { code: 'EUR', label: 'EUR — Euro' },
  { code: 'GBP', label: 'GBP — British Pound' },
  { code: 'CHF', label: 'CHF — Swiss Franc' },
  { code: 'JPY', label: 'JPY — Japanese Yen' },
  { code: 'CAD', label: 'CAD — Canadian Dollar' },
  { code: 'AUD', label: 'AUD — Australian Dollar' },
  { code: 'PLN', label: 'PLN — Polish Zloty' },
];

const accountSchema = z.object({
  accountName: z.string()
    .trim()
    .min(1, 'Account name is required')
    .max(50, 'Account name must be 50 characters or fewer'),
  currency: z.string().min(1, 'Currency is required'),
});

type AccountFormData = z.infer<typeof accountSchema>;

export default function CreateAccountModal({ onClose }: Props) {
  const queryClient = useQueryClient();

  const { register, handleSubmit, watch, formState: { errors } } = useForm<AccountFormData>({
    resolver: zodResolver(accountSchema),
    defaultValues: { accountName: '', currency: 'USD' },
  });

  const nameValue = watch('accountName');

  const mutation = useMutation({
    mutationFn: (data: AccountFormData) => createAccount(data.accountName, data.currency),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
      toast.success('Account created successfully');
      onClose();
    },
  });

  const onSubmit = (data: AccountFormData) => {
    mutation.mutate(data);
  };

  return (
    <ModalWrapper open onClose={mutation.isPending ? undefined : onClose} ariaLabel="Create Account">
      <div className="flex items-center justify-between mb-5">
        <h2 className="text-lg font-semibold text-text-primary">Create Account</h2>
        <button
          type="button"
          onClick={onClose}
          disabled={mutation.isPending}
          className="w-8 h-8 flex items-center justify-center rounded-lg text-text-muted hover:text-text-primary hover:bg-surface-hover transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          aria-label="Close dialog"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label htmlFor="accountName" className="block text-sm font-medium text-text-secondary mb-1.5">
            Account Name
          </label>
          <input
            id="accountName"
            type="text"
            {...register('accountName')}
            placeholder="e.g. Main Checking"
            maxLength={50}
            className={inputClass}
            autoFocus
          />
          <div className="flex justify-between mt-1.5">
            {errors.accountName ? (
              <p className="text-sm text-danger">{errors.accountName.message}</p>
            ) : (
              <span />
            )}
            <span className={`text-xs tabular-nums ${(nameValue?.length ?? 0) >= 40 ? 'text-warning' : 'text-text-muted'}`}>
              {nameValue?.length ?? 0}/50
            </span>
          </div>
        </div>

        <div>
          <label htmlFor="currency" className="block text-sm font-medium text-text-secondary mb-1.5">
            Currency
          </label>
          <select
            id="currency"
            {...register('currency')}
            className={inputClass}
          >
            {CURRENCY_OPTIONS.map((c) => (
              <option key={c.code} value={c.code}>{c.label}</option>
            ))}
          </select>
          {errors.currency && (
            <p className="mt-1.5 text-sm text-danger">{errors.currency.message}</p>
          )}
        </div>

        {mutation.isError && (
          <div className="bg-danger-muted text-danger rounded-lg p-3 text-sm flex items-start gap-2">
            <svg className="w-4 h-4 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
            </svg>
            {getApiErrorMessage(mutation.error, 'Failed to create account')}
          </div>
        )}

        <div className="flex gap-3 pt-2">
          <button
            type="button"
            onClick={onClose}
            disabled={mutation.isPending}
            className="flex-1 text-text-secondary hover:text-text-primary hover:bg-surface-hover py-2.5 rounded-lg text-sm font-medium transition-colors border border-border-primary disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Cancel
          </button>
          <motion.button
            whileTap={{ scale: 0.97 }}
            type="submit"
            disabled={mutation.isPending}
            className="flex-1 bg-accent hover:bg-accent-hover text-white py-2.5 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {mutation.isPending && <Spinner />}
            {mutation.isPending ? 'Creating...' : 'Create Account'}
          </motion.button>
        </div>
      </form>
    </ModalWrapper>
  );
}
