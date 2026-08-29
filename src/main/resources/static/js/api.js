/**
 * Habify API Client & Session Manager
 */
const API = (() => {
  const BASE_URL = '/api';

  function getStoredUser() {
    try {
      const data = localStorage.getItem('habify_user');
      return data ? JSON.parse(data) : null;
    } catch (e) {
      return null;
    }
  }

  function setSession(authResponse) {
    localStorage.setItem('habify_user', JSON.stringify({
      userId: authResponse.userId,
      fullName: authResponse.fullName,
      email: authResponse.email,
      username: authResponse.username,
      token: authResponse.token
    }));
  }

  function clearSession() {
    localStorage.removeItem('habify_user');
  }

  function isAuthenticated() {
    return !!getStoredUser();
  }

  function requireAuth() {
    if (!isAuthenticated()) {
      window.location.href = 'auth.html';
      return false;
    }
    return true;
  }

  async function request(endpoint, options = {}) {
    const user = getStoredUser();
    const headers = {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    };

    if (user && user.userId) {
      headers['X-User-Id'] = user.userId.toString();
      if (user.token) {
        headers['Authorization'] = `Bearer ${user.token}`;
      }
    }

    const config = {
      ...options,
      headers
    };

    try {
      const response = await fetch(`${BASE_URL}${endpoint}`, config);
      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        const errorMsg = data.message || (data.details ? data.details.join(', ') : 'Request failed');
        if (response.status === 401 && !endpoint.startsWith('/auth/')) {
          clearSession();
          window.location.href = 'auth.html';
        }
        throw new Error(errorMsg);
      }

      return data;
    } catch (error) {
      console.error(`API Error [${endpoint}]:`, error);
      throw error;
    }
  }

  // Toast Notification System
  function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      container.className = 'toast-shelf';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast-msg toast-${type}`;
    
    let icon = 'ℹ️';
    if (type === 'success') icon = '✅';
    if (type === 'error') icon = '⚠️';

    toast.innerHTML = `<span>${icon}</span> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(100%)';
      toast.style.transition = 'all 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  }

  return {
    getStoredUser,
    setSession,
    clearSession,
    isAuthenticated,
    requireAuth,
    showToast,

    auth: {
      register: (fullName, email, username, password) =>
        request('/auth/register', {
          method: 'POST',
          body: JSON.stringify({ fullName, email, username, password })
        }),
      login: (emailOrUsername, password) =>
        request('/auth/login', {
          method: 'POST',
          body: JSON.stringify({ emailOrUsername, password })
        }),
      getProfile: () => request('/auth/profile')
    },

    habits: {
      getAll: () => request('/habits'),
      create: (habitData) =>
        request('/habits', {
          method: 'POST',
          body: JSON.stringify(habitData)
        }),
      getById: (id) => request(`/habits/${id}`),
      update: (id, habitData) =>
        request(`/habits/${id}`, {
          method: 'PUT',
          body: JSON.stringify(habitData)
        }),
      toggleActive: (id) =>
        request(`/habits/${id}/toggle-active`, {
          method: 'PATCH'
        }),
      delete: (id) =>
        request(`/habits/${id}`, {
          method: 'DELETE'
        })
    },

    logs: {
      logHabit: (habitId, logData) =>
        request(`/habits/${habitId}/logs`, {
          method: 'POST',
          body: JSON.stringify(logData)
        }),
      getToday: () => request('/logs/today'),
      getHistory: (habitId) => request(`/habits/${habitId}/logs`)
    },

    analytics: {
      getDashboard: () => request('/analytics/dashboard'),
      getStreak: (habitId) => request(`/analytics/habits/${habitId}/streak`)
    }
  };
})();
