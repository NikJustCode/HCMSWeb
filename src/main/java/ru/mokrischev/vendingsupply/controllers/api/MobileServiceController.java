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
                .map(m -> new MachineDto(m.getId(), m.getName(), m.getAddressText(),
                        m.isActive() ? "ACTIVE" : "INACTIVE"))
                .collect(Collectors.toList());
        return ResponseEntity.ok(machines);
    }

    // 1.5 Get available products for consumables
    @GetMapping("/products")
    public ResponseEntity<?> getProducts() {
        List<ProductDto> products = productRepository.findByActiveTrue().stream()
                .map(p -> new ProductDto(p.getId(), p.getName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(products);
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
    }

    @Data
    static class ProductDto {
        private final Long id;
        private final String name;
    }
}
