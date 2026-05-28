package ru.mokrischev.vendingsupply.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ru.mokrischev.vendingsupply.services.ExcelReportService;
import ru.mokrischev.vendingsupply.services.PdfReportService;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

@Controller
@RequiredArgsConstructor
public class ReportController {

    private final ExcelReportService excelReportService;
    private final PdfReportService pdfReportService;

    private LocalDateTime[] getMonthBounds(String monthStr) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(monthStr);
        } catch (Exception e) {
            ym = YearMonth.now();
        }
        
        YearMonth current = YearMonth.now();
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end;
        
        if (ym.equals(current)) {
            end = LocalDateTime.now();
        } else {
            end = ym.atEndOfMonth().atTime(LocalTime.MAX);
        }
        return new LocalDateTime[]{start, end};
    }

    @GetMapping("/admin/reports/excel")
    public ResponseEntity<byte[]> getAdminExcel(@RequestParam(required = false) String month) throws Exception {
        if (month == null || month.isEmpty()) month = YearMonth.now().toString();
        LocalDateTime[] bounds = getMonthBounds(month);
        byte[] data = excelReportService.generateAdminExcel(bounds[0], bounds[1]);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"admin_report_" + month + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/admin/reports/pdf")
    public ResponseEntity<byte[]> getAdminPdf(@RequestParam(required = false) String month) throws Exception {
        if (month == null || month.isEmpty()) month = YearMonth.now().toString();
        LocalDateTime[] bounds = getMonthBounds(month);
        byte[] data = pdfReportService.generateAdminPdf(bounds[0], bounds[1], month);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"admin_report_" + month + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/franchisee/reports/excel")
    public ResponseEntity<byte[]> getFranchiseeExcel(@RequestParam(required = false) String month, Principal principal) throws Exception {
        if (month == null || month.isEmpty()) month = YearMonth.now().toString();
        LocalDateTime[] bounds = getMonthBounds(month);
        byte[] data = excelReportService.generateFranchiseeExcel(principal.getName(), bounds[0], bounds[1]);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"franchisee_report_" + month + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/franchisee/reports/pdf")
    public ResponseEntity<byte[]> getFranchiseePdf(@RequestParam(required = false) String month, Principal principal) throws Exception {
        if (month == null || month.isEmpty()) month = YearMonth.now().toString();
        LocalDateTime[] bounds = getMonthBounds(month);
        byte[] data = pdfReportService.generateFranchiseePdf(principal.getName(), bounds[0], bounds[1], month);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"franchisee_report_" + month + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}
