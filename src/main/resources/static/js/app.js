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
      document.getElementById('kpi-streak-val').textContent = `${dashSummary.bestCurrentStreak || 0} Days`;
      document.getElementById('kpi-tasks-done').textContent = `${dashSummary.completedTodayCount} / ${dashSummary.scheduledTodayCount}`;
      document.getElementById('kpi-discipline-score').textContent = (!habits || habits.length === 0)
        ? '-- / 100'
        : `${dashSummary.disciplineScore} / 100`;
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
    document.getElementById('kpi-streak-val').textContent = `0 Days`;
    document.getElementById('kpi-tasks-done').textContent = `${completedCount} / ${scheduledCount}`;
    document.getElementById('kpi-discipline-score').textContent = habits.length > 0 ? `${todayPct} / 100` : `-- / 100`;
  }

  /* ==========================================================================
     Signature Weekly Matrix Table Rendering
     ========================================================================== */
  function renderTrackerTable() {
    const tableContainerEl = document.getElementById('tracker-table-container');
    const emptyContainerEl = document.getElementById('tracker-empty-container');
    const theadEl = document.getElementById('tracker-thead');
    const tbodyEl = document.getElementById('tracker-tbody');
    const tfootEl = document.getElementById('tracker-tfoot');
    if (!tbodyEl) return;

    renderCategoryFilterShelf();

    // 1. Handle complete zero-habit onboarding state
    if (!habits || habits.length === 0) {
      if (tableContainerEl) tableContainerEl.style.display = 'none';
      if (emptyContainerEl) {
        emptyContainerEl.style.display = 'flex';
        emptyContainerEl.innerHTML = `
          <div class="onboarding-icon-ring">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
            </svg>
          </div>
          <h3 class="onboarding-title">Build Your Consistency Matrix</h3>
          <p class="onboarding-desc">Track personal daily habits, mark completions, monitor weekly streaks, and build lasting momentum.</p>
          <div class="onboarding-actions">
            <button class="btn btn-primary" onclick="App.openAddHabitModal()">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              Create Your First Routine
            </button>
            <button class="btn btn-secondary" onclick="App.seedStarterHabits()">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>
              Quick Start with 5 Starter Habits
            </button>
          </div>
        `;
      }
      return;
    }

    // Habits exist, show table container and hide empty container
    if (tableContainerEl) tableContainerEl.style.display = 'block';
    if (emptyContainerEl) emptyContainerEl.style.display = 'none';

    // 1. Build Header
    const todayStr = formatDate(new Date());
    const dayShortNames = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];

    let headerHtml = `
      <tr>
        <th class="th-task-col">TASKS</th>
        <th class="th-completion-col">COMPLETION %</th>
    `;

    weekDates.forEach((dateObj, idx) => {
      const dateStr = formatDate(dateObj);
      const isToday = dateStr === todayStr;
      const dayNum = dateObj.getDate();
      const monthShort = dateObj.toLocaleDateString('en-US', { month: 'short' }).toUpperCase();

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
          <td colspan="10" class="filter-empty-cell">
            <div class="filter-empty-box">
              <span style="font-size: 0.9rem; color: var(--text-secondary);">No habits configured under <strong>"${escapeHtml(currentCategoryFilter)}"</strong>.</span>
              <button class="btn btn-ghost btn-sm" onclick="App.filterByCategory('ALL')" style="margin-left: 12px; color: var(--primary);">Clear Filter</button>
            </div>
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
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>
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
            let icon = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="var(--primary, #F5B942)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>';
            if (i.type === 'SUCCESS') {
              borderColor = '#16a34a';
              icon = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#16a34a" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>';
            } else if (i.type === 'WARNING') {
              borderColor = '#dc2626';
              icon = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#dc2626" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>';
            }

            return `
              <div style="background: var(--bg-card, #1F1F1F); border-left: 3px solid ${borderColor}; padding: 14px; border-radius: 6px;">
                <div style="display:flex; align-items:center; gap:8px; margin-bottom: 4px;">
                  <span style="display:inline-flex; align-items:center;">${icon}</span>
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
            <div style="width: 48px; height: 48px; border-radius: 50%; background: rgba(245, 185, 66, 0.1); border: 1px solid rgba(245, 185, 66, 0.25); display: inline-flex; align-items: center; justify-content: center; margin-bottom: 12px;">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--primary, #F5B942)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/>
              </svg>
            </div>
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
        const iconSvg = getAchievementSvg(a.id, unlocked);
        return `
          <div class="content-panel" style="padding: 20px; text-align: center; border: 1px solid ${unlocked ? 'rgba(245, 185, 66, 0.4)' : 'var(--border-subtle, #262626)'}; opacity: ${unlocked ? '1' : '0.65'};">
            <div>${iconSvg}</div>
            <h4 style="font-size: 1.05rem; font-weight: 600; color: ${unlocked ? '#ffffff' : 'var(--text-secondary, #94a3b8)'}; margin-bottom: 4px;">${escapeHtml(a.title)}</h4>
            <p style="font-size: 0.82rem; color: var(--text-secondary, #94a3b8); margin-bottom: 14px; min-height: 36px;">${escapeHtml(a.description)}</p>
            
            <div class="mini-progress-track" style="height: 6px; border-radius: 3px; margin-bottom: 8px;">
              <div class="mini-progress-fill" style="width: ${a.progressPercentage}%; background: ${unlocked ? '#16a34a' : 'var(--primary, #F5B942)'};"></div>
            </div>

            <div style="font-size: 0.78rem; color: var(--text-muted, #64748b);">
              ${unlocked ? '<strong style="color:#16a34a; display:inline-flex; align-items:center; gap:4px;"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#16a34a" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg> UNLOCKED</strong>' : `${a.currentProgress} / ${a.targetProgress}`}
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
                  <td style="padding: 10px 12px; text-align:center;"><span style="display:inline-flex; align-items:center; justify-content:center; gap:4px;"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="var(--primary, #F5B942)" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/></svg>${h.currentStreak}</span></td>
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

  async function downloadReportCsv() {
    if (!API.isAuthenticated()) {
      openAuthGateModal('reports');
      return;
    }
    const user = API.getStoredUser();
    if (!user || !user.token) {
      API.showToast('Please log in to export CSV reports.', 'error');
      return;
    }

    try {
      API.showToast('Generating CSV export...', 'info');
      const response = await fetch('/api/reports/csv?type=WEEKLY', {
        headers: {
          'Authorization': `Bearer ${user.token}`
        }
      });

      if (!response.ok) {
        const err = await response.json().catch(() => ({}));
        throw new Error(err.message || 'Failed to export CSV report');
      }

      const blob = await response.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = downloadUrl;
      a.download = `Habify_Weekly_Report_${new Date().toISOString().slice(0, 10)}.csv`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(downloadUrl);
      API.showToast('CSV report downloaded successfully!', 'success');
    } catch (err) {
      API.showToast(err.message, 'error');
    }
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
  function renderCategoryFilterShelf() {
    const shelf = document.getElementById('category-filter-shelf');
    if (!shelf) return;

    const categorySet = new Set();
    categorySet.add('ALL');

    if (habits && habits.length > 0) {
      habits.forEach(h => {
        if (h.category && h.category.trim()) {
          categorySet.add(h.category.trim());
        }
      });
    } else {
      categorySet.add('Coding');
      categorySet.add('Study');
      categorySet.add('Fitness');
      categorySet.add('Health');
      categorySet.add('Reading');
    }

    const categories = Array.from(categorySet);
    if (!categories.some(c => c.toLowerCase() === currentCategoryFilter.toLowerCase())) {
      currentCategoryFilter = 'ALL';
    }

    shelf.innerHTML = categories.map(cat => {
      const isActive = cat.toLowerCase() === currentCategoryFilter.toLowerCase();
      const label = cat === 'ALL' ? 'All Habits' : escapeHtml(cat);
      return `<button class="filter-pill ${isActive ? 'active' : ''}" data-cat="${escapeHtml(cat)}" onclick="App.filterByCategory('${escapeHtml(cat)}')">${label}</button>`;
    }).join('');
  }

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

  async function seedStarterHabits() {
    const starters = [
      { title: 'Core Java Practice', category: 'Coding', priority: 'HIGH', targetValue: 1, unit: 'sessions', frequencyType: 'DAILY', preferredTime: 'MORNING' },
      { title: 'Gym & Strength Workout', category: 'Fitness', priority: 'HIGH', targetValue: 1, unit: 'workout', frequencyType: 'SPECIFIC_DAYS', scheduledDays: 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY', preferredTime: 'EVENING' },
      { title: 'Yoga & Mindful Breathing', category: 'Health', priority: 'MEDIUM', targetValue: 20, unit: 'mins', frequencyType: 'DAILY', preferredTime: 'MORNING' },
      { title: 'DSA Problem Solving', category: 'Study', priority: 'HIGH', targetValue: 2, unit: 'problems', frequencyType: 'DAILY', preferredTime: 'NIGHT' },
      { title: 'Read Technical Books', category: 'Reading', priority: 'LOW', targetValue: 15, unit: 'pages', frequencyType: 'DAILY', preferredTime: 'NIGHT' }
    ];

    const isAuth = API.isAuthenticated();
    try {
      if (isAuth) {
        for (const item of starters) {
          await API.habits.create(item);
        }
      } else {
        for (const item of starters) {
          API.guestStorage.createHabit(item);
        }
      }
      API.showToast('5 starter habits loaded! Build your streak.', 'success');
      await loadTrackerData();
    } catch (err) {
      API.showToast('Failed to seed habits: ' + err.message, 'error');
    }
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

  async function logout() {
    try {
      await API.auth.logout();
    } catch (e) {
      console.warn('Logout request failed:', e);
    }
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
    if (!cat) {
      return '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>';
    }
    const c = cat.toLowerCase();
    if (c.includes('code') || c.includes('coding') || c.includes('java') || c.includes('dsa') || c.includes('dev')) {
      return '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>';
    }
    if (c.includes('study') || c.includes('learn') || c.includes('course')) {
      return '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c3 3 9 3 12 0v-5"/></svg>';
    }
    if (c.includes('fit') || c.includes('gym') || c.includes('exercise') || c.includes('workout')) {
      return '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 5v14M18 5v14M2 9v6M22 9v6M6 12h12"/></svg>';
    }
    if (c.includes('yoga') || c.includes('health') || c.includes('diet') || c.includes('meditat')) {
      return '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"/></svg>';
    }
    if (c.includes('read') || c.includes('book')) {
      return '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1-0-5H20"/></svg>';
    }
    if (c.includes('finance') || c.includes('money')) {
      return '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>';
    }
    return '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>';
  }

  function getAchievementSvg(id, unlocked) {
    const strokeColor = unlocked ? 'var(--primary, #F5B942)' : '#64748b';
    const bgFill = unlocked ? 'rgba(245, 185, 66, 0.12)' : 'rgba(255, 255, 255, 0.04)';
    const glowStyle = unlocked ? 'box-shadow: 0 0 16px rgba(245, 185, 66, 0.25); border: 1px solid rgba(245, 185, 66, 0.4);' : 'border: 1px solid rgba(255, 255, 255, 0.08);';

    let path = '';
    switch (id) {
      case 'first_step':
        path = '<path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/><line x1="4" y1="22" x2="4" y2="15"/>';
        break;
      case 'week_warrior':
        path = '<path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/>';
        break;
      case 'routine_builder':
        path = '<polygon points="12 2 2 7 12 12 22 7 12 2"/><polyline points="2 17 12 22 22 17"/><polyline points="2 12 12 17 22 12"/>';
        break;
      case 'century_club':
        path = '<circle cx="12" cy="8" r="7"/><polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88"/>';
        break;
      case 'discipline_elite':
        path = '<path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/><path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/><path d="M4 22h16"/><path d="M10 14.66V17c0 .55-.45 1-1 1H8c-.55 0-1 .45-1 1v1c0 .55.45 1 1 1h8c.55 0 1-.45 1-1v-1c0-.55-.45-1-1-1h-1c-.55 0-1-.45-1-1v-2.34"/><path d="M18 2H6v7a6 6 0 0 0 12 0V2z"/>';
        break;
      case 'goal_getter':
        path = '<circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2"/>';
        break;
      default:
        path = '<polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>';
    }

    return `
      <div style="width: 52px; height: 52px; border-radius: 50%; background: ${bgFill}; ${glowStyle} display: inline-flex; align-items: center; justify-content: center; margin-bottom: 12px;">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="${strokeColor}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          ${path}
        </svg>
      </div>
    `;
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
    seedStarterHabits,
    renderCategoryFilterShelf,
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
