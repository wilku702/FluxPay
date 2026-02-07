import { useState } from 'react';

interface Props {
  onFilter: (filters: {
    type?: string;
    status?: string;
    from?: string;
    to?: string;
    minAmount?: number;
    maxAmount?: number;
  }) => void;
}

export default function TransactionFilters({ onFilter }: Props) {
  const [type, setType] = useState('');
  const [status, setStatus] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [minAmount, setMinAmount] = useState('');
  const [maxAmount, setMaxAmount] = useState('');

  const handleApply = () => {
    onFilter({
      type: type || undefined,
      status: status || undefined,
      from: from || undefined,
      to: to || undefined,
      minAmount: minAmount ? parseFloat(minAmount) : undefined,
      maxAmount: maxAmount ? parseFloat(maxAmount) : undefined,
    });
  };

  const handleReset = () => {
    setType('');
    setStatus('');
    setFrom('');
    setTo('');
    setMinAmount('');
    setMaxAmount('');
    onFilter({});
  };

  return (
    <div className="bg-white rounded-lg shadow p-4 mb-4">
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        <select value={type} onChange={(e) => setType(e.target.value)}
          className="border rounded px-3 py-2 text-sm">
          <option value="">All Types</option>
          <option value="CREDIT">Credit</option>
          <option value="DEBIT">Debit</option>
        </select>
        <select value={status} onChange={(e) => setStatus(e.target.value)}
          className="border rounded px-3 py-2 text-sm">
          <option value="">All Statuses</option>
          <option value="COMPLETED">Completed</option>
          <option value="PENDING">Pending</option>
          <option value="FAILED">Failed</option>
          <option value="REVERSED">Reversed</option>
        </select>
        <input type="date" value={from} onChange={(e) => setFrom(e.target.value)}
          placeholder="From" className="border rounded px-3 py-2 text-sm" />
        <input type="date" value={to} onChange={(e) => setTo(e.target.value)}
          placeholder="To" className="border rounded px-3 py-2 text-sm" />
        <input type="number" value={minAmount} onChange={(e) => setMinAmount(e.target.value)}
          placeholder="Min $" className="border rounded px-3 py-2 text-sm" step="0.01" />
        <input type="number" value={maxAmount} onChange={(e) => setMaxAmount(e.target.value)}
          placeholder="Max $" className="border rounded px-3 py-2 text-sm" step="0.01" />
      </div>
      <div className="flex gap-2 mt-3">
        <button onClick={handleApply}
          className="bg-indigo-600 text-white px-4 py-2 rounded text-sm hover:bg-indigo-700">
          Apply
        </button>
        <button onClick={handleReset}
          className="bg-gray-200 text-gray-700 px-4 py-2 rounded text-sm hover:bg-gray-300">
          Reset
        </button>
      </div>
    </div>
  );
}
