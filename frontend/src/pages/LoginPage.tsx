import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'motion/react';
import { login } from '../api/auth';
import { useAuth } from '../context/AuthContext';
import { getApiErrorMessage } from '../utils/errors';
import { inputClass } from '../utils/styles';
import Spinner from '../components/ui/Spinner';

const staggerContainer = {
  hidden: {},
  show: { transition: { staggerChildren: 0.08 } },
};

const fadeSlide = {
  hidden: { opacity: 0, y: 12 },
  show: { opacity: 1, y: 0, transition: { duration: 0.3, ease: 'easeOut' as const } },
};

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { handleAuth } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const data = await login(email, password);
      handleAuth(data);
      navigate('/dashboard');
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Login failed'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex bg-surface-primary">
      {/* Left brand panel */}
      <div className="hidden lg:flex lg:w-1/2 bg-surface-secondary relative flex-col justify-between p-12 border-r border-border-primary overflow-hidden">
        {/* Background pattern */}
        <div className="absolute inset-0 opacity-5">
          <div className="absolute inset-0" style={{
            backgroundImage: 'radial-gradient(circle at 1px 1px, rgba(16,185,129,0.4) 1px, transparent 0)',
            backgroundSize: '32px 32px',
          }} />
        </div>

        <div className="relative">
          <div className="flex items-center gap-2 mb-16">
            <span className="w-2 h-2 rounded-full bg-accent" />
            <span className="text-lg font-bold text-text-primary tracking-tight">FluxPay</span>
          </div>
          <motion.h2
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, ease: 'easeOut' }}
            className="text-4xl font-bold text-text-primary leading-tight"
          >
            Modern payment<br />ledger
          </motion.h2>
          <motion.p
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.5, delay: 0.1, ease: 'easeOut' }}
            className="mt-4 text-text-secondary text-lg max-w-md"
          >
            Secure, real-time transaction management with built-in idempotency and double-entry bookkeeping.
          </motion.p>
        </div>

        <p className="relative text-text-muted text-sm">
          Trusted by developers who build with precision.
        </p>
      </div>

      {/* Right form panel */}
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="w-full max-w-sm">
          {/* Mobile logo */}
          <div className="flex items-center gap-2 mb-8 lg:hidden">
            <span className="w-2 h-2 rounded-full bg-accent" />
            <span className="text-lg font-bold text-text-primary tracking-tight">FluxPay</span>
          </div>

          <h1 className="text-2xl font-semibold text-text-primary">Welcome back</h1>
          <p className="text-sm text-text-secondary mt-1 mb-8">Sign in to your account</p>

          <motion.form
            onSubmit={handleSubmit}
            variants={staggerContainer}
            initial="hidden"
            animate="show"
            className="space-y-5"
          >
            <motion.div variants={fadeSlide}>
              <label htmlFor="email" className="block text-sm font-medium text-text-secondary mb-1.5">Email</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className={inputClass}
                placeholder="you@example.com"
                required
              />
            </motion.div>
            <motion.div variants={fadeSlide}>
              <label htmlFor="password" className="block text-sm font-medium text-text-secondary mb-1.5">Password</label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className={inputClass}
                placeholder="Enter your password"
                required
              />
            </motion.div>

            {error && (
              <motion.div variants={fadeSlide} className="bg-danger-muted text-danger rounded-lg p-3 text-sm flex items-start gap-2">
                <svg className="w-4 h-4 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
                </svg>
                {error}
              </motion.div>
            )}

            <motion.div variants={fadeSlide}>
              <motion.button
                whileTap={{ scale: 0.97 }}
                type="submit"
                disabled={loading}
                className="w-full bg-accent hover:bg-accent-hover text-white rounded-lg py-3 font-medium text-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {loading ? (
                  <span className="flex items-center justify-center gap-2">
                    <Spinner />
                    Signing in...
                  </span>
                ) : (
                  'Sign in'
                )}
              </motion.button>
            </motion.div>
          </motion.form>

          <p className="mt-6 text-center text-sm text-text-muted">
            Don't have an account?{' '}
            <Link to="/register" className="text-accent hover:text-accent-hover transition-colors">
              Register
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
