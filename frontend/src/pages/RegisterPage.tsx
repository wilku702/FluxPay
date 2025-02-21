import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { register } from '../api/auth';
import { useAuth } from '../context/AuthContext';
import { getApiErrorMessage } from '../utils/errors';

export default function RegisterPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { handleAuth } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const data = await register(email, password, fullName);
      handleAuth(data);
      navigate('/dashboard');
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Registration failed'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex bg-surface-primary">
      {/* Left brand panel */}
      <div className="hidden lg:flex lg:w-1/2 bg-surface-secondary relative flex-col justify-between p-12 border-r border-border-primary overflow-hidden">
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
          <h2 className="text-4xl font-bold text-text-primary leading-tight">
            Start managing<br />your finances
          </h2>
          <p className="mt-4 text-text-secondary text-lg max-w-md">
            Create an account and get instant access to your personal payment ledger with real-time tracking.
          </p>
        </div>

        <p className="relative text-text-muted text-sm">
          Free to use. No credit card required.
        </p>
      </div>

      {/* Right form panel */}
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="w-full max-w-sm">
          <div className="flex items-center gap-2 mb-8 lg:hidden">
            <span className="w-2 h-2 rounded-full bg-accent" />
            <span className="text-lg font-bold text-text-primary tracking-tight">FluxPay</span>
          </div>

          <h1 className="text-2xl font-semibold text-text-primary">Create your account</h1>
          <p className="text-sm text-text-secondary mt-1 mb-8">Get started with FluxPay</p>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label htmlFor="fullName" className="block text-sm font-medium text-text-secondary mb-1.5">Full Name</label>
              <input
                id="fullName"
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                className="w-full bg-surface-secondary border border-border-primary text-text-primary rounded-lg px-4 py-3 text-sm placeholder:text-text-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors"
                placeholder="John Doe"
                required
              />
            </div>
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-text-secondary mb-1.5">Email</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full bg-surface-secondary border border-border-primary text-text-primary rounded-lg px-4 py-3 text-sm placeholder:text-text-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors"
                placeholder="you@example.com"
                required
              />
            </div>
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-text-secondary mb-1.5">Password</label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-surface-secondary border border-border-primary text-text-primary rounded-lg px-4 py-3 text-sm placeholder:text-text-muted focus:outline-none focus:border-accent focus:ring-1 focus:ring-accent transition-colors"
                placeholder="Minimum 8 characters"
                required
                minLength={8}
              />
            </div>

            {error && (
              <div className="bg-danger-muted text-danger rounded-lg p-3 text-sm flex items-start gap-2">
                <svg className="w-4 h-4 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z" />
                </svg>
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-accent hover:bg-accent-hover text-white rounded-lg py-3 font-medium text-sm transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                  Creating account...
                </span>
              ) : (
                'Register'
              )}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-text-muted">
            Already have an account?{' '}
            <Link to="/login" className="text-accent hover:text-accent-hover transition-colors">
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
