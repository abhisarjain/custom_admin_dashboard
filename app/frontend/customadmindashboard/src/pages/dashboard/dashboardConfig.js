export const DASHBOARD_CARD_ORDER = ['recentActivity', 'projects', 'tables', 'quickActions', 'roles', 'members'];

export const DASHBOARD_CARD_META = {
  recentActivity: {
    id: 'recentActivity',
    title: 'Recent Activity',
    description: 'Show the latest activity for the selected project.',
    maxItems: null,
  },
  projects: {
    id: 'projects',
    title: 'Projects',
    description: 'Choose which projects should appear as project summary cards.',
    maxItems: 3,
  },
  tables: {
    id: 'tables',
    title: 'Tables',
    description: 'Pick up to 3 tables you want quick access to on the dashboard.',
    maxItems: 3,
  },
  quickActions: {
    id: 'quickActions',
    title: 'Quick Actions',
    description: 'Choose the shortcuts you want on the dashboard.',
    maxItems: null,
  },
  roles: {
    id: 'roles',
    title: 'Roles',
    description: 'Choose which projects should show role summaries.',
    maxItems: 3,
  },
  members: {
    id: 'members',
    title: 'Members',
    description: 'Choose which projects should show member summaries.',
    maxItems: 3,
  },
};

export const QUICK_ACTION_OPTIONS = [
  { id: 'openProjectList', label: 'Open Projects', global: true },
  { id: 'openInvitations', label: 'Open Invitations', global: true },
  { id: 'openAuditLogs', label: 'Open Audit Logs', global: true },
  { id: 'createProject', label: 'Create Project', global: true },
  { id: 'openTables', label: 'Open Tables', global: false },
  { id: 'openRoles', label: 'Open Roles', global: false },
  { id: 'openMembers', label: 'Open Members', global: false },
  { id: 'openConnections', label: 'Open Connections', global: false },
];

export function createEmptyDashboardSettings() {
  return {
    showRecentActivity: true,
    cardOrder: [...DASHBOARD_CARD_ORDER],
    projectCardProjectIds: [],
    tableSelections: [],
    quickActionIds: ['openProjectList', 'openInvitations', 'openAuditLogs'],
    roleProjectIds: [],
    memberProjectIds: [],
  };
}

export function reorderCardIds(cardOrder, cardId, direction) {
  const next = [...cardOrder];
  const index = next.indexOf(cardId);
  if (index === -1) return next;

  const targetIndex = direction === 'up' ? index - 1 : index + 1;
  if (targetIndex < 0 || targetIndex >= next.length) return next;

  [next[index], next[targetIndex]] = [next[targetIndex], next[index]];
  return next;
}
