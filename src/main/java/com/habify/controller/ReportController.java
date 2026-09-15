package com.habify.controller;

import com.habify.config.AuthHelper;
import com.habify.dto.ReportSummaryDTO;
import com.habify.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final AuthHelper authHelper;

    public ReportController(ReportService reportService, AuthHelper authHelper) {
        this.reportService = reportService;
        this.authHelper = authHelper;
    }

    @GetMapping("/summary")
    public ResponseEntity<ReportSummaryDTO> getReportSummary(
            @RequestParam(name = "type", defaultValue = "WEEKLY") String type,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        Long userId = authHelper.getAuthenticatedUserId(request);
        return ResponseEntity.ok(reportService.getReportSummary(userId, type, date));
    }

    @GetMapping("/csv")
    public void exportCsv(
            @RequestParam(name = "type", defaultValue = "WEEKLY") String type,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        Long userId = authHelper.getAuthenticatedUserId(request);
        reportService.exportCsv(userId, type, date, response);
    }
}
