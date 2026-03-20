import api from './api';

export const authService = {
  register: async (data) => {
    const response = await api.post('/api/auth/register', data);
    return response.data;
  },

  login: async (data) => {
    const response = await api.post('/api/auth/login', data);
    return response.data;
  },

  getMyInvitations: async () => {
    const response = await api.get('/api/invitations/my');
    return response.data;
  },

  acceptInvitation: async (token) => {
    const response = await api.post('/api/invitations/accept', { token });
    return response.data;
  },
};