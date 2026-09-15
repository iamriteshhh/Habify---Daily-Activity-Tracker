/**
 * Habify — Complete Workspace Controller & Interactive Tracker Engine
 */
const App = (() => {
  // State
  let habits = [];
  let logsMap = {}; // key: `${habitId}_${YYYY-MM-DD}` => log object
  let weekDates = []; // 7 Date objects (Mon to Sun)
  let currentWeekOffset = 0; // 0 = current week
  let currentCategoryFilter = 'ALL';
  let activePartialContext = { habitId: null, dateStr: null, targetValue: 1, unit: 'times' };
  let currentActiveView = 'tracker';
  let calendarYear = new Date().getFullYear();
  let calendarMonth = new Date().getMonth() + 1;

  // Chart instances
  let weeklyTrendChartInstance = null;
  let categoryDistChartInstance = null;

  /* ==========================================================================
     Initialization
     ========================================================================== */
  function init() {
    setupUserNavbar();
    calculateWeekDates(currentWeekOffset);
    loadTrackerData();
  }

  function setupUserNavbar() {
    const isAuth = API.isAuthenticated();
    const user = API.getStoredUser();
    const actionsEl = document.getElementById('nav-user-actions');
    const sessionTypeEl = document.getElementById('kpi-session-type');

    if (actionsEl) {
      if (isAuth && user) {
        if (sessionTypeEl) sessionTypeEl.textContent = 'Synced Cloud Session';
        const initial = (user.fullName || user.username || 'U')[0].toUpperCase();
        actionsEl.innerHTML = `
          <div class="user-badge-pill">
            <div class="user-avatar-circle" style="background: var(--primary, #F5B942); color: #0F0F0F; font-weight:700; width:26px; height:26px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-size:0.8rem;">${initial}</div>
            <span style="font-size:0.88rem; font-weight:500;">${escapeHtml(user.fullName || user.username)}</span>
          </div>
          <button class="btn btn-secondary btn-sm" onclick="App.logout()">Logout</button>
        `;
      } else {
        if (sessionTypeEl) sessionTypeEl.textContent = 'Guest Mode (Local)';
        actionsEl.innerHTML = `
          <a href="auth.html#login" class="btn btn-ghost btn-sm">Log In</a>
          <a href="auth.html#register" class="btn btn-primary btn-sm">Get Started</a>
        `;
      }
    }
  }

  /* ==========================================================================
     Workspace View Switching
     ========================================================================== */
  function switchView(viewName) {
    // Gate protected views for guests
    if (!API.isAuthenticated() && viewName !== 'tracker') {
      openAuthGateModal(viewName);
      return;
    }

    currentActiveView = viewName;

    // Toggle tab active states
    const tabs = ['tracker', 'analytics', 'goals', 'achievements', 'calendar', 'reports'];
    tabs.forEach(t => {
      const tabEl = document.getElementById(`tab-nav-${t}`);
      const viewEl = document.getElementById(`view-${t}`);
      if (tabEl) {
        if (t === viewName) tabEl.classList.add('active');
        else tabEl.classList.remove('active');
      }
      if (viewEl) {
        viewEl.style.display = t === viewName ? 'block' : 'none';
      }
    });

    // Lazy load view data
    if (viewName === 'analytics') loadAnalyticsView();
    else if (viewName === 'goals') loadGoalsView();
    else if (viewName === 'achievements') loadAchievementsView();
    else if (viewName === 'calendar') loadCalendarView();
    else if (viewName === 'reports') loadReportsView();
    else if (viewName === 'tracker') loadTrackerData();
  }

  /* ==========================================================================
     Week Date Calculations
     ========================================================================== */
  function calculateWeekDates(offset = 0) {
    weekDates = [];
    const now = new Date();
    const currentDay = (now.getDay() + 6) % 7; // Mon = 0
    const monday = new Date(now);
    monday.setDate(now.getDate() - currentDay + (offset * 7));
    monday.setHours(0, 0, 0, 0);

    for (let i = 0; i < 7; i++) {
      const d = new Date(monday);
      d.setDate(monday.getDate() + i);
      weekDates.push(d);
    }

    const startStr = weekDates[0].toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    const endStr = weekDates[6].toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    const labelEl = document.getElementById('week-range-label');
    if (labelEl) {
      labelEl.innerHTML = offset === 0
        ? `This Week: <span style="color:#ffffff; font-weight:600;">${startStr} – ${endStr}</span>`
        : `Week: <span style="color:#ffffff; font-weight:600;">${startStr} – ${endStr}</span>`;
    }
  }

  function changeWeek(direction) {
    if (!API.isAuthenticated() && direction !== 0 && (currentWeekOffset + direction) !== 0) {
      openAuthGateModal('history');
      return;
    }
    currentWeekOffset = direction === 0 ? 0 : currentWeekOffset + direction;
    calculateWeekDates(currentWeekOffset);
    loadTrackerData();
  }

  /* ==========================================================================
     Tracker Data Loading
     ========================================================================== */
  async function loadTrackerData() {
    const isAuth = API.isAuthenticated();
    const startDateStr = formatDate(weekDates[0]);
    const endDateStr = formatDate(weekDates[6]);

    try {
      if (isAuth) {
        const [habitsRes, rangeLogsRes, dashSummary] = await Promise.all([
          API.habits.getAll(),
          API.logs.getRange(startDateStr, endDateStr),
          API.analytics.getDashboard().catch(() => null)
        ]);

        habits = habitsRes || [];
        logsMap = {};
        (rangeLogsRes || []).forEach(log => {
          logsMap[`${log.habitId}_${log.logDate}`] = log;
        });

        renderTopKPIs(dashSummary);
      } else {
        habits = API.guestStorage.getHabits();
        logsMap = API.guestStorage.getLogs() || {};
        renderGuestKPIs();
      }

      renderTrackerTable();
    } catch (err) {
      console.error('Error loading tracker:', err);
      API.showToast('Failed to load tracker: ' + err.message, 'error');
    }
  }

  function renderTopKPIs(dashSummary) {
    const today = new Date();
    const options = { weekday: 'long', day: 'numeric', month: 'long' };
    const todayLabelEl = document.getElementById('kpi-today-date');
    if (todayLabelEl) todayLabelEl.textContent = today.toLocaleDateString('en-US', options);

    if (dashSummary) {
      document.getElementById('kpi-today-pct').textContent = `${dashSummary.todayCompletionPercentage}%`;
      document.getElementById('kpi-today-fill').style.width = `${dashSummary.todayCompletionPercentage}%`;
      document.getElementById('kpi-streak-val').textContent = `🔥 ${dashSummary.bestCurrentStreak || 0} Days`;
      document.getElementById('kpi-tasks-done').textContent = `${dashSummary.completedTodayCount} / ${dashSummary.scheduledTodayCount}`;
      document.getElementById('kpi-discipline-score').textContent = `${dashSummary.disciplineScore} / 100`;
    } else {
      renderGuestKPIs();
    }
  }

  function renderGuestKPIs() {
    const today = new Date();
    const todayStr = formatDate(today);
    const options = { weekday: 'long', day: 'numeric', month: 'long' };
    const todayLabelEl = document.getElementById('kpi-today-date');
    if (todayLabelEl) todayLabelEl.textContent = today.toLocaleDateString('en-US', options);

    let scheduledCount = 0;
    let completedCount = 0;
    let earnedPct = 0;

    habits.forEach(h => {
      if (h.isActive !== false && isHabitScheduledFor(h, today)) {
        scheduledCount++;
        const log = logsMap[`${h.id}_${todayStr}`];
        if (log) {
          if (log.status === 'COMPLETED') {
            completedCount++;
            earnedPct += 100;
          } else if (log.status === 'PARTIAL') {
            earnedPct += (log.completionPercentage || 50);
          }
        }
      }
    });

    const todayPct = scheduledCount > 0 ? Math.min(100, Math.round(earnedPct / scheduledCount)) : 0;
    document.getElementById('kpi-today-pct').textContent = `${todayPct}%`;
    document.getElementById('kpi-today-fill').style.width = `${todayPct}%`;
    document.getElementById('kpi-streak-val').textContent = `🔥 0 Days`;
    document.getElementById('kpi-tasks-done').textContent = `${completedCount} / ${scheduledCount}`;
    document.getElementById('kpi-discipline-score').textContent = `-- / 100`;
  }

  /* ==========================================================================
     Signature Weekly Matrix Table Rendering
     ========================================================================== */
  function renderTrackerTable() {
    const theadEl = document.getElementById('tracker-thead');
    const tbodyEl = document.getElementById('tracker-tbody');
    const tfootEl = document.getElementById('tracker-tfoot');
    if (!theadEl || !tbodyEl) return;

    const todayStr = formatDate(new Date());
    const dayShortNames = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

    // 1. Build Header
    let headerHtml = `
      <tr>
        <th class="th-task-col">TASKS</th>
        <th class="th-completion-col">COMPLETION %</th>
    `;

    weekDates.forEach((dateObj, idx) => {
      const dateStr = formatDate(dateObj);
      const isToday = dateStr === todayStr;
      const dayNum = dateObj.getDate();
      const monthShort = dateObj.toLocaleDateString('en-US', { month: 'short' });

      headerHtml += `
        <th class="th-day-col ${isToday ? 'is-today' : ''}" title="${dateObj.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })}">
          <div class="th-day-header-box">
            <span class="th-day-name">${dayShortNames[idx]}</span>
            <span class="th-day-date">${dayNum} ${monthShort}</span>
          </div>
        </th>
      `;
    });

    headerHtml += `
        <th class="th-weekly-col">WEEKLY</th>
      </tr>
    `;
    theadEl.innerHTML = headerHtml;

    // Filter habits
    let filteredHabits = habits;
    if (currentCategoryFilter !== 'ALL') {
      filteredHabits = habits.filter(h => (h.category || '').toLowerCase() === currentCategoryFilter.toLowerCase());
    }

    if (filteredHabits.length === 0) {
      tbodyEl.innerHTML = `
        <tr>
          <td colspan="10" style="padding: 48px 20px; text-align: center;">
            <div style="font-size: 2.2rem; margin-bottom: 12px;">🎯</div>
            <h3 style="color:#ffffff; font-size:1.15rem; font-weight:600; margin-bottom: 6px;">No habits configured</h3>
            <p style="font-size:0.88rem; color:var(--text-secondary, #94a3b8); margin-bottom: 18px;">Create a habit routine to begin building your consistency matrix!</p>
            <button class="btn btn-primary btn-sm" onclick="App.openAddHabitModal()">+ Add New Habit</button>
          </td>
        </tr>
      `;
      tfootEl.innerHTML = '';
      return;
    }

    // 2. Build Body Rows
    let bodyHtml = '';
    const dayTotals = [0, 0, 0, 0, 0, 0, 0];
    const dayScheduledCounts = [0, 0, 0, 0, 0, 0, 0];
    let weekTotalEarned = 0;
    let weekTotalScheduled = 0;

    filteredHabits.forEach(habit => {
      let habitScheduledCount = 0;
      let habitEarnedPct = 0;

      let dayCellsHtml = '';
      weekDates.forEach((dateObj, dayIdx) => {
        const dateStr = formatDate(dateObj);
        const isToday = dateStr === todayStr;
        const isScheduled = isHabitScheduledFor(habit, dateObj);
        const log = logsMap[`${habit.id}_${dateStr}`];

        if (isScheduled) {
          habitScheduledCount++;
          dayScheduledCounts[dayIdx]++;
        }

        let stateClass = 'state-pending';
        let cellIcon = '○';
        let cellSub = '';
        let pct = 0;

        if (log) {
          if (log.status === 'COMPLETED') {
            stateClass = 'state-completed';
            cellIcon = '✓';
            pct = 100;
          } else if (log.status === 'PARTIAL') {
            stateClass = 'state-partial';
            cellIcon = '◐';
            pct = log.completionPercentage || 50;
            cellSub = `<span class="cell-pct-tag">${pct}%</span>`;
          } else if (log.status === 'MISSED') {
            stateClass = 'state-missed';
            cellIcon = '×';
            pct = 0;
          }
        } else if (!isScheduled) {
          stateClass = 'state-rest';
          cellIcon = '—';
          cellSub = '<span style="font-size:0.6rem; text-transform:uppercase; color:#64748b;">Rest</span>';
        }

        if (isScheduled) {
          habitEarnedPct += pct;
          dayTotals[dayIdx] += pct;
        }

        dayCellsHtml += `
          <td class="td-day-cell ${isToday ? 'is-today' : ''}">
            <button class="status-cell-btn ${stateClass}" 
                    id="cell-${habit.id}-${dateStr}"
                    title="${escapeHtml(habit.title)} on ${dayShortNames[dayIdx]}: Click to cycle status, Right click for numeric progress"
                    onclick="App.cycleHabitStatus(${habit.id}, '${dateStr}', event)"
                    oncontextmenu="App.handleCellContextMenu(${habit.id}, '${dateStr}', event)">
              <span class="cell-icon">${cellIcon}</span>
              ${cellSub}
            </button>
          </td>
        `;
      });

      const habitWeekPct = habitScheduledCount > 0 
        ? Math.round(habitEarnedPct / habitScheduledCount) 
        : 0;

      let fillClass = 'fill-zero';
      if (habitWeekPct >= 75) fillClass = 'fill-high';
      else if (habitWeekPct >= 40) fillClass = 'fill-med';
      else if (habitWeekPct > 0) fillClass = 'fill-low';

      const prioClass = (habit.priority || 'MEDIUM').toLowerCase();
      const prioDot = `<span class="task-priority-dot prio-${prioClass}" title="Priority: ${habit.priority}"></span>`;
      const catIcon = getCategorySvg(habit.category);

      weekTotalEarned += habitEarnedPct;
      weekTotalScheduled += habitScheduledCount;

      bodyHtml += `
        <tr id="habit-row-${habit.id}">
          <!-- Col 1: Task Info -->
          <td class="td-task-cell">
            <div class="task-info-card">
              <div class="task-category-icon">${catIcon}</div>
              <div class="task-details">
                <div class="task-title-line">
                  <span class="task-title" title="${escapeHtml(habit.title)}">${escapeHtml(habit.title)}</span>
                  ${prioDot}
                </div>
                <div class="task-meta-line">
                  <span>${escapeHtml(habit.category || 'General')}</span>
                  <span>&bull;</span>
                  <span>Target: ${habit.targetValue || 1} ${escapeHtml(habit.unit || 'times')}</span>
                </div>
              </div>
              <button class="row-actions-btn" title="Options" onclick="App.openEditHabitModal(${habit.id})">⋮</button>
            </div>
          </td>

          <!-- Col 2: Habit Completion % -->
          <td class="td-completion-cell">
            <div class="completion-bar-wrapper">
              <span class="completion-pct-label ${habitWeekPct > 0 ? 'highlight' : ''}">${habitWeekPct}%</span>
              <div class="completion-track">
                <div class="completion-fill ${fillClass}" style="width: ${habitWeekPct}%;"></div>
              </div>
            </div>
          </td>

          <!-- Mon to Sun Days -->
          ${dayCellsHtml}

          <!-- Weekly Overall Column -->
          <td class="td-weekly-cell">
            <span class="weekly-summary-pill ${fillClass}">${habitWeekPct}%</span>
          </td>
        </tr>
      `;
    });

    tbodyEl.innerHTML = bodyHtml;

    // 3. Build Footer: Daily Report Row
    const overallWeekPct = weekTotalScheduled > 0 
      ? Math.round(weekTotalEarned / weekTotalScheduled) 
      : 0;

    let footerHtml = `
      <tr class="tfoot-report-row">
        <td class="tf-report-label">
          <div style="display:flex; align-items:center; gap:8px;">
            <span>📊</span>
            <span>DAILY REPORT</span>
          </div>
        </td>
        <td class="tf-report-sublabel">Daily Completion (%)</td>
    `;

    for (let dayIdx = 0; dayIdx < 7; dayIdx++) {
      const scheduledOnDay = dayScheduledCounts[dayIdx];
      const earnedOnDay = dayTotals[dayIdx];
      const dayPct = scheduledOnDay > 0 ? Math.round(earnedOnDay / scheduledOnDay) : 0;
      const isToday = formatDate(weekDates[dayIdx]) === todayStr;

      let scoreClass = 'score-zero';
      if (dayPct >= 75) scoreClass = 'score-high';
      else if (dayPct >= 40) scoreClass = 'score-med';
      else if (dayPct > 0) scoreClass = 'score-low';

      footerHtml += `
        <td class="tf-day-score-cell ${isToday ? 'is-today' : ''}">
          <div class="daily-score-badge ${scoreClass}">
            ${scheduledOnDay > 0 ? `${dayPct}%` : '—'}
          </div>
        </td>
      `;
    }

    footerHtml += `
        <td class="tf-weekly-total-cell">
          <div class="week-overall-badge">
            <span class="total-pct">${overallWeekPct}%</span>
            <span class="total-label">WEEK TOTAL</span>
          </div>
        </td>
      </tr>
    `;

    tfootEl.innerHTML = footerHtml;
  }

  /* ==========================================================================
     Cell Cycling & Partial Logging
     ========================================================================== */
  async function cycleHabitStatus(habitId, dateStr, event) {
    if (event) event.preventDefault();
    const habit = habits.find(h => h.id === Number(habitId));
    if (!habit) return;

    // Check if scheduled
    const dateObj = new Date(dateStr + 'T00:00:00');
    if (!isHabitScheduledFor(habit, dateObj)) {
      API.showToast('Rest day: this habit is not scheduled on this day.', 'info');
      return;
    }

    const currentLog = logsMap[`${habitId}_${dateStr}`];
    const currentStatus = currentLog ? currentLog.status : 'PENDING';

    // Cycle: PENDING -> COMPLETED -> PARTIAL (opens modal) -> MISSED -> PENDING
    let nextStatus = 'COMPLETED';
    if (currentStatus === 'PENDING') nextStatus = 'COMPLETED';
    else if (currentStatus === 'COMPLETED') {
      openPartialModal(habitId, dateStr);
      return;
    } else if (currentStatus === 'PARTIAL') nextStatus = 'MISSED';
    else if (currentStatus === 'MISSED') nextStatus = 'PENDING';

    await saveLogDirect(habitId, dateStr, nextStatus);
  }

  function handleCellContextMenu(habitId, dateStr, event) {
    event.preventDefault();
    openPartialModal(habitId, dateStr);
  }

  async function saveLogDirect(habitId, dateStr, status) {
    const isAuth = API.isAuthenticated();

    try {
      if (status === 'PENDING') {
        if (isAuth) {
          // In API, MISSED with 0% or we can just leave it as MISSED or remove
          await API.logs.logHabit(habitId, { logDate: dateStr, status: 'MISSED', completionPercentage: 0 });
        } else {
          API.guestStorage.saveLog(habitId, dateStr, { status: 'NOT_COMPLETED' });
        }
        delete logsMap[`${habitId}_${dateStr}`];
      } else {
        const pct = status === 'COMPLETED' ? 100 : (status === 'MISSED' ? 0 : 50);
        if (isAuth) {
          const res = await API.logs.logHabit(habitId, { logDate: dateStr, status, completionPercentage: pct });
          logsMap[`${habitId}_${dateStr}`] = res;
        } else {
          const res = API.guestStorage.saveLog(habitId, dateStr, { status, completionPercentage: pct });
          logsMap[`${habitId}_${dateStr}`] = res;
        }
      }

      await loadTrackerData();
    } catch (err) {
      console.error('Error saving habit log:', err);
      API.showToast(err.message, 'error');
    }
  }

  /* ==========================================================================
     Partial Progress Modal & Numeric Calculation
     ========================================================================== */
  function openPartialModal(habitId, dateStr) {
    const habit = habits.find(h => h.id === Number(habitId));
    if (!habit) return;

    activePartialContext = {
      habitId,
      dateStr,
      targetValue: habit.targetValue || 1,
      unit: habit.unit || 'times'
    };

    document.getElementById('partial-modal-title').textContent = `Log: ${habit.title}`;
    document.getElementById('partial-target-display').textContent = `${activePartialContext.targetValue} ${activePartialContext.unit}`;
    
    const existing = logsMap[`${habitId}_${dateStr}`];
    const actualInput = document.getElementById('partial-actual-value');
    actualInput.value = (existing && existing.actualValue) ? existing.actualValue : '';
    document.getElementById('partial-note').value = (existing && existing.note) ? existing.note : '';
    
    calcDerivedPercentage();
    document.getElementById('partial-modal').style.display = 'flex';
  }

  function closePartialModal() {
    document.getElementById('partial-modal').style.display = 'none';
  }

  function calcDerivedPercentage() {
    const actualVal = parseFloat(document.getElementById('partial-actual-value').value);
    const target = activePartialContext.targetValue || 1;
    const derivedEl = document.getElementById('partial-derived-pct');

    if (isNaN(actualVal) || actualVal <= 0) {
      derivedEl.textContent = '0%';
      derivedEl.style.color = 'var(--text-secondary, #94a3b8)';
      return;
    }

    const pct = Math.min(100, Math.round((actualVal / target) * 100));
    derivedEl.textContent = `${pct}%`;
    if (pct >= 100) derivedEl.style.color = '#16a34a';
    else if (pct >= 50) derivedEl.style.color = 'var(--primary, #F5B942)';
    else derivedEl.style.color = '#ef4444';
  }

  async function submitPartialLog(e) {
    e.preventDefault();
    const actualVal = parseFloat(document.getElementById('partial-actual-value').value);
    const note = document.getElementById('partial-note').value.trim();
    const { habitId, dateStr } = activePartialContext;
    const isAuth = API.isAuthenticated();

    try {
      if (isAuth) {
        const res = await API.logs.logHabit(habitId, {
          logDate: dateStr,
          actualValue: actualVal,
          note: note || null
        });
        logsMap[`${habitId}_${dateStr}`] = res;
      } else {
        const target = activePartialContext.targetValue || 1;
        const pct = Math.min(100, Math.round((actualVal / target) * 100));
        let status = 'PARTIAL';
        if (pct >= 100) status = 'COMPLETED';
        else if (pct === 0) status = 'MISSED';

        const res = API.guestStorage.saveLog(habitId, dateStr, {
          status,
          completionPercentage: pct,
          actualValue: actualVal,
          note
        });
        logsMap[`${habitId}_${dateStr}`] = res;
      }

      closePartialModal();
      API.showToast('Progress logged successfully!', 'success');
      await loadTrackerData();
    } catch (err) {
      API.showToast(err.message, 'error');
    }
  }

  /* ==========================================================================
     Analytics View & Chart.js
     ========================================================================== */
  async function loadAnalyticsView() {
    try {
      const summary = await API.analytics.getDashboard();
      if (!summary) return;

      document.getElementById('analytics-best-day').textContent = summary.bestDay || 'N/A';
      document.getElementById('analytics-weakest-day').textContent = summary.weakestDay || 'N/A';
      document.getElementById('analytics-weekly-avg').textContent = `${summary.weeklyAveragePercentage || 0}%`;
      document.getElementById('analytics-habits-count').textContent = summary.totalHabits || 0;

      // Render Weekly Trend Chart
      const ctxTrend = document.getElementById('chart-weekly-trend');
      if (ctxTrend && summary.weeklyProgress) {
        const labels = summary.weeklyProgress.map(p => p.dayOfWeek);
        const dataValues = summary.weeklyProgress.map(p => p.completionPercentage);

        if (weeklyTrendChartInstance) weeklyTrendChartInstance.destroy();

        weeklyTrendChartInstance = new Chart(ctxTrend, {
          type: 'bar',
          data: {
            labels,
            datasets: [{
              label: 'Completion %',
              data: dataValues,
              backgroundColor: dataValues.map(v => v >= 75 ? '#16a34a' : (v >= 40 ? '#F5B942' : '#dc2626')),
              borderRadius: 6,
              borderSkipped: false
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: { display: false }
            },
            scales: {
              y: {
                beginAtZero: true,
                max: 100,
                grid: { color: 'rgba(255,255,255,0.06)' },
                ticks: { color: '#94a3b8' }
              },
              x: {
                grid: { display: false },
                ticks: { color: '#94a3b8' }
              }
            }
          }
        });
      }

      // Render Category Distribution Chart
      const ctxCat = document.getElementById('chart-category-dist');
      if (ctxCat && summary.categoryDistribution) {
        const catLabels = summary.categoryDistribution.map(c => c.category);
        const catData = summary.categoryDistribution.map(c => c.totalCompleted + c.totalPartial);

        if (categoryDistChartInstance) categoryDistChartInstance.destroy();

        categoryDistChartInstance = new Chart(ctxCat, {
          type: 'doughnut',
          data: {
            labels: catLabels,
            datasets: [{
              data: catData,
              backgroundColor: ['#F5B942', '#38bdf8', '#10b981', '#a855f7', '#ec4899', '#f97316'],
              borderWidth: 0
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: {
                position: 'bottom',
                labels: { color: '#94a3b8', font: { family: 'Inter', size: 11 } }
              }
            }
          }
        });
      }

      // Render Rule-Based Insights
      const insightsListEl = document.getElementById('analytics-insights-list');
      if (insightsListEl) {
        if (!summary.insights || summary.insights.length === 0) {
          insightsListEl.innerHTML = '<div style="color:var(--text-secondary); font-size:0.9rem;">Keep tracking habits to unlock dynamic behavioural insights!</div>';
        } else {
          insightsListEl.innerHTML = summary.insights.map(i => {
            let borderColor = 'var(--primary, #F5B942)';
            let icon = '💡';
            if (i.type === 'SUCCESS') { borderColor = '#16a34a'; icon = '✓'; }
            else if (i.type === 'WARNING') { borderColor = '#dc2626'; icon = '⚠️'; }

            return `
              <div style="background: var(--bg-card, #1F1F1F); border-left: 3px solid ${borderColor}; padding: 14px; border-radius: 6px;">
                <div style="display:flex; align-items:center; gap:8px; margin-bottom: 4px;">
                  <span>${icon}</span>
                  <strong style="color: #ffffff; font-size: 0.92rem;">${escapeHtml(i.title)}</strong>
                </div>
                <p style="font-size: 0.85rem; color: var(--text-secondary, #94a3b8); margin: 0; line-height: 1.4;">${escapeHtml(i.message)}</p>
              </div>
            `;
          }).join('');
        }
      }
    } catch (err) {
      console.error('Error loading analytics:', err);
      API.showToast('Failed to load analytics: ' + err.message, 'error');
    }
  }

  /* ==========================================================================
     Goals View & Management
     ========================================================================== */
  async function loadGoalsView() {
    const gridEl = document.getElementById('goals-grid');
    if (!gridEl) return;

    try {
      const goals = await API.goals.getAll();
      if (!goals || goals.length === 0) {
        gridEl.innerHTML = `
          <div style="grid-column: 1 / -1; text-align: center; padding: 48px; background: var(--bg-card, #1F1F1F); border-radius: 8px; border: 1px solid var(--border-subtle, #262626);">
            <div style="font-size: 2rem; margin-bottom: 8px;">🎯</div>
            <h3 style="color:#ffffff; font-size: 1.1rem; margin-bottom: 4px;">No goals created yet</h3>
            <p style="font-size: 0.88rem; color: var(--text-secondary, #94a3b8); margin-bottom: 16px;">Create a milestone goal (e.g. Complete 20 workout sessions) to track accountability!</p>
            <button class="btn btn-primary btn-sm" onclick="App.openAddGoalModal()">+ Create First Goal</button>
          </div>
        `;
        return;
      }

      gridEl.innerHTML = goals.map(g => {
        const isCompleted = g.status === 'COMPLETED';
        const isExpired = g.status === 'EXPIRED';
        let statusBadge = `<span class="badge badge-pending">Active</span>`;
        if (isCompleted) statusBadge = `<span class="badge badge-completed">✓ Completed</span>`;
        else if (isExpired) statusBadge = `<span class="badge badge-missed">Expired</span>`;

        return `
          <div class="content-panel" style="padding: 18px;">
            <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom: 8px;">
              <h4 style="font-size: 1.05rem; font-weight: 600; color:#ffffff; margin:0;">${escapeHtml(g.title)}</h4>
              ${statusBadge}
            </div>
            ${g.description ? `<p style="font-size: 0.85rem; color: var(--text-secondary, #94a3b8); margin-bottom: 12px;">${escapeHtml(g.description)}</p>` : ''}
            
            <div style="display:flex; justify-content:space-between; font-size: 0.88rem; margin-bottom: 6px;">
              <span style="color: var(--text-muted, #64748b);">Progress</span>
              <strong style="color: #ffffff;">${g.currentValue} / ${g.targetValue} ${escapeHtml(g.unit)}</strong>
            </div>

            <div class="mini-progress-track" style="height: 8px; border-radius: 4px; margin-bottom: 14px;">
              <div class="mini-progress-fill" style="width: ${g.progressPercentage}%; background: ${isCompleted ? '#16a34a' : 'var(--primary, #F5B942)'};"></div>
            </div>

            <div style="display:flex; justify-content:space-between; align-items:center;">
              <span style="font-size: 0.78rem; color: var(--text-muted, #64748b);">
                ${g.deadline ? `Deadline: ${g.deadline}` : 'No deadline set'}
              </span>
              <div style="display:flex; gap: 6px;">
                ${!isCompleted ? `<button class="btn btn-secondary btn-sm" style="padding: 4px 8px; font-size: 0.78rem;" onclick="App.incrementGoal(${g.id})">+1 Progress</button>` : ''}
                <button class="btn btn-ghost btn-sm" style="padding: 4px 8px; color: #ef4444; font-size: 0.78rem;" onclick="App.deleteGoal(${g.id})">✕</button>
              </div>
            </div>
          </div>
        `;
      }).join('');
    } catch (err) {
      console.error('Error loading goals:', err);
      API.showToast(err.message, 'error');
    }
  }

  function openAddGoalModal() {
    document.getElementById('goal-form').reset();
    document.getElementById('goal-modal').style.display = 'flex';
  }

  function closeGoalModal() {
    document.getElementById('goal-modal').style.display = 'none';
  }

  async function saveGoal(e) {
    e.preventDefault();
    const title = document.getElementById('goal-title').value.trim();
    const description = document.getElementById('goal-desc').value.trim();
    const targetValue = parseInt(document.getElementById('goal-target').value, 10);
    const unit = document.getElementById('goal-unit').value.trim();
    const deadline = document.getElementById('goal-deadline').value;

    try {
      await API.goals.create({
        title,
        description: description || null,
        targetValue,
        unit: unit || 'times',
        deadline: deadline || null
      });
      closeGoalModal();
      API.showToast('Goal created successfully!', 'success');
      loadGoalsView();
    } catch (err) {
      API.showToast(err.message, 'error');
    }
  }

  async function incrementGoal(goalId) {
    try {
      await API.goals.increment(goalId, 1);
      API.showToast('Goal progress incremented!', 'success');
      loadGoalsView();
    } catch (err) {
      API.showToast(err.message, 'error');
    }
  }

  async function deleteGoal(goalId) {
    if (!confirm('Are you sure you want to delete this goal?')) return;
    try {
      await API.goals.delete(goalId);
      API.showToast('Goal deleted.', 'info');
      loadGoalsView();
    } catch (err) {
      API.showToast(err.message, 'error');
    }
  }

  /* ==========================================================================
     Achievements View
     ========================================================================== */
  async function loadAchievementsView() {
    const gridEl = document.getElementById('achievements-grid');
    if (!gridEl) return;

    try {
      const achievements = await API.achievements.getAll();
      gridEl.innerHTML = achievements.map(a => {
        const unlocked = a.isUnlocked;
        return `
          <div class="content-panel" style="padding: 20px; text-align: center; border: 1px solid ${unlocked ? 'var(--primary, #F5B942)' : 'var(--border-subtle, #262626)'}; opacity: ${unlocked ? '1' : '0.65'};">
            <div style="font-size: 2.2rem; margin-bottom: 8px;">${a.icon}</div>
            <h4 style="font-size: 1.05rem; font-weight: 600; color: ${unlocked ? '#ffffff' : 'var(--text-secondary, #94a3b8)'}; margin-bottom: 4px;">${escapeHtml(a.title)}</h4>
            <p style="font-size: 0.82rem; color: var(--text-secondary, #94a3b8); margin-bottom: 14px; min-height: 36px;">${escapeHtml(a.description)}</p>
            
            <div class="mini-progress-track" style="height: 6px; border-radius: 3px; margin-bottom: 8px;">
              <div class="mini-progress-fill" style="width: ${a.progressPercentage}%; background: ${unlocked ? '#16a34a' : 'var(--primary, #F5B942)'};"></div>
            </div>

            <div style="font-size: 0.78rem; color: var(--text-muted, #64748b);">
              ${unlocked ? '<strong style="color:#16a34a;">✓ UNLOCKED</strong>' : `${a.currentProgress} / ${a.targetProgress}`}
            </div>
          </div>
        `;
      }).join('');
    } catch (err) {
      console.error('Error loading achievements:', err);
      API.showToast(err.message, 'error');
    }
  }

  /* ==========================================================================
     Calendar & Monthly History
     ========================================================================== */
  async function loadCalendarView() {
    const gridEl = document.getElementById('calendar-days-grid');
    const titleEl = document.getElementById('calendar-month-title');
    if (!gridEl) return;

    try {
      const data = await API.calendar.getMonth(calendarYear, calendarMonth);
      if (!data) return;

      if (titleEl) titleEl.textContent = `${data.monthName} ${data.year}`;

      // Figure out day of week offset for 1st of month (Monday = 0)
      const firstDate = new Date(data.year, data.month - 1, 1);
      const startDayOffset = (firstDate.getDay() + 6) % 7;

      let daysHtml = '';
      for (let i = 0; i < startDayOffset; i++) {
        daysHtml += `<div style="min-height: 70px; background: transparent;"></div>`;
      }

      data.days.forEach(d => {
        let intensityBg = '#171717';
        let borderColor = '#262626';
        if (d.intensityLevel === 3) { intensityBg = 'rgba(22, 163, 74, 0.25)'; borderColor = '#16a34a'; }
        else if (d.intensityLevel === 2) { intensityBg = 'rgba(245, 185, 66, 0.2)'; borderColor = 'var(--primary, #F5B942)'; }
        else if (d.intensityLevel === 1) { intensityBg = 'rgba(245, 185, 66, 0.08)'; borderColor = '#334155'; }

        daysHtml += `
          <div onclick="App.openCalendarDayModal('${d.date}')" 
               style="min-height: 75px; background: ${intensityBg}; border: 1px solid ${borderColor}; border-radius: 6px; padding: 6px; cursor: pointer; display: flex; flex-direction: column; justify-content: space-between; transition: transform 0.15s ease;"
               onmouseover="this.style.transform='scale(1.02)'" onmouseout="this.style.transform='scale(1)'">
            <div style="display:flex; justify-content:space-between; align-items:center;">
              <span style="font-weight: 700; font-size: 0.85rem; color: #ffffff;">${d.dayOfMonth}</span>
              ${d.scheduledCount > 0 ? `<span style="font-size: 0.72rem; color: var(--primary, #F5B942);">${d.completionPercentage}%</span>` : ''}
            </div>
            <div style="font-size: 0.7rem; color: var(--text-muted, #64748b);">
              ${d.completedCount > 0 ? `✓ ${d.completedCount}` : (d.scheduledCount > 0 ? `${d.scheduledCount} sched` : '—')}
            </div>
          </div>
        `;
      });

      gridEl.innerHTML = daysHtml;
    } catch (err) {
      console.error('Error loading calendar:', err);
      API.showToast(err.message, 'error');
    }
  }

  function changeCalendarMonth(direction) {
    calendarMonth += direction;
    if (calendarMonth > 12) {
      calendarMonth = 1;
      calendarYear++;
    } else if (calendarMonth < 1) {
      calendarMonth = 12;
      calendarYear--;
    }
    loadCalendarView();
  }

  async function openCalendarDayModal(dateStr) {
    try {
      const day = await API.calendar.getDay(dateStr);
      if (!day) return;

      document.getElementById('cal-day-modal-title').textContent = `Activity on ${day.date} (${day.dayOfWeek})`;
      const bodyEl = document.getElementById('cal-day-modal-body');

      if (!day.logs || day.logs.length === 0) {
        bodyEl.innerHTML = `<p style="font-size: 0.9rem; color: var(--text-secondary, #94a3b8); text-align: center; padding: 16px;">No habits logged on this day.</p>`;
      } else {
        bodyEl.innerHTML = day.logs.map(l => {
          let statusColor = '#16a34a';
          if (l.status === 'PARTIAL') statusColor = 'var(--primary, #F5B942)';
          else if (l.status === 'MISSED') statusColor = '#dc2626';

          return `
            <div style="display: flex; justify-content: space-between; align-items: center; padding: 10px 14px; background: var(--bg-card, #1F1F1F); border-radius: 6px; margin-bottom: 8px; border: 1px solid var(--border-subtle, #262626);">
              <div>
                <strong style="color: #ffffff; font-size: 0.92rem;">${escapeHtml(l.habitTitle)}</strong>
                ${l.note ? `<div style="font-size: 0.8rem; color: var(--text-secondary, #94a3b8); margin-top: 2px;">Note: ${escapeHtml(l.note)}</div>` : ''}
              </div>
              <span style="font-weight: 700; font-size: 0.85rem; color: ${statusColor};">${l.status} (${l.completionPercentage}%)</span>
            </div>
          `;
        }).join('');
      }

      document.getElementById('calendar-day-modal').style.display = 'flex';
    } catch (err) {
      API.showToast(err.message, 'error');
    }
  }

  function closeCalendarDayModal() {
    document.getElementById('calendar-day-modal').style.display = 'none';
  }

  /* ==========================================================================
     Reports View & Data Export
     ========================================================================== */
  async function loadReportsView() {
    const cardEl = document.getElementById('report-summary-card');
    if (!cardEl) return;

    try {
      const report = await API.reports.getSummary('WEEKLY');
      if (!report) return;

      cardEl.innerHTML = `
        <div style="display:flex; justify-content:space-between; align-items:center; border-bottom: 1px solid var(--border-default, #262626); padding-bottom: 16px; margin-bottom: 20px;">
          <div>
            <h3 style="font-size: 1.25rem; font-weight: 700; color: #ffffff; margin:0;">${escapeHtml(report.periodTitle)}</h3>
            <span style="font-size: 0.85rem; color: var(--text-secondary, #94a3b8);">${report.startDate} to ${report.endDate}</span>
          </div>
          <div style="text-align: right;">
            <div style="font-size: 1.8rem; font-weight: 800; color: var(--primary, #F5B942);">${report.overallCompletionPercentage}%</div>
            <div style="font-size: 0.78rem; color: var(--text-muted, #64748b);">OVERALL CONSISTENCY</div>
          </div>
        </div>

        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 14px; margin-bottom: 24px;">
          <div style="background: var(--bg-surface, #171717); padding: 12px; border-radius: 6px; border: 1px solid var(--border-subtle, #262626);">
            <div style="font-size: 0.75rem; color: var(--text-muted, #64748b);">DISCIPLINE SCORE</div>
            <div style="font-size: 1.2rem; font-weight: 700; color: #ffffff;">${report.disciplineScore} / 100</div>
          </div>
          <div style="background: var(--bg-surface, #171717); padding: 12px; border-radius: 6px; border: 1px solid var(--border-subtle, #262626);">
            <div style="font-size: 0.75rem; color: var(--text-muted, #64748b);">BEST DAY</div>
            <div style="font-size: 1rem; font-weight: 700; color: #16a34a;">${report.bestDay}</div>
          </div>
          <div style="background: var(--bg-surface, #171717); padding: 12px; border-radius: 6px; border: 1px solid var(--border-subtle, #262626);">
            <div style="font-size: 0.75rem; color: var(--text-muted, #64748b);">WEAKEST DAY</div>
            <div style="font-size: 1rem; font-weight: 700; color: #dc2626;">${report.weakestDay}</div>
          </div>
          <div style="background: var(--bg-surface, #171717); padding: 12px; border-radius: 6px; border: 1px solid var(--border-subtle, #262626);">
            <div style="font-size: 0.75rem; color: var(--text-muted, #64748b);">SCHEDULED ACTIVITIES</div>
            <div style="font-size: 1.2rem; font-weight: 700; color: #ffffff;">${report.totalScheduledActivities}</div>
          </div>
        </div>

        <h4 style="font-size: 1rem; font-weight: 600; color: #ffffff; margin-bottom: 12px;">Habit Breakdown</h4>
        <div style="overflow-x: auto;">
          <table style="width: 100%; border-collapse: collapse; text-align: left; font-size: 0.88rem;">
            <thead>
              <tr style="border-bottom: 1px solid var(--border-default, #262626); color: var(--text-muted, #64748b);">
                <th style="padding: 8px 12px;">Habit Title</th>
                <th style="padding: 8px 12px;">Category</th>
                <th style="padding: 8px 12px; text-align:center;">Scheduled</th>
                <th style="padding: 8px 12px; text-align:center;">Completed</th>
                <th style="padding: 8px 12px; text-align:center;">Rate</th>
                <th style="padding: 8px 12px; text-align:center;">Streak</th>
              </tr>
            </thead>
            <tbody>
              ${report.habitBreakdown.map(h => `
                <tr style="border-bottom: 1px solid var(--border-subtle, #262626);">
                  <td style="padding: 10px 12px; font-weight: 500; color:#ffffff;">${escapeHtml(h.title)}</td>
                  <td style="padding: 10px 12px; color: var(--text-secondary, #94a3b8);">${escapeHtml(h.category)}</td>
                  <td style="padding: 10px 12px; text-align:center;">${h.scheduledCount}</td>
                  <td style="padding: 10px 12px; text-align:center; color:#16a34a;">${h.completedCount}</td>
                  <td style="padding: 10px 12px; text-align:center; font-weight:700; color:var(--primary, #F5B942);">${h.completionPercentage}%</td>
                  <td style="padding: 10px 12px; text-align:center;">🔥 ${h.currentStreak}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      `;
    } catch (err) {
      console.error('Error loading report:', err);
      API.showToast(err.message, 'error');
    }
  }

  function downloadReportCsv() {
    if (!API.isAuthenticated()) {
      openAuthGateModal('reports');
      return;
    }
    const token = (API.getStoredUser() || {}).token;
    // Download via fetch or redirect
    const url = API.reports.getCsvUrl('WEEKLY');
    window.location.href = url;
  }

  function printReport() {
    window.print();
  }

  /* ==========================================================================
     Add / Edit Habit Modal
     ========================================================================== */
  function openAddHabitModal() {
    document.getElementById('habit-form').reset();
    document.getElementById('habit-edit-id').value = '';
    document.getElementById('habit-modal-title').textContent = 'Create Daily Habit Routine';
    document.getElementById('btn-delete-habit').style.display = 'none';
    document.getElementById('days-picker-group').style.display = 'none';
    document.getElementById('habit-modal').style.display = 'flex';
  }

  function openEditHabitModal(id) {
    const habit = habits.find(h => h.id === Number(id));
    if (!habit) return;

    document.getElementById('habit-edit-id').value = habit.id;
    document.getElementById('habit-modal-title').textContent = 'Edit Habit Routine';
    document.getElementById('habit-title').value = habit.title;
    document.getElementById('habit-category').value = habit.category || 'General';
    document.getElementById('habit-priority').value = habit.priority || 'MEDIUM';
    document.getElementById('habit-target').value = habit.targetValue || 1;
    document.getElementById('habit-unit').value = habit.unit || 'times';
    document.getElementById('habit-frequency').value = habit.frequencyType || 'DAILY';
    document.getElementById('habit-time').value = habit.preferredTime || 'ANYTIME';

    onFrequencyChange(habit.frequencyType || 'DAILY');

    if (habit.scheduledDays) {
      const scheduled = habit.scheduledDays.split(',').map(d => d.trim().toUpperCase());
      const checkboxes = document.querySelectorAll('input[name="sched-days"]');
      checkboxes.forEach(cb => {
        cb.checked = scheduled.includes(cb.value);
      });
    }

    document.getElementById('btn-delete-habit').style.display = 'block';
    document.getElementById('habit-modal').style.display = 'flex';
  }

  function closeHabitModal() {
    document.getElementById('habit-modal').style.display = 'none';
  }

  function onFrequencyChange(freq) {
    const picker = document.getElementById('days-picker-group');
    if (picker) {
      picker.style.display = freq === 'SPECIFIC_DAYS' ? 'block' : 'none';
    }
  }

  async function saveHabit(e) {
    e.preventDefault();
    const editId = document.getElementById('habit-edit-id').value;
    const title = document.getElementById('habit-title').value.trim();
    const category = document.getElementById('habit-category').value;
    const priority = document.getElementById('habit-priority').value;
    const targetValue = parseInt(document.getElementById('habit-target').value, 10);
    const unit = document.getElementById('habit-unit').value.trim();
    const frequencyType = document.getElementById('habit-frequency').value;
    const preferredTime = document.getElementById('habit-time').value;

    let scheduledDays = null;
    if (frequencyType === 'SPECIFIC_DAYS') {
      const checkedBoxes = Array.from(document.querySelectorAll('input[name="sched-days"]:checked'));
      if (checkedBoxes.length === 0) {
        API.showToast('Please select at least one day for specific day scheduling.', 'error');
        return;
      }
      scheduledDays = checkedBoxes.map(cb => cb.value).join(',');
    }

    const payload = {
      title,
      category,
      priority,
      targetValue,
      unit,
      frequencyType,
      scheduledDays,
      preferredTime
    };

    const isAuth = API.isAuthenticated();

    try {
      if (editId) {
        if (isAuth) {
          await API.habits.update(editId, payload);
        } else {
          API.guestStorage.updateHabit(editId, payload);
        }
        API.showToast('Habit updated successfully!', 'success');
      } else {
        if (isAuth) {
          await API.habits.create(payload);
        } else {
          API.guestStorage.createHabit(payload);
        }
        API.showToast('New habit routine created!', 'success');
      }

      closeHabitModal();
      await loadTrackerData();
    } catch (err) {
      API.showToast(err.message, 'error');
    }
  }

  async function deleteCurrentHabit() {
    const editId = document.getElementById('habit-edit-id').value;
    if (!editId) return;

    if (!confirm('Are you sure you want to delete this habit? Historical logs for this habit will also be removed.')) return;

    const isAuth = API.isAuthenticated();
    try {
      if (isAuth) {
        await API.habits.delete(editId);
      } else {
        API.guestStorage.deleteHabit(editId);
      }

      closeHabitModal();
      API.showToast('Habit deleted.', 'info');
      await loadTrackerData();
    } catch (err) {
      API.showToast(err.message, 'error');
    }
  }

  /* ==========================================================================
     Filters, Auth Gates, and Helpers
     ========================================================================== */
  function filterByCategory(category) {
    currentCategoryFilter = category;
    const pills = document.querySelectorAll('.filter-pill');
    pills.forEach(p => {
      if (p.getAttribute('data-cat') === category) p.classList.add('active');
      else p.classList.remove('active');
    });
    renderTrackerTable();
  }

  function filterHabits(cat) {
    filterByCategory(cat);
  }

  function refreshDashboard() {
    loadTrackerData();
    API.showToast('Tracker refreshed.', 'info');
  }

  function openAuthGateModal(featureName) {
    document.getElementById('auth-gate-modal').style.display = 'flex';
  }

  function closeAuthGateModal() {
    document.getElementById('auth-gate-modal').style.display = 'none';
  }

  function logout() {
    API.clearSession();
    API.showToast('Logged out.', 'info');
    setTimeout(() => window.location.reload(), 300);
  }

  function formatDate(d) {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  function isHabitScheduledFor(habit, dateObj) {
    if (habit.frequencyType === 'DAILY' || !habit.frequencyType) {
      return true;
    }
    if (habit.frequencyType === 'SPECIFIC_DAYS' && habit.scheduledDays) {
      const dayNames = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
      const targetDay = dayNames[dateObj.getDay()];
      const scheduled = habit.scheduledDays.split(',').map(d => d.trim().toUpperCase());
      return scheduled.includes(targetDay);
    }
    return true;
  }

  function getCategorySvg(cat) {
    if (!cat) return '⚡';
    const c = cat.toLowerCase();
    if (c.includes('study')) return '📚';
    if (c.includes('code') || c.includes('coding') || c.includes('java')) return '💻';
    if (c.includes('fit') || c.includes('gym') || c.includes('exercise')) return '🏋️';
    if (c.includes('yoga') || c.includes('health') || c.includes('diet')) return '🥗';
    if (c.includes('read') || c.includes('book')) return '📖';
    if (c.includes('finance') || c.includes('money')) return '💰';
    return '⚡';
  }

  function escapeHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  return {
    init,
    switchView,
    changeWeek,
    cycleHabitStatus,
    handleCellContextMenu,
    openPartialModal,
    closePartialModal,
    calcDerivedPercentage,
    submitPartialLog,
    openAddHabitModal,
    openEditHabitModal,
    closeHabitModal,
    onFrequencyChange,
    saveHabit,
    deleteCurrentHabit,
    filterByCategory,
    filterHabits,
    refreshDashboard,
    openAuthGateModal,
    closeAuthGateModal,
    openAddGoalModal,
    closeGoalModal,
    saveGoal,
    incrementGoal,
    deleteGoal,
    changeCalendarMonth,
    openCalendarDayModal,
    closeCalendarDayModal,
    downloadReportCsv,
    printReport,
    loadAnalyticsView,
    logout
  };
})();

document.addEventListener('DOMContentLoaded', App.init);
