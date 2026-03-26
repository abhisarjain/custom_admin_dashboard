import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Layout from '../../components/layout/Layout';
import { toast } from 'react-hot-toast';
import { ArrowLeft, Plus, Trash2, Edit2, Save, X } from 'lucide-react';
import api from '../../services/api';

export default function DashboardView() {
    const { projectId, viewId } = useParams();
    const navigate = useNavigate();
    const DATA_TYPE_OPTIONS = {
        postgres: ['TEXT', 'VARCHAR(255)', 'INTEGER', 'BIGINT', 'BOOLEAN', 'DATE', 'TIMESTAMP', 'DECIMAL(10,2)', 'JSONB'],
        mysql: ['TEXT', 'VARCHAR(255)', 'INT', 'BIGINT', 'BOOLEAN', 'DATE', 'DATETIME', 'DECIMAL(10,2)', 'JSON'],
    };

    const [view, setView] = useState(null);
    const [rows, setRows] = useState([]);
    const [loading, setLoading] = useState(true);
    const [editingRow, setEditingRow] = useState(null);
    const [editForm, setEditForm] = useState({});
    const [showCreateModal, setShowCreateModal] = useState(false);
    const [createForm, setCreateForm] = useState({});
    const [permissions, setPermissions] = useState(null);
    const [showAddColumnModal, setShowAddColumnModal] = useState(false);
    const [columnForm, setColumnForm] = useState({ columnName: '', dataType: 'TEXT' });
    const [showEditColumnModal, setShowEditColumnModal] = useState(false);
    const [editingColumn, setEditingColumn] = useState(null);
    const [editColumnForm, setEditColumnForm] = useState({ newColumnName: '', dataType: 'TEXT' });
    const [dbType, setDbType] = useState('postgres');
    const [primaryKeyColumn, setPrimaryKeyColumn] = useState('id');
    const [currentTableSchema, setCurrentTableSchema] = useState([]);

    useEffect(() => {
        fetchAll();
    }, [viewId]);

    const fetchAll = async () => {
    try {
        const [viewsRes, permsRes, connRes] = await Promise.all([
            api.get(`/api/projects/${projectId}/dashboard/views`),
            api.get(`/api/projects/${projectId}/my-permissions`),
            api.get(`/api/projects/${projectId}/connections`),
        ]);

        const foundView = viewsRes.data.data.find(v => v.id === parseInt(viewId));
        setView(foundView);
        setPermissions(permsRes.data.data);

        if (foundView) {
            const currentConnection = connRes.data.data.find((conn) => conn.id === foundView.dbConnectionId);
            const nextDbType = currentConnection?.dbType?.toLowerCase() || 'postgres';
            setDbType(nextDbType);

            const schemaRes = await api.get(`/api/projects/${projectId}/connections/${foundView.dbConnectionId}/schema`);
            const tableSchema = (schemaRes.data.data || []).find(
                (item) => item.tableName?.toLowerCase() === foundView.tableName?.toLowerCase()
            );
            const tableColumns = tableSchema?.columns || [];
            setCurrentTableSchema(tableColumns);
            setPrimaryKeyColumn(
                tableColumns.find((item) => item.isPrimaryKey || item.primaryKey)?.columnName || 'id'
            );
        } else {
            setCurrentTableSchema([]);
            setPrimaryKeyColumn('id');
        }

        const tablePerms = permsRes.data.data?.role === 'Super Admin' || permsRes.data.data?.isOwner
            ? { canViewData: true }
            : (permsRes.data.data?.tablePermissions?.[foundView?.tableName] || {});

        if (foundView && (tablePerms.canViewData || tablePerms.canView)) {
            try {
                const dataRes = await api.get(`/api/projects/${projectId}/dashboard/${viewId}/data`);
                setRows(dataRes.data.data);
            } catch (err) {
                setRows([]);
                toast.error(err.response?.data?.message || 'Failed to fetch table data');
            }
        } else {
            setRows([]);
        }
    } catch (err) {
        toast.error(err.response?.data?.message || 'Failed to fetch data');
    } finally {
        setLoading(false);
    }
    };

    const resolveRowKey = (row) => {
        if (!row) return null;
        if (primaryKeyColumn in row && row[primaryKeyColumn] != null) return row[primaryKeyColumn];
        const matchedKey = Object.keys(row).find(
            (key) => key.toLowerCase() === String(primaryKeyColumn).toLowerCase()
        );
        if (matchedKey && row[matchedKey] != null) return row[matchedKey];
        if (row.id != null) return row.id;
        return null;
    };

    const handleEdit = (row) => {
        const rowKey = resolveRowKey(row);
        if (rowKey == null) {
            toast.error('Row identifier not found for update');
            return;
        }
        setEditingRow(rowKey);
        setEditForm({ ...row });
    };

    const handleSaveEdit = async () => {
        if (editingRow == null) {
            toast.error('Row identifier not found for update');
            return;
        }
        try {
            await api.put(`/api/projects/${projectId}/dashboard/${viewId}/data/${editingRow}`, editForm);
            toast.success('Row updated!');
            setEditingRow(null);
            fetchAll();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to update row');
        }
    };

    const handleDelete = async (rowId) => {
        if (!confirm('Are you sure?')) return;
        try {
            await api.delete(`/api/projects/${projectId}/dashboard/${viewId}/data/${rowId}`);
            toast.success('Row deleted!');
            fetchAll();
        } catch (err) {
            toast.error('Failed to delete row');
        }
    };

    const handleCreate = async (e) => {
        e.preventDefault();
        try {
            await api.post(`/api/projects/${projectId}/dashboard/${viewId}/data`, createForm);
            toast.success('Row created!');
            setShowCreateModal(false);
            setCreateForm({});
            fetchAll();
        } catch (err) {
            toast.error('Failed to create row');
        }
    };
    const getDataTypeOptions = () => DATA_TYPE_OPTIONS[dbType] || DATA_TYPE_OPTIONS.postgres;
    const getRowKey = (row) => resolveRowKey(row);
    const getTablePerms = (tableName) => permissions?.tablePermissions?.[tableName] || {};

    const canViewData = (tableName) => {
        if (permissions?.role === 'Super Admin' || permissions?.isOwner) return true;
        if (!permissions) return false;
        const tablePerms = getTablePerms(tableName);
        if (tablePerms.canViewData !== undefined) return tablePerms.canViewData;
        if (tablePerms.canView !== undefined) return tablePerms.canView;
        return permissions.defaultPermissions?.canView ?? false;
    };

    const canViewStructure = (tableName) => {
        if (permissions?.role === 'Super Admin' || permissions?.isOwner) return true;
        if (!permissions) return false;
        const tablePerms = getTablePerms(tableName);
        if (tablePerms.canViewStructure !== undefined) return tablePerms.canViewStructure;
        if (tablePerms.canView !== undefined) return tablePerms.canView;
        return permissions.defaultPermissions?.canView ?? false;
    };

    const canEditData = (tableName) => {
        if (permissions?.role === 'Super Admin' || permissions?.isOwner) return true;
        if (!permissions) return false;
        const tablePerms = getTablePerms(tableName);
        if (tablePerms.canEditData !== undefined) return tablePerms.canEditData;
        if (tablePerms.canEdit !== undefined) return tablePerms.canEdit;
        return permissions.defaultPermissions?.canEdit ?? false;
    };

    const canCreateData = (tableName) => {
        if (permissions?.role === 'Super Admin' || permissions?.isOwner) return true;
        if (!permissions) return false;
        const tablePerms = getTablePerms(tableName);
        if (tablePerms.canCreateData !== undefined) return tablePerms.canCreateData;
        if (tablePerms.canCreate !== undefined) return tablePerms.canCreate;
        return permissions.defaultPermissions?.canCreate ?? false;
    };

    const canCreateStructure = (tableName) => {
        if (permissions?.role === 'Super Admin' || permissions?.isOwner) return true;
        if (!permissions) return false;
        const tablePerms = getTablePerms(tableName);
        if (tablePerms.canCreateStructure !== undefined) return tablePerms.canCreateStructure;
        return false;
    };

    const canDeleteData = (tableName) => {
        if (permissions?.role === 'Super Admin' || permissions?.isOwner) return true;
        if (!permissions) return false;
        const tablePerms = getTablePerms(tableName);
        if (tablePerms.canDeleteData !== undefined) return tablePerms.canDeleteData;
        if (tablePerms.canDelete !== undefined) return tablePerms.canDelete;
        return permissions.defaultPermissions?.canDelete ?? false;
    };

    const canEditStructure = (tableName) => {
        if (permissions?.role === 'Super Admin' || permissions?.isOwner) return true;
        if (!permissions) return false;
        const tablePerms = getTablePerms(tableName);
        if (tablePerms.canEditStructure !== undefined) return tablePerms.canEditStructure;
        if (tablePerms.canEdit !== undefined) return tablePerms.canEdit;
        return permissions.defaultPermissions?.canEdit ?? false;
    };

    const canDeleteStructure = (tableName) => {
        if (permissions?.role === 'Super Admin' || permissions?.isOwner) return true;
        if (!permissions) return false;
        const tablePerms = getTablePerms(tableName);
        if (tablePerms.canDeleteStructure !== undefined) return tablePerms.canDeleteStructure;
        if (tablePerms.canDelete !== undefined) return tablePerms.canDelete;
        return permissions.defaultPermissions?.canDelete ?? false;
    };

    const canDeleteTable = (tableName) => {
        if (permissions?.role === 'Super Admin' || permissions?.isOwner) return true;
        if (!permissions) return false;
        const tablePerms = getTablePerms(tableName);
        if (tablePerms.canDeleteTable !== undefined) return tablePerms.canDeleteTable;
        return false;
    };

const canViewColumn = (tableName, columnName) => {
    if (!permissions) return true;
    const key = `${tableName}.${columnName}`;
    const colPerm = permissions.columnPermissions[key];
    if (colPerm) return colPerm.canView;
    return true;
};

    const canCreateColumn = (tableName, columnName) => {
    if (!permissions) return canCreateData(tableName);
    const key = `${tableName}.${columnName}`;
    const colPerm = permissions.columnPermissions[key];
    if (colPerm && colPerm.canCreate !== undefined) return colPerm.canCreate;
    return canCreateData(tableName);
};

    const canEditColumn = (tableName, columnName) => {
    if (!permissions) return false;
    const key = `${tableName}.${columnName}`;
    const colPerm = permissions.columnPermissions[key];
    if (colPerm) return colPerm.canEdit;
    return canEditData(tableName);
};

    const canDeleteColumn = (tableName, columnName) => {
    if (!permissions) return canDeleteStructure(tableName);
    const key = `${tableName}.${columnName}`;
    const colPerm = permissions.columnPermissions[key];
    if (colPerm && colPerm.canDelete !== undefined) return colPerm.canDelete;
    return canDeleteStructure(tableName);
};

    const visibleColumns = view?.columns?.filter(c =>
        c.visible && canViewColumn(view.tableName, c.columnName)
    ) || [];
    const editableColumns = view?.columns?.filter(c => c.editable && c.columnName !== primaryKeyColumn) || [];
    const creatableColumns = editableColumns.filter((col) => canCreateColumn(view?.tableName, col.columnName));
    const canSeeData = canViewData(view?.tableName);
    const canSeeStructure = canViewStructure(view?.tableName);

    const handleAddColumn = async (e) => {
        e.preventDefault();
        try {
            await api.post(`/api/projects/${projectId}/dashboard/${viewId}/columns`, columnForm);
            toast.success('Column added!');
            setShowAddColumnModal(false);
            setColumnForm({ columnName: '', dataType: 'TEXT' });
            fetchAll();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to add column');
        }
    };

    const handleDeleteColumn = async (columnName) => {
        if (!confirm(`Delete column "${columnName}"?`)) return;
        try {
            await api.delete(`/api/projects/${projectId}/dashboard/${viewId}/columns/${columnName}`);
            toast.success('Column deleted!');
            fetchAll();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to delete column');
        }
    };

    const openEditColumn = (column) => {
        const schemaColumn = currentTableSchema.find((item) => item.columnName === column.columnName);
        setEditingColumn(column);
        setEditColumnForm({
            newColumnName: column.columnName,
            dataType: schemaColumn?.dataType || getDataTypeOptions()[0],
        });
        setShowEditColumnModal(true);
    };

    const handleEditColumn = async (e) => {
        e.preventDefault();
        if (!editingColumn) return;
        try {
            await api.put(`/api/projects/${projectId}/dashboard/${viewId}/columns/${editingColumn.columnName}`, editColumnForm);
            toast.success('Column updated!');
            setShowEditColumnModal(false);
            setEditingColumn(null);
            setEditColumnForm({ newColumnName: '', dataType: 'TEXT' });
            fetchAll();
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to update column');
        }
    };

    const handleDeleteTable = async () => {
        if (!confirm(`Delete table "${view?.tableName}"? This cannot be undone.`)) return;
        try {
            await api.delete(`/api/projects/${projectId}/dashboard/${viewId}/table`);
            toast.success('Table deleted!');
            navigate(`/projects/${projectId}?tab=tables`);
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to delete table');
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
            <div className="flex items-center justify-between mb-8">
                <div className="flex items-center gap-4">
                    <button onClick={() => navigate(`/projects/${projectId}`)}
                        className="text-gray-400 hover:text-white transition">
                        <ArrowLeft size={20} />
                    </button>
                    <div>
                        <h1 className="text-2xl font-bold text-white">{view?.tableName}</h1>
                        <p className="text-gray-400 text-sm mt-0.5">
                            {canSeeData ? `${rows.length} rows` : 'Structure access'}
                        </p>
                    </div>
                </div>
                <div className="flex items-center gap-3">
                    {canDeleteTable(view?.tableName) && (
                        <button
                            onClick={handleDeleteTable}
                            className="flex items-center gap-2 bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg transition"
                        >
                            <Trash2 size={16} /> Delete Table
                        </button>
                    )}
                    {canCreateData(view?.tableName) && canSeeData && (
                        <button onClick={() => { setCreateForm({}); setShowCreateModal(true); }}
                            className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg transition">
                            <Plus size={16} /> Add Row
                        </button>
                    )}
                </div>
            </div>

            {canSeeStructure && (
                <div className="bg-gray-900 border border-gray-800 rounded-xl p-5 mb-6">
                    <div className="flex items-center justify-between mb-4">
                        <h2 className="text-white font-semibold">Structure</h2>
                        {(canEditStructure(view?.tableName) || canCreateStructure(view?.tableName)) && (
                            <button
                                type="button"
                                onClick={() => setShowAddColumnModal(true)}
                                className="flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg transition"
                            >
                                <Plus size={16} /> Add Column
                            </button>
                        )}
                    </div>
                    <div className="grid gap-3">
                        {view?.columns?.filter((col) => canViewColumn(view?.tableName, col.columnName)).map((col) => (
                            <div key={col.id} className="rounded-lg bg-gray-800 px-4 py-3 flex items-center justify-between">
                                <div>
                                    <p className="text-white text-sm">{col.columnName}</p>
                                </div>
                                <div className="flex gap-2 flex-wrap">
                                    {col.visible && <span className="text-xs bg-blue-500/20 text-blue-400 px-2 py-1 rounded">Visible</span>}
                                    {col.editable && <span className="text-xs bg-yellow-500/20 text-yellow-400 px-2 py-1 rounded">Editable</span>}
                                    {col.deletable && <span className="text-xs bg-red-500/20 text-red-400 px-2 py-1 rounded">Deletable</span>}
                                    {canEditStructure(view?.tableName) && canEditColumn(view?.tableName, col.columnName) && col.columnName !== primaryKeyColumn && (
                                        <button
                                            type="button"
                                            onClick={() => openEditColumn(col)}
                                            className="text-xs bg-indigo-500/20 text-indigo-400 px-2 py-1 rounded"
                                        >
                                            Edit Column
                                        </button>
                                    )}
                                    {canDeleteStructure(view?.tableName) && canDeleteColumn(view?.tableName, col.columnName) && col.columnName !== primaryKeyColumn && (
                                        <button
                                            type="button"
                                            onClick={() => handleDeleteColumn(col.columnName)}
                                            className="text-xs bg-red-500/20 text-red-400 px-2 py-1 rounded"
                                        >
                                            Delete Column
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {canSeeData ? (
                <div className="bg-gray-900 border border-gray-800 rounded-xl overflow-hidden">
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead>
                                <tr className="border-b border-gray-800">
                                    {visibleColumns.map((col) => (
                                        <th key={col.columnName}
                                            className="text-left px-4 py-3 text-gray-400 text-sm font-medium">
                                            {col.columnName}
                                        </th>
                                    ))}
                                    {(view?.columns?.some(c => c.editable) || view?.deletable) && (
                                        <th className="text-left px-4 py-3 text-gray-400 text-sm font-medium">
                                            Actions
                                        </th>
                                    )}
                                </tr>
                            </thead>
                            <tbody>
                                {rows.length === 0 ? (
                                    <tr>
                                        <td colSpan={visibleColumns.length + 1}
                                            className="text-center py-12 text-gray-500">
                                            No rows found
                                        </td>
                                    </tr>
                                ) : (
                                    rows.map((row, index) => (
                                        <tr key={getRowKey(row) ?? index}
                                            className="border-b border-gray-800 hover:bg-gray-800/50 transition">
                                            {visibleColumns.map((col) => (
                                                <td key={col.columnName} className="px-4 py-3">
                                                    {editingRow != null && editingRow === getRowKey(row) && canEditColumn(view?.tableName, col.columnName) ? (
                                                        <input
                                                            value={editForm[col.columnName] ?? ''}
                                                            onChange={(e) => setEditForm({ ...editForm, [col.columnName]: e.target.value })}
                                                            className="w-full bg-gray-800 text-white rounded-lg px-3 py-2 border border-gray-700 focus:border-indigo-500 focus:outline-none"
                                                        />
                                                    ) : (
                                                        <span className="text-white text-sm">{row[col.columnName]?.toString?.() ?? ''}</span>
                                                    )}
                                                </td>
                                            ))}
                                            <td className="px-4 py-3">
                                                <div className="flex items-center gap-2">
                                                    {editingRow != null && editingRow === getRowKey(row) ? (
                                                        <>
                                                            <button onClick={handleSaveEdit}
                                                                className="text-green-400 hover:text-green-300 transition">
                                                                <Save size={16} />
                                                            </button>
                                                            <button onClick={() => setEditingRow(null)}
                                                                className="text-gray-400 hover:text-white transition">
                                                                <X size={16} />
                                                            </button>
                                                        </>
                                                    ) : (
                                                        <>
                                                            {view?.columns?.some(c => c.editable) && canEditData(view?.tableName) && (
                                                                <button onClick={() => handleEdit(row)}
                                                                    className="text-indigo-400 hover:text-indigo-300 transition">
                                                                    <Edit2 size={16} />
                                                                </button>
                                                            )}
                                                            {canDeleteData(view?.tableName) && getRowKey(row) != null && (
                                                                <button onClick={() => handleDelete(getRowKey(row))}
                                                                    className="text-red-400 hover:text-red-300 transition">
                                                                    <Trash2 size={16} />
                                                                </button>
                                                            )}
                                                        </>
                                                    )}
                                                </div>
                                            </td>
                                        </tr>
                                    ))
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            ) : (
                <div className="bg-gray-900 border border-gray-800 rounded-xl p-8 text-center text-gray-400">
                    You have structure access for this table, but data access is not allowed.
                </div>
            )}

            {/* Create Modal */}
            {showCreateModal && (
                <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
                    <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 w-full max-w-md max-h-[90vh] overflow-y-auto">
                        <h2 className="text-white font-semibold text-lg mb-5">Add New Row</h2>
                        <form onSubmit={handleCreate} className="space-y-4">
                            {creatableColumns.map((col) => (
                                <div key={col.columnName}>
                                    <label className="text-sm text-gray-400 mb-1 block">
                                        {col.columnName}
                                    </label>
                                    <input
                                        value={createForm[col.columnName] || ''}
                                        onChange={(e) => setCreateForm({
                                            ...createForm,
                                            [col.columnName]: e.target.value
                                        })}
                                        className="w-full bg-gray-800 text-white rounded-lg px-4 py-3 border border-gray-700 focus:border-indigo-500 focus:outline-none"
                                        placeholder={col.columnName}
                                    />
                                </div>
                            ))}
                            <div className="flex gap-3 pt-2">
                                <button type="button" onClick={() => setShowCreateModal(false)}
                                    className="flex-1 bg-gray-800 hover:bg-gray-700 text-white py-3 rounded-lg transition">
                                    Cancel
                                </button>
                                <button type="submit"
                                    className="flex-1 bg-indigo-600 hover:bg-indigo-700 text-white py-3 rounded-lg transition">
                                    Create
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {showAddColumnModal && (
                <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
                    <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 w-full max-w-md">
                        <h2 className="text-white font-semibold text-lg mb-5">Add Column</h2>
                        <form onSubmit={handleAddColumn} className="space-y-4">
                            <div>
                                <label className="text-sm text-gray-400 mb-1 block">Column Name</label>
                                <input
                                    value={columnForm.columnName}
                                    onChange={(e) => setColumnForm({ ...columnForm, columnName: e.target.value })}
                                    className="w-full bg-gray-800 text-white rounded-lg px-4 py-3 border border-gray-700 focus:border-indigo-500 focus:outline-none"
                                    required
                                />
                            </div>
                            <div>
                                <label className="text-sm text-gray-400 mb-1 block">Data Type</label>
                                <select
                                    value={columnForm.dataType}
                                    onChange={(e) => setColumnForm({ ...columnForm, dataType: e.target.value })}
                                    className="w-full bg-gray-800 text-white rounded-lg px-4 py-3 border border-gray-700 focus:border-indigo-500 focus:outline-none"
                                    required
                                >
                                    {getDataTypeOptions().map((type) => (
                                        <option key={type} value={type}>{type}</option>
                                    ))}
                                </select>
                            </div>
                            <div className="flex gap-3 pt-2">
                                <button type="button" onClick={() => setShowAddColumnModal(false)}
                                    className="flex-1 bg-gray-800 hover:bg-gray-700 text-white py-3 rounded-lg transition">
                                    Cancel
                                </button>
                                <button type="submit"
                                    className="flex-1 bg-indigo-600 hover:bg-indigo-700 text-white py-3 rounded-lg transition">
                                    Add Column
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {showEditColumnModal && (
                <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50">
                    <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 w-full max-w-md">
                        <h2 className="text-white font-semibold text-lg mb-5">Edit Column</h2>
                        <form onSubmit={handleEditColumn} className="space-y-4">
                            <div>
                                <label className="text-sm text-gray-400 mb-1 block">Column Name</label>
                                <input
                                    value={editColumnForm.newColumnName}
                                    onChange={(e) => setEditColumnForm({ ...editColumnForm, newColumnName: e.target.value })}
                                    className="w-full bg-gray-800 text-white rounded-lg px-4 py-3 border border-gray-700 focus:border-indigo-500 focus:outline-none"
                                    required
                                />
                            </div>
                            <div>
                                <label className="text-sm text-gray-400 mb-1 block">Data Type</label>
                                <select
                                    value={editColumnForm.dataType}
                                    onChange={(e) => setEditColumnForm({ ...editColumnForm, dataType: e.target.value })}
                                    className="w-full bg-gray-800 text-white rounded-lg px-4 py-3 border border-gray-700 focus:border-indigo-500 focus:outline-none"
                                    required
                                >
                                    {getDataTypeOptions().map((type) => (
                                        <option key={type} value={type}>{type}</option>
                                    ))}
                                </select>
                            </div>
                            <div className="flex gap-3 pt-2">
                                <button type="button" onClick={() => setShowEditColumnModal(false)}
                                    className="flex-1 bg-gray-800 hover:bg-gray-700 text-white py-3 rounded-lg transition">
                                    Cancel
                                </button>
                                <button type="submit"
                                    className="flex-1 bg-indigo-600 hover:bg-indigo-700 text-white py-3 rounded-lg transition">
                                    Save Changes
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </Layout>
    );
}
