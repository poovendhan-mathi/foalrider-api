package com.foalrider.modules.order.service;

import com.foalrider.modules.order.entity.Order;
import com.foalrider.modules.order.entity.OrderItem;
import com.foalrider.modules.order.repository.OrderRepository;
import com.foalrider.shared.exception.ResourceNotFoundException;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Implementation of InvoiceService for generating PDF invoices.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final OrderRepository orderRepository;

    @Value("${app.name:FoalRider}")
    private String companyName;

    @Value("${app.company.address:123 Fashion Street, New York, NY 10001}")
    private String companyAddress;

    @Value("${app.company.email:support@foalrider.com}")
    private String companyEmail;

    @Value("${app.company.phone:+1 (555) 123-4567}")
    private String companyPhone;

    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(59, 130, 246); // Blue
    private static final DeviceRgb HEADER_BG = new DeviceRgb(243, 244, 246); // Light gray
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("MMMM dd, yyyy")
            .withZone(ZoneId.systemDefault());

    @Override
    public byte[] generateInvoice(Order order) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Fonts
            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Header Section
            addHeader(document, order, boldFont, regularFont);

            // Address Section
            addAddressSection(document, order, boldFont, regularFont);

            // Order Items Table
            addItemsTable(document, order, boldFont, regularFont);

            // Totals Section
            addTotalsSection(document, order, boldFont, regularFont);

            // Footer
            addFooter(document, regularFont);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating invoice for order {}: {}", order.getOrderNumber(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate invoice", e);
        }
    }

    @Override
    public byte[] generateInvoiceById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return generateInvoice(order);
    }

    @Override
    public byte[] generateInvoiceByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return generateInvoice(order);
    }

    private void addHeader(Document document, Order order, PdfFont boldFont, PdfFont regularFont) {
        // Company name and invoice title
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        // Left: Company info
        Cell companyCell = new Cell()
                .add(new Paragraph(companyName)
                        .setFont(boldFont)
                        .setFontSize(24)
                        .setFontColor(PRIMARY_COLOR))
                .add(new Paragraph(companyAddress)
                        .setFont(regularFont)
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY))
                .add(new Paragraph(companyEmail + " | " + companyPhone)
                        .setFont(regularFont)
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT);

        // Right: Invoice details
        Cell invoiceCell = new Cell()
                .add(new Paragraph("INVOICE")
                        .setFont(boldFont)
                        .setFontSize(28)
                        .setFontColor(ColorConstants.DARK_GRAY))
                .add(new Paragraph("Invoice #: " + order.getOrderNumber())
                        .setFont(regularFont)
                        .setFontSize(10))
                .add(new Paragraph("Date: " + DATE_FORMATTER.format(order.getCreatedAt()))
                        .setFont(regularFont)
                        .setFontSize(10))
                .add(new Paragraph("Status: " + order.getStatus().name())
                        .setFont(boldFont)
                        .setFontSize(10)
                        .setFontColor(getStatusColor(order.getStatus().name())))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);

        headerTable.addCell(companyCell);
        headerTable.addCell(invoiceCell);
        document.add(headerTable);

        // Divider line
        document.add(new Paragraph("")
                .setMarginTop(10)
                .setBorderBottom(new SolidBorder(PRIMARY_COLOR, 2)));
    }

    private void addAddressSection(Document document, Order order, PdfFont boldFont, PdfFont regularFont) {
        Table addressTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(20);

        // Bill To
        StringBuilder billTo = new StringBuilder();
        billTo.append(order.getShippingName()).append("\n");
        if (order.getShippingEmail() != null) {
            billTo.append(order.getShippingEmail()).append("\n");
        }
        if (order.getShippingPhone() != null) {
            billTo.append(order.getShippingPhone());
        }

        Cell billToCell = new Cell()
                .add(new Paragraph("BILL TO")
                        .setFont(boldFont)
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY))
                .add(new Paragraph(billTo.toString())
                        .setFont(regularFont)
                        .setFontSize(10))
                .setBorder(Border.NO_BORDER);

        // Ship To
        StringBuilder shipTo = new StringBuilder();
        shipTo.append(order.getShippingName()).append("\n");
        if (order.getShippingAddressLine1() != null) {
            shipTo.append(order.getShippingAddressLine1()).append("\n");
        }
        if (order.getShippingAddressLine2() != null && !order.getShippingAddressLine2().isEmpty()) {
            shipTo.append(order.getShippingAddressLine2()).append("\n");
        }
        if (order.getShippingCity() != null) {
            shipTo.append(order.getShippingCity());
            if (order.getShippingState() != null) {
                shipTo.append(", ").append(order.getShippingState());
            }
            if (order.getShippingPostalCode() != null) {
                shipTo.append(" ").append(order.getShippingPostalCode());
            }
            shipTo.append("\n");
        }
        if (order.getShippingCountry() != null) {
            shipTo.append(order.getShippingCountry());
        }

        Cell shipToCell = new Cell()
                .add(new Paragraph("SHIP TO")
                        .setFont(boldFont)
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY))
                .add(new Paragraph(shipTo.toString())
                        .setFont(regularFont)
                        .setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT);

        addressTable.addCell(billToCell);
        addressTable.addCell(shipToCell);
        document.add(addressTable);
    }

    private void addItemsTable(Document document, Order order, PdfFont boldFont, PdfFont regularFont) {
        Table itemsTable = new Table(UnitValue.createPercentArray(new float[]{4, 1.5f, 1.5f, 2}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(30);

        // Header row
        String[] headers = {"Item", "Qty", "Unit Price", "Total"};
        for (String header : headers) {
            Cell headerCell = new Cell()
                    .add(new Paragraph(header)
                            .setFont(boldFont)
                            .setFontSize(10))
                    .setBackgroundColor(HEADER_BG)
                    .setPadding(8)
                    .setBorder(Border.NO_BORDER);
            if (header.equals("Item")) {
                headerCell.setTextAlignment(TextAlignment.LEFT);
            } else {
                headerCell.setTextAlignment(TextAlignment.RIGHT);
            }
            itemsTable.addHeaderCell(headerCell);
        }

        // Item rows
        for (OrderItem item : order.getItems()) {
            // Item name and details
            StringBuilder itemDetails = new StringBuilder();
            itemDetails.append(item.getProductName());
            if (item.getVariantName() != null && !item.getVariantName().isEmpty()) {
                itemDetails.append("\n").append(item.getVariantName());
            }
            if (item.getProductSku() != null) {
                itemDetails.append("\nSKU: ").append(item.getProductSku());
            }

            itemsTable.addCell(new Cell()
                    .add(new Paragraph(itemDetails.toString())
                            .setFont(regularFont)
                            .setFontSize(10))
                    .setPadding(8)
                    .setBorder(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));

            itemsTable.addCell(new Cell()
                    .add(new Paragraph(String.valueOf(item.getQuantity()))
                            .setFont(regularFont)
                            .setFontSize(10))
                    .setPadding(8)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBorder(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));

            itemsTable.addCell(new Cell()
                    .add(new Paragraph(formatCurrency(item.getUnitPrice()))
                            .setFont(regularFont)
                            .setFontSize(10))
                    .setPadding(8)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBorder(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));

            itemsTable.addCell(new Cell()
                    .add(new Paragraph(formatCurrency(item.getTotalPrice()))
                            .setFont(regularFont)
                            .setFontSize(10))
                    .setPadding(8)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBorder(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));
        }

        document.add(itemsTable);
    }

    private void addTotalsSection(Document document, Order order, PdfFont boldFont, PdfFont regularFont) {
        Table totalsTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                .setWidth(UnitValue.createPercentValue(40))
                .setHorizontalAlignment(HorizontalAlignment.RIGHT)
                .setMarginTop(20);

        // Subtotal
        addTotalRow(totalsTable, "Subtotal", order.getSubtotal(), regularFont, false);

        // Tax
        if (order.getTaxAmount() != null && order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(totalsTable, "Tax", order.getTaxAmount(), regularFont, false);
        }

        // Shipping
        if (order.getShippingAmount() != null && order.getShippingAmount().compareTo(BigDecimal.ZERO) > 0) {
            addTotalRow(totalsTable, "Shipping", order.getShippingAmount(), regularFont, false);
        }

        // Discount
        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            Cell labelCell = new Cell()
                    .add(new Paragraph("Discount")
                            .setFont(regularFont)
                            .setFontSize(10))
                    .setBorder(Border.NO_BORDER)
                    .setPadding(5);

            Cell valueCell = new Cell()
                    .add(new Paragraph("-" + formatCurrency(order.getDiscountAmount()))
                            .setFont(regularFont)
                            .setFontSize(10)
                            .setFontColor(new DeviceRgb(34, 197, 94))) // Green
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(5);

            totalsTable.addCell(labelCell);
            totalsTable.addCell(valueCell);
        }

        // Total
        Cell totalLabelCell = new Cell()
                .add(new Paragraph("TOTAL")
                        .setFont(boldFont)
                        .setFontSize(12))
                .setBorder(Border.NO_BORDER)
                .setBorderTop(new SolidBorder(ColorConstants.DARK_GRAY, 1))
                .setPadding(8);

        Cell totalValueCell = new Cell()
                .add(new Paragraph(formatCurrency(order.getTotalAmount()))
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setFontColor(PRIMARY_COLOR))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER)
                .setBorderTop(new SolidBorder(ColorConstants.DARK_GRAY, 1))
                .setPadding(8);

        totalsTable.addCell(totalLabelCell);
        totalsTable.addCell(totalValueCell);

        document.add(totalsTable);

        // Payment method
        if (order.getPaymentMethod() != null) {
            document.add(new Paragraph("Payment Method: " + order.getPaymentMethod())
                    .setFont(regularFont)
                    .setFontSize(10)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setMarginTop(10));
        }
    }

    private void addTotalRow(Table table, String label, BigDecimal amount, PdfFont font, boolean isBold) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label)
                        .setFont(font)
                        .setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setPadding(5);

        Cell valueCell = new Cell()
                .add(new Paragraph(formatCurrency(amount))
                        .setFont(font)
                        .setFontSize(10))
                .setTextAlignment(TextAlignment.RIGHT)
                .setBorder(Border.NO_BORDER)
                .setPadding(5);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addFooter(Document document, PdfFont regularFont) {
        document.add(new Paragraph("")
                .setMarginTop(40)
                .setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 1)));

        document.add(new Paragraph("Thank you for shopping with " + companyName + "!")
                .setFont(regularFont)
                .setFontSize(12)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(15));

        document.add(new Paragraph("If you have any questions about this invoice, please contact us at " + companyEmail)
                .setFont(regularFont)
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph("This is a computer-generated invoice and does not require a signature.")
                .setFont(regularFont)
                .setFontSize(8)
                .setFontColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20));
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "$0.00";
        }
        return String.format("$%.2f", amount);
    }

    private DeviceRgb getStatusColor(String status) {
        return switch (status) {
            case "COMPLETED", "DELIVERED" -> new DeviceRgb(34, 197, 94);  // Green
            case "CANCELLED", "REFUNDED" -> new DeviceRgb(239, 68, 68);   // Red
            case "PROCESSING", "SHIPPED" -> new DeviceRgb(59, 130, 246);  // Blue
            default -> new DeviceRgb(107, 114, 128);                        // Gray
        };
    }
}
