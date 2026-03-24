package ru.mokrischev.vendingsupply.controllers.api;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.mokrischev.vendingsupply.model.entity.*;
import ru.mokrischev.vendingsupply.repository.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mobile/v1")
@RequiredArgsConstructor
public class MobileServiceController {

    private final EmployeeRepository employeeRepository;
    private final VendingMachineRepository machineRepository;
    private final ServiceReportRepository reportRepository;
    private final ProductRepository productRepository;
    private final ru.mokrischev.vendingsupply.services.WarehouseService warehouseService;

    @Value("${app.upload.path}")
    private String uploadPath;

    // 1. Get assigned machines
    @GetMapping("/machines")
    public ResponseEntity<?> getMachines(Authentication authentication) {
        Long employeeId = (Long) authentication.getPrincipal();
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null)
            return ResponseEntity.status(401).build();

        List<MachineDto> machines = employee.getMachines().stream()
                .map(m -> {
                    ServiceReport lastReport = reportRepository.findTopByMachineIdOrderByServiceDateDesc(m.getId());
                    boolean needsService = true;
                    if (lastReport != null) {
                        needsService = lastReport.getServiceDate().isBefore(LocalDateTime.now().minusDays(2)); // Service every 2 days
                    }
                    return new MachineDto(m.getId(), m.getName(), m.getAddressText(),
                            m.isActive() ? "ACTIVE" : "INACTIVE", needsService);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(machines);
    }

    // 1.1 Get employee profile
    @GetMapping("/me")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        Long employeeId = (Long) authentication.getPrincipal();
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) return ResponseEntity.status(401).build();

        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", employee.getId());
        map.put("fullName", employee.getFullName() != null ? employee.getFullName() : "");
        map.put("phone", employee.getPhone() != null ? employee.getPhone() : "");
        map.put("email", employee.getEmail() != null ? employee.getEmail() : "");
        map.put("scheduleType", employee.getScheduleType() != null ? employee.getScheduleType().name() : "");
        map.put("shiftPattern", employee.getShiftPattern() != null ? employee.getShiftPattern() : "");
        map.put("workingDays", employee.getWorkingDays() != null ? 
            employee.getWorkingDays().stream().map(Enum::name).collect(Collectors.toList()) : 
            new ArrayList<>());
            
        boolean isWorkingToday = true; // Default fallback for shift pattern without reference date
        if (employee.getScheduleType() == ru.mokrischev.vendingsupply.model.enums.ScheduleType.WEEKLY_DAYS 
                && employee.getWorkingDays() != null) {
            isWorkingToday = employee.getWorkingDays().contains(LocalDateTime.now().getDayOfWeek());
        }
        map.put("isWorkingToday", isWorkingToday);

