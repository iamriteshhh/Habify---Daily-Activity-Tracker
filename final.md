HABIFY
Full Implementation Plan: 70% → 100%
Project Philosophy
Habify is a personal discipline and daily activity tracker that turns everyday actions into measurable progress.

The final application should prioritize:
Simple tracking → Accurate data → Meaningful analytics → Motivation → Long-term consistency
The website must feel like a real productivity product, not an AI-generated dashboard.
1. FINAL PRODUCT REQUIREMENTS
1.1 Core Requirements
Habify must allow a user to:
- Create personal habits/activities
- Set frequency
- Set target
- Set category
- Set priority
- Set preferred time
- Track daily activity
- Mark activity as:
  - Completed
  - Partial
  - Missed
- See daily completion percentage
- See individual habit completion percentage
- See weekly completion percentage
- See current streak
- See longest streak
- See historical activity
- View analytics
- Set goals
- Earn achievements
- View calendar/history
- Generate reports
2. THE MOST IMPORTANT UI DECISION
Your hand-drawn design becomes the primary tracker interface.






   1




Main tracker
┌─────────────────────────────────────────────────────────────────────────────┐
│ HABIFY                                      Week: Sep 14 - Sep 20             │
├──────────────┬──────────────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┬──────┤
│ TASKS        │ COMPLETION % │ MON │ TUE │ WED │ THU │ FRI │ SAT │ SUN │ WEEK │
├──────────────┼──────────────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼──────┤
│ Java         │ 50%          │  ✓  │  ✓  │  ◐  │  ×  │  ✓  │  ○  │  ✓  │ 71%  │
│ Exercise     │ 30%          │  ✓  │  ×  │  ◐  │  ×  │  ○  │  ✓  │  ×  │ 43%  │
│ Yoga         │ 90%          │  ✓  │  ✓  │  ✓  │  ✓  │  ◐  │  ✓  │  ✓  │ 93%  │
│ Study        │ 40%          │  ◐  │  ×  │  ✓  │  ○  │  ✓  │  ×  │  ◐  │ 57%  │
├──────────────┼──────────────┼─────┼─────┼─────┼─────┼─────┼─────┼─────┼──────┤
│ DAILY REPORT │              │ 75% │ 50% │ 80% │ 40% │ 70% │ 60% │ 65% │ 63%  │
└──────────────┴──────────────┴─────┴─────┴─────┴─────┴─────┴─────┴─────┴──────┘
This is not just a design element.
This is the central product experience.
3. UI/UX DESIGN SYSTEM
3.1 Design Goal
The website should make someone think:
"This is clean. I can actually use this every day."

Not:
"An AI generated a SaaS dashboard."

Therefore:
No:
- Neon colors
- Excessive gradients
- Glowing cards
- Giant rounded rectangles
- Excessive glassmorphism
- Random decorative blobs
- Excessive animations
- Emoji everywhere
- 15 different colors
- Fake futuristic UI
4. HABIFY COLOR SYSTEM
Since you specifically want the black + warm yellow identity, keep that.
Primary
Warm Yellow
#F5B942
Dark structure
Black
#0F0F0F

Dark Surface
#171717

Secondary Surface
#1F1F1F
Light option for certain pages
Background
#F7F7F5

Surface
#FFFFFF

