import { useState, useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { createAccount } from '../api/accounts';

interface Props {
  onClose: () => void;
}

const CURRENCIES = ['USD', 'EUR', 'GBP', 'CHF', 'JPY', 'CAD', 'AUD', 'PLN'];

const inputClass = "w-full bg-surface-secondary border border-border-primary text-text-primary rounded-lg px-4 py-2.5 text-sm placeholder:text-text-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors";

export default function CreateAccountModal({ onClose }: Props) {
  const [accountName, setAccountName] = useState('');
  const [currency, setCurrency] = useState('USD');
  const [error, setError] = useState('');
  const queryClient = useQueryClient();
  const dialogRef = useRef<HTMLDivElement>(null);
  const isPendingRef = useRef(false);

  const mutation = useMutation({
    mutationFn: () => createAccount(accountName.trim(), currency),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
      onClose();
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Failed to create account';
      setError(msg);
    },
  });

  isPendingRef.current = mutation.isPending;

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

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    mutation.mutate();
  };

  const safeClose = mutation.isPending ? undefined : onClose;

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm" onClick={safeClose}>
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-account-title"
        className="bg-surface-elevated border border-border-primary rounded-xl p-6 w-full max-w-md mx-4 shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-5">
          <h2 id="create-account-title" className="text-lg font-semibold text-text-primary">Create Account</h2>
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

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="accountName" className="block text-sm font-medium text-text-secondary mb-1.5">
              Account Name
            </label>
            <input
              id="accountName"
              type="text"
              value={accountName}
              onChange={(e) => setAccountName(e.target.value)}
              placeholder="e.g. Main Checking"
              maxLength={50}
              className={inputClass}
              required
              autoFocus
            />
          </div>

          <div>
            <label htmlFor="currency" className="block text-sm font-medium text-text-secondary mb-1.5">
              Currency
            </label>
            <select
              id="currency"
              value={currency}
              onChange={(e) => setCurrency(e.target.value)}
              className={inputClass}
            >
              {CURRENCIES.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
          </div>

          {error && (
            <div className="bg-danger-muted text-danger rounded-lg p-3 text-sm">{error}</div>
          )}

          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              disabled={mutation.isPending}
              className="flex-1 bg-surface-secondary hover:bg-surface-hover text-text-primary py-2.5 rounded-lg text-sm font-medium transition-colors border border-border-primary disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={mutation.isPending}
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
      </div>
    </div>,
    document.body
  );
}
