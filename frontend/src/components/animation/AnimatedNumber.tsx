import { useEffect, useRef, useState } from 'react';
import { animate } from 'motion';

interface Props {
  value: number;
  duration?: number;
  formatFn?: (n: number) => string;
  className?: string;
}

export default function AnimatedNumber({ value, duration = 0.8, formatFn, className }: Props) {
  const [display, setDisplay] = useState(value);
  const prevValue = useRef(value);

  useEffect(() => {
    const from = prevValue.current;
    prevValue.current = value;

    if (from === value) {
      setDisplay(value);
      return;
    }

    const controls = animate(from, value, {
      duration,
      ease: 'easeOut',
      onUpdate: (latest) => setDisplay(latest),
    });

    return () => controls.stop();
  }, [value, duration]);

  return <span className={className}>{formatFn ? formatFn(display) : display.toFixed(2)}</span>;
}
