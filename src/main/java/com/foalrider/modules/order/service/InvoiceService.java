package com.foalrider.modules.order.service;

import com.foalrider.modules.order.entity.Order;

/**
 * Service interface for generating PDF invoices.
 */
public interface InvoiceService {

    /**
     * Generate a PDF invoice for the given order.
     *
     * @param order The order to generate an invoice for
     * @return byte array containing the PDF data
     */
    byte[] generateInvoice(Order order);

    /**
     * Generate a PDF invoice for the given order ID.
     *
     * @param orderId The ID of the order
     * @return byte array containing the PDF data
     */
    byte[] generateInvoiceById(java.util.UUID orderId);

    /**
     * Generate a PDF invoice for the given order number.
     *
     * @param orderNumber The order number
     * @return byte array containing the PDF data
     */
    byte[] generateInvoiceByOrderNumber(String orderNumber);
}
