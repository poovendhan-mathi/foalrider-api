package com.foalrider.modules.pricing.config;

import com.foalrider.modules.pricing.entity.*;
import com.foalrider.modules.pricing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

import java.math.BigDecimal;

/**
 * Initializes default currencies, regions, tax rates, and shipping rates
 * for US, Singapore, India, and UK markets.
 * 
 * Run this only in dev/test profiles to seed initial data.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class PricingDataInitializer {

    private final CurrencyRepository currencyRepository;
    private final RegionRepository regionRepository;
    private final TaxRateRepository taxRateRepository;
    private final ShippingRateRepository shippingRateRepository;

    @Bean
    @Order(1)
    @Profile({"dev", "test"})
    public CommandLineRunner initPricingData() {
        return args -> {
            if (currencyRepository.count() > 0) {
                log.info("Pricing data already exists, skipping initialization");
                return;
            }

            log.info("Initializing pricing data for US, SG, IN, GB...");

            // =====================
            // CURRENCIES
            // =====================
            
            // US Dollar (base currency)
            Currency usd = currencyRepository.save(Currency.builder()
                    .code("USD")
                    .name("US Dollar")
                    .symbol("$")
                    .symbolPosition("BEFORE")
                    .decimalPlaces(2)
                    .decimalSeparator(".")
                    .thousandsSeparator(",")
                    .exchangeRateToUsd(BigDecimal.ONE)
                    .stripeMultiplier(100)
                    .isActive(true)
                    .isDefault(true)
                    .build());
            log.info("Created currency: USD");

            // Singapore Dollar
            Currency sgd = currencyRepository.save(Currency.builder()
                    .code("SGD")
                    .name("Singapore Dollar")
                    .symbol("S$")
                    .symbolPosition("BEFORE")
                    .decimalPlaces(2)
                    .decimalSeparator(".")
                    .thousandsSeparator(",")
                    .exchangeRateToUsd(new BigDecimal("1.35")) // 1 USD = 1.35 SGD
                    .stripeMultiplier(100)
                    .isActive(true)
                    .isDefault(false)
                    .build());
            log.info("Created currency: SGD");

            // Indian Rupee
            Currency inr = currencyRepository.save(Currency.builder()
                    .code("INR")
                    .name("Indian Rupee")
                    .symbol("₹")
                    .symbolPosition("BEFORE")
                    .decimalPlaces(2)
                    .decimalSeparator(".")
                    .thousandsSeparator(",")
                    .exchangeRateToUsd(new BigDecimal("83.00")) // 1 USD = 83 INR
                    .stripeMultiplier(100)
                    .isActive(true)
                    .isDefault(false)
                    .build());
            log.info("Created currency: INR");

            // British Pound
            Currency gbp = currencyRepository.save(Currency.builder()
                    .code("GBP")
                    .name("British Pound")
                    .symbol("£")
                    .symbolPosition("BEFORE")
                    .decimalPlaces(2)
                    .decimalSeparator(".")
                    .thousandsSeparator(",")
                    .exchangeRateToUsd(new BigDecimal("0.79")) // 1 USD = 0.79 GBP
                    .stripeMultiplier(100)
                    .isActive(true)
                    .isDefault(false)
                    .build());
            log.info("Created currency: GBP");

            // Euro (for future expansion)
            Currency eur = currencyRepository.save(Currency.builder()
                    .code("EUR")
                    .name("Euro")
                    .symbol("€")
                    .symbolPosition("BEFORE")
                    .decimalPlaces(2)
                    .decimalSeparator(",")
                    .thousandsSeparator(".")
                    .exchangeRateToUsd(new BigDecimal("0.92")) // 1 USD = 0.92 EUR
                    .stripeMultiplier(100)
                    .isActive(true)
                    .isDefault(false)
                    .build());
            log.info("Created currency: EUR");

            // =====================
            // REGIONS
            // =====================

            // United States
            Region us = regionRepository.save(Region.builder()
                    .code("US")
                    .name("United States")
                    .timezone("America/New_York")
                    .defaultCurrency(usd)
                    .localeCode("en-US")
                    .dateFormat("MM/dd/yyyy")
                    .isActive(true)
                    .isDefault(true)
                    .build());
            log.info("Created region: US");

            // Singapore
            Region sg = regionRepository.save(Region.builder()
                    .code("SG")
                    .name("Singapore")
                    .timezone("Asia/Singapore")
                    .defaultCurrency(sgd)
                    .localeCode("en-SG")
                    .dateFormat("dd/MM/yyyy")
                    .isActive(true)
                    .isDefault(false)
                    .build());
            log.info("Created region: SG");

            // India
            Region in = regionRepository.save(Region.builder()
                    .code("IN")
                    .name("India")
                    .timezone("Asia/Kolkata")
                    .defaultCurrency(inr)
                    .localeCode("en-IN")
                    .dateFormat("dd/MM/yyyy")
                    .isActive(true)
                    .isDefault(false)
                    .build());
            log.info("Created region: IN");

            // United Kingdom
            Region gb = regionRepository.save(Region.builder()
                    .code("GB")
                    .name("United Kingdom")
                    .timezone("Europe/London")
                    .defaultCurrency(gbp)
                    .localeCode("en-GB")
                    .dateFormat("dd/MM/yyyy")
                    .isActive(true)
                    .isDefault(false)
                    .build());
            log.info("Created region: GB");

            // =====================
            // TAX RATES
            // =====================

            // US - No federal sales tax (state level varies, using average ~8%)
            taxRateRepository.save(TaxRate.builder()
                    .region(us)
                    .name("Sales Tax")
                    .description("Average state sales tax")
                    .rate(new BigDecimal("0.08"))
                    .displayRate("8%")
                    .isInclusive(false)
                    .isActive(true)
                    .priority(0)
                    .build());
            log.info("Created tax rate: US Sales Tax 8%");

            // Singapore - GST 9% (increased from 8% in 2024)
            taxRateRepository.save(TaxRate.builder()
                    .region(sg)
                    .name("GST")
                    .description("Goods and Services Tax")
                    .rate(new BigDecimal("0.09"))
                    .displayRate("9%")
                    .isInclusive(false)
                    .isActive(true)
                    .priority(0)
                    .build());
            log.info("Created tax rate: SG GST 9%");

            // India - GST 18% (standard rate for most goods)
            taxRateRepository.save(TaxRate.builder()
                    .region(in)
                    .name("GST")
                    .description("Goods and Services Tax")
                    .rate(new BigDecimal("0.18"))
                    .displayRate("18%")
                    .isInclusive(false)
                    .isActive(true)
                    .priority(0)
                    .build());
            log.info("Created tax rate: IN GST 18%");

            // UK - VAT 20% (inclusive pricing is common in UK)
            taxRateRepository.save(TaxRate.builder()
                    .region(gb)
                    .name("VAT")
                    .description("Value Added Tax")
                    .rate(new BigDecimal("0.20"))
                    .displayRate("20%")
                    .isInclusive(true) // UK typically shows VAT-inclusive prices
                    .isActive(true)
                    .priority(0)
                    .build());
            log.info("Created tax rate: GB VAT 20%");

            // =====================
            // SHIPPING RATES
            // =====================

            // US Shipping
            shippingRateRepository.save(ShippingRate.builder()
                    .region(us)
                    .shippingMethod("STANDARD")
                    .name("Standard Shipping")
                    .description("5-7 business days")
                    .baseCost(new BigDecimal("5.99"))
                    .costPerKg(BigDecimal.ZERO)
                    .freeShippingThreshold(new BigDecimal("50.00"))
                    .minDeliveryDays(5)
                    .maxDeliveryDays(7)
                    .isActive(true)
                    .isDefault(true)
                    .build());

            shippingRateRepository.save(ShippingRate.builder()
                    .region(us)
                    .shippingMethod("EXPRESS")
                    .name("Express Shipping")
                    .description("2-3 business days")
                    .baseCost(new BigDecimal("12.99"))
                    .costPerKg(BigDecimal.ZERO)
                    .freeShippingThreshold(new BigDecimal("100.00"))
                    .minDeliveryDays(2)
                    .maxDeliveryDays(3)
                    .isActive(true)
                    .isDefault(false)
                    .build());

            shippingRateRepository.save(ShippingRate.builder()
                    .region(us)
                    .shippingMethod("OVERNIGHT")
                    .name("Overnight Shipping")
                    .description("Next business day")
                    .baseCost(new BigDecimal("24.99"))
                    .costPerKg(BigDecimal.ZERO)
                    .freeShippingThreshold(null)
                    .minDeliveryDays(1)
                    .maxDeliveryDays(1)
                    .isActive(true)
                    .isDefault(false)
                    .build());
            log.info("Created shipping rates: US");

            // Singapore Shipping
            shippingRateRepository.save(ShippingRate.builder()
                    .region(sg)
                    .shippingMethod("STANDARD")
                    .name("Standard Delivery")
                    .description("3-5 business days")
                    .baseCost(new BigDecimal("4.50"))
                    .costPerKg(BigDecimal.ZERO)
                    .freeShippingThreshold(new BigDecimal("60.00"))
                    .minDeliveryDays(3)
                    .maxDeliveryDays(5)
                    .isActive(true)
                    .isDefault(true)
                    .build());

            shippingRateRepository.save(ShippingRate.builder()
                    .region(sg)
                    .shippingMethod("EXPRESS")
                    .name("Express Delivery")
                    .description("1-2 business days")
                    .baseCost(new BigDecimal("8.00"))
                    .costPerKg(BigDecimal.ZERO)
                    .freeShippingThreshold(new BigDecimal("100.00"))
                    .minDeliveryDays(1)
                    .maxDeliveryDays(2)
                    .isActive(true)
                    .isDefault(false)
                    .build());

            shippingRateRepository.save(ShippingRate.builder()
                    .region(sg)
                    .shippingMethod("SAME_DAY")
                    .name("Same Day Delivery")
                    .description("Same day (order by 2pm)")
                    .baseCost(new BigDecimal("12.00"))
                    .costPerKg(BigDecimal.ZERO)
                    .freeShippingThreshold(null)
                    .minDeliveryDays(0)
                    .maxDeliveryDays(0)
                    .isActive(true)
                    .isDefault(false)
                    .build());
            log.info("Created shipping rates: SG");

            // India Shipping
            shippingRateRepository.save(ShippingRate.builder()
                    .region(in)
                    .shippingMethod("STANDARD")
                    .name("Standard Delivery")
                    .description("5-7 business days")
                    .baseCost(new BigDecimal("99.00")) // INR
                    .costPerKg(new BigDecimal("20.00"))
                    .freeShippingThreshold(new BigDecimal("999.00"))
                    .minDeliveryDays(5)
                    .maxDeliveryDays(7)
                    .isActive(true)
                    .isDefault(true)
                    .build());

            shippingRateRepository.save(ShippingRate.builder()
                    .region(in)
                    .shippingMethod("EXPRESS")
                    .name("Express Delivery")
                    .description("2-3 business days")
                    .baseCost(new BigDecimal("199.00")) // INR
                    .costPerKg(new BigDecimal("30.00"))
                    .freeShippingThreshold(new BigDecimal("1999.00"))
                    .minDeliveryDays(2)
                    .maxDeliveryDays(3)
                    .isActive(true)
                    .isDefault(false)
                    .build());

            shippingRateRepository.save(ShippingRate.builder()
                    .region(in)
                    .shippingMethod("PREMIUM")
                    .name("Premium Delivery")
                    .description("1-2 business days (Metro cities)")
                    .baseCost(new BigDecimal("349.00")) // INR
                    .costPerKg(new BigDecimal("40.00"))
                    .freeShippingThreshold(null)
                    .minDeliveryDays(1)
                    .maxDeliveryDays(2)
                    .isActive(true)
                    .isDefault(false)
                    .build());
            log.info("Created shipping rates: IN");

            // UK Shipping
            shippingRateRepository.save(ShippingRate.builder()
                    .region(gb)
                    .shippingMethod("STANDARD")
                    .name("Standard Delivery")
                    .description("3-5 business days")
                    .baseCost(new BigDecimal("3.99")) // GBP
                    .costPerKg(BigDecimal.ZERO)
                    .freeShippingThreshold(new BigDecimal("40.00"))
                    .minDeliveryDays(3)
                    .maxDeliveryDays(5)
                    .isActive(true)
                    .isDefault(true)
                    .build());

            shippingRateRepository.save(ShippingRate.builder()
                    .region(gb)
                    .shippingMethod("EXPRESS")
                    .name("Express Delivery")
                    .description("1-2 business days")
                    .baseCost(new BigDecimal("6.99")) // GBP
                    .costPerKg(BigDecimal.ZERO)
                    .freeShippingThreshold(new BigDecimal("75.00"))
                    .minDeliveryDays(1)
                    .maxDeliveryDays(2)
                    .isActive(true)
                    .isDefault(false)
                    .build());

            shippingRateRepository.save(ShippingRate.builder()
                    .region(gb)
                    .shippingMethod("NEXT_DAY")
                    .name("Next Day Delivery")
                    .description("Next business day (order by 6pm)")
                    .baseCost(new BigDecimal("9.99")) // GBP
                    .costPerKg(BigDecimal.ZERO)
                    .freeShippingThreshold(null)
                    .minDeliveryDays(1)
                    .maxDeliveryDays(1)
                    .isActive(true)
                    .isDefault(false)
                    .build());
            log.info("Created shipping rates: GB");

            log.info("✅ Pricing data initialization complete!");
            log.info("   - Currencies: 5 (USD, SGD, INR, GBP, EUR)");
            log.info("   - Regions: 4 (US, SG, IN, GB)");
            log.info("   - Tax Rates: 4");
            log.info("   - Shipping Rates: 12");
        };
    }
}
