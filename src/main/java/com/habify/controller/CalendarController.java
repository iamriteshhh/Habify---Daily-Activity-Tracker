package com.habify.controller;

import com.habify.config.AuthHelper;
import com.habify.dto.CalendarDayDTO;
import com.habify.dto.CalendarMonthDTO;
import com.habify.service.CalendarService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;
    private final AuthHelper authHelper;

    public CalendarController(CalendarService calendarService, AuthHelper authHelper) {
        this.calendarService = calendarService;
        this.authHelper = authHelper;
    }

    @GetMapping("/month")
    public ResponseEntity<CalendarMonthDTO> getMonthView(@RequestParam("year") int year,
                                                         @RequestParam("month") int month,
                                                         HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        return ResponseEntity.ok(calendarService.getMonthView(userId, year, month));
    }

    @GetMapping("/day")
    public ResponseEntity<CalendarDayDTO> getDayDetail(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        return ResponseEntity.ok(calendarService.getDayDetail(userId, date));
    }
}
