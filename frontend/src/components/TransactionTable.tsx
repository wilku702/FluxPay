import type { TransactionResponse } from '../types/api';

interface Props {
  transactions: TransactionResponse[];
  onSort?: (field: string) => void;
  sortBy?: string;
  sortDir?: string;
}

export default function TransactionTable({ transactions, onSort, sortBy = 'createdAt', sortDir = 'desc' }: Props) {
  const renderSortIcon = (field: string) => {
    if (sortBy !== field) return null;
    return sortDir === 'asc' ? ' \u2191' : ' \u2193';
  };

  const handleSort = (field: string) => {
    onSort?.(field);
  };

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th
              className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer"
              onClick={() => handleSort('createdAt')}
            >
              Date{renderSortIcon('createdAt')}
            </th>
            <th
              className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer"
              onClick={() => handleSort('type')}
            >
              Type{renderSortIcon('type')}
            </th>
            <th
              className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer"
              onClick={() => handleSort('amount')}
            >
              Amount{renderSortIcon('amount')}
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Description
            </th>
            <th
              className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase cursor-pointer"
              onClick={() => handleSort('status')}
            >
              Status{renderSortIcon('status')}
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              Balance After
            </th>
          </tr>
        </thead>
        <tbody className="bg-white divide-y divide-gray-200">
          {transactions.map((tx) => (
            <tr key={tx.id}>
              <td className="px-4 py-3 text-sm text-gray-600">
                {new Date(tx.createdAt).toLocaleDateString()}
              </td>
              <td className="px-4 py-3 text-sm">
                <span className={tx.type === 'CREDIT' ? 'text-green-600' : 'text-red-600'}>
                  {tx.type}
                </span>
              </td>
              <td className="px-4 py-3 text-sm font-medium">
                <span className={tx.type === 'CREDIT' ? 'text-green-600' : 'text-red-600'}>
                  {tx.type === 'CREDIT' ? '+' : '-'}${tx.amount.toFixed(2)}
                </span>
              </td>
              <td className="px-4 py-3 text-sm text-gray-600">{tx.description}</td>
              <td className="px-4 py-3 text-sm text-gray-600">{tx.status}</td>
              <td className="px-4 py-3 text-sm text-gray-900 font-medium">
                ${tx.balanceAfter.toFixed(2)}
              </td>
            </tr>
          ))}
          {transactions.length === 0 && (
            <tr>
              <td colSpan={6} className="px-4 py-8 text-center text-gray-500">
                No transactions found.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
