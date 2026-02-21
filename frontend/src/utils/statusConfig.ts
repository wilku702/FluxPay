export const txStatusConfig: Record<string, string> = {
  COMPLETED: 'bg-success/15 text-success',
  PENDING: 'bg-warning/15 text-warning',
  FAILED: 'bg-danger/15 text-danger',
  REVERSED: 'bg-surface-hover text-text-muted',
};

export const accountStatusConfig: Record<string, { dot: string; badge: string }> = {
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
