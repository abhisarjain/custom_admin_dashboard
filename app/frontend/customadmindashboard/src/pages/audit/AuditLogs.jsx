import { useEffect, useMemo, useState } from 'react';
import { History, RefreshCw, FolderKanban, CalendarClock, ArrowRightLeft } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Layout from '../../components/layout/Layout';
import api from '../../services/api';

function formatAction(action) {
  return (action || 'UNKNOWN')
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function parsePayload(value) {
  if (!value) return null;
  if (typeof value === 'object') return value;
  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}

function renderPayload(payload) {
  const parsed = parsePayload(payload);

  if (!parsed) {
    return <p className="text-gray-500 text-xs">No details</p>;
  }

  if (typeof parsed === 'string') {
    return <p className="text-gray-300 text-xs break-words">{parsed}</p>;
  }

  return (
    <div className="space-y-2">
      {Object.entries(parsed).map(([key, value]) => (
        <div key={key} className="flex items-start justify-between gap-3 text-xs">
          <span className="text-gray-500">{key}</span>
          <span className="text-gray-200 text-right break-all">
            {typeof value === 'object' ? JSON.stringify(value) : String(value)}
          </span>
        </div>
      ))}
    </div>
  );
}

export default function AuditLogs() {
  const [projects, setProjects] = useState([]);
  const [selectedProjectId, setSelectedProjectId] = useState('');
  const [logs, setLogs] = useState([]);
  const [projectsLoading, setProjectsLoading] = useState(true);
  const [logsLoading, setLogsLoading] = useState(false);
  const [permissionMessage, setPermissionMessage] = useState('');

  useEffect(() => {
    fetchProjects();
  }, []);

  useEffect(() => {
    if (selectedProjectId) {
      fetchLogs(selectedProjectId);
    }
  }, [selectedProjectId]);

  const fetchProjects = async () => {
    try {
      const res = await api.get('/api/projects');
      const nextProjects = res.data.data || [];
      setProjects(nextProjects);
      if (nextProjects.length > 0) {
        setSelectedProjectId(String(nextProjects[0].id));
      }
    } catch (err) {
      toast.error('Failed to fetch projects');
    } finally {
      setProjectsLoading(false);
    }
  };

  const fetchLogs = async (projectId) => {
    setLogsLoading(true);
    try {
      const res = await api.get(`/api/projects/${projectId}/audit`);
      setPermissionMessage('');
      setLogs(res.data.data || []);
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to fetch audit logs';
      setLogs([]);
      if (message === 'You do not have permission to view audit logs') {
        setPermissionMessage(message);
      } else {
        setPermissionMessage('');
        toast.error(message);
      }
    } finally {
      setLogsLoading(false);
    }
  };

  const selectedProject = useMemo(
    () => projects.find((project) => String(project.id) === String(selectedProjectId)),
    [projects, selectedProjectId]
  );

  const uniqueActions = new Set(logs.map((log) => log.action)).size;

  return (
    <Layout>
      <div className="flex items-start justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl font-bold text-white">Audit Logs</h1>
          <p className="text-gray-400 mt-1">
            Track who changed what, when it happened, and what was affected.
          </p>
        </div>
        <button
          onClick={() => selectedProjectId && fetchLogs(selectedProjectId)}
          disabled={!selectedProjectId || logsLoading}
          className="flex items-center gap-2 bg-gray-800 hover:bg-gray-700 disabled:opacity-60 disabled:cursor-not-allowed text-white px-4 py-2 rounded-lg transition"
        >
          <RefreshCw size={16} className={logsLoading ? 'animate-spin' : ''} />
          Refresh
        </button>
      </div>

      <div className="grid grid-cols-4 gap-4 mb-6">
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-5">
          <div className="flex items-center justify-between mb-3">
            <span className="text-gray-400 text-sm">Project</span>
            <FolderKanban size={16} className="text-indigo-400" />
          </div>
          <p className="text-white font-semibold truncate">{selectedProject?.name || 'Select project'}</p>
        </div>
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-5">
          <div className="flex items-center justify-between mb-3">
            <span className="text-gray-400 text-sm">Recent Events</span>
            <History size={16} className="text-yellow-400" />
          </div>
          <p className="text-2xl font-bold text-white">{logs.length}</p>
        </div>
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-5">
          <div className="flex items-center justify-between mb-3">
            <span className="text-gray-400 text-sm">Action Types</span>
            <ArrowRightLeft size={16} className="text-emerald-400" />
          </div>
          <p className="text-2xl font-bold text-white">{uniqueActions}</p>
        </div>
        <div className="bg-gray-900 border border-gray-800 rounded-xl p-5">
          <div className="flex items-center justify-between mb-3">
            <span className="text-gray-400 text-sm">Latest Event</span>
            <CalendarClock size={16} className="text-sky-400" />
          </div>
          <p className="text-white font-semibold">
            {logs[0]?.createdAt ? new Date(logs[0].createdAt).toLocaleString() : 'No activity'}
          </p>
        </div>
      </div>

      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-6">
          <div>
            <h2 className="text-white font-semibold">Recent Activity</h2>
            <p className="text-gray-400 text-sm mt-1">Latest 20 events for the selected project.</p>
          </div>
          <select
            value={selectedProjectId}
            onChange={(e) => setSelectedProjectId(e.target.value)}
            disabled={projectsLoading || projects.length === 0}
            className="bg-gray-800 text-white border border-gray-700 rounded-lg px-4 py-2.5 min-w-[240px] focus:outline-none focus:border-indigo-500"
          >
            {projects.length === 0 ? (
              <option value="">No projects found</option>
            ) : (
              projects.map((project) => (
                <option key={project.id} value={project.id}>
                  {project.name}
                </option>
              ))
            )}
          </select>
        </div>

        {projectsLoading || logsLoading ? (
          <div className="text-gray-400 text-center py-12">Loading audit activity...</div>
        ) : permissionMessage ? (
          <div className="border border-dashed border-gray-800 rounded-xl p-10 text-center">
            <History size={32} className="text-gray-600 mx-auto mb-3" />
            <p className="text-gray-300">{permissionMessage}</p>
          </div>
        ) : logs.length === 0 ? (
          <div className="border border-dashed border-gray-800 rounded-xl p-10 text-center">
            <History size={32} className="text-gray-600 mx-auto mb-3" />
            <p className="text-gray-400">No audit events yet for this project.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {logs.map((log) => (
              <div key={log.id} className="border border-gray-800 rounded-xl p-5 bg-gray-950/40">
                <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-3 mb-4">
                  <div>
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="px-2.5 py-1 rounded-full bg-indigo-500/15 text-indigo-300 text-xs font-medium">
                        {formatAction(log.action)}
                      </span>
                      {log.tableName && (
                        <span className="px-2.5 py-1 rounded-full bg-gray-800 text-gray-300 text-xs">
                          {log.tableName}
                        </span>
                      )}
                      {log.recordId && (
                        <span className="px-2.5 py-1 rounded-full bg-gray-800 text-gray-400 text-xs">
                          ID: {log.recordId}
                        </span>
                      )}
                    </div>
                    <p className="text-white text-sm font-medium mt-3">
                      {log.actorName || 'Unknown user'}
                      {log.actorEmail ? (
                        <span className="text-gray-400 font-normal"> · {log.actorEmail}</span>
                      ) : null}
                    </p>
                    <p className="text-gray-400 text-sm mt-3">
                      {new Date(log.createdAt).toLocaleString()}
                    </p>
                  </div>
                </div>

                <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
                  <div className="bg-gray-900 border border-gray-800 rounded-lg p-4">
                    <p className="text-gray-400 text-xs uppercase tracking-wide mb-3">Before</p>
                    {renderPayload(log.oldValue)}
                  </div>
                  <div className="bg-gray-900 border border-gray-800 rounded-lg p-4">
                    <p className="text-gray-400 text-xs uppercase tracking-wide mb-3">After</p>
                    {renderPayload(log.newValue)}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </Layout>
  );
}
