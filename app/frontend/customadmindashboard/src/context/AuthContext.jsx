import { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [tenant, setTenant] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedTenant = localStorage.getItem('tenant');
    const token = localStorage.getItem('token');
    if (storedTenant && token) {
      setTenant(JSON.parse(storedTenant));
    }
    setLoading(false);
  }, []);

  const login = (data) => {
    localStorage.setItem('token', data.token);
    localStorage.setItem('tenant', JSON.stringify({
      tenantId: data.tenantId,
      name: data.name,
      email: data.email,
    }));
    setTenant({
      tenantId: data.tenantId,
      name: data.name,
      email: data.email,
    });
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('tenant');
    setTenant(null);
  };

  return (
    <AuthContext.Provider value={{ tenant, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);