import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import { getAccountSummaries } from '../api/summaries';
import Skeleton from './animation/SkeletonShimmer';

interface BalanceChartProps {
  accountId: number;
  currency: string;
  days?: number;
}

export default function BalanceChart({ accountId, currency, days = 30 }: BalanceChartProps) {
  const { from, to } = useMemo(() => {
    const now = new Date();
    return {
      to: now.toISOString().split('T')[0],
      from: new Date(now.getTime() - days * 86400000).toISOString().split('T')[0],
    };
  }, [days]);

  const { data: summaries, isLoading } = useQuery({
    queryKey: ['summaries', accountId, from, to],
    queryFn: () => getAccountSummaries(accountId, from, to),
  });

  if (isLoading) {
    return (
      <div className="bg-surface-elevated border border-border-primary rounded-xl p-6">
        <Skeleton className="h-5 w-40 mb-4" />
        <Skeleton className="h-48 w-full rounded-lg" />
      </div>
    );
  }

  if (!summaries || summaries.length === 0) {
    return (
      <div className="bg-surface-elevated border border-border-primary rounded-xl p-6">
        <h3 className="text-xs font-semibold text-text-muted uppercase tracking-wider mb-3">
          Balance Trend
        </h3>
        <p className="text-sm text-text-secondary text-center py-8">
          No transaction data available for the selected period.
        </p>
      </div>
    );
  }

  const chartData = summaries.map((s) => ({
    date: new Date(s.summaryDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
    balance: s.closingBalance ?? 0,
    credits: s.totalCredits,
    debits: s.totalDebits,
  }));

  return (
    <div className="bg-surface-elevated border border-border-primary rounded-xl p-6">
      <h3 className="text-xs font-semibold text-text-muted uppercase tracking-wider mb-4">
        Balance Trend ({days}d)
      </h3>

      <ResponsiveContainer width="100%" height={200}>
        <AreaChart data={chartData} margin={{ top: 4, right: 4, left: 4, bottom: 0 }}>
          <defs>
            <linearGradient id="balanceGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3} />
              <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border-primary)" />
          <XAxis dataKey="date" tick={{ fontSize: 11, fill: 'var(--color-text-muted)' }} />
          <YAxis tick={{ fontSize: 11, fill: 'var(--color-text-muted)' }} width={60} />
          <Tooltip
            contentStyle={{
              backgroundColor: 'var(--color-surface-elevated)',
              border: '1px solid var(--color-border-primary)',
              borderRadius: '8px',
              fontSize: '12px',
            }}
            formatter={(value: number | undefined) =>
              new Intl.NumberFormat('en-US', {
                style: 'currency',
                currency,
                minimumFractionDigits: 2,
              }).format(value ?? 0)
            }
          />
          <Area
            type="monotone"
            dataKey="balance"
            stroke="#6366f1"
            strokeWidth={2}
            fill="url(#balanceGrad)"
            name="Balance"
          />
        </AreaChart>
      </ResponsiveContainer>

      <h3 className="text-xs font-semibold text-text-muted uppercase tracking-wider mt-6 mb-4">
        Daily Credits & Debits
      </h3>

      <ResponsiveContainer width="100%" height={160}>
        <BarChart data={chartData} margin={{ top: 4, right: 4, left: 4, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border-primary)" />
          <XAxis dataKey="date" tick={{ fontSize: 11, fill: 'var(--color-text-muted)' }} />
          <YAxis tick={{ fontSize: 11, fill: 'var(--color-text-muted)' }} width={60} />
          <Tooltip
            contentStyle={{
              backgroundColor: 'var(--color-surface-elevated)',
              border: '1px solid var(--color-border-primary)',
              borderRadius: '8px',
              fontSize: '12px',
            }}
            formatter={(value: number | undefined) =>
              new Intl.NumberFormat('en-US', {
                style: 'currency',
                currency,
                minimumFractionDigits: 2,
              }).format(value ?? 0)
            }
          />
          <Legend wrapperStyle={{ fontSize: '12px' }} />
          <Bar dataKey="credits" fill="#22c55e" name="Credits" radius={[2, 2, 0, 0]} />
          <Bar dataKey="debits" fill="#ef4444" name="Debits" radius={[2, 2, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
