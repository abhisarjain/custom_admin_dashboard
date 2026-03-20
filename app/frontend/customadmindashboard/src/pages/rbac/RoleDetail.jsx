import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Layout from '../../components/layout/Layout';
import { toast } from 'react-hot-toast';
import { ArrowLeft, Shield, Users, Save, Table, ChevronDown } from 'lucide-react';
import api from '../../services/api';

const Toggle = ({ label, value, onChange, disabled = false }) => (
    <div className={`flex items-center justify-between p-3 rounded-lg transition ${
        disabled ? 'bg-gray-800/50 opacity-40' : 'bg-gray-800 cursor-pointer hover:bg-gray-700'
    }`}
        onClick={() => !disabled && onChange(!value)}>
        <span className="text-gray-300 text-sm">{label}</span>
        <div style={{
            width: '40px', height: '20px', borderRadius: '10px',
            backgroundColor: value ? '#4f46e5' : '#4b5563',
            position: 'relative', transition: 'background-color 0.2s',
            cursor: disabled ? 'not-allowed' : 'pointer'
        }}>
            <div style={{
                width: '16px', height: '16px', borderRadius: '50%',
                backgroundColor: 'white', position: 'absolute',
                top: '2px', left: value ? '22px' : '2px',
                transition: 'left 0.2s'
            }} />
        </div>
    </div>
);

const TablePermissionCheckbox = ({ checked, onChange, disabled = false, label }) => (
    <label className={`flex items-center gap-2 text-sm ${disabled ? 'text-gray-600' : 'text-gray-300'}`}>
        <input
            type="checkbox"
            checked={checked}
            onChange={(e) => onChange(e.target.checked)}
            disabled={disabled}
            className="w-4 h-4 accent-indigo-500"
        />
        <span>{label}</span>
    </label>
);

