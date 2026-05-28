package ru.mokrischev.vendingsupply.services;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
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
public class PdfReportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ServiceReportRepository serviceReportRepository;
    private final StockMovementRepository stockMovementRepository;
    private final WarehouseItemRepository warehouseItemRepository;

    private Font getBaseFont() {
        try {
            BaseFont bf = BaseFont.createFont("C:/Windows/Fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            return new Font(bf, 12, Font.NORMAL);
        } catch (Exception e) {
            return new Font(Font.HELVETICA, 12, Font.NORMAL);
        }
    }

    private Font getTitleFont() {
        try {
            BaseFont bf = BaseFont.createFont("C:/Windows/Fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            return new Font(bf, 18, Font.BOLD);
        } catch (Exception e) {
            return new Font(Font.HELVETICA, 18, Font.BOLD);
        }
    }

    public byte[] generateAdminPdf(LocalDateTime start, LocalDateTime end, String monthStr) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        Font font = getBaseFont();
        Font titleFont = getTitleFont();

        document.add(new Paragraph("Отчет администратора - " + monthStr, titleFont));
        document.add(new Paragraph(" "));

        List<Order> orders = orderRepository.findAll();
        Map<String, BigDecimal> productProfit = new HashMap<>();
        Map<String, BigDecimal> franchiseeProfit = new HashMap<>();

        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.DELIVERED && order.getCreatedAt() != null
                    && !order.getCreatedAt().isBefore(start) && !order.getCreatedAt().isAfter(end)) {

                String franEmail = order.getFranchisee().getEmail();
                franchiseeProfit.merge(franEmail, order.getTotalPrice(), BigDecimal::add);

                List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                for (OrderItem item : items) {
                    String pName = item.getProduct().getName();
                    productProfit.merge(pName, item.getQuantity().multiply(item.getPriceAtMoment()), BigDecimal::add);
                }
            }
        }

        if (!productProfit.isEmpty()) {
            CategoryChart productChart = new CategoryChartBuilder().width(1000).height(800).title("Прибыль по товарам")
                    .xAxisTitle("Товар").yAxisTitle("Прибыль (₽)").build();
            productChart.getStyler().setXAxisLabelRotation(45);
            productChart.getStyler().setLegendVisible(false);
            productChart.getStyler().setStacked(true);
            
            java.awt.Font baseFont = new java.awt.Font("Arial", java.awt.Font.PLAIN, 24);
            productChart.getStyler().setAxisTickLabelsFont(baseFont);
            productChart.getStyler().setAxisTitleFont(baseFont);
            productChart.getStyler().setChartTitleFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 30));
            
            List<String> pLabels = new ArrayList<>(productProfit.keySet());
            for (String label : pLabels) {
                List<Number> pValues = new ArrayList<>();
                for (String l : pLabels) {
                    if (l.equals(label)) pValues.add(productProfit.get(l).doubleValue());
                    else pValues.add(0.0);
                }
                productChart.addSeries(label, pLabels, pValues);
            }

            byte[] chartBytes = BitmapEncoder.getBitmapBytes(productChart, BitmapEncoder.BitmapFormat.PNG);
            Image img = Image.getInstance(chartBytes);
            img.scaleToFit(500, 400);
            document.add(img);
        } else {
            document.add(new Paragraph("Нет данных по продажам за этот период.", font));
        }

        document.add(new Paragraph(" "));

        if (!franchiseeProfit.isEmpty()) {
            PieChart franChart = new PieChartBuilder().width(1000).height(800).title("Выручка по франчайзи").build();
            java.awt.Font baseFont = new java.awt.Font("Arial", java.awt.Font.PLAIN, 24);
            franChart.getStyler().setChartTitleFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 30));
            franChart.getStyler().setLegendFont(baseFont);

            for (Map.Entry<String, BigDecimal> entry : franchiseeProfit.entrySet()) {
                franChart.addSeries(entry.getKey(), entry.getValue().doubleValue());
            }

            byte[] chartBytes = BitmapEncoder.getBitmapBytes(franChart, BitmapEncoder.BitmapFormat.PNG);
            Image img = Image.getInstance(chartBytes);
            img.scaleToFit(500, 400);
            document.add(img);
            
            document.add(new Paragraph(" "));
            for (Map.Entry<String, BigDecimal> entry : franchiseeProfit.entrySet()) {
                document.add(new Paragraph("Франчайзи " + entry.getKey() + " принес выручки: " + entry.getValue().stripTrailingZeros().toPlainString() + " ₽", font));
            }

        } else {
            document.add(new Paragraph("Нет данных по франчайзи за этот период.", font));
        }

        document.close();
        return out.toByteArray();
    }

    public byte[] generateFranchiseePdf(String email, LocalDateTime start, LocalDateTime end, String monthStr)
            throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        Font font = getBaseFont();
        Font titleFont = getTitleFont();

        document.add(new Paragraph("Отчет франчайзи - " + monthStr, titleFont));
        document.add(new Paragraph("Пользователь: " + email, font));
        document.add(new Paragraph(" "));

        List<ServiceReport> reports = serviceReportRepository.findAll();
        Map<String, BigDecimal> machineExpenses = new HashMap<>();
        Map<String, BigDecimal> productExpenses = new HashMap<>();
        List<ServiceReport> monthReports = new ArrayList<>();

        for (ServiceReport report : reports) {
            if (report.getEmployee().getFranchisee().getEmail().equals(email)
                    && report.getServiceDate() != null
                    && !report.getServiceDate().isBefore(start) && !report.getServiceDate().isAfter(end)) {

                monthReports.add(report);
                String mName = report.getMachine().getName();

                for (ServiceReportConsumable consumable : report.getConsumables()) {
                    String pName = consumable.getProduct().getName();
                    BigDecimal cost = consumable.getProduct().getPrice()
                            .multiply(BigDecimal.valueOf(consumable.getQuantity()));

                    machineExpenses.merge(mName, cost, BigDecimal::add);
                    productExpenses.merge(pName, cost, BigDecimal::add);
                }
            }
        }

        if (!machineExpenses.isEmpty()) {
            CategoryChart mChart = new CategoryChartBuilder().width(1000).height(800).title("Расходы по автоматам")
                    .xAxisTitle("Автомат").yAxisTitle("Расходы (₽)").build();
            mChart.getStyler().setXAxisLabelRotation(45);
            mChart.getStyler().setLegendVisible(false);
            mChart.getStyler().setStacked(true);
            
            java.awt.Font baseFont = new java.awt.Font("Arial", java.awt.Font.PLAIN, 24);
            mChart.getStyler().setAxisTickLabelsFont(baseFont);
            mChart.getStyler().setAxisTitleFont(baseFont);
            mChart.getStyler().setChartTitleFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 30));

            List<String> mLabels = new ArrayList<>(machineExpenses.keySet());
            for (String label : mLabels) {
                List<Number> mValues = new ArrayList<>();
                for (String l : mLabels) {
                    if (l.equals(label)) mValues.add(machineExpenses.get(l).doubleValue());
                    else mValues.add(0.0);
                }
                mChart.addSeries(label, mLabels, mValues);
            }

            byte[] chartBytes = BitmapEncoder.getBitmapBytes(mChart, BitmapEncoder.BitmapFormat.PNG);
            Image img = Image.getInstance(chartBytes);
            img.scaleToFit(500, 400);
            document.add(img);
        } else {
            document.add(new Paragraph("Нет данных по расходам на автоматы.", font));
        }

        document.add(new Paragraph(" "));

        if (!productExpenses.isEmpty()) {
            PieChart pChart = new PieChartBuilder().width(1000).height(800).title("Расходы по товарам (расходникам)")
                    .build();
            java.awt.Font baseFont = new java.awt.Font("Arial", java.awt.Font.PLAIN, 24);
            pChart.getStyler().setChartTitleFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 30));
            pChart.getStyler().setLegendFont(baseFont);
            
            for (Map.Entry<String, BigDecimal> entry : productExpenses.entrySet()) {
                pChart.addSeries(entry.getKey(), entry.getValue().doubleValue());
            }

            byte[] chartBytes = BitmapEncoder.getBitmapBytes(pChart, BitmapEncoder.BitmapFormat.PNG);
            Image img = Image.getInstance(chartBytes);
            img.scaleToFit(500, 400);
            document.add(img);
        } else {
            document.add(new Paragraph("Нет данных по расходу товаров.", font));
        }

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Детализация отчетов обслуживания:", titleFont));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        for (ServiceReport sr : monthReports) {
            document.add(new Paragraph(" "));
            LineSeparator ls = new LineSeparator();
            ls.setLineColor(java.awt.Color.GRAY);
            document.add(new Chunk(ls));
            document.add(new Paragraph(" "));
            
            document.add(new Paragraph("Отчет №: " + sr.getId() + " | Автомат: " + sr.getMachine().getName()
                    + " | Дата: " + sr.getServiceDate().format(dtf), font));
            document.add(new Paragraph("Комментарий: " + sr.getComment(), font));
            document.add(new Paragraph("Расходные материалы: ", font));
            for (ServiceReportConsumable c : sr.getConsumables()) {
                document.add(new Paragraph(
                        " - " + c.getProduct().getName() + ": " + c.getQuantity() + " " + c.getProduct().getUnit(),
                        font));
            }
            for (ServiceReportPhoto p : sr.getPhotos()) {
                try {
                    String photoPath = p.getPhotoUrl().replace("/uploads/", "uploads/");
                    Image img = Image.getInstance(photoPath);
                    img.scaleToFit(300, 300);
                    img.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                    document.add(new Paragraph(" "));
                    document.add(img);
                } catch (Exception ex) {
                    document.add(new Paragraph("[Не удалось загрузить фото: " + p.getPhotoUrl() + "]", font));
                }
            }
            document.add(new Paragraph(" "));
            document.add(new Chunk(ls));
        }

        document.close();
        return out.toByteArray();
    }
}
