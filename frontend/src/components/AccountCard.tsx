import { Link } from 'react-router-dom';
import type { AccountResponse } from '../types/api';

interface Props {
  account: AccountResponse;
}

const statusColors: Record<string, string> = {
  ACTIVE: 'bg-green-100 text-green-800',
  FROZEN: 'bg-blue-100 text-blue-800',
  CLOSED: 'bg-gray-100 text-gray-800',
};

export default function AccountCard({ account }: Props) {
  return (
    <Link
      to={`/accounts/${account.id}`}
      className="block bg-white rounded-lg shadow p-6 hover:shadow-md transition-shadow"
    >
      <div className="flex justify-between items-start">
        <div>
          <h3 className="text-lg font-semibold text-gray-900">{account.accountName}</h3>
          <p className="text-sm text-gray-500">{account.currency}</p>
        </div>
        <span className={`px-2 py-1 rounded-full text-xs font-medium ${statusColors[account.status]}`}>
          {account.status}
        </span>
      </div>
      <p className="mt-4 text-2xl font-bold text-gray-900">
        ${account.balance.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
      </p>
    </Link>
  );
}
