/**
 * Habify - API Client, Session Manager & Guest Storage Engine
 */
const API = (() => {
  const BASE_URL = '/api';
  const GUEST_HABITS_KEY = 'habify_guest_habits';
  const GUEST_LOGS_KEY = 'habify_guest_logs';

  const thirtyDaysAgoStr = new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0];

  // Starter demo habits for instant frictionless guest onboarding
  const DEFAULT_STARTER_HABITS = [
    {
      id: 101,
      title: 'Java Programming & Spring Boot',
      description: 'Practice core Java, concurrency, and REST endpoints',
      category: 'Coding',
      priority: 'HIGH',
      frequencyType: 'DAILY',
      scheduledDays: null,
      targetValue: 2,
      unit: 'hours',
      preferredTime: 'AFTERNOON',
      isActive: true,
      createdAt: thirtyDaysAgoStr
    },
    {
      id: 102,
      title: 'Gym Strength & Cardio',
      description: 'Compound lifts and high-intensity interval training',
      category: 'Fitness',
      priority: 'HIGH',
      frequencyType: 'DAILY',
      scheduledDays: null,
      targetValue: 45,
      unit: 'mins',
      preferredTime: 'EVENING',
      isActive: true,
      createdAt: thirtyDaysAgoStr
    },
    {
      id: 103,
      title: 'Morning Yoga & Meditation',
      description: 'Breathwork, flexibility, and mindfulness',
      category: 'Health',
      priority: 'MEDIUM',
      frequencyType: 'DAILY',
      scheduledDays: null,
      targetValue: 20,
      unit: 'mins',
      preferredTime: 'MORNING',
      isActive: true,
      createdAt: thirtyDaysAgoStr
    },
    {
      id: 104,
      title: 'Data Structures & Algorithms',
      description: 'Solve 2 LeetCode problems and review complexity',
      category: 'Study',
      priority: 'HIGH',
      frequencyType: 'DAILY',
      scheduledDays: null,
      targetValue: 1,
      unit: 'session',
      preferredTime: 'MORNING',
      isActive: true,
      createdAt: thirtyDaysAgoStr
    },
    {
      id: 105,
      title: 'Read Non-Fiction / Tech Books',
      description: 'Read Clean Architecture & Atomic Habits',
      category: 'Reading',
      priority: 'MEDIUM',
      frequencyType: 'DAILY',
      scheduledDays: null,
      targetValue: 25,
      unit: 'pages',
      preferredTime: 'EVENING',
      isActive: true,
      createdAt: thirtyDaysAgoStr
    }
  ];

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

    if (user && user.token) {
      headers['Authorization'] = `Bearer ${user.token}`;
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
    
    let iconSvg = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>';
    let iconColor = 'var(--primary, #F5B942)';
    if (type === 'success') {
      iconSvg = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>';
      iconColor = '#16a34a';
    } else if (type === 'error') {
      iconSvg = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>';
      iconColor = '#ef4444';
    } else if (type === 'info') {
      iconSvg = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>';
      iconColor = '#3b82f6';
    }

    const iconSpan = document.createElement('span');
    iconSpan.style.display = 'inline-flex';
    iconSpan.style.alignItems = 'center';
    iconSpan.style.color = iconColor;
    iconSpan.innerHTML = iconSvg;

    const textSpan = document.createElement('span');
    textSpan.textContent = message || '';

    toast.appendChild(iconSpan);
    toast.appendChild(document.createTextNode(' '));
    toast.appendChild(textSpan);
    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateY(12px) scale(0.95)';
      toast.style.transition = 'all 0.25s ease';
      setTimeout(() => toast.remove(), 250);
    }, 3200);
  }

  /* ==========================================================================
     GUEST STORAGE ENGINE (Offline-First / Zero Friction Tracking)
     ========================================================================== */
  const guestStorage = {
    getHabits() {
      try {
        const raw = localStorage.getItem(GUEST_HABITS_KEY);
        if (!raw) {
          localStorage.setItem(GUEST_HABITS_KEY, JSON.stringify(DEFAULT_STARTER_HABITS));
          return [...DEFAULT_STARTER_HABITS];
        }
        return JSON.parse(raw);
      } catch (e) {
        return [...DEFAULT_STARTER_HABITS];
      }
    },

    saveHabits(habits) {
      localStorage.setItem(GUEST_HABITS_KEY, JSON.stringify(habits));
    },

    createHabit(habitData) {
      const habits = guestStorage.getHabits();
      const newHabit = {
        ...habitData,
        id: Date.now(),
        isActive: true,
        createdAt: new Date().toISOString().split('T')[0]
      };
      habits.push(newHabit);
      guestStorage.saveHabits(habits);
      return newHabit;
    },

    updateHabit(id, habitData) {
      const habits = guestStorage.getHabits();
      const idx = habits.findIndex(h => h.id === Number(id));
      if (idx !== -1) {
        habits[idx] = { ...habits[idx], ...habitData };
        guestStorage.saveHabits(habits);
        return habits[idx];
      }
      throw new Error('Habit not found in local storage');
    },

    deleteHabit(id) {
      let habits = guestStorage.getHabits();
      habits = habits.filter(h => h.id !== Number(id));
      guestStorage.saveHabits(habits);
    },

    toggleHabitActive(id) {
      const habits = guestStorage.getHabits();
      const habit = habits.find(h => h.id === Number(id));
      if (habit) {
        habit.isActive = !habit.isActive;
        guestStorage.saveHabits(habits);
        return habit;
      }
    },

    getLogs() {
      try {
        const raw = localStorage.getItem(GUEST_LOGS_KEY);
        if (!raw) {
          // Pre-populate sample week logs to illustrate the tracker immediately
          const initialLogs = {};
          const today = new Date();
          
          // Seed some past days in the current week
          const habits = guestStorage.getHabits();
          habits.forEach((h, hIdx) => {
            for (let d = 0; d < 7; d++) {
              const dt = new Date(today);
              const dayOfWeek = (dt.getDay() + 6) % 7; // Monday=0
              dt.setDate(dt.getDate() - (dayOfWeek - d));
              const dateKey = dt.toISOString().split('T')[0];
              const logKey = `${h.id}_${dateKey}`;

              // Seed natural realistic patterns for days up to today
              if (d < dayOfWeek) {
                if ((hIdx + d) % 3 === 0) {
                  initialLogs[logKey] = { habitId: h.id, logDate: dateKey, status: 'COMPLETED', completionPercentage: 100 };
                } else if ((hIdx + d) % 3 === 1) {
                  initialLogs[logKey] = { habitId: h.id, logDate: dateKey, status: 'COMPLETED', completionPercentage: 100 };
                } else {
                  initialLogs[logKey] = { habitId: h.id, logDate: dateKey, status: 'PARTIAL', completionPercentage: 60 };
                }
              } else if (d === dayOfWeek) {
                // Today default
                if (hIdx % 2 === 0) {
                  initialLogs[logKey] = { habitId: h.id, logDate: dateKey, status: 'COMPLETED', completionPercentage: 100 };
                }
              }
            }
          });

          localStorage.setItem(GUEST_LOGS_KEY, JSON.stringify(initialLogs));
          return initialLogs;
        }
        return JSON.parse(raw);
      } catch (e) {
        return {};
      }
    },

    saveLogs(logs) {
      localStorage.setItem(GUEST_LOGS_KEY, JSON.stringify(logs));
    },

    logHabit(habitId, logData) {
      const logs = guestStorage.getLogs();
      const dateKey = logData.logDate || new Date().toISOString().split('T')[0];
      const key = `${habitId}_${dateKey}`;

      if (logData.status === 'NOT_COMPLETED' || logData.status === 'PENDING' || !logData.status) {
        delete logs[key];
      } else {
        logs[key] = {
          habitId: Number(habitId),
          logDate: dateKey,
          status: logData.status,
          completionPercentage: logData.completionPercentage || (logData.status === 'COMPLETED' ? 100 : (logData.status === 'MISSED' ? 0 : 50)),
          note: logData.note || null
        };
      }

      guestStorage.saveLogs(logs);
      return logs[key] || null;
    },

    saveLog(habitId, dateStrOrLogData, maybeLogData) {
      let logData = {};
      if (typeof dateStrOrLogData === 'string') {
        logData = { ...(maybeLogData || {}), logDate: dateStrOrLogData };
      } else if (dateStrOrLogData && typeof dateStrOrLogData === 'object') {
        logData = { ...dateStrOrLogData };
      }
      return guestStorage.logHabit(habitId, logData);
    }
  };

  return {
    getStoredUser,
    setSession,
    clearSession,
    isAuthenticated,
    requireAuth,
    showToast,
    guestStorage,

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
      verifyEmail: (token) => request(`/auth/verify-email?token=${encodeURIComponent(token)}`),
      forgotPassword: (email) =>
        request('/auth/forgot-password', {
          method: 'POST',
          body: JSON.stringify({ email })
        }),
      resetPassword: (token, newPassword) =>
        request('/auth/reset-password', {
          method: 'POST',
          body: JSON.stringify({ token, newPassword })
        }),
      getProfile: () => request('/auth/profile'),
      logout: () => request('/auth/logout', { method: 'POST' }).catch(() => {}),
      resendVerification: () => request('/auth/resend-verification', { method: 'POST' })
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
      getRange: (startDate, endDate) => request(`/logs/range?startDate=${startDate}&endDate=${endDate}`),
      getHistory: (habitId) => request(`/habits/${habitId}/logs`)
    },

    analytics: {
      getDashboard: () => request('/analytics/dashboard'),
      getStreak: (habitId) => request(`/analytics/habits/${habitId}/streak`)
    },

    goals: {
      getAll: () => request('/goals'),
      create: (goalData) =>
        request('/goals', {
          method: 'POST',
          body: JSON.stringify(goalData)
        }),
      increment: (id, amount = 1) =>
        request(`/goals/${id}/increment?amount=${amount}`, {
          method: 'PATCH'
        }),
      delete: (id) =>
        request(`/goals/${id}`, {
          method: 'DELETE'
        })
    },

    achievements: {
      getAll: () => request('/achievements')
    },

    calendar: {
      getMonth: (year, month) => request(`/calendar/month?year=${year}&month=${month}`),
      getDay: (dateStr) => request(`/calendar/day?date=${dateStr}`)
    },

    reports: {
      getSummary: (type = 'WEEKLY', date = '') =>
        request(`/reports/summary?type=${type}${date ? `&date=${date}` : ''}`),
      getCsvUrl: (type = 'WEEKLY', date = '') =>
        `/api/reports/csv?type=${type}${date ? `&date=${date}` : ''}`
    }
  };
})();
