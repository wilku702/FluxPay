import '@testing-library/jest-dom/vitest';
import { vi } from 'vitest';
import React from 'react';

// Helper to strip motion-specific props
function stripMotionProps(props: Record<string, unknown>) {
  const {
    initial, animate, exit, transition, variants,
    whileHover, whileTap, whileFocus, whileDrag, whileInView,
    layout, layoutId, onAnimationStart, onAnimationComplete,
    custom, ...rest
  } = props;
  return rest;
}

// Mock motion/react so animated components render immediately as plain elements
vi.mock('motion/react', () => {
  const createPassthrough = (Component: React.ElementType) =>
    React.forwardRef((props: Record<string, unknown>, ref: unknown) => {
      return React.createElement(Component, { ...stripMotionProps(props), ref });
    });

  const handler: ProxyHandler<object> = {
    get(_target, prop) {
      if (prop === '__esModule') return true;
      if (prop === 'create') return createPassthrough;
      if (typeof prop === 'string') {
        return React.forwardRef((props: Record<string, unknown>, ref: unknown) => {
          return React.createElement(prop, { ...stripMotionProps(props), ref });
        });
      }
      return undefined;
    },
  };

  const motionProxy = new Proxy({}, handler);

  return {
    motion: motionProxy,
    AnimatePresence: ({ children }: { children: React.ReactNode }) => children,
    useAnimation: () => ({ start: vi.fn(), stop: vi.fn(), set: vi.fn() }),
    useMotionValue: (initial: number) => ({
      get: () => initial,
      set: vi.fn(),
      on: vi.fn(),
    }),
    useTransform: (_value: unknown, _input: unknown, output: number[]) => ({
      get: () => output?.[0] ?? 0,
      set: vi.fn(),
      on: vi.fn(),
    }),
    useSpring: (initial: number) => ({
      get: () => initial,
      set: vi.fn(),
      on: vi.fn(),
    }),
  };
});

// Mock motion (for animate function used by AnimatedNumber)
vi.mock('motion', () => ({
  animate: (_from: number, to: number, opts: { onUpdate?: (v: number) => void }) => {
    opts?.onUpdate?.(to);
    return { stop: vi.fn() };
  },
}));
