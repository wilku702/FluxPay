import { Link } from 'react-router-dom';
import { motion } from 'motion/react';
import type { AccountResponse } from '../types/api';
import { formatBalance } from '../utils/currency';
import AnimatedNumber from './animation/AnimatedNumber';
import { staggerItem } from './animation/staggerVariants';

interface Props {
  account: AccountResponse;
}

const statusConfig: Record<string, { dot: string; badge: string }> = {
  ACTIVE: {
    dot: 'bg-success',
    badge: 'bg-success/15 text-success',
  },
  FROZEN: {
    dot: 'bg-warning',
    badge: 'bg-warning/15 text-warning',
  },
  CLOSED: {
    dot: 'bg-text-muted',
    badge: 'bg-surface-hover text-text-muted',
  },
};

const MotionLink = motion.create(Link);

export default function AccountCard({ account }: Props) {
  const status = statusConfig[account.status] || statusConfig.CLOSED;

  return (
    <MotionLink
      to={`/accounts/${account.id}`}
      variants={staggerItem}
      whileHover={{ y: -2, boxShadow: '0 8px 25px -5px rgba(16,185,129,0.1)' }}
      whileTap={{ scale: 0.985 }}
      transition={{ type: 'spring', damping: 20, stiffness: 300 }}
      className="group block bg-surface-elevated border border-border-primary rounded-xl p-6 hover:border-border-secondary hover:bg-surface-hover transition-colors duration-200"
    >
      <div className="flex justify-between items-start">
        <div>
          <p className="text-sm font-medium text-text-secondary uppercase tracking-wide">
            {account.accountName}
          </p>
          <p className="text-xs text-text-muted mt-0.5">{account.currency}</p>
        </div>
        <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${status.badge}`}>
          <span className={`w-1.5 h-1.5 rounded-full ${status.dot}`} />
          {account.status}
        </span>
      </div>
      <p className="mt-5 text-3xl font-bold text-text-primary tabular-nums tracking-tight">
        <AnimatedNumber
          value={account.balance}
          duration={0.6}
          formatFn={(n) => formatBalance(n, account.currency)}
        />
      </p>
      <div className="mt-5 pt-4 border-t border-border-primary flex items-center justify-between">
        <span className="text-xs text-text-muted">View transactions</span>
        <svg className="w-4 h-4 text-text-muted group-hover:text-accent group-hover:translate-x-0.5 transition-all duration-200" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5} aria-hidden="true">
          <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
        </svg>
      </div>
    </MotionLink>
  );
}