Text
#171717
Status colors
Completed → muted green
Partial   → warm amber/yellow
Missed    → muted red
Pending   → neutral gray
Rest Day  → soft gray
The colors should be functional, not decorative.
5. TYPOGRAPHY
Use a normal modern web font.
Preferred
Inter
Fallback:
Inter,
-apple-system,
BlinkMacSystemFont,
"Segoe UI",
Roboto,
Helvetica,
Arial,
sans-serif
One main typeface throughout the site.
Font hierarchy
Body       15-16px
Labels     13-14px
Buttons    14-15px
Heading    24-32px
Hero       40-52px maximum
No unnecessarily huge text.
6. ICONS
Current code uses a lot of emojis.
That should be reduced.
For example:
📚 Study
🏋️ Fitness
🥗 Health
💻 Coding
🔥 Streak
should eventually become a consistent SVG icon set.
Lucide Icons would be a good choice.
Emoji can remain where they genuinely add personality, but they should not be the primary UI icon system.
7. RESPONSIVE DESIGN
Desktop
The weekly tracker gets maximum horizontal space.
Tablet
Tracker becomes horizontally scrollable.
Mobile
Don't try to squeeze seven days into 320px.
Use:
Horizontal scrolling
or a mobile-specific:
Habit
↓
Mon Tue Wed...
view.
The data must remain usable.
8. CURRENT CODE AUDIT
Your current implementation already has:
Backend
Spring Boot
Java 17
Spring Data JPA
Hibernate
MySQL
H2
REST APIs
Entities
User
Habit
HabitLog
Core services
UserService
HabitService
HabitLogService
StreakService
ScoringService
AnalyticsService
Frontend
index.html
auth.html
dashboard.html
app.js
api.js
style.css
Already implemented
- Habit CRUD
- Habit categories
- Priorities
- Daily frequency
- Specific weekday frequency
- Habit activation/deactivation
- Daily logs
- Completed/Partial/Missed
- Completion percentage
- Streak calculation
- Longest streak
- Weekly progress
- Discipline score
- User registration
- Login
- BCrypt password hashing
- Profile
- MySQL schema
- H2 testing
- Unit tests
- Horizontal weekly tracker UI
So the remaining work is primarily completion, correction, expansion and polish, rather than rebuilding Habify.
9. MODULE 1
CORE HABIT & TRACKING ENGINE
Current: ~90%
Goal: 100%
Your module is the foundation of everything.
9.1 Habit Management
Finish and verify:
- Create
- Read
- Update
- Activate/deactivate
- Delete
- Category
- Priority
- Frequency
- Target
- Unit
- Preferred time
9.2 Scheduling
A habit must correctly answer:
"Should this habit be tracked today?"

Examples:
Daily
MON ✓
TUE ✓
WED ✓
THU ✓
FRI ✓
SAT ✓
SUN ✓
Monday/Wednesday/Friday
MON ✓
TUE -
WED ✓
THU -
FRI ✓
SAT -
SUN -
A non-scheduled day must never become a missed day.
9.3 Target-based progress
This is an important improvement.
Currently you have:
targetValue = 2
unit = hours
but the user can effectively enter a percentage.
We should make the target meaningful.
Example:
Target = 2 hours

User enters:
Actual = 1.5 hours

System calculates:

1.5 / 2 × 100
= 75%
So the user doesn't have to invent:
"I'll call this 75%."

The system calculates it.
For count-based habits:
Target = 20 push-ups
Actual = 15

Completion = 75%
9.4 Status rules
Completed → 100%

Partial → 1-99%

Missed → 0%
But the final business rule for streak treatment of Partial must be explicitly chosen.
I recommend:
Partial ≥ 50% counts toward streak.

This matches your current StreakService behavior.
9.5 One log per habit per day
Maintain:
Habit + Date = Unique
Already supported by the database.
9.6 Historical preservation
Normal user action should be:
Deactivate Habit
rather than permanently deleting it.
Permanent deletion should be clearly destructive.
That prevents:
"I deleted my old habit and suddenly my history disappeared."

10. MODULE 2
USER / AUTHENTICATION / PROFILE
Current: ~75-80%
Goal: 100%
This needs the biggest security upgrade.
10.1 Registration
Required:
Full Name
Email
Username
Password
Validate:
- Empty values
- Length
- Email format
- Duplicate email
- Duplicate username
- Password strength
11. REAL EMAIL VERIFICATION
This requires an important distinction.
A regex can determine:
example@gmail.com

