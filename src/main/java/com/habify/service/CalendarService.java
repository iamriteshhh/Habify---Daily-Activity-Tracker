package com.habify.service;

import com.habify.dto.CalendarDayDTO;
import com.habify.dto.CalendarMonthDTO;
import com.habify.dto.HabitLogResponse;
import com.habify.entity.Habit;
import com.habify.entity.HabitLog;
import com.habify.entity.HabitStatus;
import com.habify.repository.HabitLogRepository;
import com.habify.repository.HabitRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CalendarService
 * 
 * Generates monthly habit activity heatmaps and day drill-down inspections.
 */
@Service
public class CalendarService {

    private final HabitLogRepository habitLogRepository;
    private final HabitRepository habitRepository;
    private final HabitLogService habitLogService;

    public CalendarService(HabitLogRepository habitLogRepository,
                           HabitRepository habitRepository,
                           HabitLogService habitLogService) {
        this.habitLogRepository = habitLogRepository;
        this.habitRepository = habitRepository;
        this.habitLogService = habitLogService;
    }

    /**
     * Builds month-level heatmap data and activity summaries for every day of the month.
     */
    public CalendarMonthDTO getMonthView(Long userId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        List<Habit> activeHabits = habitRepository.findByUserIdAndIsActiveTrue(userId);
        List<HabitLog> monthLogs = habitLogRepository.findByUserIdAndLogDateBetween(userId, startDate, endDate);

        Map<LocalDate, List<HabitLog>> logsByDate = monthLogs.stream()
                .collect(Collectors.groupingBy(HabitLog::getLogDate));

        List<CalendarDayDTO> dayDTOs = new ArrayList<>();
        int totalScheduledSum = 0;
        int totalEarnedSum = 0;
        int totalActivitiesLogged = 0;

        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate date = ym.atDay(day);
            List<HabitLog> dayLogs = logsByDate.getOrDefault(date, Collections.emptyList());

            int scheduledCount = 0;
            for (Habit h : activeHabits) {
                LocalDate habitCreated = h.getCreatedAt() != null ? h.getCreatedAt().toLocalDate() : date;
                if (!date.isBefore(habitCreated) && h.isScheduledFor(date)) {
                    scheduledCount++;
                }
            }

            int completedCount = 0;
            int partialCount = 0;
            int missedCount = 0;
            int earnedPct = 0;

            for (HabitLog l : dayLogs) {
                totalActivitiesLogged++;
                if (l.getStatus() == HabitStatus.COMPLETED) {
                    completedCount++;
                    earnedPct += 100;
                } else if (l.getStatus() == HabitStatus.PARTIAL) {
                    partialCount++;
                    earnedPct += (l.getCompletionPercentage() != null ? l.getCompletionPercentage() : 50);
                } else if (l.getStatus() == HabitStatus.MISSED) {
                    missedCount++;
                }
            }

            int dayPct = scheduledCount > 0 ? Math.min(100, Math.round((float) earnedPct / scheduledCount)) : 0;
            totalScheduledSum += scheduledCount;
            totalEarnedSum += earnedPct;

            int intensity = 0;
            if (completedCount > 0 || partialCount > 0) {
                if (dayPct >= 80) intensity = 3;
                else if (dayPct >= 50) intensity = 2;
                else intensity = 1;
            }

            List<HabitLogResponse> mappedLogs = dayLogs.stream()
                    .map(habitLogService::mapEntityToResponse)
                    .collect(Collectors.toList());

            dayDTOs.add(new CalendarDayDTO(
                    date,
                    day,
                    date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    scheduledCount,
                    completedCount,
                    partialCount,
                    missedCount,
                    dayPct,
                    intensity,
                    mappedLogs
            ));
        }

        int avgCompletion = totalScheduledSum > 0 ? Math.round((float) totalEarnedSum / totalScheduledSum) : 0;

        return new CalendarMonthDTO(
                year,
                month,
                ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                totalActivitiesLogged,
                avgCompletion,
                dayDTOs
        );
    }

    /**
     * Retrieves specific day details.
     */
    public CalendarDayDTO getDayDetail(Long userId, LocalDate date) {
        YearMonth ym = YearMonth.from(date);
        CalendarMonthDTO month = getMonthView(userId, ym.getYear(), ym.getMonthValue());
        return month.getDays().stream()
                .filter(d -> d.getDate().equals(date))
                .findFirst()
                .orElse(null);
    }
}