export default function RoleDetail() {
    const { projectId, roleId } = useParams();
    const navigate = useNavigate();

    const [role, setRole] = useState(null);
    const [activeTab, setActiveTab] = useState('role');
    const [loading, setLoading] = useState(true);

    // Current user ki permissions
    const [myRole, setMyRole] = useState(null);
    const [myRoleAccess, setMyRoleAccess] = useState({
        canView: false, canCreate: false, canEdit: false, canDelete: false,
    });
    const [myMemberAccess, setMyMemberAccess] = useState({
        canInvite: false, canView: false, canEdit: false, canRemove: false,
        grantInvite: false, grantView: false, grantEdit: false, grantRemove: false, grantDelegate: false,
    });

    // Role permissions
    const [rolePerms, setRolePerms] = useState({
        canView: false, canCreate: false, canEdit: false, canDelete: false,
    });

    // Grant permissions (from roles table)
    const [grantPerms, setGrantPerms] = useState({
        canGrantView: false, canGrantCreate: false,
        canGrantEdit: false, canGrantDelete: false, canGrantDelegate: false,
    });

    // Member permissions
    const [memberPerms, setMemberPerms] = useState({
        canInvite: false, canView: false, canEdit: false, canRemove: false,
        grantInvite: false, grantView: false, grantEdit: false, grantRemove: false, grantDelegate: false,
    });
    const [tables, setTables] = useState([]);
    const [tablePerms, setTablePerms] = useState({});
    const [myTablePerms, setMyTablePerms] = useState({});
    const [openGroups, setOpenGroups] = useState({
        canView: true,
        canCreate: false,
        canEdit: false,
        canDelete: false,
        canGrantView: false,
        canGrantCreate: false,
        canGrantEdit: false,
        canGrantDelete: false,
        canGrantDelegate: false,
    });

    useEffect(() => {
        fetchAll();
    }, [roleId]);

    const createEmptyTablePerm = () => ({
        canViewData: false,
        canViewStructure: false,
        canCreate: false,
        canCreateData: false,
        canCreateStructure: false,
        canEditData: false,
        canEditStructure: false,
        canDeleteData: false,
        canDeleteStructure: false,
        canDeleteTable: false,
        canGrantView: false,
        canGrantCreate: false,
        canGrantEdit: false,
        canGrantDelete: false,
        canGrantDelegate: false,
    });

    const normalizeTablePerm = (perm) => {
        const next = { ...createEmptyTablePerm(), ...perm };

        next.canCreate = next.canCreate || next.canCreateData || next.canCreateStructure;

        if (next.canCreateData || next.canEditData || next.canDeleteData) {
            next.canViewData = true;
        }

        if (next.canCreateStructure || next.canEditStructure || next.canDeleteStructure || next.canDeleteTable) {
            next.canViewStructure = true;
        }

        return next;
    };

    const mapTablePermList = (list = []) => list.reduce((acc, item) => {
        acc[item.tableName] = normalizeTablePerm({
            canViewData: item.canViewData || false,
            canViewStructure: item.canViewStructure || false,
            canCreate: item.canCreate || false,
            canCreateData: item.canCreateData || false,
            canCreateStructure: item.canCreateStructure || false,
            canEditData: item.canEditData || false,
            canEditStructure: item.canEditStructure || false,
            canDeleteData: item.canDeleteData || false,
            canDeleteStructure: item.canDeleteStructure || false,
            canDeleteTable: item.canDeleteTable || false,
            canGrantView: item.canGrantView || false,
            canGrantCreate: item.canGrantCreate || false,
            canGrantEdit: item.canGrantEdit || false,
            canGrantDelete: item.canGrantDelete || false,
            canGrantDelegate: item.canGrantDelegate || false,
        });
        return acc;
    }, {});

    const fetchAll = async () => {
        try {
            const [rolesRes, rolePermsRes, memberPermsRes, myPermsRes, tablePermsRes, connRes] = await Promise.all([
                api.get(`/api/projects/${projectId}/roles`),
                api.get(`/api/projects/${projectId}/roles/${roleId}/permissions`),
                api.get(`/api/projects/${projectId}/roles/${roleId}/member-permissions`),
                api.get(`/api/projects/${projectId}/my-permissions`),
                api.get(`/api/projects/${projectId}/roles/${roleId}/table-permissions`),
                api.get(`/api/projects/${projectId}/connections`),
            ]);

            const foundRole = rolesRes.data.data.find(r => r.id === parseInt(roleId));
            setRole(foundRole);
            setMyRole(myPermsRes.data.data);

            const myRoleId = myPermsRes.data.data?.roleId;
            if (myRoleId) {
                const [myRolePermsRes, myMemberPermsRes, myTablePermsRes] = await Promise.all([
                    api.get(`/api/projects/${projectId}/roles/${myRoleId}/permissions`),
                    api.get(`/api/projects/${projectId}/roles/${myRoleId}/member-permissions`),
                    api.get(`/api/projects/${projectId}/roles/${myRoleId}/table-permissions`),
                ]);

                const currentRolePerms = myRolePermsRes.data.data || {};
                setMyRoleAccess({
                    canView: currentRolePerms.canView || false,
                    canCreate: currentRolePerms.canCreate || false,
                    canEdit: currentRolePerms.canEdit || false,
                    canDelete: currentRolePerms.canDelete || false,
                });

                const currentMemberPerms = myMemberPermsRes.data.data || {};
                setMyMemberAccess({
                    canInvite: currentMemberPerms.canInvite || false,
                    canView: currentMemberPerms.canView || false,
                    canEdit: currentMemberPerms.canEdit || false,
                    canRemove: currentMemberPerms.canRemove || false,
                    grantInvite: currentMemberPerms.grantInvite || false,
                    grantView: currentMemberPerms.grantView || false,
                    grantEdit: currentMemberPerms.grantEdit || false,
                    grantRemove: currentMemberPerms.grantRemove || false,
                    grantDelegate: currentMemberPerms.grantDelegate || false,
                });
                setMyTablePerms(mapTablePermList(myTablePermsRes.data.data));
            }

            // Grant perms role object se
            setGrantPerms({
                canGrantView: foundRole?.canGrantView || false,
                canGrantCreate: foundRole?.canGrantCreate || false,
                canGrantEdit: foundRole?.canGrantEdit || false,
                canGrantDelete: foundRole?.canGrantDelete || false,
                canGrantDelegate: foundRole?.canGrantDelegate || false,
            });

            // Role permissions
            const rp = rolePermsRes.data.data;
            const isTargetSuperAdmin = foundRole?.name === 'Super Admin';
            setRolePerms({
                canView: isTargetSuperAdmin ? true : (rp.canView || false),
                canCreate: isTargetSuperAdmin ? true : (rp.canCreate || false),
                canEdit: isTargetSuperAdmin ? true : (rp.canEdit || false),
                canDelete: isTargetSuperAdmin ? true : (rp.canDelete || false),
            });

            // Member permissions
            const mp = memberPermsRes.data.data;
            setMemberPerms({
                canInvite: isTargetSuperAdmin ? true : (mp.canInvite || false),
                canView: isTargetSuperAdmin ? true : (mp.canView || false),
                canEdit: isTargetSuperAdmin ? true : (mp.canEdit || false),
                canRemove: isTargetSuperAdmin ? true : (mp.canRemove || false),
                grantInvite: isTargetSuperAdmin ? true : (mp.grantInvite || false),
                grantView: isTargetSuperAdmin ? true : (mp.grantView || false),
                grantEdit: isTargetSuperAdmin ? true : (mp.grantEdit || false),
                grantRemove: isTargetSuperAdmin ? true : (mp.grantRemove || false),
                grantDelegate: isTargetSuperAdmin ? true : (mp.grantDelegate || false),
            });
            setTablePerms(mapTablePermList(tablePermsRes.data.data));

            if (connRes.data.data.length > 0) {
                const schemaRes = await api.get(`/api/projects/${projectId}/connections/${connRes.data.data[0].id}/schema`);
                setTables(schemaRes.data.data || []);
            } else {
                setTables([]);
            }

        } catch (err) {
            toast.error('Failed to fetch role details');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        if (!loading && !myRoleAccess.canView && !myMemberAccess.canView) {
            toast.error('You do not have permission to view this role');
            navigate(`/projects/${projectId}`);
        }
    }, [loading, myRoleAccess.canView, myMemberAccess.canView, navigate, projectId]);

    useEffect(() => {
        if (activeTab === 'role' && !myRoleAccess.canView && myMemberAccess.canView) {
            setActiveTab('member');
        }
        if (activeTab === 'member' && !myMemberAccess.canView && myRoleAccess.canView) {
            setActiveTab('role');
        }
    }, [activeTab, myRoleAccess.canView, myMemberAccess.canView]);

    // Current user ke grant permissions check karo
    const getMyGrantPerms = () => {
        if (!myRole) return {};
        // Agar Super Admin hai toh sab enable
        if (myRole.role === 'Super Admin') {
            return {
                canGrantView: true, canGrantCreate: true,
                canGrantEdit: true, canGrantDelete: true, canGrantDelegate: true,
            };
        }
        // Warna my role ke grant perms
        return myRole.grantPermissions || {};
    };

    const myGrants = getMyGrantPerms();

    // Role tab toggles disabled logic
    const isRoleToggleDisabled = (field) => {
        // can_view enabled agar my grant_view true
        // can_create enabled agar my grant_create true
        // etc.
        const grantMap = {
            canView: myGrants.canGrantView,
            canCreate: myGrants.canGrantCreate,
            canEdit: myGrants.canGrantEdit,
            canDelete: myGrants.canGrantDelete,
        };
        return !grantMap[field];
    };

    const isGrantToggleDisabled = (field) => {
        // grant_* enabled sirf agar my grant_delegate true
        if (!myGrants.canGrantDelegate) return true;
        return false;
    };

    const handleSaveRolePerms = async () => {
        if (!myRoleAccess.canView) {
            toast.error('You do not have permission to view role permissions');
            return;
        }
        try {
            await api.post(`/api/projects/${projectId}/roles/${roleId}/permissions`, rolePerms);
            // Grant perms bhi save karo
            await api.put(`/api/projects/${projectId}/roles/${roleId}`, {
                name: role?.name || '',
                ...grantPerms,
            });
            toast.success('Role permissions saved!');
        } catch (err) {
            toast.error('Failed to save');
        }
    };

    const handleSaveMemberPerms = async () => {
        if (!myMemberAccess.canView) {
            toast.error('You do not have permission to view member permissions');
            return;
        }
        try {
            await api.post(`/api/projects/${projectId}/roles/${roleId}/member-permissions`, memberPerms);
            toast.success('Member permissions saved!');
        } catch (err) {
            toast.error('Failed to save');
        }
    };

    const getTablePerm = (tableName) => {
        if (role?.name === 'Super Admin') {
            return {
                ...createEmptyTablePerm(),
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
                canGrantView: true,
                canGrantCreate: true,
                canGrantEdit: true,
                canGrantDelete: true,
                canGrantDelegate: true,
            };
        }
        return tablePerms[tableName] || createEmptyTablePerm();
    };
    const getMyTablePerm = (tableName) => {
        if (myRole?.role === 'Super Admin') {
            return {
                ...createEmptyTablePerm(),
                canCreate: true,
                canCreateData: true,
                canCreateStructure: true,
                canViewData: true,
                canViewStructure: true,
                canEditData: true,
                canEditStructure: true,
                canDeleteData: true,
                canDeleteStructure: true,
                canDeleteTable: true,
                canGrantView: true,
                canGrantCreate: true,
                canGrantEdit: true,
                canGrantDelete: true,
                canGrantDelegate: true,
            };
        }
        return myTablePerms[tableName] || createEmptyTablePerm();
    };

    const canManageTableField = (tableName, field) => {
        const myTablePerm = getMyTablePerm(tableName);

        if (field === 'canGrantDelegate') {
            return !!(myTablePerm.canGrantDelegate || myGrants.canGrantDelegate);
        }

        if (field.startsWith('canGrant')) {
            return !!(myTablePerm.canGrantDelegate || myGrants.canGrantDelegate);
        }

        if (field === 'canCreate' || field === 'canCreateData' || field === 'canCreateStructure') {
            return !!(myTablePerm.canGrantCreate || myGrants.canGrantCreate);
        }
        if (field.startsWith('canView')) return !!myTablePerm.canGrantView;
        if (field.startsWith('canEdit')) return !!myTablePerm.canGrantEdit;
        if (field.startsWith('canDelete')) return !!myTablePerm.canGrantDelete;
        return false;
    };

    const updateTableField = (tableName, field, value) => {
        setTablePerms((prev) => ({
            ...prev,
            [tableName]: normalizeTablePerm({
                ...getTablePerm(tableName),
                [field]: value,
            }),
        }));
    };

    const updateTableFieldForAll = (field, value) => {
        const next = {};
        tables.forEach((table) => {
            next[table.tableName] = normalizeTablePerm({
                ...getTablePerm(table.tableName),
                [field]: value,
            });
        });
        setTablePerms((prev) => ({ ...prev, ...next }));
    };

    const isAllChecked = (field) => tables.length > 0 && tables.every((table) => getTablePerm(table.tableName)[field]);
    const isAllDisabled = (field) => tables.length === 0 || tables.every((table) => !canManageTableField(table.tableName, field));
    const specialCreatePerm = getTablePerm('__new_table__');

    const handleSaveTablePerms = async () => {
        if (!myRoleAccess.canView) {
            toast.error('You do not have permission to manage table permissions');
            return;
        }

        try {
            const payloads = [
                ...tables.map((table) => {
                const perm = normalizeTablePerm(getTablePerm(table.tableName));
                return api.post(`/api/projects/${projectId}/roles/${roleId}/table-permissions`, {
                    tableName: table.tableName,
                    canView: perm.canViewData || perm.canViewStructure,
                    canViewData: perm.canViewData,
                    canViewStructure: perm.canViewStructure,
                    canCreate: perm.canCreate,
                    canCreateData: perm.canCreateData,
                    canCreateStructure: perm.canCreateStructure,
                    canEdit: perm.canEditData || perm.canEditStructure,
                    canEditData: perm.canEditData,
                    canEditStructure: perm.canEditStructure,
                    canDelete: perm.canDeleteData || perm.canDeleteStructure || perm.canDeleteTable,
                    canDeleteData: perm.canDeleteData,
                    canDeleteStructure: perm.canDeleteStructure,
                    canDeleteTable: perm.canDeleteTable,
                    canGrantView: perm.canGrantView,
                    canGrantCreate: perm.canGrantCreate,
                    canGrantEdit: perm.canGrantEdit,
                    canGrantDelete: perm.canGrantDelete,
                    canGrantDelegate: perm.canGrantDelegate,
                });
                }),
                api.post(`/api/projects/${projectId}/roles/${roleId}/table-permissions`, {
                    tableName: '__new_table__',
                    canCreate: specialCreatePerm.canCreate,
                    canCreateData: false,
                    canCreateStructure: false,
                    canView: false,
                    canViewData: false,
                    canViewStructure: false,
                    canEdit: false,
                    canEditData: false,
                    canEditStructure: false,
                    canDelete: false,
                    canDeleteData: false,
                    canDeleteStructure: false,
                    canDeleteTable: false,
                    canGrantView: false,
                    canGrantCreate: false,
                    canGrantEdit: false,
                    canGrantDelete: false,
                    canGrantDelegate: false,
                }),
            ];
            await Promise.all(payloads);
            toast.success('Table permissions saved!');
        } catch (err) {
            toast.error('Failed to save table permissions');
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
                <button onClick={() => navigate(`/projects/${projectId}?tab=roles`)}
                    className="text-gray-400 hover:text-white transition">
                    <ArrowLeft size={20} />
                </button>
                <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-purple-500/20 flex items-center justify-center">
                        <Shield size={20} className="text-purple-400" />
                    </div>
                    <div>
                        <h1 className="text-2xl font-bold text-white">{role?.name}</h1>
                        <p className="text-gray-400 text-sm mt-0.5">Manage permissions for this role</p>
                    </div>
                </div>
            </div>

            {/* Tabs */}
            <div className="flex gap-1 bg-gray-900 border border-gray-800 rounded-xl p-1 mb-6 w-fit">
                {[
                    ...(myRoleAccess.canView ? [{ id: 'role', label: 'Role Permissions', icon: Shield }] : []),
                    ...(myMemberAccess.canView ? [{ id: 'member', label: 'Member Permissions', icon: Users }] : []),
                    ...(myRoleAccess.canView ? [{ id: 'tables', label: 'Tables', icon: Table }] : []),
                ].map((tab) => {
                    const Icon = tab.icon;
                    return (
                        <button key={tab.id} onClick={() => setActiveTab(tab.id)}
                            className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition ${
                                activeTab === tab.id ? 'bg-indigo-600 text-white' : 'text-gray-400 hover:text-white'
                            }`}>
                            <Icon size={16} />
                            {tab.label}
                        </button>
                    );
                })}
            </div>

            {/* Role Permissions Tab */}
            {activeTab === 'role' && (
                <div className="max-w-md space-y-6">
                    {/* Can permissions */}
                    <div>
                        <p className="text-gray-400 text-xs uppercase tracking-widest mb-3">Access Permissions</p>
                        <div className="space-y-2">
                            <Toggle label="Can View" value={rolePerms.canView}
                                onChange={(v) => setRolePerms({ ...rolePerms, canView: v })}
                                disabled={isRoleToggleDisabled('canView')} />
                            <Toggle label="Can Create" value={rolePerms.canCreate}
                                onChange={(v) => setRolePerms({ ...rolePerms, canCreate: v })}
                                disabled={isRoleToggleDisabled('canCreate')} />
                            <Toggle label="Can Edit" value={rolePerms.canEdit}
                                onChange={(v) => setRolePerms({ ...rolePerms, canEdit: v })}
                                disabled={isRoleToggleDisabled('canEdit')} />
                            <Toggle label="Can Delete" value={rolePerms.canDelete}
                                onChange={(v) => setRolePerms({ ...rolePerms, canDelete: v })}
                                disabled={isRoleToggleDisabled('canDelete')} />
                        </div>
                    </div>

                    {/* Grant permissions */}
                    <div>
                        <p className="text-gray-400 text-xs uppercase tracking-widest mb-3">Grant Permissions</p>
                        <div className="space-y-2">
                            <Toggle label="Grant View" value={grantPerms.canGrantView}
                                onChange={(v) => setGrantPerms({ ...grantPerms, canGrantView: v })}
                                disabled={isGrantToggleDisabled('canGrantView')} />
                            <Toggle label="Grant Create" value={grantPerms.canGrantCreate}
                                onChange={(v) => setGrantPerms({ ...grantPerms, canGrantCreate: v })}
                                disabled={isGrantToggleDisabled('canGrantCreate')} />
                            <Toggle label="Grant Edit" value={grantPerms.canGrantEdit}
                                onChange={(v) => setGrantPerms({ ...grantPerms, canGrantEdit: v })}
                                disabled={isGrantToggleDisabled('canGrantEdit')} />
                            <Toggle label="Grant Delete" value={grantPerms.canGrantDelete}
                                onChange={(v) => setGrantPerms({ ...grantPerms, canGrantDelete: v })}
                                disabled={isGrantToggleDisabled('canGrantDelete')} />
                            <Toggle label="Grant Delegate" value={grantPerms.canGrantDelegate}
                                onChange={(v) => setGrantPerms({ ...grantPerms, canGrantDelegate: v })}
                                disabled={isGrantToggleDisabled('canGrantDelegate')} />
                        </div>
                    </div>

                    <button onClick={handleSaveRolePerms}
                        className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-3 rounded-lg transition">
                        <Save size={16} /> Save Role Permissions
                    </button>
                </div>
            )}

            {/* Member Permissions Tab */}
            {activeTab === 'member' && (
                <div className="max-w-md space-y-6">
                    {/* Can permissions */}
                    <div>
                        <p className="text-gray-400 text-xs uppercase tracking-widest mb-3">Member Access</p>
                        <div className="space-y-2">
                            <Toggle label="Can Invite" value={memberPerms.canInvite}
                                onChange={(v) => setMemberPerms({ ...memberPerms, canInvite: v })}
                                disabled={!myMemberAccess.grantInvite && myRole?.role !== 'Super Admin'} />
                            <Toggle label="Can View" value={memberPerms.canView}
                                onChange={(v) => setMemberPerms({ ...memberPerms, canView: v })}
                                disabled={!myMemberAccess.grantView && myRole?.role !== 'Super Admin'} />
                            <Toggle label="Can Edit" value={memberPerms.canEdit}
                                onChange={(v) => setMemberPerms({ ...memberPerms, canEdit: v })}
                                disabled={!myMemberAccess.grantEdit && myRole?.role !== 'Super Admin'} />
                            <Toggle label="Can Remove" value={memberPerms.canRemove}
                                onChange={(v) => setMemberPerms({ ...memberPerms, canRemove: v })}
                                disabled={!myMemberAccess.grantRemove && myRole?.role !== 'Super Admin'} />
                        </div>
                    </div>

                    {/* Grant permissions */}
                    <div>
                        <p className="text-gray-400 text-xs uppercase tracking-widest mb-3">Grant Member Access</p>
                        <div className="space-y-2">
                            <Toggle label="Grant Invite" value={memberPerms.grantInvite}
                                onChange={(v) => setMemberPerms({ ...memberPerms, grantInvite: v })}
                                disabled={!myMemberAccess.grantDelegate && myRole?.role !== 'Super Admin'} />
                            <Toggle label="Grant View" value={memberPerms.grantView}
                                onChange={(v) => setMemberPerms({ ...memberPerms, grantView: v })}
                                disabled={!myMemberAccess.grantDelegate && myRole?.role !== 'Super Admin'} />
                            <Toggle label="Grant Edit" value={memberPerms.grantEdit}
                                onChange={(v) => setMemberPerms({ ...memberPerms, grantEdit: v })}
                                disabled={!myMemberAccess.grantDelegate && myRole?.role !== 'Super Admin'} />
                            <Toggle label="Grant Remove" value={memberPerms.grantRemove}
                                onChange={(v) => setMemberPerms({ ...memberPerms, grantRemove: v })}
                                disabled={!myMemberAccess.grantDelegate && myRole?.role !== 'Super Admin'} />
                            <Toggle label="Grant Delegate" value={memberPerms.grantDelegate}
                                onChange={(v) => setMemberPerms({ ...memberPerms, grantDelegate: v })}
                                disabled={!myMemberAccess.grantDelegate && myRole?.role !== 'Super Admin'} />
                        </div>
                    </div>

                    <button onClick={handleSaveMemberPerms}
                        className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-3 rounded-lg transition">
                        <Save size={16} /> Save Member Permissions
                    </button>
                </div>
            )}

            {activeTab === 'tables' && (
                <div className="space-y-6">
                    {tables.length === 0 ? (
                        <div className="bg-gray-900 border border-gray-800 rounded-xl p-6 text-gray-400">
                            Sync a database connection to manage table permissions.
                        </div>
                    ) : (
                        <>
                            {[
                                {
                                    key: 'canView',
                                    title: 'Can View',
                                    fields: [
                                        { field: 'canViewData', label: 'Data' },
                                        { field: 'canViewStructure', label: 'Structure' },
                                    ],
                                },
                                {
                                    key: 'canCreate',
                                    title: 'Can Create',
                                    fields: [
                                        { field: 'canCreateData', label: 'Data' },
                                        { field: 'canCreateStructure', label: 'Structure' },
                                        { field: 'canCreate', label: 'New Table', tableName: '__new_table__' },
                                    ],
                                },
                                {
                                    key: 'canEdit',
                                    title: 'Can Edit',
                                    fields: [
                                        { field: 'canEditData', label: 'Data' },
                                        { field: 'canEditStructure', label: 'Structure' },
                                    ],
                                },
                                {
                                    key: 'canDelete',
                                    title: 'Can Delete',
                                    fields: [
                                        { field: 'canDeleteData', label: 'Data' },
                                        { field: 'canDeleteStructure', label: 'Structure' },
                                        { field: 'canDeleteTable', label: 'Delete Table' },
                                    ],
                                },
                                {
                                    key: 'canGrantView',
                                    title: 'Grant View',
                                    fields: [{ field: 'canGrantView', label: 'Grant' }],
                                },
                                {
                                    key: 'canGrantCreate',
                                    title: 'Grant Create',
                                    fields: [{ field: 'canGrantCreate', label: 'Grant' }],
                                },
                                {
                                    key: 'canGrantEdit',
                                    title: 'Grant Edit',
                                    fields: [{ field: 'canGrantEdit', label: 'Grant' }],
                                },
                                {
                                    key: 'canGrantDelete',
                                    title: 'Grant Delete',
                                    fields: [{ field: 'canGrantDelete', label: 'Grant' }],
                                },
                                {
                                    key: 'canGrantDelegate',
                                    title: 'Grant Delegate',
                                    fields: [{ field: 'canGrantDelegate', label: 'Grant' }],
                                },
                            ].map((group) => (
                                <div key={group.key} className="bg-gray-900 border border-gray-800 rounded-xl overflow-hidden">
                                    <button
                                        type="button"
                                        onClick={() => setOpenGroups((prev) => ({ ...prev, [group.key]: !prev[group.key] }))}
                                        className="w-full flex items-center justify-between px-5 py-4 text-left"
                                    >
                                        <div>
                                            <p className="text-white font-medium">{group.title}</p>
                                            <p className="text-gray-400 text-sm mt-1">All and individual table access</p>
                                        </div>
                                        <ChevronDown
                                            size={18}
                                            className={`text-gray-400 transition ${openGroups[group.key] ? 'rotate-180' : ''}`}
                                        />
                                    </button>

                                    {openGroups[group.key] && (
                                        <div className="border-t border-gray-800 px-5 py-4 space-y-4">
                                            <div
                                                className="grid gap-4 text-xs uppercase tracking-wider text-gray-500"
                                                style={{ gridTemplateColumns: `180px repeat(${group.fields.length}, minmax(0, 1fr))` }}
                                            >
                                                <span>Table</span>
                                                {group.fields.map((item) => <span key={`${group.key}-${item.field}-header`}>{item.label}</span>)}
                                            </div>

                                            <div
                                                className="grid gap-4 items-center rounded-lg bg-gray-800/70 px-4 py-3"
                                                style={{ gridTemplateColumns: `180px repeat(${group.fields.length}, minmax(0, 1fr))` }}
                                            >
                                                <span className="text-white text-sm font-medium">All</span>
                                                {group.fields.map((item) => (
                                                    <TablePermissionCheckbox
                                                        key={`${group.key}-${item.field}-all`}
                                                        checked={
                                                            item.tableName === '__new_table__'
                                                                ? getTablePerm('__new_table__')[item.field]
                                                                : isAllChecked(item.field)
                                                        }
                                                        onChange={(checked) => {
                                                            if (item.tableName === '__new_table__') {
                                                                updateTableField('__new_table__', item.field, checked);
                                                                return;
                                                            }
                                                            updateTableFieldForAll(item.field, checked);
                                                        }}
                                                        disabled={
                                                            item.tableName === '__new_table__'
                                                                ? !canManageTableField('__new_table__', item.field)
                                                                : isAllDisabled(item.field)
                                                        }
                                                        label={item.label}
                                                    />
                                                ))}
                                            </div>

                                            {tables.map((table) => (
                                                <div
                                                    key={`${group.key}-${table.tableName}`}
                                                    className="grid gap-4 items-center rounded-lg bg-gray-800/40 px-4 py-3"
                                                    style={{ gridTemplateColumns: `180px repeat(${group.fields.length}, minmax(0, 1fr))` }}
                                                >
                                                    <div>
                                                        <p className="text-white text-sm">{table.tableName}</p>
                                                        <p className="text-gray-500 text-xs">{table.columns?.length || 0} columns</p>
                                                    </div>
                                                    {group.fields.map((item) => (
                                                        item.tableName ? (
                                                            <span key={`${group.key}-${table.tableName}-${item.field}`} />
                                                        ) : (
                                                            <TablePermissionCheckbox
                                                                key={`${group.key}-${table.tableName}-${item.field}`}
                                                                checked={getTablePerm(table.tableName)[item.field]}
                                                                onChange={(checked) => updateTableField(table.tableName, item.field, checked)}
                                                                disabled={!canManageTableField(table.tableName, item.field)}
                                                                label={item.label}
                                                            />
                                                        )
                                                    ))}
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </div>
                            ))}

                            <button onClick={handleSaveTablePerms}
                                className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-3 rounded-lg transition">
                                <Save size={16} /> Save Table Permissions
                            </button>
                        </>
                    )}
                </div>
            )}
        </Layout>
    );
}
