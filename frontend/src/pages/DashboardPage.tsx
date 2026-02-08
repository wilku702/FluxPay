import { useQuery } from '@tanstack/react-query';
import { getAccounts } from '../api/accounts';
import AccountCard from '../components/AccountCard';
import QuickTransfer from '../components/QuickTransfer';

export default function DashboardPage() {
  const { data: accounts, isLoading, error } = useQuery({
    queryKey: ['accounts'],
    queryFn: getAccounts,
  });

  if (isLoading) return <div className="text-center py-8 text-gray-500">Loading accounts...</div>;
  if (error) return <div className="text-center py-8 text-red-500">Failed to load accounts.</div>;

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Your Accounts</h2>
          {accounts && accounts.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {accounts.map((account) => (
                <AccountCard key={account.id} account={account} />
              ))}
            </div>
          ) : (
            <div className="bg-white rounded-lg shadow p-8 text-center text-gray-500">
              No accounts yet. Create one to get started.
            </div>
          )}
        </div>

        <div>
          {accounts && accounts.length >= 2 && (
            <QuickTransfer accounts={accounts} />
          )}
        </div>
      </div>
    </div>
  );
}
