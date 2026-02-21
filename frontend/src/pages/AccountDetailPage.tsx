import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getAccount } from '../api/accounts';
import { getTransactions, type TransactionFilters } from '../api/transactions';
import TransactionTable from '../components/TransactionTable';
import TransactionFilterBar from '../components/TransactionFilters';
import Pagination from '../components/Pagination';
import { formatBalance } from '../utils/currency';
import { accountStatusConfig } from '../utils/statusConfig';
import AccountStatusModal from '../components/AccountStatusModal';
import { exportTransactions } from '../api/transactions';

export default function AccountDetailPage() {
  const { id } = useParams<{ id: string }>();
  const accountId = parseInt(id!);

  const [filters, setFilters] = useState<Partial<TransactionFilters>>({});
  const [page, setPage] = useState(0);
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDir, setSortDir] = useState('desc');
  const [showStatusModal, setShowStatusModal] = useState(false);

  const { data: account, isLoading: accountLoading } = useQuery({
    queryKey: ['account', accountId],
    queryFn: () => getAccount(accountId),
  });

  const { data: txPage, isLoading: txLoading } = useQuery({
    queryKey: ['transactions', accountId, filters, page, sortBy, sortDir],
    queryFn: () => getTransactions({
      accountId,
      ...filters,
      page,
      size: 20,
      sortBy,
      sortDir,
    }),
  });

  const handleSort = (field: string) => {
    if (field === sortBy) {
      setSortDir(sortDir === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(field);
      setSortDir('desc');
    }
  };

  const handleFilter = (newFilters: Partial<TransactionFilters>) => {
    setFilters(newFilters);
    setPage(0);
  };

  if (accountLoading) {
    return (
      <div>
        <div className="bg-surface-elevated border border-border-primary rounded-xl p-6 mb-6">
          <div className="flex justify-between items-start">
            <div>
              <div className="h-7 w-48 bg-surface-hover rounded-lg animate-pulse" />
              <div className="h-4 w-16 bg-surface-hover rounded animate-pulse mt-2" />
            </div>
            <div className="h-6 w-20 bg-surface-hover rounded-full animate-pulse" />
          </div>
          <div className="h-10 w-44 bg-surface-hover rounded-lg animate-pulse mt-6" />
        </div>
        <div className="bg-surface-elevated border border-border-primary rounded-xl overflow-hidden">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="flex items-center gap-4 px-4 py-3.5 border-b border-border-primary last:border-0">
              <div className="h-4 w-20 bg-surface-hover rounded animate-pulse" />
              <div className="h-5 w-14 bg-surface-hover rounded-full animate-pulse" />
              <div className="h-4 w-20 bg-surface-hover rounded animate-pulse" />
              <div className="h-4 w-32 bg-surface-hover rounded animate-pulse flex-1" />
              <div className="h-5 w-20 bg-surface-hover rounded-full animate-pulse" />
              <div className="h-4 w-20 bg-surface-hover rounded animate-pulse" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (!account) {
    return (
      <div className="flex flex-col items-center justify-center py-16 gap-4">
        <div className="w-12 h-12 rounded-xl bg-danger-muted flex items-center justify-center">
          <svg className="w-6 h-6 text-danger" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
          </svg>
        </div>
        <p className="text-sm font-medium text-text-primary">Account not found</p>
      </div>
    );
  }

  const status = accountStatusConfig[account.status] || accountStatusConfig.CLOSED;

  return (
    <div>
      {/* Account header card */}
      <div className="bg-surface-elevated border border-border-primary rounded-xl p-6 mb-6">
        <div className="flex justify-between items-start">
          <div>
            <h1 className="text-2xl font-semibold text-text-primary">{account.accountName}</h1>
            <p className="text-sm text-text-muted mt-1">{account.currency}</p>
          </div>
          <div className="flex items-center gap-2">
            <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${status.badge}`}>
              <span className={`w-1.5 h-1.5 rounded-full ${status.dot}`} />
              {account.status}
            </span>
            <button
              onClick={() => setShowStatusModal(true)}
              className="px-2.5 py-1 rounded-full text-xs font-medium border border-border-primary text-text-secondary hover:bg-surface-hover transition-colors"
            >
              Manage
            </button>
          </div>
        </div>
        <div className="mt-5">
          <p className="text-xs font-semibold text-text-muted uppercase tracking-wider mb-1">Available Balance</p>
          <p className="text-4xl font-bold text-text-primary tabular-nums tracking-tight">
            {formatBalance(account.balance, account.currency)}
          </p>
        </div>
      </div>

      <div className="flex items-center justify-between mb-4">
        <h2 className="text-xs font-semibold text-text-muted uppercase tracking-wider">Transaction History</h2>
        <button
          onClick={() => exportTransactions({ accountId, ...filters })}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium border border-border-primary text-text-secondary hover:bg-surface-hover transition-colors"
        >
          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" />
          </svg>
          Export CSV
        </button>
      </div>
      <TransactionFilterBar onFilter={handleFilter} />

      {txLoading ? (
        <div className="bg-surface-elevated border border-border-primary rounded-xl overflow-hidden">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="flex items-center gap-4 px-4 py-3.5 border-b border-border-primary last:border-0">
              <div className="h-4 w-20 bg-surface-hover rounded animate-pulse" />
              <div className="h-5 w-14 bg-surface-hover rounded-full animate-pulse" />
              <div className="h-4 w-20 bg-surface-hover rounded animate-pulse" />
              <div className="h-4 w-32 bg-surface-hover rounded animate-pulse flex-1" />
              <div className="h-5 w-20 bg-surface-hover rounded-full animate-pulse" />
              <div className="h-4 w-20 bg-surface-hover rounded animate-pulse" />
            </div>
          ))}
        </div>
      ) : (
        <>
          <TransactionTable
            transactions={txPage?.content || []}
            currency={account.currency}
            onSort={handleSort}
            sortBy={sortBy}
            sortDir={sortDir}
          />
          {txPage && (
            <Pagination
              page={txPage.number}
              totalPages={txPage.totalPages}
              onPageChange={setPage}
            />
          )}
        </>
      )}

      {showStatusModal && account && (
        <AccountStatusModal account={account} onClose={() => setShowStatusModal(false)} />
      )}
    </div>
  );
}