looks like a valid email.
It cannot reliably determine that the mailbox actually exists.
Therefore the correct solution is:
Step 1
Check format.
abc@gmail.com
       ↓
valid format
Step 2
Create account as:
emailVerified = false
Step 3
Send verification email.
Verify your Habify account
[Verify Email]
Step 4
User clicks link.
emailVerified = true
Step 5
Unlock persistent analytics/dashboard features.
This catches the problem you're concerned about:
Someone types abc123@gmail.com when that mailbox doesn't belong to them.

They won't be able to verify it.
12. FORGOT PASSWORD
Complete flow:
Forgot Password
       ↓
Enter Email
       ↓
System generates reset token
       ↓
Email sent
       ↓
User opens link
       ↓
New Password
       ↓
Token invalidated
       ↓
Login
Token should:
- Expire
- Be single-use
- Never expose the password
- Never be stored as plain password
13. AUTHENTICATION SECURITY
Current system has:
X-User-Id
and a custom token containing the user ID.
That is not sufficient security.
Someone could potentially manipulate the user ID.
Final system should use:
Spring Security
+
JWT
Flow:
Login
 ↓
Validate credentials
 ↓
Generate signed JWT
 ↓
Browser stores token
 ↓
Request
 ↓
JWT Filter
 ↓
Validate token
 ↓
Extract authenticated user
 ↓
Controller
No protected endpoint should trust:
?userId=5
or:
X-User-Id: 5
as the source of authentication.
14. MODULE 3
DATABASE & PERSISTENCE
Current: ~90%
Goal: 100%
Keep MySQL.
I would not switch Habify to MongoDB.
Your current structure is naturally relational:
User
  ↓
Habit
  ↓
HabitLog
You already use:
JPA
Hibernate
Repositories
Foreign Keys
Unique Constraints
MySQL fits the project extremely well and is easier to defend academically.
Final database
users
habits
habit_logs
goals
achievements
Potential future support:
password_reset_tokens
email_verification_tokens
Database requirements
Ensure:
- Foreign keys
- Unique email
- Unique username
- Unique habit/date
- Proper indexes
- Correct data types
- No orphan records
- User ownership
And:
DB_USER
DB_PASSWORD
JWT_SECRET
MAIL_USERNAME
MAIL_PASSWORD
must not be hard-coded into GitHub.
15. MODULE 4
ANALYTICS / SCORING / INSIGHTS
Current: ~70%
Goal: 100%
This is where Habify becomes more than a to-do list.
16. DAILY COMPLETION
For a day:
5 scheduled habits

Completed = 3
Partial 50% = 1
Missed = 1
The system should calculate:
(100 + 100 + 100 + 50 + 0) / 5
= 70%
No hard-coded numbers.
17. WEEKLY COMPLETION
For each habit:
Monday    100%
Tuesday   100%
Wednesday 50%
Thursday  0%
Friday    100%
Saturday  -
Sunday    100%
Only scheduled occurrences count.
Then:
Habit Weekly %
=
earned percentage
/
scheduled occurrences
18. DAILY REPORT
The bottom row of your sketch becomes:
MON 75%
TUE 60%
WED 80%
THU 50%
FRI 90%
SAT 40%
SUN 70%
And:
WEEK TOTAL = 66%
Every number must be derived from the underlying logs.
19. GRAPH ACCURACY RULE
This should become one of our non-negotiable completion criteria.
If the database says:
Monday = 60%
Tuesday = 80%
Wednesday = 40%
the graph must show exactly:
60
80
40
Not:
65
75
45
No independent frontend calculation that contradicts backend data.
Prefer:
Database
 ↓
Service
 ↓
DTO
 ↓
API
 ↓
Chart
One source of truth.
20. ANALYTICS TO ADD
Weekly trend
Mon → Sun
Habit comparison
Java       80%
Exercise   65%
Reading    45%
Category distribution
Study
Fitness
Health
Reading
Coding
Best day
Wednesday was your strongest day at 87%.

