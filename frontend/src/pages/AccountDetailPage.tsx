import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getAccount } from '../api/accounts';
import { getTransactions, type TransactionFilters } from '../api/transactions';
import TransactionTable from '../components/TransactionTable';
import TransactionFilterBar from '../components/TransactionFilters';
import Pagination from '../components/Pagination';

export default function AccountDetailPage() {
  const { id } = useParams<{ id: string }>();
  const accountId = parseInt(id!);

  const [filters, setFilters] = useState<Partial<TransactionFilters>>({});
  const [page, setPage] = useState(0);
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDir, setSortDir] = useState('desc');

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

  if (accountLoading) return <div className="text-center py-8 text-gray-500">Loading...</div>;
  if (!account) return <div className="text-center py-8 text-red-500">Account not found.</div>;

  const statusColors: Record<string, string> = {
    ACTIVE: 'bg-green-100 text-green-800',
    FROZEN: 'bg-blue-100 text-blue-800',
    CLOSED: 'bg-gray-100 text-gray-800',
  };

  return (
    <div>
      <div className="bg-white rounded-lg shadow p-6 mb-6">
        <div className="flex justify-between items-start">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{account.accountName}</h1>
            <p className="text-sm text-gray-500 mt-1">{account.currency}</p>
          </div>
          <span className={`px-3 py-1 rounded-full text-sm font-medium ${statusColors[account.status]}`}>
            {account.status}
          </span>
        </div>
        <p className="mt-4 text-3xl font-bold text-gray-900">
          ${account.balance.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
        </p>
      </div>

      <h2 className="text-lg font-semibold text-gray-900 mb-4">Transaction History</h2>
      <TransactionFilterBar onFilter={handleFilter} />

      {txLoading ? (
        <div className="text-center py-8 text-gray-500">Loading transactions...</div>
      ) : (
        <>
          <div className="bg-white rounded-lg shadow">
            <TransactionTable
              transactions={txPage?.content || []}
              onSort={handleSort}
              sortBy={sortBy}
              sortDir={sortDir}
            />
          </div>
          {txPage && (
            <Pagination
              page={txPage.number}
              totalPages={txPage.totalPages}
              onPageChange={setPage}
            />
          )}
        </>
      )}
    </div>
  );
}
