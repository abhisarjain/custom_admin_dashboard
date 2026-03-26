import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Layout from '../../components/layout/Layout';
import { authService } from '../../services/authService';

export default function Invitations() {
  const navigate = useNavigate();
  const [invitations, setInvitations] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchInvitations();
  }, []);

  const fetchInvitations = async () => {
    try {
      const res = await authService.getMyInvitations();
      setInvitations(res.data || []);
    } catch (err) {
      toast.error('Failed to fetch invitations');
    } finally {
      setLoading(false);
    }
  };

  const handleAccept = async (token) => {
    try {
      await authService.acceptInvitation(token);
      toast.success('Invitation accepted!');
      await fetchInvitations();
      navigate('/projects');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to accept');
    }
  };

  return (
    <Layout>
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-white">Invitations</h1>
        <p className="text-gray-400 mt-1">Review and accept project invitations.</p>
      </div>

      {loading ? (
        <div className="text-gray-400 text-center py-12">Loading...</div>
      ) : invitations.length > 0 ? (
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
          <h2 className="text-white font-semibold mb-4 flex items-center gap-2">
            <Bell size={18} className="text-yellow-400" />
            Pending Invitations
          </h2>
          <div className="space-y-3">
            {invitations.map((inv) => (
              <div
                key={inv.id}
                className="flex items-center justify-between bg-gray-800 rounded-lg px-4 py-3"
              >
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
      ) : (
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-8 text-center">
          <Bell size={32} className="text-gray-600 mx-auto mb-3" />
          <p className="text-gray-400">No pending invitations</p>
        </div>
      )}
    </Layout>
  );
}
