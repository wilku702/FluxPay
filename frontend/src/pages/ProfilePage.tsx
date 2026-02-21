import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { getMe } from '../api/auth';
import { useAuth } from '../context/AuthContext';

export default function ProfilePage() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const { data: profile, isLoading } = useQuery({
    queryKey: ['profile'],
    queryFn: getMe,
  });

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (isLoading) {
    return (
      <div className="max-w-lg mx-auto">
        <div className="h-7 w-32 bg-surface-hover rounded-lg animate-pulse mb-6" />
        <div className="bg-surface-elevated border border-border-primary rounded-xl p-6">
          <div className="space-y-4">
            <div className="h-4 w-24 bg-surface-hover rounded animate-pulse" />
            <div className="h-5 w-48 bg-surface-hover rounded animate-pulse" />
            <div className="h-4 w-24 bg-surface-hover rounded animate-pulse mt-4" />
            <div className="h-5 w-36 bg-surface-hover rounded animate-pulse" />
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-lg mx-auto">
      <h1 className="text-2xl font-semibold text-text-primary mb-1">Profile</h1>
      <p className="text-sm text-text-secondary mb-6">Your account information</p>

      <div className="bg-surface-elevated border border-border-primary rounded-xl p-6">
        {/* Avatar */}
        <div className="flex items-center gap-4 mb-6 pb-6 border-b border-border-primary">
          <div className="w-14 h-14 rounded-full bg-accent-muted flex items-center justify-center text-lg font-semibold text-accent">
            {profile?.fullName
              ? profile.fullName.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2)
              : '??'}
          </div>
          <div>
            <p className="text-lg font-semibold text-text-primary">{profile?.fullName}</p>
            <p className="text-sm text-text-muted">User ID: {profile?.id}</p>
          </div>
        </div>

        {/* Details */}
        <div className="space-y-4">
          <div>
            <p className="text-xs font-semibold text-text-muted uppercase tracking-wider mb-1">Email</p>
            <p className="text-sm text-text-primary">{profile?.email}</p>
          </div>
          <div>
            <p className="text-xs font-semibold text-text-muted uppercase tracking-wider mb-1">Full Name</p>
            <p className="text-sm text-text-primary">{profile?.fullName}</p>
          </div>
        </div>

        {/* Logout */}
        <div className="mt-6 pt-6 border-t border-border-primary">
          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-2 py-2.5 rounded-lg text-sm font-medium text-danger bg-danger-muted hover:bg-danger/20 transition-colors"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15m3 0l3-3m0 0l-3-3m3 3H9" />
            </svg>
            Logout
          </button>
        </div>
      </div>
    </div>
  );
}
