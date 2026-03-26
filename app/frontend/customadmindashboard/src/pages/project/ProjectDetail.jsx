import { useState, useEffect } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import Layout from '../../components/layout/Layout';
import { toast } from 'react-hot-toast';
import {
  Database, Shield, Users, ArrowLeft,
  Plus, RefreshCw, Eye, Table,LayoutDashboard, ChevronRight, Columns, Trash2, Pencil
} from 'lucide-react';
import api from '../../services/api';

export default function ProjectDetail() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [project, setProject] = useState(null);
  const [connections, setConnections] = useState([]);
  const [roles, setRoles] = useState([]);
  const [members, setMembers] = useState([]);
  const [schema, setSchema] = useState([]);
  const [activeTab, setActiveTab] = useState(searchParams.get('tab') || 'connections');
  const [loading, setLoading] = useState(true);
  const [myPermissions, setMyPermissions] = useState(null);
  const [roleAccess, setRoleAccess] = useState({
    canView: false,
    canCreate: false,
    canEdit: false,
    canDelete: false,
  });
  const [memberAccess, setMemberAccess] = useState({
    canInvite: false,
    canView: false,
    canEdit: false,
    canRemove: false,
    grantInvite: false,
    grantView: false,
    grantEdit: false,
    grantRemove: false,
    grantDelegate: false,
  });

  // Modals
  const [showConnModal, setShowConnModal] = useState(false);
  const [showRoleModal, setShowRoleModal] = useState(false);
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [showMemberRoleModal, setShowMemberRoleModal] = useState(false);
  const [showCreateTableModal, setShowCreateTableModal] = useState(false);
  const [editingRole, setEditingRole] = useState(null);
  const [editingMember, setEditingMember] = useState(null);

  const [connForm, setConnForm] = useState({
    dbType: 'postgres', host: '', port: 5432,
    databaseName: '', username: '', password: '', sslEnabled: false
  });

  const [roleForm, setRoleForm] = useState({
    name: '', canGrantView: false, canGrantCreate: false,
    canGrantEdit: false, canGrantDelete: false, canGrantDelegate: false
  });

  const [inviteForm, setInviteForm] = useState({ email: '', roleId: '' });
  const [memberRoleForm, setMemberRoleForm] = useState({ roleId: '' });
  const [newTableForm, setNewTableForm] = useState({ tableName: '' });

  const [dashboardViews, setDashboardViews] = useState([]);
const [showDashboardModal, setShowDashboardModal] = useState(false);
const [dashboardForm, setDashboardForm] = useState({
    tableName: '', isCreatable: true, isDeletable: false
});

  useEffect(() => {
    fetchAll();
  }, [projectId]);

  const setTab = (tabId) => {
    setActiveTab(tabId);
    setSearchParams({ tab: tabId });
  };

