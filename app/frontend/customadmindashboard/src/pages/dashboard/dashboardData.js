import api from '../../services/api';
import { authService } from '../../services/authService';
import { createEmptyDashboardSettings, DASHBOARD_CARD_ORDER, QUICK_ACTION_OPTIONS } from './dashboardConfig';

const DASHBOARD_SETTINGS_STORAGE_PREFIX = 'dashboard_settings_v1';

function uniqueBy(list, getKey) {
  const seen = new Set();
  return list.filter((item) => {
    const key = getKey(item);
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function getTableAccess(permissions, tableName) {
  if (permissions?.isOwner || permissions?.role === 'Super Admin') {
    return {
      canViewData: true,
      canViewStructure: true,
      canCreateData: true,
      canEditData: true,
      canDeleteData: true,
      canDeleteTable: true,
    };
  }

  const tablePerm = permissions?.tablePermissions?.[tableName] || {};
  return {
    canViewData: !!tablePerm.canViewData,
    canViewStructure: !!tablePerm.canViewStructure,
    canCreateData: !!tablePerm.canCreateData,
    canEditData: !!tablePerm.canEditData,
    canDeleteData: !!tablePerm.canDeleteData,
    canDeleteTable: !!tablePerm.canDeleteTable,
  };
}

async function fetchProjectCapability(project) {
  const projectId = project.id;

  const [permissionsRes, connectionsRes, viewsRes] = await Promise.all([
    api.get(`/api/projects/${projectId}/my-permissions`),
    api.get(`/api/projects/${projectId}/connections`).catch(() => ({ data: { data: [] } })),
    api.get(`/api/projects/${projectId}/dashboard/views`).catch(() => ({ data: { data: [] } })),
  ]);

  const permissions = permissionsRes.data?.data || {};
  const connections = connectionsRes.data?.data || [];
  const dashboardViews = viewsRes.data?.data || [];
  const viewMap = dashboardViews.reduce((acc, view) => {
    acc[view.tableName] = view.id;
    return acc;
  }, {});

  let schema = [];
  if (connections[0]?.id) {
    try {
      const schemaRes = await api.get(`/api/projects/${projectId}/connections/${connections[0].id}/schema`);
      schema = schemaRes.data?.data || [];
    } catch {
      schema = [];
    }
  }

  let roleViewAccess = !!permissions?.isOwner;
  let memberViewAccess = !!permissions?.isOwner;
  let auditAccess = !!permissions?.isOwner;

  if (!permissions?.isOwner && permissions?.roleId) {
    const [rolePermRes, memberPermRes] = await Promise.all([
      api.get(`/api/projects/${projectId}/roles/${permissions.roleId}/permissions`).catch(() => ({ data: { data: {} } })),
      api.get(`/api/projects/${projectId}/roles/${permissions.roleId}/member-permissions`).catch(() => ({ data: { data: {} } })),
    ]);

    const rolePerms = rolePermRes.data?.data || {};
    const memberPerms = memberPermRes.data?.data || {};

    roleViewAccess = !!rolePerms.canView;
    memberViewAccess = !!memberPerms.canView;
    auditAccess = !!rolePerms.canViewAuditLogs;
  }

  const accessibleTables = schema
    .filter((table) => getTableAccess(permissions, table.tableName).canViewData)
    .map((table) => ({
      projectId,
      projectName: project.name,
      tableName: table.tableName,
      columnsCount: table.columns?.length || 0,
      viewId: viewMap[table.tableName] || null,
    }));

  return {
    ...project,
    permissions,
    connections,
    connectionCount: connections.length,
    schema,
    totalTablesCount: schema.length,
    accessibleTables,
    roleViewAccess,
    memberViewAccess,
    auditAccess,
  };
}

export async function fetchDashboardCatalog() {
  const [projectsResult, invitationsResult] = await Promise.allSettled([
    api.get('/api/projects'),
    authService.getMyInvitations(),
  ]);

  if (projectsResult.status !== 'fulfilled') {
    throw projectsResult.reason;
  }

  const baseProjects = projectsResult.value.data?.data || [];
  const invitationsList = invitationsResult.status === 'fulfilled'
    ? (invitationsResult.value.data || [])
    : [];

  const projectCapabilities = await Promise.allSettled(baseProjects.map(fetchProjectCapability));

  const projects = projectCapabilities
    .filter((result) => result.status === 'fulfilled')
    .map((result) => result.value)
    .sort((a, b) => a.name.localeCompare(b.name));

  return {
    projects,
    invitations: invitationsList,
    totals: {
      projects: projects.length,
      invitations: invitationsList.length,
      connections: projects.reduce((sum, project) => sum + project.connectionCount, 0),
      tables: projects.reduce((sum, project) => sum + project.accessibleTables.length, 0),
    },
  };
}

export function hydrateDashboardSettings(rawSettings, catalog) {
  const settings = {
    ...createEmptyDashboardSettings(),
    ...(rawSettings || {}),
  };

  const projectIds = new Set(catalog.projects.map((project) => Number(project.id)));
  const tableKeys = new Set(
    catalog.projects.flatMap((project) =>
      project.accessibleTables.map((table) => `${project.id}::${table.tableName}`)
    )
  );
  const roleProjectIds = new Set(
    catalog.projects.filter((project) => project.roleViewAccess).map((project) => Number(project.id))
  );
  const memberProjectIds = new Set(
    catalog.projects.filter((project) => project.memberViewAccess).map((project) => Number(project.id))
  );
  const quickActionIds = new Set(QUICK_ACTION_OPTIONS.map((action) => action.id));

  const cardOrder = uniqueBy(
    [...(settings.cardOrder || []).filter((cardId) => DASHBOARD_CARD_ORDER.includes(cardId)), ...DASHBOARD_CARD_ORDER],
    (cardId) => cardId
  );

  return {
    showRecentActivity: settings.showRecentActivity !== false,
    cardOrder,
    projectCardProjectIds: uniqueBy(
      (settings.projectCardProjectIds || []).filter((id) => projectIds.has(Number(id))).slice(0, 3),
      (id) => Number(id)
    ),
    tableSelections: uniqueBy(
      (settings.tableSelections || [])
        .filter((item) => item?.projectId && item?.tableName)
        .filter((item) => tableKeys.has(`${Number(item.projectId)}::${item.tableName}`))
        .slice(0, 3),
      (item) => `${Number(item.projectId)}::${item.tableName}`
    ),
    quickActionIds: uniqueBy(
      (settings.quickActionIds || []).filter((id) => quickActionIds.has(id)),
      (id) => id
    ),
    roleProjectIds: uniqueBy(
      (settings.roleProjectIds || []).filter((id) => roleProjectIds.has(Number(id))).slice(0, 3),
      (id) => Number(id)
    ),
    memberProjectIds: uniqueBy(
      (settings.memberProjectIds || []).filter((id) => memberProjectIds.has(Number(id))).slice(0, 3),
      (id) => Number(id)
    ),
  };
}

export function resolveDashboardSettings(remoteSettings, tenantId, catalog) {
  const localSettings = readLocalDashboardSettings(tenantId);
  const preferredSettings = localSettings || remoteSettings || null;
  const hydrated = hydrateDashboardSettings(preferredSettings, catalog);
  writeLocalDashboardSettings(tenantId, hydrated);
  return hydrated;
}

export function buildSelectedProjectState(settings, catalog) {
  const firstProjectId = catalog.projects[0]?.id || '';
  const tableProjectIds = uniqueBy((settings.tableSelections || []).map((item) => Number(item.projectId)), (id) => id);

  return {
    recentActivity: catalog.projects.find((project) => project.auditAccess)?.id || firstProjectId,
    projects: settings.projectCardProjectIds[0] || firstProjectId,
    tables: tableProjectIds[0] || firstProjectId,
    quickActions: settings.projectCardProjectIds[0] || firstProjectId,
    roles: settings.roleProjectIds[0] || firstProjectId,
    members: settings.memberProjectIds[0] || firstProjectId,
  };
}

export function getDashboardSettingsStorageKey(tenantId) {
  return `${DASHBOARD_SETTINGS_STORAGE_PREFIX}:${tenantId || 'guest'}`;
}

export function readLocalDashboardSettings(tenantId) {
  try {
    const raw = localStorage.getItem(getDashboardSettingsStorageKey(tenantId));
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function writeLocalDashboardSettings(tenantId, settings) {
  try {
    localStorage.setItem(getDashboardSettingsStorageKey(tenantId), JSON.stringify(settings));
  } catch {
    // Ignore storage failures and keep dashboard usable.
  }
}
