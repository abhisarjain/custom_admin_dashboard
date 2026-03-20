import { useState, useEffect } from 'react';
import Layout from '../../components/layout/Layout';
import { useAuth } from '../../context/AuthContext';
import { authService } from '../../services/authService';
import { toast } from 'react-hot-toast';
import { FolderKanban, Database, Shield, Bell } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export default function Dashboard() {
  const { tenant } = useAuth();
  const navigate = useNavigate();
  const [invitations, setInvitations] = useState([]);

  useEffect(() => {
    fetchInvitations();
  }, []);

  const fetchInvitations = async () => {
    try {
      const res = await authService.getMyInvitations();
      setInvitations(res.data || []);
    } catch (err) {
      console.error(err);
    }
  };

  const handleAccept = async (token) => {
    try {
      await authService.acceptInvitation(token);
      toast.success('Invitation accepted!');
      fetchInvitations();
      navigate('/projects');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to accept');
    }
  };

  const stats = [
    { label: 'Projects', icon: FolderKanban, color: 'indigo' },
    { label: 'Connections', icon: Database, color: 'green' },
    { label: 'Roles', icon: Shield, color: 'purple' },
    { label: 'Invitations', value: invitations.length, icon: Bell, color: 'yellow' },
  ];

  return (
    <Layout>
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-white">
          Welcome back, <span className="text-indigo-400">{tenant?.name}</span> 👋
        </h1>
        <p className="text-gray-400 mt-1">Here's what's happening with your projects.</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-4 gap-4 mb-8">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <div key={stat.label} className="bg-gray-900 border border-gray-800 rounded-xl p-5">
              <div className="flex items-center justify-between mb-3">
                <span className="text-gray-400 text-sm">{stat.label}</span>
                <div className={`w-8 h-8 rounded-lg bg-${stat.color}-500/20 flex items-center justify-center`}>
                  <Icon size={16} className={`text-${stat.color}-400`} />
                </div>
              </div>
              <p className="text-2xl font-bold text-white">{stat.value ?? '—'}</p>
            </div>
          );
        })}
      </div>

      {/* Pending Invitations */}
      {invitations.length > 0 && (
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
          <h2 className="text-white font-semibold mb-4 flex items-center gap-2">
            <Bell size={18} className="text-yellow-400" />
            Pending Invitations
          </h2>
          <div className="space-y-3">
            {invitations.map((inv) => (
              <div key={inv.id}
                className="flex items-center justify-between bg-gray-800 rounded-lg px-4 py-3">
                <div>
                  <p className="text-white text-sm font-medium">{inv.projectName}</p>
                  <p className="text-gray-400 text-xs mt-0.5">
                    Invited by {inv.invitedByName} · Role: {inv.roleName}
                  </p>
                </div>
                <button
                  onClick={() => handleAccept(inv.token)}
                  className="bg-indigo-600 hover:bg-indigo-700 text-white text-sm px-4 py-2 rounded-lg transition"
                >
                  Accept
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {invitations.length === 0 && (
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-8 text-center">
          <Bell size={32} className="text-gray-600 mx-auto mb-3" />
          <p className="text-gray-400">No pending invitations</p>
        </div>
      )}

    </Layout>
  );
}
