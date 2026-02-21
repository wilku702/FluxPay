import { motion } from 'motion/react';
import type { ReactNode } from 'react';

interface Props {
  children: ReactNode;
  stagger?: number;
  className?: string;
}

const container = {
  hidden: {},
  show: (stagger: number) => ({
    transition: {
      staggerChildren: stagger,
    },
  }),
};

export const staggerItem = {
  hidden: { opacity: 0, y: 12 },
  show: { opacity: 1, y: 0, transition: { duration: 0.3, ease: 'easeOut' as const } },
};

export default function StaggeredList({ children, stagger = 0.05, className }: Props) {
  return (
    <motion.div
      variants={container}
      initial="hidden"
      animate="show"
      custom={stagger}
      className={className}
    >
      {children}
    </motion.div>
  );
}
