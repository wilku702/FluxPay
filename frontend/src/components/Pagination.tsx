interface Props {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export default function Pagination({ page, totalPages, onPageChange }: Props) {
  if (totalPages <= 1) return null;

  return (
    <div className="flex items-center justify-between mt-4 px-4 py-3 bg-surface-elevated border border-border-primary rounded-xl">
      <span className="text-sm text-text-secondary">
        Page <span className="font-medium text-text-primary">{page + 1}</span> of{' '}
        <span className="font-medium text-text-primary">{totalPages}</span>
      </span>
      <div className="flex items-center gap-1">
        <button
          onClick={() => onPageChange(page - 1)}
          disabled={page === 0}
          className="flex items-center gap-1.5 h-8 px-3 text-sm font-medium text-text-secondary border border-border-primary rounded-lg hover:bg-surface-hover hover:text-text-primary transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
          </svg>
          Previous
        </button>
        <button
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1}
          className="flex items-center gap-1.5 h-8 px-3 text-sm font-medium text-text-secondary border border-border-primary rounded-lg hover:bg-surface-hover hover:text-text-primary transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
        >
          Next
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
          </svg>
        </button>
      </div>
    </div>
  );
}