Weakest day
Saturday was your weakest day at 42%.

Streak insights
Your longest active streak is 12 days.

21. RULE-BASED INSIGHTS
No need for unnecessary AI.
Simple logic is enough.
Examples:
IF weekend completion < weekday completion
→ "Your consistency drops on weekends."
IF habit streak >= 7
→ "You've maintained this habit for a week."
IF category completion > 80
→ "Your strongest category this week is Study."
This is actually more appropriate for a BCA PBL than stuffing ChatGPT into every corner.
22. MODULE 5
FRONTEND & DASHBOARD
Current: ~75%
Goal: 100%
This is where we'll spend a lot of time.
23. PUBLIC WEBSITE
Landing page:
Habify
────────────────────────────────

Build consistency.
Understand your progress.

[Start Tracking] [Log In]

How Habify Works
1. Add habits
2. Track activity
3. Understand progress
4. Improve consistency

Features
• Habit Tracking
• Streaks
• Weekly Analytics
• Goals
• Achievements
No need for an enormous marketing page.
Keep it clean.
24. GUEST MODE
Your requirement:
Users can add habits and activities without logging in.

So:
Guest
 ↓
Add habit
 ↓
Track today
 ↓
See immediate progress
Use localStorage.
This is good because it removes friction.
25. LOGIN GATE
But:
Weekly Progress
Analytics
History
Long-term data
requires an account.
When guest clicks:
Weekly Report

show:
Want to keep your progress?

Create an account to save your activity
and unlock weekly progress reports.

[Create Account]
[Log In]
[Continue as Guest]
That is much better UX than simply throwing a 401 error.
26. DASHBOARD STRUCTURE
After login:
Habify
────────────────────────────────────────────
Today | Habits | Analytics | Goals | History
────────────────────────────────────────────

Today
Your progress today: 72%

────────────────────────────────────────────

WEEKLY HABIT TRACKER

Tasks | % | Mon | Tue | Wed | Thu | Fri | Sat | Sun | Week

...

────────────────────────────────────────────

Weekly Summary

[Completion]
[Discipline Score]
[Current Streak]
[Best Day]

────────────────────────────────────────────

Charts

────────────────────────────────────────────

Insights
The tracker remains the star.
Don't bury it under 12 KPI cards.
27. DASHBOARD BUG FIXING
The current dashboard.html expects functions such as:
App.switchView()
App.refreshDashboard()
App.filterHabits()
while your current app.js does not expose those functions.
That needs to be resolved.
Either:
Option A
Make dashboard.html use the existing application architecture.
Option B
Make app.js properly implement the dashboard API.
I prefer Option A + simplifying the duplicate dashboard architecture.
We don't want:
index.html = one dashboard
dashboard.html = another dashboard
with two competing systems.
28. ONE CONSISTENT WORKSPACE
Final structure should be:
/
Landing / Guest Tracker

/auth
Login / Register / Verification / Reset

/app
Main logged-in workspace

/app/habits
Habit management

/app/analytics
Analytics

/app/goals
Goals

/app/history
History / Calendar

/app/profile
Profile
Or equivalent frontend routing.
The important thing is one coherent application, not disconnected pages.
29. MODULE 6
GOALS / ACHIEVEMENTS / CALENDAR / REPORTS
Current: ~0%
Goal: 100%
This is the largest genuinely missing module.
30. GOALS
User creates:
Goal:
Complete Exercise 20 times

Target:
20

Current:
14

Deadline:
October 1
Progress:
██████████████░░░░░░
70%
Goal states:
ACTIVE
COMPLETED
EXPIRED
31. ACHIEVEMENTS
Rule-based achievements.
Examples:
First Step
Complete first habit.
Week One
Maintain a 7-day streak.
Consistent
Complete 30 scheduled activities.
Discipline
Achieve 90% weekly completion.
Dedicated
Reach a 30-day streak.
100 Club
Complete 100 activities.
Achievements should be calculated from real logs.
32. CALENDAR
Month view:
       September 2026

