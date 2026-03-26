import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, Bell, CalendarClock, FolderKanban, History, Settings2, Shield, Table, Users, Zap } from 'lucide-react';
import { toast } from 'react-hot-toast';
import Layout from '../../components/layout/Layout';
import api from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { DASHBOARD_CARD_META, QUICK_ACTION_OPTIONS, createEmptyDashboardSettings } from './dashboardConfig';
import { buildSelectedProjectState, fetchDashboardCatalog, resolveDashboardSettings } from './dashboardData';

const cardIcons = {
  recentActivity: History,
  projects: FolderKanban,
  tables: Table,
  quickActions: Zap,
  roles: Shield,
  members: Users,
};

function formatAction(action) {
  return (action || 'UNKNOWN')
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function formatRoleLabel(project) {
  if (!project) return 'No access';
  if (project.permissions?.isOwner) return 'Owner';
  return project.permissions?.role || 'Member';
}

function DashboardCard({ icon: Icon, title, description, projectOptions = [], selectedProjectId, onProjectChange, children, action }) {
  return (
    <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6">
      <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4 mb-5">
        <div className="flex items-start gap-3">
          <div className="w-11 h-11 rounded-xl bg-indigo-500/15 flex items-center justify-center shrink-0">
            <Icon size={18} className="text-indigo-300" />
          </div>
          <div>
            <h2 className="text-white font-semibold">{title}</h2>
            {description ? <p className="text-gray-400 text-sm mt-1">{description}</p> : null}
          </div>
        </div>
        <div className="flex items-center gap-3">
          {projectOptions.length > 0 ? (
            <select
              value={selectedProjectId || ''}
              onChange={(e) => onProjectChange?.(Number(e.target.value))}
              className="bg-gray-800 text-white border border-gray-700 rounded-lg px-4 py-2.5 min-w-[220px] focus:outline-none focus:border-indigo-500"
            >
              {projectOptions.map((project) => (
                <option key={project.id} value={project.id}>
                  {project.name}
                </option>
              ))}
            </select>
          ) : null}
          {action}
        </div>
      </div>
      {children}
    </div>
  );
}

export default function Dashboard() {
  const navigate = useNavigate();
  const { tenant } = useAuth();

  const [catalog, setCatalog] = useState(null);
  const [settings, setSettings] = useState(createEmptyDashboardSettings());
  const [selectedProjects, setSelectedProjects] = useState({
    recentActivity: '',
    projects: '',
    tables: '',
    quickActions: '',
    roles: '',
    members: '',
  });
  const [loading, setLoading] = useState(true);

  const [recentActivity, setRecentActivity] = useState({
    loading: false,
    logs: [],
    message: '',
  });
  const [rolesByProject, setRolesByProject] = useState({});
  const [membersByProject, setMembersByProject] = useState({});

  useEffect(() => {
    loadDashboard();
  }, []);

  useEffect(() => {
    if (!catalog) return;
    setSelectedProjects(buildSelectedProjectState(settings, catalog));
  }, [catalog, settings]);

  useEffect(() => {
    const projectId = selectedProjects.recentActivity;
    if (!projectId) return;
    loadRecentActivity(projectId);
  }, [selectedProjects.recentActivity]);

  useEffect(() => {
    const projectId = selectedProjects.roles;
    if (projectId) loadRoles(projectId);
  }, [selectedProjects.roles]);

  useEffect(() => {
    const projectId = selectedProjects.members;
    if (projectId) loadMembers(projectId);
  }, [selectedProjects.members]);

  const loadDashboard = async () => {
    const [catalogResult, settingsResult] = await Promise.allSettled([
      fetchDashboardCatalog(),
      api.get('/api/dashboard/settings'),
    ]);

    if (catalogResult.status === 'fulfilled') {
      const catalogData = catalogResult.value;
      setCatalog(catalogData);

      const remoteSettings = settingsResult.status === 'fulfilled'
        ? settingsResult.value.data?.data
        : null;

      setSettings(resolveDashboardSettings(remoteSettings, tenant?.tenantId, catalogData));
    } else {
      setCatalog({
        projects: [],
        invitations: [],
        totals: {
          projects: 0,
          invitations: 0,
          connections: 0,
          tables: 0,
        },
      });
      setSettings(createEmptyDashboardSettings());
      toast.error('Failed to load dashboard');
    }
    setLoading(false);
  };

  const loadRecentActivity = async (projectId) => {
    setRecentActivity((prev) => ({ ...prev, loading: true }));
    try {
      const response = await api.get(`/api/projects/${projectId}/audit`);
      setRecentActivity({
        loading: false,
        logs: response.data?.data || [],
        message: '',
      });
    } catch (err) {
      setRecentActivity({
        loading: false,
        logs: [],
        message: err.response?.data?.message || 'Failed to load recent activity',
      });
    }
  };

  const loadRoles = async (projectId) => {
    if (rolesByProject[projectId]) return;
    try {
      const response = await api.get(`/api/projects/${projectId}/roles`);
      setRolesByProject((prev) => ({ ...prev, [projectId]: response.data?.data || [] }));
    } catch (err) {
      setRolesByProject((prev) => ({ ...prev, [projectId]: [] }));
    }
  };

  const loadMembers = async (projectId) => {
    if (membersByProject[projectId]) return;
    try {
      const response = await api.get(`/api/projects/${projectId}/members`);
      setMembersByProject((prev) => ({ ...prev, [projectId]: response.data?.data || [] }));
    } catch (err) {
      setMembersByProject((prev) => ({ ...prev, [projectId]: [] }));
    }
  };

  const cardProjects = useMemo(() => {
    if (!catalog) {
      return {
        projects: [],
        tables: [],
        quickActions: [],
        roles: [],
        members: [],
      };
    }

    const findProjects = (ids) => ids
      .map((id) => catalog.projects.find((project) => Number(project.id) === Number(id)))
      .filter(Boolean);

    const tableProjectIds = [...new Set(settings.tableSelections.map((item) => Number(item.projectId)))];

    return {
      projects: findProjects(settings.projectCardProjectIds),
      tables: findProjects(tableProjectIds),
      quickActions: catalog.projects,
      roles: findProjects(settings.roleProjectIds),
      members: findProjects(settings.memberProjectIds),
    };
  }, [catalog, settings]);

  const selectedProjectSummary = cardProjects.projects.find((project) => Number(project.id) === Number(selectedProjects.projects));
  const selectedTablesProject = cardProjects.tables.find((project) => Number(project.id) === Number(selectedProjects.tables));
  const selectedRolesProject = cardProjects.roles.find((project) => Number(project.id) === Number(selectedProjects.roles));
  const selectedMembersProject = cardProjects.members.find((project) => Number(project.id) === Number(selectedProjects.members));
  const selectedQuickActionsProject = cardProjects.quickActions.find((project) => Number(project.id) === Number(selectedProjects.quickActions));

  const selectedTables = settings.tableSelections
    .filter((item) => Number(item.projectId) === Number(selectedProjects.tables))
    .map((item) => selectedTablesProject?.accessibleTables.find((table) => table.tableName === item.tableName))
    .filter(Boolean);

  const configuredCardIds = settings.cardOrder.filter((cardId) => {
    if (cardId === 'recentActivity') return settings.showRecentActivity !== false;
    if (cardId === 'projects') return settings.projectCardProjectIds.length > 0;
    if (cardId === 'tables') return settings.tableSelections.length > 0;
    if (cardId === 'quickActions') return settings.quickActionIds.length > 0;
    if (cardId === 'roles') return settings.roleProjectIds.length > 0;
    if (cardId === 'members') return settings.memberProjectIds.length > 0;
    return false;
  });

  const quickActions = QUICK_ACTION_OPTIONS.filter((action) => settings.quickActionIds.includes(action.id));

  const runQuickAction = (actionId) => {
    switch (actionId) {
      case 'openProjectList':
        navigate('/projects');
        break;
      case 'openInvitations':
        navigate('/invitations');
        break;
      case 'openAuditLogs':
        navigate('/audit');
        break;
      case 'createProject':
        navigate('/projects');
        break;
      case 'openTables':
        if (selectedQuickActionsProject) navigate(`/projects/${selectedQuickActionsProject.id}?tab=tables`);
        break;
      case 'openRoles':
        if (selectedQuickActionsProject) navigate(`/projects/${selectedQuickActionsProject.id}?tab=roles`);
        break;
      case 'openMembers':
        if (selectedQuickActionsProject) navigate(`/projects/${selectedQuickActionsProject.id}?tab=members`);
        break;
      case 'openConnections':
        if (selectedQuickActionsProject) navigate(`/projects/${selectedQuickActionsProject.id}?tab=connections`);
        break;
      default:
        break;
    }
  };

  const handleCardProjectChange = (cardId, projectId) => {
    setSelectedProjects((prev) => ({ ...prev, [cardId]: projectId }));
  };

  if (loading) {
    return (
      <Layout>
        <div className="text-gray-400 text-center py-16">Loading dashboard...</div>
      </Layout>
    );
  }

  if (!catalog) {
    return (
      <Layout>
        <div className="rounded-2xl border border-dashed border-gray-800 p-10 text-center">
          <Settings2 size={30} className="text-gray-600 mx-auto mb-3" />
          <p className="text-white font-medium">Dashboard could not be loaded right now.</p>
          <p className="text-gray-400 mt-2">Try refreshing once. If the issue continues, open dashboard settings again.</p>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4 mb-8">
        <div>
          <h1 className="text-3xl font-bold text-white">
            Welcome back, <span className="text-indigo-400">{tenant?.name}</span>
          </h1>
          <p className="text-gray-400 mt-2">
            Your dashboard is now a personal workspace. Pick the cards you want and switch project context from each card.
          </p>
        </div>
        <button
          onClick={() => navigate('/dashboard/settings')}
          className="flex items-center gap-2 bg-gray-800 hover:bg-gray-700 text-white px-5 py-3 rounded-xl transition"
        >
          <Settings2 size={16} />
          Customize Dashboard
        </button>
      </div>

      {configuredCardIds.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-gray-800 p-10 text-center">
          <Settings2 size={30} className="text-gray-600 mx-auto mb-3" />
          <p className="text-white font-medium">Your dashboard is ready to be customized.</p>
          <p className="text-gray-400 mt-2 mb-5">
            Pick projects, tables, quick actions, roles, and members cards from dashboard settings.
          </p>
          <button
            onClick={() => navigate('/dashboard/settings')}
            className="inline-flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-3 rounded-xl transition"
          >
            <Settings2 size={16} />
            Open Dashboard Settings
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
          {configuredCardIds.map((cardId) => {
            if (cardId === 'recentActivity') {
              return (
                <DashboardCard
                  key={cardId}
                  icon={cardIcons.recentActivity}
                  title="Recent Activity"
                  description="Latest activity for the selected project."
                  projectOptions={catalog.projects}
                  selectedProjectId={selectedProjects.recentActivity}
                  onProjectChange={(projectId) => handleCardProjectChange('recentActivity', projectId)}
                >
                  {recentActivity.loading ? (
                    <div className="text-gray-400 py-10 text-center">Loading recent activity...</div>
                  ) : recentActivity.message ? (
                    <div className="rounded-xl border border-dashed border-gray-800 p-8 text-center">
                      <History size={30} className="text-gray-600 mx-auto mb-3" />
                      <p className="text-gray-300">{recentActivity.message}</p>
                    </div>
                  ) : recentActivity.logs.length === 0 ? (
                    <div className="rounded-xl border border-dashed border-gray-800 p-8 text-center">
                      <History size={30} className="text-gray-600 mx-auto mb-3" />
                      <p className="text-gray-400">No activity found for this project yet.</p>
                    </div>
                  ) : (
                    <div className="space-y-3">
                      {recentActivity.logs.slice(0, 6).map((log) => (
                        <div key={log.id} className="rounded-xl border border-gray-800 bg-gray-950/50 px-4 py-4">
                          <div className="flex items-center justify-between gap-3 flex-wrap">
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className="px-2.5 py-1 rounded-full bg-indigo-500/15 text-indigo-300 text-xs font-medium">
                                {formatAction(log.action)}
                              </span>
                              {log.tableName ? (
                                <span className="px-2.5 py-1 rounded-full bg-gray-800 text-gray-300 text-xs">
                                  {log.tableName}
                                </span>
                              ) : null}
                            </div>
                            <span className="text-xs text-gray-500">
                              {new Date(log.createdAt).toLocaleString()}
                            </span>
                          </div>
                          <p className="text-sm text-white mt-3">
                            {log.actorName || 'Unknown user'}
                            {log.actorEmail ? <span className="text-gray-500"> · {log.actorEmail}</span> : null}
                          </p>
                        </div>
                      ))}
                    </div>
                  )}
                </DashboardCard>
              );
            }

            if (cardId === 'projects') {
              return (
                <DashboardCard
                  key={cardId}
                  icon={cardIcons.projects}
                  title="Projects"
                  description="Selected project summaries with quick context."
                  projectOptions={cardProjects.projects}
                  selectedProjectId={selectedProjects.projects}
                  onProjectChange={(projectId) => handleCardProjectChange('projects', projectId)}
                  action={
                    selectedProjectSummary ? (
                      <button
                        onClick={() => navigate(`/projects/${selectedProjectSummary.id}`)}
                        className="text-sm text-indigo-300 hover:text-indigo-200 transition"
                      >
                        Open project
                      </button>
                    ) : null
                  }
                >
                  {selectedProjectSummary ? (
                    <div className="space-y-4">
                      <div className="rounded-xl bg-gray-950/50 border border-gray-800 p-5">
                        <div className="flex items-start justify-between gap-4">
                          <div>
                            <p className="text-white font-semibold text-lg">{selectedProjectSummary.name}</p>
                            <p className="text-gray-400 text-sm mt-2">
                              {selectedProjectSummary.description || 'No project description added yet.'}
                            </p>
                          </div>
                          <span className="px-3 py-1 rounded-full bg-indigo-500/15 text-indigo-300 text-xs font-medium">
                            {formatRoleLabel(selectedProjectSummary)}
                          </span>
                        </div>
                        <div className="grid grid-cols-3 gap-3 mt-5">
                          <div className="rounded-xl bg-gray-900 border border-gray-800 px-4 py-4">
                            <p className="text-xs text-gray-500">Connections</p>
                            <p className="text-xl font-semibold text-white mt-1">{selectedProjectSummary.connectionCount}</p>
                          </div>
                          <div className="rounded-xl bg-gray-900 border border-gray-800 px-4 py-4">
                            <p className="text-xs text-gray-500">Accessible Tables</p>
                            <p className="text-xl font-semibold text-white mt-1">{selectedProjectSummary.accessibleTables.length}</p>
                          </div>
                          <div className="rounded-xl bg-gray-900 border border-gray-800 px-4 py-4">
                            <p className="text-xs text-gray-500">All Tables</p>
                            <p className="text-xl font-semibold text-white mt-1">{selectedProjectSummary.totalTablesCount}</p>
                          </div>
                        </div>
                      </div>
                    </div>
                  ) : (
                    <p className="text-gray-400">Select a project from settings to show this card.</p>
                  )}
                </DashboardCard>
              );
            }

            if (cardId === 'tables') {
              return (
                <DashboardCard
                  key={cardId}
                  icon={cardIcons.tables}
                  title="Tables"
                  description="Your chosen tables, grouped by project."
                  projectOptions={cardProjects.tables}
                  selectedProjectId={selectedProjects.tables}
                  onProjectChange={(projectId) => handleCardProjectChange('tables', projectId)}
                  action={
                    selectedTablesProject ? (
                      <button
                        onClick={() => navigate(`/projects/${selectedTablesProject.id}?tab=tables`)}
                        className="text-sm text-indigo-300 hover:text-indigo-200 transition"
                      >
                        Open tables
                      </button>
                    ) : null
                  }
                >
                  {selectedTables.length === 0 ? (
                    <p className="text-gray-400">No tables selected for this project.</p>
                  ) : (
                    <div className="space-y-3">
                      {selectedTables.map((table) => (
                        <div key={table.tableName} className="rounded-xl border border-gray-800 bg-gray-950/50 px-4 py-4 flex items-center justify-between gap-4">
                          <div>
                            <p className="text-white font-medium">{table.tableName}</p>
                            <p className="text-gray-500 text-sm mt-1">{table.columnsCount} columns</p>
                          </div>
                          <button
                            onClick={() => {
                              if (table.viewId) {
                                navigate(`/projects/${table.projectId}/dashboard/${table.viewId}`);
                              } else {
                                navigate(`/projects/${table.projectId}?tab=tables`);
                              }
                            }}
                            className="text-sm text-indigo-300 hover:text-indigo-200 transition inline-flex items-center gap-1"
                          >
                            Open <ArrowRight size={14} />
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </DashboardCard>
              );
            }

            if (cardId === 'quickActions') {
              return (
                <DashboardCard
                  key={cardId}
                  icon={cardIcons.quickActions}
                  title="Quick Actions"
                  description="Shortcuts you chose for your daily workflow."
                  projectOptions={cardProjects.quickActions}
                  selectedProjectId={selectedProjects.quickActions}
                  onProjectChange={(projectId) => handleCardProjectChange('quickActions', projectId)}
                >
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {quickActions.map((action) => {
                      const disabled = !action.global && !selectedQuickActionsProject;
                      return (
                        <button
                          key={action.id}
                          onClick={() => runQuickAction(action.id)}
                          disabled={disabled}
                          className="rounded-xl border border-gray-800 bg-gray-950/50 px-4 py-4 text-left hover:border-indigo-500 transition disabled:opacity-45 disabled:hover:border-gray-800"
                        >
                          <p className="text-white font-medium">{action.label}</p>
                          <p className="text-gray-500 text-sm mt-1">
                            {action.global
                              ? 'Works globally'
                              : `Uses ${selectedQuickActionsProject?.name || 'the selected project'} as context`}
                          </p>
                        </button>
                      );
                    })}
                  </div>
                </DashboardCard>
              );
            }

            if (cardId === 'roles') {
              const roles = rolesByProject[selectedProjects.roles] || [];
              return (
                <DashboardCard
                  key={cardId}
                  icon={cardIcons.roles}
                  title="Roles"
                  description="Role overview for the selected project."
                  projectOptions={cardProjects.roles}
                  selectedProjectId={selectedProjects.roles}
                  onProjectChange={(projectId) => handleCardProjectChange('roles', projectId)}
                  action={
                    selectedRolesProject ? (
                      <button
                        onClick={() => navigate(`/projects/${selectedRolesProject.id}?tab=roles`)}
                        className="text-sm text-indigo-300 hover:text-indigo-200 transition"
                      >
                        Manage roles
                      </button>
                    ) : null
                  }
                >
                  {roles.length === 0 ? (
                    <p className="text-gray-400">No roles available for this project yet.</p>
                  ) : (
                    <div className="space-y-3">
                      {roles.slice(0, 5).map((role) => (
                        <div key={role.id} className="rounded-xl border border-gray-800 bg-gray-950/50 px-4 py-4">
                          <div className="flex items-center justify-between gap-3">
                            <p className="text-white font-medium">{role.name}</p>
                            <span className="text-xs text-gray-500">
                              {[role.canGrantView, role.canGrantCreate, role.canGrantEdit, role.canGrantDelete, role.canGrantDelegate].filter(Boolean).length} grants
                            </span>
                          </div>
                          <div className="flex gap-2 flex-wrap mt-3">
                            {role.canGrantView ? <span className="text-xs bg-blue-500/20 text-blue-400 px-2 py-1 rounded">Grant View</span> : null}
                            {role.canGrantCreate ? <span className="text-xs bg-green-500/20 text-green-400 px-2 py-1 rounded">Grant Create</span> : null}
                            {role.canGrantEdit ? <span className="text-xs bg-yellow-500/20 text-yellow-400 px-2 py-1 rounded">Grant Edit</span> : null}
                            {role.canGrantDelete ? <span className="text-xs bg-red-500/20 text-red-400 px-2 py-1 rounded">Grant Delete</span> : null}
                            {role.canGrantDelegate ? <span className="text-xs bg-purple-500/20 text-purple-400 px-2 py-1 rounded">Grant Delegate</span> : null}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </DashboardCard>
              );
            }

            if (cardId === 'members') {
              const members = membersByProject[selectedProjects.members] || [];
              const activeMembers = members.filter((member) => !member.isPending);
              const pendingMembers = members.filter((member) => member.isPending);

              return (
                <DashboardCard
                  key={cardId}
                  icon={cardIcons.members}
                  title="Members"
                  description="Member snapshot for the selected project."
                  projectOptions={cardProjects.members}
                  selectedProjectId={selectedProjects.members}
                  onProjectChange={(projectId) => handleCardProjectChange('members', projectId)}
                  action={
                    selectedMembersProject ? (
                      <button
                        onClick={() => navigate(`/projects/${selectedMembersProject.id}?tab=members`)}
                        className="text-sm text-indigo-300 hover:text-indigo-200 transition"
                      >
                        Manage members
                      </button>
                    ) : null
                  }
                >
                  {members.length === 0 ? (
                    <p className="text-gray-400">No members visible for this project.</p>
                  ) : (
                    <div className="space-y-4">
                      <div className="grid grid-cols-3 gap-3">
                        <div className="rounded-xl bg-gray-950/50 border border-gray-800 px-4 py-4">
                          <p className="text-xs text-gray-500">Active</p>
                          <p className="text-xl font-semibold text-white mt-1">{activeMembers.length}</p>
                        </div>
                        <div className="rounded-xl bg-gray-950/50 border border-gray-800 px-4 py-4">
                          <p className="text-xs text-gray-500">Pending</p>
                          <p className="text-xl font-semibold text-white mt-1">{pendingMembers.length}</p>
                        </div>
                        <div className="rounded-xl bg-gray-950/50 border border-gray-800 px-4 py-4">
                          <p className="text-xs text-gray-500">Owners</p>
                          <p className="text-xl font-semibold text-white mt-1">{activeMembers.filter((member) => member.isOwner).length}</p>
                        </div>
                      </div>
                      <div className="space-y-3">
                        {members.slice(0, 5).map((member) => (
                          <div key={member.id} className="rounded-xl border border-gray-800 bg-gray-950/50 px-4 py-4 flex items-center justify-between gap-4">
                            <div>
                              <p className="text-white font-medium">{member.name}</p>
                              <p className="text-gray-500 text-sm mt-1">{member.email}</p>
                            </div>
                            <div className="flex gap-2 flex-wrap justify-end">
                              <span className="text-xs bg-purple-500/20 text-purple-400 px-2 py-1 rounded">{member.role}</span>
                              {member.isPending ? <span className="text-xs bg-yellow-500/20 text-yellow-400 px-2 py-1 rounded">Pending</span> : null}
                              {member.isOwner ? <span className="text-xs bg-amber-500/20 text-amber-400 px-2 py-1 rounded">Owner</span> : null}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </DashboardCard>
              );
            }

            return null;
          })}
        </div>
      )}
    </Layout>
  );
}