const fetchAll = async () => {
    try {
        const [projRes, connRes, rolesRes, dashboardRes, myPermsRes] = await Promise.all([
            api.get(`/api/projects/${projectId}`),
            api.get(`/api/projects/${projectId}/connections`),
            api.get(`/api/projects/${projectId}/roles`),
            api.get(`/api/projects/${projectId}/dashboard/views`),
            api.get(`/api/projects/${projectId}/my-permissions`),
        ]);
        setProject(projRes.data.data);
        setConnections(connRes.data.data);
        setRoles(rolesRes.data.data);
        setDashboardViews(dashboardRes.data.data);
        setMyPermissions(myPermsRes.data.data);

        const myRoleId = myPermsRes.data.data?.roleId;
        if (myRoleId) {
            const [myRolePermsRes, myMemberPermsRes] = await Promise.all([
                api.get(`/api/projects/${projectId}/roles/${myRoleId}/permissions`),
                api.get(`/api/projects/${projectId}/roles/${myRoleId}/member-permissions`),
            ]);

            const rolePerms = myRolePermsRes.data.data || {};
            setRoleAccess({
                canView: rolePerms.canView || false,
                canCreate: rolePerms.canCreate || false,
                canEdit: rolePerms.canEdit || false,
                canDelete: rolePerms.canDelete || false,
            });

            const memberPerms = myMemberPermsRes.data.data || {};
            setMemberAccess({
                canInvite: memberPerms.canInvite || false,
                canView: memberPerms.canView || false,
                canEdit: memberPerms.canEdit || false,
                canRemove: memberPerms.canRemove || false,
                grantInvite: memberPerms.grantInvite || false,
                grantView: memberPerms.grantView || false,
                grantEdit: memberPerms.grantEdit || false,
                grantRemove: memberPerms.grantRemove || false,
                grantDelegate: memberPerms.grantDelegate || false,
            });
        }

        // Members alag fetch karo
        try {
            const membersRes = await api.get(`/api/projects/${projectId}/members`);
            setMembers(membersRes.data.data);
        } catch (e) {
            console.log('Members fetch failed');
        }

        // Schema fetch karo
        if (connRes.data.data.length > 0) {
            const connId = connRes.data.data[0].id;
            const schemaRes = await api.get(`/api/projects/${projectId}/connections/${connId}/schema`);
            setSchema(schemaRes.data.data);
        }
    } catch (err) {
        toast.error('Failed to fetch project details');
    } finally {
        setLoading(false);
    }
};

  useEffect(() => {
    if (loading) return;

    const requestedTab = searchParams.get('tab') || 'connections';

    if (requestedTab === 'roles' && !roleAccess.canView) {
      setTab('connections');
      return;
    }

    if (requestedTab === 'members' && !memberAccess.canView) {
      setTab(roleAccess.canView ? 'roles' : 'connections');
      return;
    }

    if (activeTab !== requestedTab) {
      setActiveTab(requestedTab);
    }
  }, [loading, searchParams, roleAccess.canView, memberAccess.canView, activeTab]);

  const canOpenRoleDetail = roleAccess.canView;
  const canInviteMembers = memberAccess.canInvite;
  const canRemoveMembers = memberAccess.canRemove || myPermissions?.role === 'Super Admin';
  const getTableAccess = (tableName) => {
    if (myPermissions?.role === 'Super Admin' || myPermissions?.isOwner) {
      return {
        canViewData: true,
        canViewStructure: true,
        canCreate: true,
        canCreateData: true,
        canCreateStructure: true,
        canEditData: true,
        canEditStructure: true,
        canDeleteData: true,
        canDeleteStructure: true,
        canDeleteTable: true,
      };
    }
    const perm = myPermissions?.tablePermissions?.[tableName] || {};
    return {
      canViewData: perm.canViewData || false,
      canViewStructure: perm.canViewStructure || false,
      canCreate: perm.canCreate || false,
      canCreateData: perm.canCreateData || false,
      canCreateStructure: perm.canCreateStructure || false,
      canEditData: perm.canEditData || false,
      canEditStructure: perm.canEditStructure || false,
      canDeleteData: perm.canDeleteData || false,
      canDeleteStructure: perm.canDeleteStructure || false,
      canDeleteTable: perm.canDeleteTable || false,
    };
  };
  const canCreateNewTable = !!myPermissions?.tablePermissions?.['__new_table__']?.canCreate || myPermissions?.role === 'Super Admin' || myPermissions?.isOwner;
  const hasAnyTableAccess = schema.some((table) => {
    const access = getTableAccess(table.tableName);
    return Object.values(access).some(Boolean);
  }) || canCreateNewTable;

  const handleCreateConnection = async (e) => {
    e.preventDefault();
    try {
      await api.post(`/api/projects/${projectId}/connections`, connForm);
      toast.success('Database connected!');
      setShowConnModal(false);
      fetchAll();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Connection failed');
    }
  };

  const handleSync = async (connectionId) => {
    try {
      const res = await api.post(`/api/projects/${projectId}/connections/${connectionId}/sync`);
      setSchema(res.data.data);
      setTab('schema');
      toast.success('Schema synced!');
    } catch (err) {
      toast.error('Sync failed');
    }
  };

  const handleCreateRole = async (e) => {
    e.preventDefault();
    if (!roleAccess.canCreate) {
      toast.error('You do not have permission to create roles');
      return;
    }
    try {
      await api.post(`/api/projects/${projectId}/roles`, roleForm);
      toast.success('Role created!');
      setShowRoleModal(false);
      fetchAll();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create role');
    }
  };

  const handleUpdateRole = async (e) => {
    e.preventDefault();
    if (!roleAccess.canEdit) {
      toast.error('You do not have permission to edit roles');
      return;
    }

    try {
      await api.put(`/api/projects/${projectId}/roles/${editingRole.id}`, roleForm);
      toast.success('Role updated!');
      setShowRoleModal(false);
      setEditingRole(null);
      setRoleForm({
        name: '', canGrantView: false, canGrantCreate: false,
        canGrantEdit: false, canGrantDelete: false, canGrantDelegate: false
      });
      fetchAll();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update role');
    }
  };

  const handleInvite = async (e) => {
    e.preventDefault();
    if (!memberAccess.canInvite) {
      toast.error('You do not have permission to invite members');
      return;
    }
    try {
      await api.post(`/api/projects/${projectId}/invite`, inviteForm);
      toast.success('Invitation sent!');
      setShowInviteModal(false);
      setInviteForm({ email: '', roleId: '' });
      fetchAll();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to send invitation');
    }
  };

  const handleCancelInvitation = async (member) => {
    if (!confirm(`Cancel invitation for ${member.email}?`)) return;

    try {
      await api.delete(`/api/projects/${projectId}/invitations/${member.invitationId}`);
      toast.success('Invitation cancelled!');
      fetchAll();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to cancel invitation');
    }
  };

  const handleRemoveMember = async (member) => {
    if (!canRemoveMembers) {
      toast.error('You do not have permission to remove members');
      return;
    }

    if (member.isOwner) {
      toast.error('Project owner cannot be removed');
      return;
    }

    if (!confirm(`Remove ${member.name} from this project?`)) return;

    try {
      await api.delete(`/api/projects/${projectId}/members/${member.id}`);
      toast.success('Member removed!');
      fetchAll();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to remove member');
    }
  };

  const handleUpdateMemberRole = async (e) => {
    e.preventDefault();
    if (!memberAccess.canEdit) {
      toast.error('You do not have permission to edit members');
      return;
    }

    try {
      await api.put(`/api/projects/${projectId}/members/${editingMember.id}`, {
        roleId: Number(memberRoleForm.roleId),
      });
      toast.success('Member role updated!');
      setShowMemberRoleModal(false);
      setEditingMember(null);
      setMemberRoleForm({ roleId: '' });
      fetchAll();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update member role');
    }
  };

  const handleDeleteRole = async (e, role) => {
    e.stopPropagation();
    if (!roleAccess.canDelete) {
      toast.error('You do not have permission to delete roles');
      return;
    }

    const confirmed = window.confirm(`Delete role "${role.name}"?`);
    if (!confirmed) return;

    try {
      await api.delete(`/api/projects/${projectId}/roles/${role.id}`);
      toast.success('Role deleted!');
      fetchAll();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to delete role');
    }
  };

  const openCreateRoleModal = () => {
    setEditingRole(null);
    setRoleForm({
      name: '', canGrantView: false, canGrantCreate: false,
      canGrantEdit: false, canGrantDelete: false, canGrantDelegate: false
    });
    setShowRoleModal(true);
  };

  const openEditRoleModal = (e, role) => {
    e.stopPropagation();
    if (!roleAccess.canEdit) {
      toast.error('You do not have permission to edit roles');
      return;
    }

    setEditingRole(role);
    setRoleForm({
      name: role.name || '',
      canGrantView: role.canGrantView || false,
      canGrantCreate: role.canGrantCreate || false,
      canGrantEdit: role.canGrantEdit || false,
      canGrantDelete: role.canGrantDelete || false,
      canGrantDelegate: role.canGrantDelegate || false
    });
    setShowRoleModal(true);
  };

  const openEditMemberModal = (member) => {
    if (!memberAccess.canEdit) {
      toast.error('You do not have permission to edit members');
      return;
    }

    setEditingMember(member);
    setMemberRoleForm({ roleId: String(member.roleId || '') });
    setShowMemberRoleModal(true);
  };

  const openTableView = async (tableName, accessOverride = null) => {
    const access = accessOverride || getTableAccess(tableName);
    if (!access.canViewData && !access.canViewStructure) {
      toast.error('You do not have access to this table');
      return;
    }

    const existingView = dashboardViews.find((view) => view.tableName === tableName);
    if (existingView) {
      navigate(`/projects/${projectId}/dashboard/${existingView.id}`);
      return;
    }

    if (!connections[0]) {
      toast.error('No database connection found for this table');
      return;
    }

    try {
      const res = await api.post(`/api/projects/${projectId}/dashboard/views`, {
        dbConnectionId: connections[0].id,
        tableName,
        isVisible: true,
        isCreatable: access.canCreateData,
        isDeletable: access.canDeleteData,
      });
      navigate(`/projects/${projectId}/dashboard/${res.data.data.id}`);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to open table');
    }
  };

  const handleCreateTable = async (e) => {
    e.preventDefault();
    if (!canCreateNewTable) {
      toast.error('You do not have permission to create tables');
      return;
    }

    try {
      const createdTableName = newTableForm.tableName;
      await api.post(`/api/projects/${projectId}/dashboard/tables`, newTableForm);
      setShowCreateTableModal(false);
      setNewTableForm({ tableName: '' });
      toast.success('Table created!');

      try {
        await fetchAll();
      } catch {
        toast.error('Table was created, but schema refresh failed');
        return;
      }

      try {
        await openTableView(createdTableName, {
          canViewData: true,
          canViewStructure: true,
          canCreateData: true,
          canDeleteData: true,
        });
      } catch {
        toast.error('Table was created, but opening it failed');
      }
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to create table');
    }
  };

  const tabs = [
    { id: 'connections', label: 'Connections', icon: Database },
    { id: 'schema', label: 'Schema', icon: Table },
    ...(roleAccess.canView ? [{ id: 'roles', label: 'Roles', icon: Shield }] : []),
    ...(memberAccess.canView ? [{ id: 'members', label: 'Members', icon: Users }] : []),
    ...(schema.length > 0 && hasAnyTableAccess ? [{ id: 'tables', label: 'Tables', icon: Table }] : []),
  ];

  const handleAddDashboardView = async (e) => {
    e.preventDefault();
    try {
        const conn = connections[0];
        await api.post(`/api/projects/${projectId}/dashboard/views`, {
            dbConnectionId: conn.id,
            tableName: dashboardForm.tableName,
            isVisible: true,
            isCreatable: dashboardForm.isCreatable,
            isDeletable: dashboardForm.isDeletable,
        });
        toast.success('Table added to dashboard!');
        setShowDashboardModal(false);
        setDashboardForm({ tableName: '', isCreatable: true, isDeletable: false });
        fetchAll();
    } catch (err) {
        toast.error(err.response?.data?.message || 'Failed to add table');
    }
};

  if (loading) return (
    <Layout>
      <div className="text-gray-400 text-center py-12">Loading...</div>
    </Layout>
  );

  return (
    <Layout>
      {/* Header */}
      <div className="flex items-center gap-4 mb-8">
        <button onClick={() => navigate('/projects')}
          className="text-gray-400 hover:text-white transition">
          <ArrowLeft size={20} />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-white">{project?.name}</h1>
          <p className="text-gray-400 mt-1">{project?.description || 'No description'}</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-gray-900 border border-gray-800 rounded-xl p-1 mb-6 w-fit">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          return (
            <button key={tab.id} onClick={() => setTab(tab.id)}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition ${
                activeTab === tab.id
                  ? 'bg-indigo-600 text-white'
                  : 'text-gray-400 hover:text-white'
              }`}>
              <Icon size={16} />
              {tab.label}
            </button>
          );
        })}
      </div>

      {/* Connections Tab */}
      {activeTab === 'connections' && (
        <div>
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-white font-semibold">Database Connections</h2>
            <button onClick={() => setShowConnModal(true)}
              className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg text-sm transition">
              <Plus size={16} /> Add Connection
            </button>
          </div>
          {connections.length === 0 ? (
            <div className="bg-gray-900 border border-gray-800 rounded-xl p-12 text-center">
              <Database size={40} className="text-gray-600 mx-auto mb-3" />
              <p className="text-gray-400">No connections yet</p>
            </div>
          ) : (
            <div className="space-y-3">
              {connections.map((conn) => (
                <div key={conn.id}
                  className="bg-gray-900 border border-gray-800 rounded-xl p-5 flex items-center justify-between">
                  <div className="flex items-center gap-4">
                    <div className="w-10 h-10 rounded-lg bg-green-500/20 flex items-center justify-center">
                      <Database size={20} className="text-green-400" />
                    </div>
                    <div>
                      <p className="text-white font-medium">{conn.databaseName}</p>
                      <p className="text-gray-400 text-sm">{conn.host}:{conn.port} · {conn.dbType}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className={`text-xs px-2 py-1 rounded-full ${
                      conn.status === 'active'
                        ? 'bg-green-500/20 text-green-400'
                        : 'bg-red-500/20 text-red-400'
                    }`}>
                      {conn.status}
                    </span>
                    <button onClick={() => handleSync(conn.id)}
                      className="flex items-center gap-1 text-indigo-400 hover:text-indigo-300 text-sm transition">
                      <RefreshCw size={14} /> Sync Schema
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Schema Tab */}
      {activeTab === 'schema' && (
        <div>
          <h2 className="text-white font-semibold mb-4">Detected Schema</h2>
          {schema.length === 0 ? (
            <div className="bg-gray-900 border border-gray-800 rounded-xl p-12 text-center">
              <Table size={40} className="text-gray-600 mx-auto mb-3" />
              <p className="text-gray-400">Sync a connection to see schema</p>
            </div>
          ) : (
            <div className="space-y-4">
              {schema.map((table) => (
                <div key={table.tableName}
                  className="bg-gray-900 border border-gray-800 rounded-xl overflow-hidden">
                  <div className="px-5 py-3 border-b border-gray-800 flex items-center gap-2">
                    <Table size={16} className="text-indigo-400" />
                    <span className="text-white font-medium">{table.tableName}</span>
                    <span className="text-gray-500 text-xs ml-auto">{table.columns.length} columns</span>
                  </div>
                  <div className="divide-y divide-gray-800">
                    {table.columns.map((col) => (
                      <div key={col.columnName}
                        className="px-5 py-2.5 flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <span className="text-white text-sm">{col.columnName}</span>
                          {col.primaryKey && (
                            <span className="text-xs bg-indigo-500/20 text-indigo-400 px-1.5 py-0.5 rounded">PK</span>
                          )}
                          {col.foreignKey && (
                            <span className="text-xs bg-yellow-500/20 text-yellow-400 px-1.5 py-0.5 rounded">
                              FK → {col.referencedTable}
                            </span>
                          )}
                        </div>
                        <span className="text-gray-500 text-xs">{col.dataType}</span>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {activeTab === 'tables' && (
        <div className="space-y-6">
          {canCreateNewTable && (
            <div className="flex justify-end">
              <button
                type="button"
                onClick={() => setShowCreateTableModal(true)}
                className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg transition"
              >
                <Plus size={16} /> Add New Table
              </button>
            </div>
          )}
          {[
            {
              title: 'Can View',
              filter: (access) => access.canViewData || access.canViewStructure,
            },
            {
              title: 'Can Edit',
              filter: (access) => access.canEditData || access.canEditStructure,
            },
            {
              title: 'Can Delete',
              filter: (access) => access.canDeleteData || access.canDeleteStructure || access.canDeleteTable,
            },
            {
              title: 'Can Create',
              filter: (access) => access.canCreateData || access.canCreateStructure,
            },
          ].map((section) => {
            const sectionTables = schema.filter((table) => section.filter(getTableAccess(table.tableName)));

            return (
              <div key={section.title} className="bg-gray-900 border border-gray-800 rounded-xl p-5">
                <h2 className="text-white font-semibold mb-4">{section.title}</h2>
                {sectionTables.length === 0 ? (
                  <p className="text-gray-500 text-sm">No tables in this category</p>
                ) : (
                  <div className="flex flex-wrap gap-3">
                    {sectionTables.map((table) => {
                      const access = getTableAccess(table.tableName);
                      return (
                        <button
                          key={`${section.title}-${table.tableName}`}
                          type="button"
                          onClick={() => openTableView(table.tableName)}
                          className="rounded-xl border border-gray-700 bg-gray-800 px-4 py-3 text-left transition hover:border-indigo-500"
                        >
                          <p className="text-white text-sm font-medium">{table.tableName}</p>
                          <div className="mt-2 flex gap-2 flex-wrap">
                            {access.canViewData && <span className="text-xs bg-blue-500/20 text-blue-400 px-2 py-1 rounded">Data</span>}
                            {access.canViewStructure && <span className="text-xs bg-cyan-500/20 text-cyan-400 px-2 py-1 rounded">Structure</span>}
                            {access.canCreateData && <span className="text-xs bg-green-500/20 text-green-400 px-2 py-1 rounded">Add Row</span>}
                            {access.canCreateStructure && <span className="text-xs bg-emerald-500/20 text-emerald-400 px-2 py-1 rounded">Add Column</span>}
                            {(access.canEditData || access.canEditStructure) && <span className="text-xs bg-yellow-500/20 text-yellow-400 px-2 py-1 rounded">Edit</span>}
                            {(access.canDeleteData || access.canDeleteStructure) && <span className="text-xs bg-red-500/20 text-red-400 px-2 py-1 rounded">Delete</span>}
                            {access.canDeleteTable && <span className="text-xs bg-rose-500/20 text-rose-400 px-2 py-1 rounded">Delete Table</span>}
                          </div>
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Roles Tab */}
      {activeTab === 'roles' && (
        <div>
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-white font-semibold">Roles</h2>
            <button onClick={openCreateRoleModal}
              disabled={!roleAccess.canCreate}
              className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-700 disabled:text-gray-400 text-white px-4 py-2 rounded-lg text-sm transition">
              <Plus size={16} /> Add Role
            </button>
          </div>
          <div className="space-y-3">
            {roles.map((role) => (
              <div key={role.id}
              onClick={() => canOpenRoleDetail && navigate(`/projects/${projectId}/roles/${role.id}`)}
                className={`bg-gray-900 border border-gray-800 rounded-xl p-5 ${
                  canOpenRoleDetail ? 'cursor-pointer hover:border-indigo-500 transition' : ''
                }`}>
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-8 h-8 rounded-lg bg-purple-500/20 flex items-center justify-center">
                    <Shield size={16} className="text-purple-400" />
                  </div>
                  <span className="text-white font-medium">{role.name}</span>
                  <button
                    type="button"
                    onClick={(e) => openEditRoleModal(e, role)}
                    disabled={!roleAccess.canEdit}
                    className="ml-auto flex items-center gap-1 rounded-lg border border-indigo-500/20 px-3 py-1.5 text-xs text-indigo-400 transition hover:bg-indigo-500/10 disabled:border-gray-700 disabled:text-gray-500"
                  >
                    <Pencil size={14} /> Edit
                  </button>
                  <button
                    type="button"
                    onClick={(e) => handleDeleteRole(e, role)}
                    disabled={!roleAccess.canDelete}
                    className="flex items-center gap-1 rounded-lg border border-red-500/20 px-3 py-1.5 text-xs text-red-400 transition hover:bg-red-500/10 disabled:border-gray-700 disabled:text-gray-500"
                  >
                    <Trash2 size={14} /> Delete
                  </button>
                </div>
                <div className="flex gap-2 flex-wrap">
                  {role.canGrantView && <span className="text-xs bg-blue-500/20 text-blue-400 px-2 py-1 rounded">Grant View</span>}
                  {role.canGrantCreate && <span className="text-xs bg-green-500/20 text-green-400 px-2 py-1 rounded">Grant Create</span>}
                  {role.canGrantEdit && <span className="text-xs bg-yellow-500/20 text-yellow-400 px-2 py-1 rounded">Grant Edit</span>}
                  {role.canGrantDelete && <span className="text-xs bg-red-500/20 text-red-400 px-2 py-1 rounded">Grant Delete</span>}
                  {role.canGrantDelegate && <span className="text-xs bg-purple-500/20 text-purple-400 px-2 py-1 rounded">Grant Delegate</span>}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Members Tab */}
{activeTab === 'members' && (
    <div>
        <div className="flex justify-between items-center mb-4">
            <h2 className="text-white font-semibold">Members</h2>
            <button onClick={() => setShowInviteModal(true)}
                disabled={!canInviteMembers}
                className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-700 disabled:text-gray-400 text-white px-4 py-2 rounded-lg text-sm transition">
                <Plus size={16} /> Invite Member
            </button>
        </div>
        {members.length === 0 ? (
            <div className="bg-gray-900 border border-gray-800 rounded-xl p-12 text-center">
                <Users size={40} className="text-gray-600 mx-auto mb-3" />
                <p className="text-gray-400">No members yet</p>
            </div>
        ) : (
            <div className="space-y-3">
                {members.map((member) => (
                    <div key={member.id}
                        className="bg-gray-900 border border-gray-800 rounded-xl p-4 flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <div className="w-9 h-9 rounded-full bg-indigo-600 flex items-center justify-center text-white font-bold text-sm">
                                {member.name?.charAt(0).toUpperCase()}
                            </div>
                            <div>
                                <p className="text-white text-sm font-medium">{member.name}</p>
                                <p className="text-gray-400 text-xs">{member.email}</p>
                            </div>
                        </div>
                        <div className="flex items-center gap-3">
                            {member.isPending && (
                              <span className="text-xs bg-yellow-500/20 text-yellow-400 px-3 py-1 rounded-full">
                                Pending
                              </span>
                            )}
                            <span className="text-xs bg-purple-500/20 text-purple-400 px-3 py-1 rounded-full">
                                {member.role}
                            </span>
                            {member.isOwner && (
                              <span className="text-xs bg-amber-500/20 text-amber-400 px-3 py-1 rounded-full">
                                Owner
                              </span>
                            )}
                            {member.isPending ? (
                              <button
                                  type="button"
                                  onClick={() => handleCancelInvitation(member)}
                                  className="rounded-lg border border-red-500/20 px-3 py-1.5 text-xs text-red-400 transition hover:bg-red-500/10"
                              >
                                  Cancel Invitation
                              </button>
                            ) : (
                              <>
                                <button
                                    type="button"
                                    onClick={() => openEditMemberModal(member)}
                                    disabled={!memberAccess.canEdit}
                                    className="rounded-lg border border-indigo-500/20 px-3 py-1.5 text-xs text-indigo-400 transition hover:bg-indigo-500/10 disabled:border-gray-700 disabled:text-gray-500"
                                >
                                    Edit Role
                                </button>
                                <button
                                    type="button"
                                    onClick={() => handleRemoveMember(member)}
                                    disabled={!canRemoveMembers || member.isOwner}
                                    className="rounded-lg border border-red-500/20 px-3 py-1.5 text-xs text-red-400 transition hover:bg-red-500/10 disabled:border-gray-700 disabled:text-gray-500"
                                >
                                    Remove
                                </button>
                              </>
                            )}
                        </div>
                    </div>
                ))}
            </div>
        )}
    </div>
)}


{/* Dashboard Tab */}
{activeTab === 'dashboard' && (
    <div>
        <div className="flex justify-between items-center mb-4">
            <h2 className="text-white font-semibold">Dashboard Views</h2>
            <button onClick={() => setShowDashboardModal(true)}
                className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg text-sm transition">
                <Plus size={16} /> Add Table View
            </button>
        </div>

        {dashboardViews.length === 0 ? (
            <div className="bg-gray-900 border border-gray-800 rounded-xl p-12 text-center">
                <LayoutDashboard size={40} className="text-gray-600 mx-auto mb-3" />
                <p className="text-gray-400">No dashboard views yet — add a table!</p>
            </div>
        ) : (
            <div className="space-y-3">
                {dashboardViews.map((view) => (
                    <div key={view.id}
                        className="bg-gray-900 border border-gray-800 rounded-xl p-5 flex items-center justify-between hover:border-indigo-500 transition cursor-pointer"
                        onClick={() => navigate(`/projects/${projectId}/dashboard/${view.id}`)}>
                        <div className="flex items-center gap-3">
                            <div className="w-9 h-9 rounded-lg bg-indigo-500/20 flex items-center justify-center">
                                <Table size={18} className="text-indigo-400" />
                            </div>
                            <div>
                                <p className="text-white font-medium">{view.tableName}</p>
                                <p className="text-gray-400 text-xs mt-0.5">
                                    {view.columns?.length} columns · 
                                    {view.isCreatable ? ' ✓ Create' : ''} 
                                    {view.isDeletable ? ' ✓ Delete' : ''}
                                </p>
                            </div>
                        </div>
                        <ChevronRight size={16} className="text-gray-500" />
                    </div>
                ))}
            </div>
        )}

        {/* Dashboard Modal */}
        {showDashboardModal && (
            <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
                <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 w-full max-w-md">
                    <h2 className="text-white font-semibold text-lg mb-5">Add Table to Dashboard</h2>
                    <form onSubmit={handleAddDashboardView} className="space-y-4">
                        <div>
                            <label className="text-sm text-gray-400 mb-1 block">Select Table</label>
                            <select value={dashboardForm.tableName}
                                onChange={(e) => setDashboardForm({ ...dashboardForm, tableName: e.target.value })}
                                className="w-full bg-gray-800 text-white rounded-lg px-4 py-3 border border-gray-700 focus:border-indigo-500 focus:outline-none"
                                required>
                                <option value="">Select table</option>
                                {schema.map((t) => (
                                    <option key={t.tableName} value={t.tableName}>{t.tableName}</option>
                                ))}
                            </select>
                        </div>
                        <div className="space-y-2">
                            <label className="flex items-center gap-3 cursor-pointer p-2">
                                <input type="checkbox" checked={dashboardForm.isCreatable}
                                    onChange={(e) => setDashboardForm({ ...dashboardForm, isCreatable: e.target.checked })}
                                    className="w-4 h-4 accent-indigo-500" />
                                <span className="text-gray-300 text-sm">Allow Create (new rows)</span>
                            </label>
                            <label className="flex items-center gap-3 cursor-pointer p-2">
                                <input type="checkbox" checked={dashboardForm.isDeletable}
                                    onChange={(e) => setDashboardForm({ ...dashboardForm, isDeletable: e.target.checked })}
                                    className="w-4 h-4 accent-indigo-500" />
                                <span className="text-gray-300 text-sm">Allow Delete (rows)</span>
                            </label>
                        </div>
                        <div className="flex gap-3 pt-2">
                            <button type="button" onClick={() => setShowDashboardModal(false)}
                                className="flex-1 bg-gray-800 hover:bg-gray-700 text-white py-3 rounded-lg transition">
                                Cancel
                            </button>
                            <button type="submit"
                                className="flex-1 bg-indigo-600 hover:bg-indigo-700 text-white py-3 rounded-lg transition">
                                Add
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        )}
    </div>
)}

      {/* Connection Modal */}
      {showConnModal && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 w-full max-w-md max-h-[90vh] overflow-y-auto">
            <h2 className="text-white font-semibold text-lg mb-5">Add Database Connection</h2>
            <form onSubmit={handleCreateConnection} className="space-y-4">
              <div>
                <label className="text-sm text-gray-400 mb-1 block">DB Type</label>
                <select value={connForm.dbType}
                  onChange={(e) => setConnForm({ ...connForm, dbType: e.target.value })}
                  className="w-full bg-gray-800 text-white rounded-lg px-4 py-3 border border-gray-700 focus:border-indigo-500 focus:outline-none">
                  <option value="postgres">PostgreSQL</option>
                  <option value="mysql">MySQL</option>
                </select>
              </div>
              {['host', 'databaseName', 'username', 'password'].map((field) => (
                <div key={field}>
                  <label className="text-sm text-gray-400 mb-1 block capitalize">
                    {field === 'databaseName' ? 'Database Name' : field}
                  </label>
                  <input
                    type={field === 'password' ? 'password' : 'text'}
                    value={connForm[field]}
                    onChange={(e) => setConnForm({ ...connForm, [field]: e.target.value })}
                    className="w-full bg-gray-800 text-white rounded-lg px-4 py-3 border border-gray-700 focus:border-indigo-500 focus:outline-none"
                    required
                  />
                </div>
              ))}
              <div>
                <label className="text-sm text-gray-400 mb-1 block">Port</label>
                <input type="number" value={connForm.port}
                  onChange={(e) => setConnForm({ ...connForm, port: parseInt(e.target.value) })}
                  className="w-full bg-gray-800 text-white rounded-lg px-4 py-3 border border-gray-700 focus:border-indigo-500 focus:outline-none" />
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowConnModal(false)}
                  className="flex-1 bg-gray-800 hover:bg-gray-700 text-white py-3 rounded-lg transition">
                  Cancel
                </button>
                <button type="submit"
                  className="flex-1 bg-indigo-600 hover:bg-indigo-700 text-white py-3 rounded-lg transition">
                  Connect
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Role Modal */}
      {showRoleModal && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 w-full max-w-md">
            <h2 className="text-white font-semibold text-lg mb-5">
              {editingRole ? 'Edit Role' : 'Create Role'}
            </h2>
            <form onSubmit={editingRole ? handleUpdateRole : handleCreateRole} className="space-y-4">
              <div>
                <label className="text-sm text-gray-400 mb-1 block">Role Name</label>
                <input type="text" value={roleForm.name}
                  onChange={(e) => setRoleForm({ ...roleForm, name: e.target.value })}
                  className="w-full bg-gray-800 text-white rounded-lg px-4 py-3 border border-gray-700 focus:border-indigo-500 focus:outline-none"
                  placeholder="e.g. Editor" required />
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => {
                  setShowRoleModal(false);
                  setEditingRole(null);
                }}
                  className="flex-1 bg-gray-800 hover:bg-gray-700 text-white py-3 rounded-lg transition">
                  Cancel
                </button>
                <button type="submit"
                  className="flex-1 bg-indigo-600 hover:bg-indigo-700 text-white py-3 rounded-lg transition">
                  {editingRole ? 'Save Changes' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Invite Modal */}
      {showInviteModal && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 w-full max-w-md">
            <h2 className="text-white font-semibold text-lg mb-5">Invite Member</h2>
            <form onSubmit={handleInvite} className="space-y-4">
              <div>
                <label className="text-sm text-gray-400 mb-1 block">Email</label>
                <input type="email" value={inviteForm.email}
                  onChange={(e) => setInviteForm({ ...inviteForm, email: e.target.value })}
                  className="w-full bg-gray-800 text-white rounded-lg px-4 py-3 border border-gray-700 focus:border-indigo-500 focus:outline-none"
                  placeholder="member@example.com" required />
              </div>
              <div>
                <label className="text-sm text-gray-400 mb-1 block">Role</label>
                <select value={inviteForm.roleId}
                  onChange={(e) => setInviteForm({ ...inviteForm, roleId: e.target.value })}
                  className="w-full bg-gray-800 text-white rounded-lg px-4 py-3 border border-gray-700 focus:border-indigo-500 focus:outline-none"
                  required>
                  <option value="">Select a role</option>
                  {roles.map((role) => (
                    <option key={role.id} value={role.id}>{role.name}</option>
                  ))}
                </select>
              </div>
              <div className="flex gap-3 pt-2">
                <button type="button" onClick={() => setShowInviteModal(false)}
                  className="flex-1 bg-gray-800 hover:bg-gray-700 text-white py-3 rounded-lg transition">
                  Cancel
                </button>
                <button type="submit"
                  className="flex-1 bg-indigo-600 hover:bg-indigo-700 text-white py-3 rounded-lg transition">
                  Send Invite
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showMemberRoleModal && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 w-full max-w-md">
            <h2 className="text-white font-semibold text-lg mb-5">Edit Member Role</h2>
            <form onSubmit={handleUpdateMemberRole} className="space-y-4">
              <div>
                <label className="text-sm text-gray-400 mb-1 block">Member</label>
                <div className="w-full bg-gray-800 text-white rounded-lg px-4 py-3 border border-gray-700">
                  {editingMember?.name} ({editingMember?.email})
                </div>
              </div>
              <div>
                <label className="text-sm text-gray-400 mb-1 block">Role</label>
                <select
                  value={memberRoleForm.roleId}
                  onChange={(e) => setMemberRoleForm({ roleId: e.target.value })}
                  className="w-full bg-gray-800 text-white rounded-lg px-4 py-3 border border-gray-700 focus:border-indigo-500 focus:outline-none"
                  required
                >
                  <option value="">Select a role</option>
                  {roles.map((role) => (
                    <option key={role.id} value={role.id}>{role.name}</option>
                  ))}
                </select>
              </div>
              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => {
                    setShowMemberRoleModal(false);
                    setEditingMember(null);
                    setMemberRoleForm({ roleId: '' });
                  }}
                  className="flex-1 bg-gray-800 hover:bg-gray-700 text-white py-3 rounded-lg transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 bg-indigo-600 hover:bg-indigo-700 text-white py-3 rounded-lg transition"
                >
                  Save Changes
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {showCreateTableModal && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
          <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 w-full max-w-md">
            <h2 className="text-white font-semibold text-lg mb-5">Add New Table</h2>
            <form onSubmit={handleCreateTable} className="space-y-4">
              <div>
                <label className="text-sm text-gray-400 mb-1 block">Table Name</label>
                <input
                  value={newTableForm.tableName}
                  onChange={(e) => setNewTableForm({ tableName: e.target.value })}
                  className="w-full bg-gray-800 text-white rounded-lg px-4 py-3 border border-gray-700 focus:border-indigo-500 focus:outline-none"
                  placeholder="orders"
                  required
                />
              </div>
              <div className="flex gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowCreateTableModal(false)}
                  className="flex-1 bg-gray-800 hover:bg-gray-700 text-white py-3 rounded-lg transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 bg-indigo-600 hover:bg-indigo-700 text-white py-3 rounded-lg transition"
                >
                  Create Table
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </Layout>
  );
}
