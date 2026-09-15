package com.habify.service;

import com.habify.dto.HabitPerformanceDTO;
import com.habify.dto.ReportSummaryDTO;
import com.habify.entity.Habit;
import com.habify.entity.HabitLog;
import com.habify.entity.HabitStatus;
import com.habify.repository.HabitLogRepository;
import com.habify.repository.HabitRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ReportService
 * 
 * Generates structured weekly and monthly analytical reports,
 * and streams downloadable CSV summaries.
 */
@Service
public class ReportService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final StreakService streakService;
    private final ScoringService scoringService;

    public ReportService(HabitRepository habitRepository,
                         HabitLogRepository habitLogRepository,
                         StreakService streakService,
                         ScoringService scoringService) {
        this.habitRepository = habitRepository;
        this.habitLogRepository = habitLogRepository;
        this.streakService = streakService;
        this.scoringService = scoringService;
    }

    /**
     * Generates report summary for a weekly or monthly timeframe.
     */
    public ReportSummaryDTO getReportSummary(Long userId, String type, LocalDate referenceDate) {
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;
        String periodTitle;

        if ("MONTHLY".equalsIgnoreCase(type)) {
            YearMonth ym = YearMonth.from(ref);
            startDate = ym.atDay(1);
            endDate = ym.atEndOfMonth();
            periodTitle = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym.getYear();
        } else {
            // Default to WEEKLY: Monday to Sunday
            int dayOfWeek = (ref.getDayOfWeek().getValue() + 6) % 7; // Monday = 0
            startDate = ref.minusDays(dayOfWeek);
            endDate = startDate.plusDays(6);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d");
            periodTitle = "Week of " + startDate.format(fmt) + " - " + endDate.format(fmt) + ", " + endDate.getYear();
        }

        List<Habit> habits = habitRepository.findByUserIdAndIsActiveTrue(userId);
        List<HabitLog> logs = habitLogRepository.findByUserIdAndLogDateBetween(userId, startDate, endDate);

        Map<Long, List<HabitLog>> logsByHabit = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getHabit().getId()));

        Map<LocalDate, List<HabitLog>> logsByDate = logs.stream()
                .collect(Collectors.groupingBy(HabitLog::getLogDate));

        List<HabitPerformanceDTO> breakdowns = new ArrayList<>();
        int totalScheduledSum = 0;
        int completedSum = 0;
        int partialSum = 0;
        int missedSum = 0;
        int earnedPctSum = 0;

        for (Habit h : habits) {
            int scheduledCount = 0;
            int completedCount = 0;
            int partialCount = 0;
            int missedCount = 0;
            int earnedPct = 0;

            List<HabitLog> hLogs = logsByHabit.getOrDefault(h.getId(), Collections.emptyList());
            Map<LocalDate, HabitLog> hLogMap = hLogs.stream()
                    .collect(Collectors.toMap(HabitLog::getLogDate, l -> l, (a, b) -> a));

            LocalDate cur = startDate;
            while (!cur.isAfter(endDate)) {
                if (h.isScheduledFor(cur)) {
                    scheduledCount++;
                    HabitLog log = hLogMap.get(cur);
                    if (log != null) {
                        if (log.getStatus() == HabitStatus.COMPLETED) {
                            completedCount++;
                            earnedPct += 100;
                        } else if (log.getStatus() == HabitStatus.PARTIAL) {
                            partialCount++;
                            earnedPct += (log.getCompletionPercentage() != null ? log.getCompletionPercentage() : 50);
                        } else if (log.getStatus() == HabitStatus.MISSED) {
                            missedCount++;
                        }
                    }
                }
                cur = cur.plusDays(1);
            }

            int habitPct = scheduledCount > 0 ? Math.min(100, Math.round((float) earnedPct / scheduledCount)) : 0;
            int streak = streakService.calculateStreak(h).getCurrentStreak();

            totalScheduledSum += scheduledCount;
            completedSum += completedCount;
            partialSum += partialCount;
            missedSum += missedCount;
            earnedPctSum += earnedPct;

            breakdowns.add(new HabitPerformanceDTO(
                    h.getId(),
                    h.getTitle(),
                    h.getCategory() != null ? h.getCategory() : "General",
                    scheduledCount,
                    completedCount,
                    partialCount,
                    missedCount,
                    habitPct,
                    streak
            ));
        }

        int overallPct = totalScheduledSum > 0 ? Math.min(100, Math.round((float) earnedPctSum / totalScheduledSum)) : 0;
        int disciplineScore = scoringService.calculateDisciplineScore(userId, habits);

        // Find Best & Weakest Days
        String bestDay = "N/A";
        String weakestDay = "N/A";
        int maxDayPct = -1;
        int minDayPct = 101;

        LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            int scheduledOnDay = 0;
            for (Habit h : habits) {
                if (h.isScheduledFor(cur)) scheduledOnDay++;
            }
            if (scheduledOnDay > 0) {
                List<HabitLog> dayLogs = logsByDate.getOrDefault(cur, Collections.emptyList());
                int dayEarned = 0;
                for (HabitLog l : dayLogs) {
                    if (l.getStatus() == HabitStatus.COMPLETED) dayEarned += 100;
                    else if (l.getStatus() == HabitStatus.PARTIAL) dayEarned += (l.getCompletionPercentage() != null ? l.getCompletionPercentage() : 50);
                }
                int dayPct = Math.round((float) dayEarned / scheduledOnDay);
                String dayLabel = cur.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " (" + cur.getDayOfMonth() + " " + cur.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + ")";

                if (dayPct > maxDayPct) {
                    maxDayPct = dayPct;
                    bestDay = dayLabel + " — " + dayPct + "%";
                }
                if (dayPct < minDayPct) {
                    minDayPct = dayPct;
                    weakestDay = dayLabel + " — " + dayPct + "%";
                }
            }
            cur = cur.plusDays(1);
        }

        return new ReportSummaryDTO(
                type != null ? type.toUpperCase() : "WEEKLY",
                periodTitle,
                startDate,
                endDate,
                habits.size(),
                totalScheduledSum,
                completedSum,
                partialSum,
                missedSum,
                overallPct,
                disciplineScore,
                bestDay,
                weakestDay,
                breakdowns
        );
    }

    /**
     * Streams CSV report directly to the client response.
     */
    public void exportCsv(Long userId, String type, LocalDate referenceDate, HttpServletResponse response) throws IOException {
        ReportSummaryDTO summary = getReportSummary(userId, type, referenceDate);

        String filename = "Habify_" + summary.getPeriodType().toLowerCase() + "_report_" + summary.getStartDate() + ".csv";
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        PrintWriter writer = response.getWriter();

        writer.println("Habify Activity Report");
        writer.println("Period: " + summary.getPeriodTitle());
        writer.println("Date Range: " + summary.getStartDate() + " to " + summary.getEndDate());
        writer.println("Overall Consistency: " + summary.getOverallCompletionPercentage() + "%");
        writer.println("Discipline Score: " + summary.getDisciplineScore() + " / 100");
        writer.println("Best Day: " + summary.getBestDay());
        writer.println("Weakest Day: " + summary.getWeakestDay());
        writer.println();

        // Table Header
        writer.println("Habit Title,Category,Scheduled Activities,Completed,Partial,Missed,Completion Rate (%),Current Streak (Days)");

        for (HabitPerformanceDTO h : summary.getHabitBreakdown()) {
            writer.printf("\"%s\",\"%s\",%d,%d,%d,%d,%d%%,%d\n",
                    h.getTitle().replace("\"", "\"\""),
                    h.getCategory().replace("\"", "\"\""),
                    h.getScheduledCount(),
                    h.getCompletedCount(),
                    h.getPartialCount(),
                    h.getMissedCount(),
                    h.getCompletionPercentage(),
                    h.getCurrentStreak()
            );
        }

        writer.println();
        writer.printf("TOTALS,,%d,%d,%d,%d,%d%%,\n",
                summary.getTotalScheduledActivities(),
                summary.getCompletedActivities(),
                summary.getPartialActivities(),
                summary.getMissedActivities(),
                summary.getOverallCompletionPercentage()
        );

        writer.flush();
    }
}
