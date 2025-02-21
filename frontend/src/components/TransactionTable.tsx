import type { TransactionResponse } from '../types/api';

interface Props {
  transactions: TransactionResponse[];
  onSort?: (field: string) => void;
  sortBy?: string;
  sortDir?: string;
}

const statusConfig: Record<string, string> = {
  COMPLETED: 'bg-success/15 text-success',
  PENDING: 'bg-warning/15 text-warning',
  FAILED: 'bg-danger/15 text-danger',
  REVERSED: 'bg-surface-hover text-text-muted',
};

export default function TransactionTable({ transactions, onSort, sortBy = 'createdAt', sortDir = 'desc' }: Props) {
  const renderSortIcon = (field: string) => {
    if (sortBy !== field) return null;
    return (
      <svg className="w-3.5 h-3.5 inline ml-1 text-accent" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        {sortDir === 'asc' ? (
          <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 15.75l7.5-7.5 7.5 7.5" />
        ) : (
          <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
        )}
      </svg>
    );
  };

  const handleSort = (field: string) => {
    onSort?.(field);
  };

  const thClass = "px-4 py-3 text-left text-xs font-semibold text-text-muted uppercase tracking-wider cursor-pointer select-none hover:text-text-secondary transition-colors";

  return (
    <div className="bg-surface-elevated border border-border-primary rounded-xl overflow-hidden">
      <div className="overflow-x-auto">
        <table className="min-w-full">
          <thead className="bg-surface-secondary border-b border-border-primary">
            <tr>
              <th className={thClass} onClick={() => handleSort('createdAt')}>
                Date{renderSortIcon('createdAt')}
              </th>
              <th className={thClass} onClick={() => handleSort('type')}>
                Type{renderSortIcon('type')}
              </th>
              <th className={thClass} onClick={() => handleSort('amount')}>
                Amount{renderSortIcon('amount')}
              </th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-text-muted uppercase tracking-wider">
                Description
              </th>
              <th className={thClass} onClick={() => handleSort('status')}>
                Status{renderSortIcon('status')}
              </th>
              <th className="px-4 py-3 text-right text-xs font-semibold text-text-muted uppercase tracking-wider">
                Balance After
              </th>
            </tr>
          </thead>
          <tbody>
            {transactions.map((tx) => (
              <tr key={tx.id} className="border-b border-border-primary last:border-0 hover:bg-surface-hover transition-colors duration-100">
                <td className="px-4 py-3.5 text-sm text-text-secondary tabular-nums whitespace-nowrap">
                  {new Date(tx.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}
                  <span className="block text-xs text-text-muted">
                    {new Date(tx.createdAt).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })}
                  </span>
                </td>
                <td className="px-4 py-3.5 text-sm">
                  <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                    tx.type === 'CREDIT' ? 'bg-success/15 text-success' : 'bg-danger/15 text-danger'
                  }`}>
                    {tx.type}
                  </span>
                </td>
                <td className="px-4 py-3.5 text-sm font-semibold tabular-nums">
                  <span className={tx.type === 'CREDIT' ? 'text-success' : 'text-danger'}>
                    {tx.type === 'CREDIT' ? '+' : '-'}${tx.amount.toFixed(2)}
                  </span>
                </td>
                <td className="px-4 py-3.5 text-sm text-text-secondary max-w-[200px] truncate">
                  {tx.description}
                </td>
                <td className="px-4 py-3.5 text-sm">
                  <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium ${statusConfig[tx.status] || statusConfig.COMPLETED}`}>
                    <span className={`w-1.5 h-1.5 rounded-full ${
                      tx.status === 'COMPLETED' ? 'bg-success' :
                      tx.status === 'PENDING' ? 'bg-warning animate-pulse' :
                      tx.status === 'FAILED' ? 'bg-danger' : 'bg-text-muted'
                    }`} />
                    {tx.status}
                  </span>
                </td>
                <td className="px-4 py-3.5 text-sm font-medium text-text-primary tabular-nums text-right">
                  ${tx.balanceAfter.toFixed(2)}
                </td>
              </tr>
            ))}
            {transactions.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-16 text-center">
                  <div className="flex flex-col items-center gap-3">
                    <div className="w-12 h-12 rounded-xl bg-surface-hover flex items-center justify-center">
                      <svg className="w-6 h-6 text-text-muted" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
                      </svg>
                    </div>
                    <p className="text-sm font-medium text-text-primary">No transactions found</p>
                    <p className="text-sm text-text-secondary">Transactions will appear here once activity begins.</p>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
