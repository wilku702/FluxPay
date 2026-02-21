import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createAccount } from '../api/accounts';
import { getApiErrorMessage } from '../utils/errors';

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

const inputClass = "w-full bg-surface-secondary border border-border-primary text-text-primary rounded-lg px-4 py-3 text-sm placeholder:text-text-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors";

export default function CreateAccountModal({ onClose }: Props) {
  const queryClient = useQueryClient();
  const dialogRef = useRef<HTMLDivElement>(null);
  const isPendingRef = useRef(false);
  const [visible, setVisible] = useState(false);
  const [success, setSuccess] = useState(false);

  const { register, handleSubmit, watch, formState: { errors } } = useForm<AccountFormData>({
    resolver: zodResolver(accountSchema),
    defaultValues: { accountName: '', currency: 'USD' },
  });

  const nameValue = watch('accountName');

  const mutation = useMutation({
    mutationFn: (data: AccountFormData) => createAccount(data.accountName, data.currency),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
      setSuccess(true);
      setTimeout(() => onClose(), 1200);
    },
  });

  isPendingRef.current = mutation.isPending || success;

  const onSubmit = (data: AccountFormData) => {
    mutation.mutate(data);
  };

  // Entry animation
  useEffect(() => {
    requestAnimationFrame(() => setVisible(true));
  }, []);

  // Scroll lock + focus restore
  useEffect(() => {
    const previousElement = document.activeElement as HTMLElement | null;
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = originalOverflow;
      previousElement?.focus();
    };
  }, []);

  // Escape key handler
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !isPendingRef.current) onClose();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  // Focus trap
  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key !== 'Tab') return;
      const focusable = dialog.querySelectorAll<HTMLElement>(
        'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
      );
      if (focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault();
        first.focus();
      }
    };
    dialog.addEventListener('keydown', handleKeyDown);
    return () => dialog.removeEventListener('keydown', handleKeyDown);
  }, []);

  const isLocked = mutation.isPending || success;
  const safeClose = isLocked ? undefined : onClose;

  return createPortal(
    <div
      className={`fixed inset-0 z-50 flex items-end sm:items-center justify-center transition-opacity duration-200 ${visible ? 'bg-black/50 backdrop-blur-sm' : 'bg-black/0'}`}
      onClick={safeClose}
    >
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-account-title"
        className={`bg-surface-elevated border border-border-primary rounded-t-xl sm:rounded-xl p-6 w-full sm:max-w-md sm:mx-4 shadow-2xl transition-all duration-200 ${visible ? 'translate-y-0 opacity-100 sm:scale-100' : 'translate-y-4 opacity-0 sm:scale-95 sm:translate-y-0'}`}
        onClick={(e) => e.stopPropagation()}
      >
        {success ? (
          <div className="flex flex-col items-center py-6 gap-3">
            <div className="w-12 h-12 rounded-full bg-accent-muted flex items-center justify-center">
              <svg className="w-6 h-6 text-accent" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <p className="text-sm font-medium text-text-primary">Account created successfully</p>
          </div>
        ) : (
          <>
            <div className="flex items-center justify-between mb-5">
              <h2 id="create-account-title" className="text-lg font-semibold text-text-primary">Create Account</h2>
              <button
                type="button"
                onClick={onClose}
                disabled={isLocked}
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
                  disabled={isLocked}
                  className="flex-1 text-text-secondary hover:text-text-primary hover:bg-surface-hover py-2.5 rounded-lg text-sm font-medium transition-colors border border-border-primary disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isLocked}
                  className="flex-1 bg-accent hover:bg-accent-hover text-white py-2.5 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                >
                  {mutation.isPending && (
                    <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                    </svg>
                  )}
                  {mutation.isPending ? 'Creating...' : 'Create Account'}
                </button>
              </div>
            </form>
          </>
        )}
      </div>
    </div>,
    document.body
  );
}