Mon Tue Wed Thu Fri Sat Sun
    1   2   3   4   5   6
 7  8   9  10  11  12  13
14 15  16  17  18  19  20
...
Days can show activity intensity.
Clicking a day:
September 15

Java          ✓ 100%
Exercise      ◐ 60%
Reading       × 0%
33. REPORTS
User can generate:
Weekly Report
Monthly Report
Containing:
- Overall completion
- Daily percentages
- Habit performance
- Streaks
- Best day
- Weakest day
- Goals
- Achievements
- Insights
Export:
CSV
PDF
CSV can be the simpler first implementation.
PDF comes after the data is correct.
34. STRUCTURAL BUILD ORDER
This is the order I recommend.
PHASE 0
Freeze architecture
Decide:
Java 17
Spring Boot
Spring Data JPA
MySQL
HTML/CSS/JS
Chart.js
Spring Security
JWT
Email SMTP
No unnecessary technology additions.
PHASE 1
Stabilize Current Backend
Before adding features:
Habit
HabitLog
User
Repositories
Services
Controllers
must be stable.
Fix:
- Validation
- Ownership
- Scheduling
- Target handling
- Duplicate logs
- Delete/deactivate behavior
- Status rules
PHASE 2
Authentication Upgrade
Implement:
Spring Security
JWT
Then remove reliance on:
X-User-Id
?userId=
Everything else should use the authenticated identity.
PHASE 3
Habit Tracking Completion
Implement target-based progress.
Example:
Target: 2 hours
Actual: 1.5
       ↓
75%
Then make the weekly tracker use this data.
PHASE 4
Fix the Main UI
Build the actual hand-drawn tracker.
Tasks | Completion | Mon | Tue | Wed | Thu | Fri | Sat | Sun | Weekly
This should become Habify's signature interface.
PHASE 5
Accurate Analytics
Connect:
Habit Logs
      ↓
Analytics Service
      ↓
DTO
      ↓
Frontend
      ↓
Charts
Then manually verify calculations.
PHASE 6
Email System
Implement:
Email verification
Forgot password
Password reset
PHASE 7
Goals
Build:
Goal Entity
Goal Repository
Goal Service
Goal Controller
Goal UI
PHASE 8
Achievements
Build rule engine using existing:
Logs
Streaks
Completion
Goals
PHASE 9
Calendar + History
Use existing log data.
Don't create unnecessary duplicate data.
PHASE 10
Reports
Build:
Weekly report
Monthly report
CSV
PDF
PHASE 11
Complete UI/UX Pass
Only now do the final visual polish.
Check:
- Typography
- Colors
- Spacing
- Buttons
- Icons
- Tables
- Modals
- Empty states
- Error messages
- Loading states
- Mobile layout
PHASE 12
Full Testing
Test the entire system as a real user.
PHASE 13
SECURITY AUDIT
Do this after functionality is finished, exactly as you requested.
Check:
Authentication
Authorization
JWT
Password handling
Email verification
Reset tokens
Input validation
SQL injection
XSS
CSRF where applicable
CORS
Secrets
Database credentials
API ownership
Rate limiting for auth endpoints
H2 console
Error information leakage
35. FINAL ARCHITECTURE
  



42%





36. COMPLETE USER FLOW
  



66%





37. HABIT TRACKING FLOW
This is particularly important for your Module 1.
  



100%





