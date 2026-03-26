import { useEffect, useMemo, useState } from 'react';
import { ArrowLeft, ChevronDown, ChevronUp, History, Save, Settings2, Table, Users, Zap, Shield, FolderKanban } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import Layout from '../../components/layout/Layout';
import api from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { createEmptyDashboardSettings, DASHBOARD_CARD_META, QUICK_ACTION_OPTIONS, reorderCardIds } from './dashboardConfig';
import { fetchDashboardCatalog, hydrateDashboardSettings, resolveDashboardSettings, writeLocalDashboardSettings } from './dashboardData';

const SectionShell = ({ icon: Icon, title, description, children }) => (
  <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6">
    <div className="flex items-start gap-3 mb-5">
      <div className="w-10 h-10 rounded-xl bg-indigo-500/15 flex items-center justify-center shrink-0">
        <Icon size={18} className="text-indigo-300" />
      </div>
      <div>
        <h2 className="text-white font-semibold">{title}</h2>
        <p className="text-gray-400 text-sm mt-1">{description}</p>
      </div>
    </div>
    {children}
  </div>
);

function projectOptionLabel(project) {
  if (!project) return '';
  const roleLabel = project.permissions?.isOwner ? 'Owner' : project.permissions?.role;
  return `${project.name}${roleLabel ? ` · ${roleLabel}` : ''}`;
}

