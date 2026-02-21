import { motion } from 'motion/react';

interface Props {
  size?: number;
  className?: string;
}

export default function AnimatedCheckmark({ size = 48, className }: Props) {
  return (
    <div className={`rounded-full bg-accent-muted flex items-center justify-center ${className ?? ''}`} style={{ width: size, height: size }}>
      <svg width={size * 0.5} height={size * 0.5} viewBox="0 0 24 24" fill="none">
        <motion.path
          d="M5 13l4 4L19 7"
          stroke="currentColor"
          strokeWidth={2.5}
          strokeLinecap="round"
          strokeLinejoin="round"
          className="text-accent"
          initial={{ pathLength: 0 }}
          animate={{ pathLength: 1 }}
          transition={{ duration: 0.4, delay: 0.15, ease: 'easeOut' }}
        />
      </svg>
    </div>
  );
}
