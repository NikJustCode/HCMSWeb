package ru.mokrischev.vendingsupply.services;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import ru.mokrischev.vendingsupply.model.entity.*;
import ru.mokrischev.vendingsupply.model.enums.OrderStatus;
import ru.mokrischev.vendingsupply.repository.*;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExcelReportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ServiceReportRepository serviceReportRepository;
    private final StockMovementRepository stockMovementRepository;
    private final WarehouseItemRepository warehouseItemRepository;

    // ADMIN EXCEL
    public byte[] generateAdminExcel(LocalDateTime start, LocalDateTime end) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            List<Order> orders = orderRepository.findAll();

            Sheet sheet1 = workbook.createSheet("Общие продажи");
            createHeader(sheet1, "Товар", "Количество", "Прибыль (₽)");

            Map<Product, BigDecimal> productQty = new HashMap<>();
            Map<Product, BigDecimal> productProfit = new HashMap<>();

            Map<User, Map<Product, BigDecimal>> franchiseeProductQty = new HashMap<>();

            for (Order order : orders) {
                if (order.getStatus() == OrderStatus.DELIVERED && order.getCreatedAt() != null
                        && !order.getCreatedAt().isBefore(start) && !order.getCreatedAt().isAfter(end)) {

                    User franchisee = order.getFranchisee();
                    franchiseeProductQty.putIfAbsent(franchisee, new HashMap<>());

                    List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                    for (OrderItem item : items) {
                        Product p = item.getProduct();
                        productQty.merge(p, item.getQuantity(), BigDecimal::add);
                        productProfit.merge(p, item.getQuantity().multiply(item.getPriceAtMoment()), BigDecimal::add);

                        franchiseeProductQty.get(franchisee).merge(p, item.getQuantity(), BigDecimal::add);
                    }
                }
            }

            int rowIdx = 1;
            BigDecimal totalProfit = BigDecimal.ZERO;
            for (Map.Entry<Product, BigDecimal> entry : productQty.entrySet()) {
                Row row = sheet1.createRow(rowIdx++);
                row.createCell(0).setCellValue(entry.getKey().getName());
                row.createCell(1).setCellValue(entry.getValue().doubleValue());
                BigDecimal profit = productProfit.get(entry.getKey());
                row.createCell(2).setCellValue(profit.doubleValue());
                totalProfit = totalProfit.add(profit);
            }

            Row totalRow = sheet1.createRow(rowIdx++);
            Cell totalLabelCell = totalRow.createCell(0);
            totalLabelCell.setCellValue("ОБЩИЕ ИТОГИ:");
            Cell totalValueCell = totalRow.createCell(2);
            totalValueCell.setCellValue(totalProfit.doubleValue());

            CellStyle boldStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            boldStyle.setFont(font);
            totalLabelCell.setCellStyle(boldStyle);
            totalValueCell.setCellStyle(boldStyle);

            Sheet sheet2 = workbook.createSheet("Продажи по франчайзи");
            createHeader(sheet2, "Франчайзи", "Товар", "Количество");

            rowIdx = 1;
            for (Map.Entry<User, Map<Product, BigDecimal>> fpEntry : franchiseeProductQty.entrySet()) {
                User franchisee = fpEntry.getKey();
                for (Map.Entry<Product, BigDecimal> pEntry : fpEntry.getValue().entrySet()) {
                    Row row = sheet2.createRow(rowIdx++);
                    row.createCell(0).setCellValue(franchisee.getEmail());
                    row.createCell(1).setCellValue(pEntry.getKey().getName());
                    row.createCell(2).setCellValue(pEntry.getValue().doubleValue());
                }
            }

            for (int i = 0; i < 3; i++) {
                sheet1.autoSizeColumn(i);
                sheet2.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // FRANCHISEE EXCEL
    public byte[] generateFranchiseeExcel(String email, LocalDateTime start, LocalDateTime end) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet1 = workbook.createSheet("Затраты на автоматы");
            createHeader(sheet1, "Автомат", "Товар", "Количество", "Стоимость (₽)");

            List<ServiceReport> reports = serviceReportRepository.findAll();
            Map<VendingMachine, Map<Product, Integer>> machineProductQty = new HashMap<>();

            for (ServiceReport report : reports) {
                if (report.getEmployee().getFranchisee().getEmail().equals(email)
                        && report.getServiceDate() != null
                        && !report.getServiceDate().isBefore(start) && !report.getServiceDate().isAfter(end)) {

                    VendingMachine machine = report.getMachine();
                    machineProductQty.putIfAbsent(machine, new HashMap<>());

                    for (ServiceReportConsumable consumable : report.getConsumables()) {
                        machineProductQty.get(machine).merge(consumable.getProduct(), consumable.getQuantity(),
                                Integer::sum);
                    }
                }
            }

            int rowIdx = 1;
            BigDecimal grandTotal = BigDecimal.ZERO;

            for (Map.Entry<VendingMachine, Map<Product, Integer>> entry : machineProductQty.entrySet()) {
                VendingMachine machine = entry.getKey();
                BigDecimal machineTotal = BigDecimal.ZERO;

                for (Map.Entry<Product, Integer> prodEntry : entry.getValue().entrySet()) {
                    Product p = prodEntry.getKey();
                    int qty = prodEntry.getValue();
                    BigDecimal cost = p.getPrice().multiply(BigDecimal.valueOf(qty));
                    machineTotal = machineTotal.add(cost);
                    grandTotal = grandTotal.add(cost);

                    Row row = sheet1.createRow(rowIdx++);
                    row.createCell(0).setCellValue(machine.getName() + " (" + machine.getAddressText() + ")");
                    row.createCell(1).setCellValue(p.getName());
                    row.createCell(2).setCellValue(qty);
                    row.createCell(3).setCellValue(cost.doubleValue());
                }
                Row row = sheet1.createRow(rowIdx++);
                row.createCell(0).setCellValue("ИТОГО " + machine.getName());
                row.createCell(3).setCellValue(machineTotal.doubleValue());
                rowIdx++;
            }

            Row grandRow = sheet1.createRow(rowIdx++);
            Cell grandTotalLabel = grandRow.createCell(0);
            grandTotalLabel.setCellValue("ОБЩИЙ ИТОГ");
            Cell grandTotalValue = grandRow.createCell(3);
            grandTotalValue.setCellValue(grandTotal.doubleValue());

            CellStyle boldStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            boldStyle.setFont(font);
            grandTotalLabel.setCellStyle(boldStyle);
            grandTotalValue.setCellStyle(boldStyle);

            Sheet sheet2 = workbook.createSheet("История движения товара");

            List<StockMovement> movements = stockMovementRepository
                    .findByFranchiseeEmailOrderByOperationDateDesc(email);

            List<WarehouseItem> items = warehouseItemRepository.findByFranchiseeEmail(email);
            Map<Product, BigDecimal> startStock = new HashMap<>();
            Map<Product, BigDecimal> currentStock = new HashMap<>();

            for (WarehouseItem item : items) {
                currentStock.put(item.getProduct(), item.getQuantity());
                startStock.put(item.getProduct(), item.getQuantity());
            }

            List<StockMovement> monthMovements = new ArrayList<>();
            for (StockMovement sm : movements) {
                if (sm.getOperationDate() != null && !sm.getOperationDate().isBefore(start)) {
                    startStock.merge(sm.getProduct(), sm.getAmount().negate(), BigDecimal::add);

                    if (!sm.getOperationDate().isAfter(end)) {
                        monthMovements.add(sm);
                    }
                }
            }

            int rowIdx2 = 0;

            Row startTitleRow = sheet2.createRow(rowIdx2++);
            Cell startTitleCell = startTitleRow.createCell(0);
            startTitleCell.setCellValue("СОСТОЯНИЕ СКЛАДА (НАЧАЛО ПЕРИОДА)");
            startTitleCell.setCellStyle(boldStyle);
            for (Map.Entry<Product, BigDecimal> ss : startStock.entrySet()) {
                Row row = sheet2.createRow(rowIdx2++);
                row.createCell(0).setCellValue(ss.getKey().getName());
                row.createCell(1).setCellValue(ss.getValue().stripTrailingZeros().toPlainString());
            }
            rowIdx2++;

            Row endTitleRow = sheet2.createRow(rowIdx2++);
            Cell endTitleCell = endTitleRow.createCell(0);
            endTitleCell.setCellValue("СОСТОЯНИЕ СКЛАДА (КОНЕЦ ПЕРИОДА)");
            endTitleCell.setCellStyle(boldStyle);
            for (Map.Entry<Product, BigDecimal> cs : currentStock.entrySet()) {
                Row row = sheet2.createRow(rowIdx2++);
                row.createCell(0).setCellValue(cs.getKey().getName());
                row.createCell(1).setCellValue(cs.getValue().stripTrailingZeros().toPlainString());
            }
            rowIdx2++;

            Row historyHeaderRow = sheet2.createRow(rowIdx2++);
            String[] historyHeaders = { "Дата", "Операция", "Автомат", "Товар", "Изменение", "Описание" };
            for (int i = 0; i < historyHeaders.length; i++) {
                Cell cell = historyHeaderRow.createCell(i);
                cell.setCellValue(historyHeaders[i]);
                cell.setCellStyle(boldStyle);
            }

            monthMovements.sort(Comparator.comparing(StockMovement::getOperationDate));
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

            for (StockMovement sm : monthMovements) {
                Row row = sheet2.createRow(rowIdx2++);
                row.createCell(0).setCellValue(sm.getOperationDate().format(dtf));
                row.createCell(1).setCellValue(sm.getType().name());
                row.createCell(2).setCellValue(sm.getVendingMachine() != null ? sm.getVendingMachine().getName() : "-");
                row.createCell(3).setCellValue(sm.getProduct().getName());
                row.createCell(4).setCellValue(sm.getAmount().doubleValue());
                row.createCell(5).setCellValue(sm.getDescription());
            }

            for (int i = 0; i < 6; i++) {
                sheet1.autoSizeColumn(i);
                sheet2.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createHeader(Sheet sheet, String... headers) {
        Row headerRow = sheet.createRow(0);
        Workbook wb = sheet.getWorkbook();
        CellStyle boldStyle = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        boldStyle.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(boldStyle);
        }
    }
}