        return ResponseEntity.ok(map);
    }

    // 1.5 Get available products for consumables
    @GetMapping("/products")
    public ResponseEntity<?> getProducts() {
        List<ProductDto> products = productRepository.findByActiveTrue().stream()
                .map(p -> new ProductDto(p.getId(), p.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(products);
    }

    // 1.6 Get reports history
    @GetMapping("/reports")
    public ResponseEntity<?> getReportsHistory(
            Authentication authentication,
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        
        Long employeeId = (Long) authentication.getPrincipal();

        List<ServiceReport> reports;
        if (machineId != null) {
            reports = reportRepository.findByEmployeeIdAndMachineIdOrderByServiceDateDesc(employeeId, machineId);
        } else {
            reports = reportRepository.findByEmployeeIdOrderByServiceDateDesc(employeeId);
        }

        if (date != null) {
            reports = reports.stream()
                .filter(r -> r.getServiceDate() != null && r.getServiceDate().toLocalDate().equals(date))
                .collect(Collectors.toList());
        }

        List<java.util.Map<String, Object>> result = new ArrayList<>();
        for (ServiceReport r : reports) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", r.getId());
            map.put("serviceDate", r.getServiceDate() != null ? r.getServiceDate().toString() : "");
            map.put("comment", r.getComment() != null ? r.getComment() : "");
            if (r.getMachine() != null) {
                map.put("machineId", r.getMachine().getId());
                map.put("machineName", r.getMachine().getName());
            }

            List<java.util.Map<String, Object>> cons = new ArrayList<>();
            for (ServiceReportConsumable c : r.getConsumables()) {
                if (c.getProduct() != null) {
                    cons.add(java.util.Map.of("productId", c.getProduct().getId(), "productName", c.getProduct().getName(), "quantity", c.getQuantity()));
                }
            }
            map.put("consumables", cons);

            List<String> photos = new ArrayList<>();
            for (ServiceReportPhoto p : r.getPhotos()) {
                photos.add(p.getPhotoUrl());
            }
            map.put("photos", photos);

            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

    // 2. Submit service report
    @PostMapping("/reports")
    public ResponseEntity<?> submitReport(
            Authentication authentication,
            @RequestParam("machineId") Long machineId,
            @RequestParam("comment") String comment,
            @RequestParam(value = "consumables", required = false) List<String> consumables, // Format:
                                                                                             // productId:quantity
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos) {

        Long employeeId = (Long) authentication.getPrincipal();
        Employee employee = employeeRepository.findById(employeeId).orElseThrow();
        VendingMachine machine = machineRepository.findById(machineId).orElseThrow();

        if (!employee.getMachines().contains(machine)) {
            return ResponseEntity.status(403).body("У вас нет доступа к этому автомату");
        }

        ServiceReport report = ServiceReport.builder()
                .employee(employee)
                .machine(machine)
                .serviceDate(LocalDateTime.now())
                .comment(comment)
                .consumables(new ArrayList<>())
                .photos(new ArrayList<>())
                .build();

        if (consumables != null) {
            for (String cons : consumables) {
                // Разбиваем строку по запятой, если товары переданы строкой "1:2, 3:4"
                String[] items = cons.split(",");
                for (String item : items) {
                    String[] parts = item.split(":");
                    if (parts.length == 2) {
                        try {
                            // .trim() убирает пробелы до и после числа
                            Long productId = Long.parseLong(parts[0].trim());
                            int qty = Integer.parseInt(parts[1].trim());
                            Product product = productRepository.findById(productId).orElse(null);
                            if (product != null) {
                                ServiceReportConsumable src = ServiceReportConsumable.builder()
                                        .report(report)
                                        .product(product)
                                        .quantity(qty)
                                        .build();
                                report.getConsumables().add(src);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }

        if (photos != null && !photos.isEmpty()) {
            for (MultipartFile file : photos) {
                if (!file.isEmpty()) {
                    try {
                        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                        Path uploadDir = Paths.get(uploadPath, "reports");
                        if (!Files.exists(uploadDir))
                            Files.createDirectories(uploadDir);
                        file.transferTo(uploadDir.resolve(filename).toAbsolutePath().toFile());

                        ServiceReportPhoto photo = ServiceReportPhoto.builder()
                                .report(report)
                                .photoUrl("/uploads/reports/" + filename)
                                .build();
                        report.getPhotos().add(photo);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        report = reportRepository.save(report);

        // Deduct inventory
        if (!report.getConsumables().isEmpty()) {
            java.util.Map<Long, Integer> qtyMap = new java.util.HashMap<>();
            for (ServiceReportConsumable src : report.getConsumables()) {
                qtyMap.put(src.getProduct().getId(), src.getQuantity());
            }
            if (!qtyMap.isEmpty()) {
                warehouseService.registerServiceReport(employee.getFranchisee().getEmail(), qtyMap, machine, report.getId());
            }
        }

        return ResponseEntity.ok("Отчет успешно сохранен");
    }

    @Data
    static class MachineDto {
        private final Long id;
        private final String name;
        private final String address;
        private final String status;
        private final boolean needsServiceToday;
    }

    @Data
    static class ProductDto {
        private final Long id;
        private final String name;
    }
}
