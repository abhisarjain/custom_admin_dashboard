import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import ProjectDetail from './pages/project/ProjectDetail';
// Pages
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import Dashboard from './pages/dashboard/Dashboard';
import Projects from './pages/project/Projects';
import RoleDetail from './pages/rbac/RoleDetail';
import DashboardView from './pages/dashboard/DashboardView';
const PrivateRoute = ({ children }) => {
  const { tenant, loading } = useAuth();
  if (loading) return <div className="flex items-center justify-center h-screen">Loading...</div>;
  return tenant ? children : <Navigate to="/login" />;
};

const PublicRoute = ({ children }) => {
  const { tenant, loading } = useAuth();
  if (loading) return <div className="flex items-center justify-center h-screen">Loading...</div>;
  return !tenant ? children : <Navigate to="/dashboard" />;
};

export default function App() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<PublicRoute><Login /></PublicRoute>} />
      <Route path="/register" element={<PublicRoute><Register /></PublicRoute>} />

      {/* Private routes */}
      <Route path="/dashboard" element={<PrivateRoute><Dashboard /></PrivateRoute>} />
      <Route path="/projects" element={<PrivateRoute><Projects /></PrivateRoute>} />

      {/* Default */}
      <Route path="/" element={<Navigate to="/dashboard" />} />
      <Route path="*" element={<Navigate to="/dashboard" />} />
      <Route path="/projects/:projectId" element={<PrivateRoute><ProjectDetail /></PrivateRoute>} />
      <Route path="/projects/:projectId/roles/:roleId" element={<PrivateRoute><RoleDetail /></PrivateRoute>} />
      <Route path="/projects/:projectId/dashboard/:viewId" element={<PrivateRoute><DashboardView /></PrivateRoute>} />
    </Routes>
  );
}