38. 100% COMPLETION CHECKPOINTS
Habify is NOT finished until all of these are true.
🟢 Core
- User can create a habit
- User can edit a habit
- User can deactivate a habit
- User can reactivate a habit
- User can delete a habit intentionally
- Daily habits work
- Selected-day habits work
- Rest days work correctly
- Future logs are rejected
- Duplicate daily logs are prevented
- Target-based progress works
- Completed/Partial/Missed work
- Streaks work
- Longest streak works
🔐 Authentication
- Registration works
- Login works
- BCrypt password hashing works
- JWT authentication works
- Client-supplied user IDs are not trusted
- Protected endpoints require authentication
- Email format validation works
- Email verification works
- Fake/unreachable email cannot become a verified account
- Forgot password works
- Reset token expires
- Reset token is single-use
📊 Analytics
- Daily percentage is accurate
- Individual habit percentage is accurate
- Weekly percentage is accurate
- Discipline score is accurate
- Streak data is accurate
- Graph values exactly match backend data
- Category analytics work
- Best/weakest day works
- Rule-based insights work
🖥️ Website
- Landing page works
- Guest tracking works
- Login works
- Registration works
- Main weekly tracker matches intended design
- Tracker works smoothly
- Add habit modal works
- Edit habit works
- Partial progress works
- Filters work
- Week navigation works
- Loading states exist
- Error states exist
- Empty states exist
- Mobile layout works
- No console errors
🎯 Module 6
- Goals work
- Goal progress is accurate
- Goals can be completed
- Achievements work
- Achievement rules work
- Calendar works
- History works
- Weekly report works
- Monthly report works
- CSV export works
- PDF export works
🎨 Design
- Inter/system typography
- Black + warm yellow identity
- No neon
- No excessive gradients
- No unnecessary glow
- Consistent icons
- Consistent spacing
- Consistent buttons
- Clean cards
- Good contrast
- Fast interaction
- Minimal unnecessary animation
🔒 Security
- JWT secret externalized
- Database credentials externalized
- Email credentials externalized
- No secrets committed to GitHub
- Ownership checks verified
- Input validation verified
- SQL injection review completed
- XSS review completed
- CORS reviewed
- H2 console disabled for demo/production
- Authentication endpoints protected against abuse
- Security audit completed after final integration
39. THE FINAL "DONE" TEST
I would add one brutal but useful rule:
If a feature cannot survive a complete fresh-user test, it isn't finished.

Create a completely new account.
Then:
Register
 ↓
Verify email
 ↓
Login
 ↓
Create 3 habits
 ↓
Set different schedules
 ↓
Track Monday-Sunday
 ↓
Use Completed
 ↓
Use Partial
 ↓
Use Missed
 ↓
Check streak
 ↓
Check daily %
 ↓
Check weekly %
 ↓
Check graph
 ↓
Create goal
 ↓
Earn achievement
 ↓
Open calendar
 ↓
Generate report
 ↓
Logout
 ↓
Login again
 ↓
Confirm data still exists
Then test:
Wrong password
Wrong email
Unverified email
Forgot password
Future date
Rest day
Duplicate log
Inactive habit
Another user's habit ID
Invalid percentage
Invalid target
If all of those behave correctly:
Habify = 100%
🏁 FINAL DEVELOPMENT MAP
If we compress everything into one roadmap:
CURRENT
  │
  ▼
70%
  │
  ├── Fix Core Logic
  │
  ├── Upgrade Authentication
  │
  ├── Target-based Tracking
  │
  ├── Accurate Analytics
  │
  ├── Rebuild Weekly UI
  │
  ├── Email Verification
  │
  ├── Forgot Password
  │
  ├── Goals
  │
  ├── Achievements
  │
  ├── Calendar / History
  │
  ├── Reports / Export
  │
  ├── Responsive Design
  │
  ├── Testing
  │
  └── Security Audit
  │
  ▼
100%
One thing I'd strongly recommend
Don't let the team start coding all of this simultaneously.
The dependency chain should be:
DATABASE
   ↓
AUTHENTICATION
   ↓
HABIT ENGINE
   ↓
TRACKING
   ↓
ANALYTICS
   ↓
WEEKLY UI
   ↓
GOALS
   ↓
ACHIEVEMENTS
   ↓
CALENDAR
   ↓
REPORTS
   ↓
POLISH
   ↓
TESTING
   ↓
SECURITY
   ↓
🎓 FINAL PBL