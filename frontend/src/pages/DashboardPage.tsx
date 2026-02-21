import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { motion } from 'motion/react';
import { getAccounts } from '../api/accounts';
import { useAuth } from '../context/AuthContext';
import AccountCard from '../components/AccountCard';
import QuickTransfer from '../components/QuickTransfer';
import CreateAccountModal from '../components/CreateAccountModal';
import AnimatedPage from '../components/animation/AnimatedPage';
import StaggeredList from '../components/animation/StaggeredList';
import AnimatedNumber from '../components/animation/AnimatedNumber';
import Skeleton from '../components/animation/SkeletonShimmer';
import { formatBalance } from '../utils/currency';

export default function DashboardPage() {
  const { fullName } = useAuth();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const { data: accounts, isLoading, error } = useQuery({
    queryKey: ['accounts'],
    queryFn: getAccounts,
  });

  const firstName = fullName?.split(' ')[0] || 'there';

  const balanceByCurrency = accounts?.reduce<Record<string, number>>((acc, a) => {
    acc[a.currency] = (acc[a.currency] ?? 0) + a.balance;
    return acc;
  }, {}) ?? {};
  const currencyEntries = Object.entries(balanceByCurrency);

  if (isLoading) {
    return (
      <div>
        <div className="mb-8">
          <Skeleton className="h-8 w-64" />
          <Skeleton className="h-5 w-40 mt-2" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="bg-surface-elevated border border-border-primary rounded-xl p-6">
              <div className="flex justify-between">
                <Skeleton className="h-4 w-28" />
                <Skeleton className="h-5 w-16 rounded-full" />
              </div>
              <Skeleton className="h-9 w-36 mt-6" />
              <div className="border-t border-border-primary mt-5 pt-4">
                <Skeleton className="h-3 w-24" />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center py-16 gap-4">
        <div className="w-12 h-12 rounded-xl bg-danger-muted flex items-center justify-center">
          <svg className="w-6 h-6 text-danger" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
          </svg>
        </div>
        <p className="text-sm font-medium text-text-primary">Failed to load accounts</p>
        <p className="text-sm text-text-secondary">Please try again later.</p>
      </div>
    );
  }

  return (
    <AnimatedPage>
      {/* Header */}
      <div className="flex items-start justify-between mb-8">
        <div>
          <h1 className="text-2xl font-semibold text-text-primary">Welcome back, {firstName}</h1>
          {accounts && accounts.length > 0 && currencyEntries.length > 0 && (
            <p className="text-text-secondary mt-1">
              Total balance:{' '}
              {currencyEntries.map(([currency, total], i) => (
                <span key={currency}>
                  {i > 0 && <span className="text-text-muted mx-1">/</span>}
                  <span className="text-text-primary font-semibold tabular-nums">
                    <AnimatedNumber
                      value={total}
                      formatFn={(n) => formatBalance(n, currency)}
                    />
                  </span>
                </span>
              ))}
            </p>
          )}
        </div>
        <motion.button
          whileTap={{ scale: 0.97 }}
          onClick={() => setShowCreateModal(true)}
          className="bg-accent hover:bg-accent-hover text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors flex items-center gap-2"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2} aria-hidden="true">
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          New Account
        </motion.button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <h2 className="text-xs font-semibold text-text-muted uppercase tracking-wider mb-4">Your Accounts</h2>
          {accounts && accounts.length > 0 ? (
            <StaggeredList stagger={0.05} className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {accounts.map((account) => (
                <AccountCard key={account.id} account={account} />
              ))}
            </StaggeredList>
          ) : (
            <div className="bg-surface-elevated border border-border-primary rounded-xl p-12 text-center">
              <div className="w-12 h-12 rounded-xl bg-surface-hover flex items-center justify-center mx-auto mb-4">
                <svg className="w-6 h-6 text-text-muted" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5} aria-hidden="true">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M2.25 8.25h19.5M2.25 9h19.5m-16.5 5.25h6m-6 2.25h3m-3.75 3h15a2.25 2.25 0 002.25-2.25V6.75A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25v10.5A2.25 2.25 0 004.5 19.5z" />
                </svg>
              </div>
              <p className="text-sm font-medium text-text-primary">No accounts yet</p>
              <p className="text-sm text-text-secondary mt-1">
                Accounts hold a balance in a single currency and record all transactions.
              </p>
              <motion.button
                whileTap={{ scale: 0.97 }}
                onClick={() => setShowCreateModal(true)}
                className="mt-5 bg-accent hover:bg-accent-hover text-white px-6 py-2.5 rounded-lg text-sm font-medium transition-colors"
              >
                Create Account
              </motion.button>
            </div>
          )}
        </div>

        <div>
          {accounts && accounts.length >= 2 && (
            <QuickTransfer accounts={accounts} />
          )}
        </div>
      </div>
      {showCreateModal && <CreateAccountModal onClose={() => setShowCreateModal(false)} />}
    </AnimatedPage>
  );
}
