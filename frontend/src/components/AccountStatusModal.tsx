import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { updateAccountStatus } from '../api/accounts';
import { getApiErrorMessage } from '../utils/errors';
import { accountStatusConfig } from '../utils/statusConfig';
import type { AccountResponse } from '../types/api';

interface Props {
  account: AccountResponse;
  onClose: () => void;
}

type TargetStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED';

interface Action {
  label: string;
  target: TargetStatus;
  style: string;
  description: string;
}

const actionsByStatus: Record<string, Action[]> = {
  ACTIVE: [
    { label: 'Freeze Account', target: 'FROZEN', style: 'bg-warning/15 text-warning hover:bg-warning/25', description: 'Temporarily freeze this account. No transactions will be allowed until reactivated.' },
    { label: 'Close Account', target: 'CLOSED', style: 'bg-danger/15 text-danger hover:bg-danger/25', description: 'Permanently close this account. This action cannot be undone.' },
  ],
  FROZEN: [
    { label: 'Activate Account', target: 'ACTIVE', style: 'bg-success/15 text-success hover:bg-success/25', description: 'Reactivate this account to resume normal operations.' },
    { label: 'Close Account', target: 'CLOSED', style: 'bg-danger/15 text-danger hover:bg-danger/25', description: 'Permanently close this account. This action cannot be undone.' },
  ],
  CLOSED: [],
};

export default function AccountStatusModal({ account, onClose }: Props) {
  const [confirming, setConfirming] = useState<Action | null>(null);
  const queryClient = useQueryClient();

  const mutation = useMutation({
    mutationFn: (status: string) => updateAccountStatus(account.id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['account', account.id] });
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
      onClose();
    },
  });

  const actions = actionsByStatus[account.status] || [];
  const statusStyle = accountStatusConfig[account.status] || accountStatusConfig.CLOSED;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={onClose} />
      <div className="relative bg-surface-elevated border border-border-primary rounded-xl w-full max-w-md mx-4 p-6 shadow-xl">
        {/* Header */}
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-lg font-semibold text-text-primary">Manage Account</h2>
          <button onClick={onClose} className="text-text-muted hover:text-text-primary transition-colors">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Current status */}
        <div className="bg-surface-secondary border border-border-primary rounded-lg p-4 mb-5">
          <p className="text-xs font-semibold text-text-muted uppercase tracking-wider mb-2">Current Status</p>
          <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${statusStyle.badge}`}>
            <span className={`w-1.5 h-1.5 rounded-full ${statusStyle.dot}`} />
            {account.status}
          </span>
        </div>

        {/* Error */}
        {mutation.isError && (
          <div className="bg-danger-muted text-danger rounded-lg p-3 text-sm mb-4 flex items-start gap-2">
            <svg className="w-4 h-4 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
            </svg>
            {getApiErrorMessage(mutation.error, 'Failed to update account status')}
          </div>
        )}

        {/* Confirmation view */}
        {confirming ? (
          <div className="space-y-4">
            <div className="bg-surface-secondary border border-border-primary rounded-lg p-4">
              <p className="text-sm font-medium text-text-primary mb-1">{confirming.label}?</p>
              <p className="text-sm text-text-secondary">{confirming.description}</p>
            </div>
            <div className="flex gap-3">
              <button
                onClick={() => setConfirming(null)}
                className="flex-1 py-2.5 rounded-lg text-sm font-medium border border-border-primary text-text-secondary hover:bg-surface-hover transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={() => mutation.mutate(confirming.target)}
                disabled={mutation.isPending}
                className={`flex-1 py-2.5 rounded-lg text-sm font-medium transition-colors disabled:opacity-50 ${confirming.style}`}
              >
                {mutation.isPending ? 'Updating...' : 'Confirm'}
              </button>
            </div>
          </div>
        ) : (
          <>
            {/* Actions */}
            {actions.length > 0 ? (
              <div className="space-y-2">
                {actions.map((action) => (
                  <button
                    key={action.target}
                    onClick={() => setConfirming(action)}
                    className={`w-full py-2.5 rounded-lg text-sm font-medium transition-colors ${action.style}`}
                  >
                    {action.label}
                  </button>
                ))}
              </div>
            ) : (
              <p className="text-sm text-text-muted text-center py-4">
                This account is closed. No actions available.
              </p>
            )}
          </>
        )}
      </div>
    </div>
  );
}
