/**
 * Habify - Dashboard Application Logic
 */
const App = (() => {
  let dashboardData = null;
  let allHabits = [];
  let currentFilter = 'ALL';

  function init() {
    if (!API.requireAuth()) return;

    const user = API.getStoredUser();
    if (user) {
      const displayNameEl = document.getElementById('user-display-name');
      if (displayNameEl) {
        displayNameEl.textContent = user.fullName || user.username;
      }
    }

    // Set today's date badge
    const today = new Date();
    const options = { weekday: 'short', month: 'short', day: 'numeric' };
    const dateBadge = document.getElementById('today-date-badge');
    if (dateBadge) {
      dateBadge.textContent = today.toLocaleDateString('en-US', options);
    }

    loadDashboard();
  }

  function switchView(view) {
    const dashView = document.getElementById('dashboard-view');
    const habitsView = document.getElementById('habits-view');
    const dashTab = document.getElementById('nav-dash-tab');
    const habitsTab = document.getElementById('nav-habits-tab');
    const titleEl = document.getElementById('view-title');
    const subEl = document.getElementById('view-subtitle');

    if (view === 'dashboard') {
      dashView.style.display = 'block';
      habitsView.style.display = 'none';
      dashTab.classList.add('active');
      habitsTab.classList.remove('active');
      titleEl.textContent = 'Daily Activity Dashboard';
      subEl.textContent = 'Monitor consistency, maintain streaks, and analyze your progress';
      loadDashboard();
    } else {
      dashView.style.display = 'none';
      habitsView.style.display = 'block';
      dashTab.classList.remove('active');
      habitsTab.classList.add('active');
      titleEl.textContent = 'Habits Management';
      subEl.textContent = 'Configure and customize your daily and scheduled routines';
      loadHabits();
    }
  }

  async function loadDashboard() {
    try {
      dashboardData = await API.analytics.getDashboard();
      renderKPIs(dashboardData);
      renderTodayChecklist(dashboardData.todayHabits || []);
      renderWeeklyChart(dashboardData.weeklyProgress || []);
    } catch (err) {
      API.showToast('Failed to load dashboard: ' + err.message, 'error');
    }
  }

  function renderKPIs(data) {
    document.getElementById('kpi-discipline-score').textContent = `${data.disciplineScore} / 100`;
    document.getElementById('kpi-today-progress').textContent = `${data.todayCompletionPercentage}%`;
    document.getElementById('kpi-today-ratio').textContent = `${data.completedTodayCount} of ${data.scheduledTodayCount} scheduled habits`;
    document.getElementById('kpi-best-streak').textContent = `${data.bestCurrentStreak} days`;
    document.getElementById('kpi-total-habits').textContent = data.totalHabitsCount;
  }

  function renderTodayChecklist(habits) {
    const listEl = document.getElementById('today-habits-list');
    if (!habits || habits.length === 0) {
      listEl.innerHTML = `
        <div class="empty-box">
          <div class="empty-box-icon">🎯</div>
          <h3>No Habits Created Yet</h3>
          <p>Create your first daily habit to start building discipline!</p>
          <button class="btn btn-primary" style="margin-top: 14px;" onclick="App.openAddHabitModal()">+ Create First Habit</button>
        </div>
      `;
      return;
    }

    listEl.innerHTML = habits.map(habit => {
      let statusClass = 'is-pending';
      let statusBadge = '<span class="badge badge-pending">Pending</span>';

      if (!habit.isScheduledToday) {
        statusClass = 'is-rest';
        statusBadge = '<span class="badge badge-pending">Rest Day</span>';
      } else if (habit.status === 'COMPLETED') {
        statusClass = 'is-completed';
        statusBadge = '<span class="badge badge-completed">✓ Completed (100%)</span>';
      } else if (habit.status === 'PARTIAL') {
        statusClass = 'is-partial';
        statusBadge = `<span class="badge badge-partial">◐ Partial (${habit.completionPercentage}%)</span>`;
      } else if (habit.status === 'MISSED') {
        statusClass = 'is-missed';
        statusBadge = '<span class="badge badge-missed">× Missed</span>';
      }

      const streakStr = habit.currentStreak > 0 ? `🔥 ${habit.currentStreak}d streak` : '0d streak';
      const categoryIcon = getCategoryEmoji(habit.category);

      return `
        <div class="habit-checklist-card ${statusClass}">
          <div class="habit-info-col">
            <div class="habit-name-line">
              <span class="habit-name">${escapeHtml(habit.title)}</span>
              ${statusBadge}
            </div>
            <div class="habit-tags-line">
              <span>${categoryIcon} ${escapeHtml(habit.category)}</span>
              <span>&bull;</span>
              <span>🎯 ${habit.targetValue} ${escapeHtml(habit.unit || 'times')}</span>
              <span>&bull;</span>
              <span class="streak-indicator">${streakStr}</span>
            </div>
          </div>

          <div class="habit-actions-group">
            <button class="btn-status-act btn-status-complete ${habit.status === 'COMPLETED' ? 'active' : ''}" 
                    title="Mark Complete (100%)" 
                    onclick="App.quickLogHabit(${habit.id}, 'COMPLETED', 100)">
              ✓ Complete
            </button>
            <button class="btn-status-act btn-status-partial ${habit.status === 'PARTIAL' ? 'active' : ''}" 
                    title="Record Partial Progress" 
                    onclick="App.openPartialModal(${habit.id}, ${habit.completionPercentage || 50})">
              ◐ Partial
            </button>
            <button class="btn-status-act btn-status-miss ${habit.status === 'MISSED' ? 'active' : ''}" 
                    title="Mark as Missed" 
                    onclick="App.quickLogHabit(${habit.id}, 'MISSED', 0)">
              × Miss
            </button>
          </div>
        </div>
      `;
    }).join('');
  }

  function renderWeeklyChart(weeklyProgress) {
    const chartEl = document.getElementById('weekly-chart-bars');
    if (!weeklyProgress || weeklyProgress.length === 0) {
      chartEl.innerHTML = '<p style="color: var(--text-muted); padding: 20px;">No activity data available.</p>';
      return;
    }

    chartEl.innerHTML = weeklyProgress.map(day => {
      const pct = day.completionPercentage || 0;
      let heightPct = pct;
      if (heightPct > 100) heightPct = 100;
      if (heightPct < 5 && pct > 0) heightPct = 5;

      let fillClass = 'low';
      if (pct >= 80) fillClass = 'high';
      else if (pct >= 40) fillClass = 'med';

      return `
        <div class="chart-col">
          <div class="chart-col-val">${pct}%</div>
          <div class="chart-bar-well" title="${day.date}: ${day.completedCount} completed, ${day.scheduledCount} scheduled">
            <div class="chart-bar-fill ${fillClass}" style="height: ${heightPct}%;"></div>
          </div>
          <div class="chart-col-label">${day.dayOfWeek}</div>
        </div>
      `;
    }).join('');
  }

  async function quickLogHabit(habitId, status, percentage) {
    try {
      await API.logs.logHabit(habitId, {
        status,
        completionPercentage: percentage
      });
      API.showToast(status === 'COMPLETED' ? 'Habit marked complete! 🔥' : 'Activity status recorded.', 'success');
      loadDashboard();
    } catch (err) {
      API.showToast(err.message, 'error');
    }
  }

  function openPartialModal(habitId, currentPct) {
    document.getElementById('partial-habit-id').value = habitId;
    document.getElementById('partial-percentage').value = currentPct;
    document.getElementById('partial-slider').value = currentPct;
    document.getElementById('partial-note').value = '';
    document.getElementById('partial-modal').classList.add('open');
  }

  function closePartialModal() {
    document.getElementById('partial-modal').classList.remove('open');
  }

  async function submitPartialLog(e) {
    e.preventDefault();
    const habitId = document.getElementById('partial-habit-id').value;
    const percentage = parseInt(document.getElementById('partial-percentage').value, 10);
    const note = document.getElementById('partial-note').value.trim();

    try {
      await API.logs.logHabit(habitId, {
        status: 'PARTIAL',
        completionPercentage: percentage,
        note: note || null
      });
      closePartialModal();
      API.showToast(`Recorded ${percentage}% partial progress.`, 'success');
      loadDashboard();
    } catch (err) {
      API.showToast(err.message, 'error');
    }
  }

  /* --- HABITS MANAGEMENT --- */

  async function loadHabits() {
    try {
      allHabits = await API.habits.getAll();
      renderHabitsGrid();
    } catch (err) {
      API.showToast('Failed to load habits: ' + err.message, 'error');
    }
  }

  function filterHabits(filter) {
    currentFilter = filter;
    document.querySelectorAll('.filter-btn').forEach(btn => btn.classList.remove('active'));
    
    if (filter === 'ALL') document.getElementById('filter-all-btn').classList.add('active');
    else if (filter === 'ACTIVE') document.getElementById('filter-active-btn').classList.add('active');
    else {
      const matchBtn = document.querySelector(`.filter-btn[onclick*="'${filter}'"]`);
      if (matchBtn) matchBtn.classList.add('active');
    }

    renderHabitsGrid();
  }

  function renderHabitsGrid() {
    const gridEl = document.getElementById('my-habits-grid');
    let list = allHabits;

    if (currentFilter === 'ACTIVE') {
      list = list.filter(h => h.isActive);
    } else if (currentFilter !== 'ALL') {
      list = list.filter(h => h.category.toLowerCase() === currentFilter.toLowerCase());
    }

    if (list.length === 0) {
      gridEl.innerHTML = `
        <div class="empty-box" style="grid-column: 1 / -1;">
          <div class="empty-box-icon">📋</div>
          <h3>No Habits Found</h3>
          <p>No habits match the selected filter category.</p>
        </div>
      `;
      return;
    }

    gridEl.innerHTML = list.map(habit => {
      const streakStr = habit.currentStreak > 0 ? `🔥 ${habit.currentStreak} Days Streak` : '0 Days Streak';
      const scheduleStr = habit.frequencyType === 'DAILY' ? 'Every Day (Daily)' : `Specific: ${habit.scheduledDays || 'None'}`;
      const statusBadge = habit.isActive 
        ? '<span class="badge badge-completed">Active</span>' 
        : '<span class="badge badge-missed">Paused</span>';

      return `
        <div class="habit-manage-card ${habit.isActive ? '' : 'is-paused'}">
          <div>
            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px;">
              <span class="badge badge-category">${getCategoryEmoji(habit.category)} ${escapeHtml(habit.category)}</span>
              ${statusBadge}
            </div>
            <h3 style="font-size: 1.15rem; color: #ffffff; margin-bottom: 6px;">${escapeHtml(habit.title)}</h3>
            <p style="font-size: 0.86rem; color: var(--text-secondary); line-height: 1.5; margin-bottom: 12px;">
              ${escapeHtml(habit.description || 'No description provided.')}
            </p>
          </div>

          <div class="habit-card-meta">
            <span>📅 ${scheduleStr}</span>
            <strong style="color: var(--accent-streak);">${streakStr}</strong>
          </div>

          <div class="habit-card-footer">
            <button class="btn-icon-act" title="Edit Habit" onclick="App.openEditHabitModal(${habit.id})">✏️</button>
            <button class="btn-icon-act" title="${habit.isActive ? 'Pause Habit' : 'Resume Habit'}" onclick="App.toggleHabitActive(${habit.id})">
              ${habit.isActive ? '⏸️' : '▶️'}
            </button>
            <button class="btn-icon-act btn-icon-danger" title="Delete Habit" onclick="App.deleteHabit(${habit.id})">🗑️</button>
          </div>
        </div>
      `;
    }).join('');
  }

  function openAddHabitModal() {
    document.getElementById('habit-modal-title').textContent = 'Create New Habit';
    document.getElementById('habit-id').value = '';
    document.getElementById('habit-form').reset();
    document.getElementById('habit-frequency').value = 'DAILY';
    document.getElementById('scheduled-days-group').style.display = 'none';
    document.getElementById('habit-modal').classList.add('open');
  }

  function openEditHabitModal(id) {
    const habit = allHabits.find(h => h.id === id);
    if (!habit) return;

    document.getElementById('habit-modal-title').textContent = 'Edit Habit';
    document.getElementById('habit-id').value = habit.id;
    document.getElementById('habit-title').value = habit.title;
    document.getElementById('habit-description').value = habit.description || '';
    document.getElementById('habit-category').value = habit.category || 'Other';
    document.getElementById('habit-priority').value = habit.priority || 'MEDIUM';
    document.getElementById('habit-frequency').value = habit.frequencyType || 'DAILY';
    document.getElementById('habit-preferred-time').value = habit.preferredTime || 'ANYTIME';
    document.getElementById('habit-target-val').value = habit.targetValue || 1;
    document.getElementById('habit-unit').value = habit.unit || 'times';

    onFrequencyChange();

    // Check scheduled days
    if (habit.scheduledDays) {
      const days = habit.scheduledDays.split(',').map(d => d.trim().toUpperCase());
      document.querySelectorAll('#scheduled-days-group input[type="checkbox"]').forEach(cb => {
        cb.checked = days.includes(cb.value);
      });
    }

    document.getElementById('habit-modal').classList.add('open');
  }

  function closeHabitModal() {
    document.getElementById('habit-modal').classList.remove('open');
  }

  function onFrequencyChange() {
    const freq = document.getElementById('habit-frequency').value;
    const daysGroup = document.getElementById('scheduled-days-group');
    daysGroup.style.display = freq === 'SPECIFIC_DAYS' ? 'block' : 'none';
  }

  async function saveHabit(e) {
    e.preventDefault();
    const id = document.getElementById('habit-id').value;
    const title = document.getElementById('habit-title').value.trim();
    const description = document.getElementById('habit-description').value.trim();
    const category = document.getElementById('habit-category').value;
    const priority = document.getElementById('habit-priority').value;
    const frequencyType = document.getElementById('habit-frequency').value;
    const preferredTime = document.getElementById('habit-preferred-time').value;
    const targetValue = parseInt(document.getElementById('habit-target-val').value, 10) || 1;
    const unit = document.getElementById('habit-unit').value.trim() || 'times';

    let scheduledDays = null;
    if (frequencyType === 'SPECIFIC_DAYS') {
      const selected = [];
      document.querySelectorAll('#scheduled-days-group input[type="checkbox"]:checked').forEach(cb => {
        selected.push(cb.value);
      });
      if (selected.length === 0) {
        API.showToast('Please select at least one scheduled day.', 'error');
        return;
      }
      scheduledDays = selected.join(',');
    }

    const payload = {
      title,
      description: description || null,
      category,
      priority,
      frequencyType,
      scheduledDays,
      preferredTime,
      targetValue,
      unit
    };

    try {
      if (id) {
        await API.habits.update(id, payload);
        API.showToast('Habit updated successfully!', 'success');
      } else {
        await API.habits.create(payload);
        API.showToast('New habit created! Start tracking today.', 'success');
      }

      closeHabitModal();
      loadDashboard();
      if (document.getElementById('habits-view').style.display !== 'none') {
        loadHabits();
      }
    } catch (err) {
      API.showToast(err.message, 'error');
    }
  }

  async function toggleHabitActive(id) {
    try {
      await API.habits.toggleActive(id);
      API.showToast('Habit status toggled.', 'info');
      loadHabits();
      loadDashboard();
    } catch (err) {
      API.showToast(err.message, 'error');
    }
  }

  async function deleteHabit(id) {
    if (!confirm('Are you sure you want to delete this habit and all its history?')) return;

    try {
      await API.habits.delete(id);
      API.showToast('Habit deleted.', 'info');
      loadHabits();
      loadDashboard();
    } catch (err) {
      API.showToast(err.message, 'error');
    }
  }

  function logout() {
    API.clearSession();
    window.location.href = 'index.html';
  }

  function getCategoryEmoji(cat) {
    if (!cat) return '✨';
    const c = cat.toLowerCase();
    if (c.includes('study')) return '📚';
    if (c.includes('code') || c.includes('coding')) return '💻';
    if (c.includes('fit') || c.includes('gym')) return '🏋️';
    if (c.includes('health') || c.includes('diet')) return '🥗';
    if (c.includes('read') || c.includes('book')) return '📖';
    if (c.includes('finance') || c.includes('money')) return '💰';
    if (c.includes('sleep')) return '🌙';
    return '✨';
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;')
              .replace(/</g, '&lt;')
              .replace(/>/g, '&gt;')
              .replace(/"/g, '&quot;')
              .replace(/'/g, '&#039;');
  }

  return {
    init,
    switchView,
    refreshDashboard: loadDashboard,
    quickLogHabit,
    openPartialModal,
    closePartialModal,
    submitPartialLog,
    filterHabits,
    openAddHabitModal,
    openEditHabitModal,
    closeHabitModal,
    onFrequencyChange,
    saveHabit,
    toggleHabitActive,
    deleteHabit,
    logout
  };
})();

document.addEventListener('DOMContentLoaded', App.init);