export default function DashboardSettings() {
  const navigate = useNavigate();
  const { tenant } = useAuth();
  const [catalog, setCatalog] = useState(null);
  const [settings, setSettings] = useState(createEmptyDashboardSettings());
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadPage();
  }, []);

  const loadPage = async () => {
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
        totals: { projects: 0, invitations: 0, connections: 0, tables: 0 },
      });
      setSettings(createEmptyDashboardSettings());
      toast.error('Failed to load dashboard settings');
    }
    setLoading(false);
  };

  const projectOptions = catalog?.projects || [];
  const roleProjectOptions = projectOptions.filter((project) => project.roleViewAccess);
  const memberProjectOptions = projectOptions.filter((project) => project.memberViewAccess);

  const groupedTableOptions = useMemo(
    () => (catalog?.projects || []).filter((project) => project.accessibleTables.length > 0),
    [catalog]
  );

  const handleMultiSelectToggle = (field, value, maxItems = null) => {
    setSettings((prev) => {
      const current = prev[field] || [];
      const exists = current.includes(value);
      if (exists) {
        return { ...prev, [field]: current.filter((item) => item !== value) };
      }
      if (maxItems && current.length >= maxItems) {
        toast.error(`You can select only ${maxItems} items here`);
        return prev;
      }
      return { ...prev, [field]: [...current, value] };
    });
  };

  const handleTableToggle = (projectId, tableName) => {
    setSettings((prev) => {
      const key = `${projectId}::${tableName}`;
      const current = prev.tableSelections || [];
      const exists = current.some((item) => `${item.projectId}::${item.tableName}` === key);
      if (exists) {
        return {
          ...prev,
          tableSelections: current.filter((item) => `${item.projectId}::${item.tableName}` !== key),
        };
      }
      if (current.length >= DASHBOARD_CARD_META.tables.maxItems) {
        toast.error(`You can select only ${DASHBOARD_CARD_META.tables.maxItems} tables`);
        return prev;
      }
      return {
        ...prev,
        tableSelections: [...current, { projectId, tableName }],
      };
    });
  };

  const handleOrderMove = (cardId, direction) => {
    setSettings((prev) => ({
      ...prev,
      cardOrder: reorderCardIds(prev.cardOrder, cardId, direction),
    }));
  };

  const handleQuickActionToggle = (actionId) => {
    handleMultiSelectToggle('quickActionIds', actionId);
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      writeLocalDashboardSettings(tenant?.tenantId, settings);
      const response = await api.put('/api/dashboard/settings', settings);
      const nextSettings = hydrateDashboardSettings(response.data?.data, catalog);
      setSettings(nextSettings);
      writeLocalDashboardSettings(tenant?.tenantId, nextSettings);
      toast.success('Dashboard settings saved');
    } catch (err) {
      writeLocalDashboardSettings(tenant?.tenantId, settings);
      toast.error(err.response?.data?.message || 'Saved locally, but backend dashboard settings could not be updated');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <Layout>
        <div className="text-gray-400 text-center py-16">Loading dashboard settings...</div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="flex items-start justify-between gap-4 mb-8">
        <div className="flex items-start gap-4">
          <button
            onClick={() => navigate('/dashboard')}
            className="text-gray-400 hover:text-white transition mt-1"
          >
            <ArrowLeft size={20} />
          </button>
          <div>
            <h1 className="text-2xl font-bold text-white">Dashboard Settings</h1>
            <p className="text-gray-400 mt-1">
              Pick which cards you want, choose the content they should show, and arrange the order.
            </p>
          </div>
        </div>
        <button
          onClick={handleSave}
          disabled={saving}
          className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white px-5 py-3 rounded-xl transition"
        >
          <Save size={16} />
          {saving ? 'Saving...' : 'Save Settings'}
        </button>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-[1.1fr,0.9fr] gap-6">
        <div className="space-y-6">
          <SectionShell
            icon={History}
            title="Recent Activity"
            description="This card is always available on the dashboard, and you can now place it anywhere in the card order."
          >
            <label className="flex items-center gap-3 rounded-xl border border-gray-800 bg-gray-950/50 px-4 py-4 cursor-pointer">
              <input
                type="checkbox"
                className="w-4 h-4 accent-indigo-500"
                checked={settings.showRecentActivity !== false}
                onChange={() => setSettings((prev) => ({ ...prev, showRecentActivity: prev.showRecentActivity === false }))}
              />
              <div>
                <p className="text-sm text-white font-medium">Display Recent Activity</p>
                <p className="text-xs text-gray-400 mt-1">
                  Turn this card on or off, and use Card Order below to place it anywhere on the dashboard.
                </p>
              </div>
            </label>
          </SectionShell>

          <SectionShell
            icon={FolderKanban}
            title="Projects Card"
            description="Choose up to 3 projects that should be available on the Projects card."
          >
            <div className="space-y-3">
              {projectOptions.length === 0 ? (
                <p className="text-gray-500 text-sm">No accessible projects found.</p>
              ) : (
                projectOptions.map((project) => (
                  <label key={project.id} className="flex items-center gap-3 rounded-xl bg-gray-800/70 px-4 py-3 cursor-pointer">
                    <input
                      type="checkbox"
                      className="w-4 h-4 accent-indigo-500"
                      checked={settings.projectCardProjectIds.includes(project.id)}
                      onChange={() => handleMultiSelectToggle('projectCardProjectIds', project.id, DASHBOARD_CARD_META.projects.maxItems)}
                    />
                    <span className="text-sm text-gray-200">{projectOptionLabel(project)}</span>
                  </label>
                ))
              )}
            </div>
          </SectionShell>

          <SectionShell
            icon={Table}
            title="Tables Card"
            description="Choose up to 3 tables from the projects where you have data view access."
          >
            <div className="space-y-5">
              {groupedTableOptions.length === 0 ? (
                <p className="text-gray-500 text-sm">No tables with data access are available yet.</p>
              ) : (
                groupedTableOptions.map((project) => (
                  <div key={project.id} className="rounded-xl border border-gray-800 overflow-hidden">
                    <div className="px-4 py-3 bg-gray-950/50 border-b border-gray-800">
                      <p className="text-white text-sm font-medium">{project.name}</p>
                    </div>
                    <div className="p-4 space-y-3">
                      {project.accessibleTables.map((table) => {
                        const checked = settings.tableSelections.some(
                          (item) => Number(item.projectId) === Number(project.id) && item.tableName === table.tableName
                        );
                        return (
                          <label key={`${project.id}-${table.tableName}`} className="flex items-center gap-3 rounded-lg bg-gray-800/70 px-4 py-3 cursor-pointer">
                            <input
                              type="checkbox"
                              className="w-4 h-4 accent-indigo-500"
                              checked={checked}
                              onChange={() => handleTableToggle(project.id, table.tableName)}
                            />
                            <div>
                              <p className="text-sm text-gray-200">{table.tableName}</p>
                              <p className="text-xs text-gray-500">{table.columnsCount} columns</p>
                            </div>
                          </label>
                        );
                      })}
                    </div>
                  </div>
                ))
              )}
            </div>
          </SectionShell>
        </div>

        <div className="space-y-6">
          <SectionShell
            icon={Zap}
            title="Quick Actions"
            description="Choose the shortcuts that should appear in the Quick Actions card."
          >
            <div className="space-y-3">
              {QUICK_ACTION_OPTIONS.map((action) => (
                <label key={action.id} className="flex items-center gap-3 rounded-xl bg-gray-800/70 px-4 py-3 cursor-pointer">
                  <input
                    type="checkbox"
                    className="w-4 h-4 accent-indigo-500"
                    checked={settings.quickActionIds.includes(action.id)}
                    onChange={() => handleQuickActionToggle(action.id)}
                  />
                  <span className="text-sm text-gray-200">{action.label}</span>
                </label>
              ))}
            </div>
          </SectionShell>

          <SectionShell
            icon={Shield}
            title="Roles Card"
            description="Choose up to 3 projects where role summaries should be shown."
          >
            <div className="space-y-3">
              {roleProjectOptions.length === 0 ? (
                <p className="text-gray-500 text-sm">No projects with role view access.</p>
              ) : (
                roleProjectOptions.map((project) => (
                  <label key={project.id} className="flex items-center gap-3 rounded-xl bg-gray-800/70 px-4 py-3 cursor-pointer">
                    <input
                      type="checkbox"
                      className="w-4 h-4 accent-indigo-500"
                      checked={settings.roleProjectIds.includes(project.id)}
                      onChange={() => handleMultiSelectToggle('roleProjectIds', project.id, DASHBOARD_CARD_META.roles.maxItems)}
                    />
                    <span className="text-sm text-gray-200">{projectOptionLabel(project)}</span>
                  </label>
                ))
              )}
            </div>
          </SectionShell>

          <SectionShell
            icon={Users}
            title="Members Card"
            description="Choose up to 3 projects where member summaries should be shown."
          >
            <div className="space-y-3">
              {memberProjectOptions.length === 0 ? (
                <p className="text-gray-500 text-sm">No projects with member view access.</p>
              ) : (
                memberProjectOptions.map((project) => (
                  <label key={project.id} className="flex items-center gap-3 rounded-xl bg-gray-800/70 px-4 py-3 cursor-pointer">
                    <input
                      type="checkbox"
                      className="w-4 h-4 accent-indigo-500"
                      checked={settings.memberProjectIds.includes(project.id)}
                      onChange={() => handleMultiSelectToggle('memberProjectIds', project.id, DASHBOARD_CARD_META.members.maxItems)}
                    />
                    <span className="text-sm text-gray-200">{projectOptionLabel(project)}</span>
                  </label>
                ))
              )}
            </div>
          </SectionShell>

          <SectionShell
            icon={Settings2}
            title="Card Order"
            description="Move cards up or down to control the dashboard order."
          >
            <div className="space-y-3">
              {settings.cardOrder.map((cardId, index) => (
                <div key={cardId} className="flex items-center justify-between rounded-xl bg-gray-800/70 px-4 py-3">
                  <div>
                    <p className="text-sm text-white font-medium">{DASHBOARD_CARD_META[cardId].title}</p>
                    <p className="text-xs text-gray-500 mt-1">{DASHBOARD_CARD_META[cardId].description}</p>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <button
                      type="button"
                      onClick={() => handleOrderMove(cardId, 'up')}
                      disabled={index === 0}
                      className="rounded-lg border border-gray-700 p-2 text-gray-300 hover:bg-gray-700 disabled:opacity-40"
                    >
                      <ChevronUp size={16} />
                    </button>
                    <button
                      type="button"
                      onClick={() => handleOrderMove(cardId, 'down')}
                      disabled={index === settings.cardOrder.length - 1}
                      className="rounded-lg border border-gray-700 p-2 text-gray-300 hover:bg-gray-700 disabled:opacity-40"
                    >
                      <ChevronDown size={16} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </SectionShell>
        </div>
      </div>
    </Layout>
  );
}
