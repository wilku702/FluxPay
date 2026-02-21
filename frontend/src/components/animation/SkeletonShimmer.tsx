interface Props {
  className?: string;
}

export default function Skeleton({ className = '' }: Props) {
  return (
    <div className={`animate-shimmer rounded bg-surface-hover ${className}`} />
  );
}
