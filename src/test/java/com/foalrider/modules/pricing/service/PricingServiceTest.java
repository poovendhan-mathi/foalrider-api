package com.foalrider.modules.pricing.service;

import com.foalrider.modules.pricing.dto.*;
import com.foalrider.modules.pricing.entity.Currency;
import com.foalrider.modules.pricing.entity.Region;
import com.foalrider.modules.pricing.repository.CurrencyRepository;
import com.foalrider.modules.pricing.repository.RegionRepository;
import com.foalrider.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PricingService.
 * Tests currency conversion, regional pricing, and price calculations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PricingService Tests")
class PricingServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private PricingService pricingService;

    private Currency usdCurrency;
    private Currency eurCurrency;
    private Currency gbpCurrency;
    private Region usRegion;
    private Region euRegion;

    @BeforeEach
    void setUp() {
        // Setup currencies
        usdCurrency = Currency.builder()
                .id(UUID.randomUUID())
                .code("USD")
                .name("US Dollar")
                .symbol("$")
                .symbolPosition("BEFORE")
                .decimalPlaces(2)
                .exchangeRateToUsd(BigDecimal.ONE)
                .isActive(true)
                .isDefault(true)
                .build();

        eurCurrency = Currency.builder()
                .id(UUID.randomUUID())
                .code("EUR")
                .name("Euro")
                .symbol("€")
                .symbolPosition("BEFORE")
                .decimalPlaces(2)
                .exchangeRateToUsd(new BigDecimal("0.92"))
                .isActive(true)
                .isDefault(false)
                .build();

        gbpCurrency = Currency.builder()
                .id(UUID.randomUUID())
                .code("GBP")
                .name("British Pound")
                .symbol("£")
                .symbolPosition("BEFORE")
                .decimalPlaces(2)
                .exchangeRateToUsd(new BigDecimal("0.79"))
                .isActive(true)
                .isDefault(false)
                .build();

        // Setup regions
        usRegion = Region.builder()
                .id(UUID.randomUUID())
                .code("US")
                .name("United States")
                .currency(usdCurrency)
                .taxRate(new BigDecimal("0.08"))
                .isActive(true)
                .build();

        euRegion = Region.builder()
                .id(UUID.randomUUID())
                .code("EU")
                .name("European Union")
                .currency(eurCurrency)
                .taxRate(new BigDecimal("0.20"))
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("Currency Conversion Tests")
    class CurrencyConversionTests {

        @Test
        @DisplayName("Should convert USD to EUR successfully")
        void convertCurrency_UsdToEur_ShouldSucceed() {
            // Arrange
            BigDecimal amount = new BigDecimal("100.00");
            when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usdCurrency));
            when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eurCurrency));

            // Act
            CurrencyConversionResponse response = pricingService.convertCurrency(amount, "USD", "EUR");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getOriginalAmount()).isEqualByComparingTo(amount);
            assertThat(response.getOriginalCurrency()).isEqualTo("USD");
            assertThat(response.getTargetCurrency()).isEqualTo("EUR");
            assertThat(response.getConvertedAmount()).isEqualByComparingTo(new BigDecimal("92.00"));
        }

        @Test
        @DisplayName("Should convert EUR to USD successfully")
        void convertCurrency_EurToUsd_ShouldSucceed() {
            // Arrange
            BigDecimal amount = new BigDecimal("92.00");
            when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eurCurrency));
            when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usdCurrency));

            // Act
            CurrencyConversionResponse response = pricingService.convertCurrency(amount, "EUR", "USD");

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getTargetCurrency()).isEqualTo("USD");
            assertThat(response.getConvertedAmount()).isGreaterThan(amount);
        }

        @Test
        @DisplayName("Should throw exception for invalid currency code")
        void convertCurrency_WithInvalidCurrency_ShouldThrowException() {
            // Arrange
            BigDecimal amount = new BigDecimal("100.00");
            when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usdCurrency));
            when(currencyRepository.findByCode("XXX")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> pricingService.convertCurrency(amount, "USD", "XXX"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should return same amount when converting to same currency")
        void convertCurrency_SameCurrency_ShouldReturnSameAmount() {
            // Arrange
            BigDecimal amount = new BigDecimal("100.00");
            when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usdCurrency));

            // Act
            CurrencyConversionResponse response = pricingService.convertCurrency(amount, "USD", "USD");

            // Assert
            assertThat(response.getConvertedAmount()).isEqualByComparingTo(amount);
        }
    }

    @Nested
    @DisplayName("Get Currencies Tests")
    class GetCurrenciesTests {

        @Test
        @DisplayName("Should get all active currencies")
        void getAllCurrencies_ShouldReturnActiveCurrencies() {
            // Arrange
            when(currencyRepository.findAllByIsActiveTrue())
                    .thenReturn(Arrays.asList(usdCurrency, eurCurrency, gbpCurrency));

            // Act
            List<CurrencyDTO> currencies = pricingService.getAllCurrencies();

            // Assert
            assertThat(currencies).hasSize(3);
            assertThat(currencies).extracting(CurrencyDTO::getCode)
                    .containsExactlyInAnyOrder("USD", "EUR", "GBP");
        }

        @Test
        @DisplayName("Should get currency by code")
        void getCurrencyByCode_WithValidCode_ShouldReturnCurrency() {
            // Arrange
            when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usdCurrency));

            // Act
            CurrencyDTO currency = pricingService.getCurrencyByCode("USD");

            // Assert
            assertThat(currency).isNotNull();
            assertThat(currency.getCode()).isEqualTo("USD");
            assertThat(currency.getSymbol()).isEqualTo("$");
        }

        @Test
        @DisplayName("Should throw exception for invalid currency code")
        void getCurrencyByCode_WithInvalidCode_ShouldThrowException() {
            // Arrange
            when(currencyRepository.findByCode("XXX")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> pricingService.getCurrencyByCode("XXX"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Regions Tests")
    class GetRegionsTests {

        @Test
        @DisplayName("Should get all active regions")
        void getAllRegions_ShouldReturnActiveRegions() {
            // Arrange
            when(regionRepository.findAllByIsActiveTrue())
                    .thenReturn(Arrays.asList(usRegion, euRegion));

            // Act
            List<RegionDTO> regions = pricingService.getAllRegions();

            // Assert
            assertThat(regions).hasSize(2);
            assertThat(regions).extracting(RegionDTO::getCode)
                    .containsExactlyInAnyOrder("US", "EU");
        }

        @Test
        @DisplayName("Should get region by code")
        void getRegionByCode_WithValidCode_ShouldReturnRegion() {
            // Arrange
            when(regionRepository.findByCode("US")).thenReturn(Optional.of(usRegion));

            // Act
            RegionDTO region = pricingService.getRegionByCode("US");

            // Assert
            assertThat(region).isNotNull();
            assertThat(region.getCode()).isEqualTo("US");
            assertThat(region.getName()).isEqualTo("United States");
        }
    }

    @Nested
    @DisplayName("Price Calculation Tests")
    class PriceCalculationTests {

        @Test
        @DisplayName("Should calculate price with tax for US region")
        void calculatePrice_ForUsRegion_ShouldIncludeTax() {
            // Arrange
            BigDecimal basePrice = new BigDecimal("100.00");
            when(regionRepository.findByCode("US")).thenReturn(Optional.of(usRegion));

            // Act
            PricingCalculationDTO result = pricingService.calculatePrice(basePrice, "US");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getBasePrice()).isEqualByComparingTo(basePrice);
            assertThat(result.getTaxAmount()).isEqualByComparingTo(new BigDecimal("8.00"));
            assertThat(result.getTotalPrice()).isEqualByComparingTo(new BigDecimal("108.00"));
        }

        @Test
        @DisplayName("Should calculate price with higher tax for EU region")
        void calculatePrice_ForEuRegion_ShouldIncludeHigherTax() {
            // Arrange
            BigDecimal basePrice = new BigDecimal("100.00");
            when(regionRepository.findByCode("EU")).thenReturn(Optional.of(euRegion));

            // Act
            PricingCalculationDTO result = pricingService.calculatePrice(basePrice, "EU");

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTaxRate()).isEqualByComparingTo(new BigDecimal("0.20"));
            assertThat(result.getTaxAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(result.getTotalPrice()).isEqualByComparingTo(new BigDecimal("120.00"));
        }

        @Test
        @DisplayName("Should format price correctly")
        void formatPrice_ShouldReturnFormattedString() {
            // Arrange
            BigDecimal amount = new BigDecimal("1234.56");
            when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usdCurrency));

            // Act
            String formattedPrice = pricingService.formatPrice(amount, "USD");

            // Assert
            assertThat(formattedPrice).isEqualTo("$1,234.56");
        }

        @Test
        @DisplayName("Should format price with symbol after for some currencies")
        void formatPrice_WithSymbolAfter_ShouldFormatCorrectly() {
            // Arrange
            Currency inrCurrency = Currency.builder()
                    .code("INR")
                    .symbol("₹")
                    .symbolPosition("BEFORE")
                    .decimalPlaces(2)
                    .build();
            BigDecimal amount = new BigDecimal("1000.00");
            when(currencyRepository.findByCode("INR")).thenReturn(Optional.of(inrCurrency));

            // Act
            String formattedPrice = pricingService.formatPrice(amount, "INR");

            // Assert
            assertThat(formattedPrice).startsWith("₹");
        }
    }

    @Nested
    @DisplayName("Exchange Rate Tests")
    class ExchangeRateTests {

        @Test
        @DisplayName("Should get current exchange rate")
        void getExchangeRate_ShouldReturnCorrectRate() {
            // Arrange
            when(currencyRepository.findByCode("EUR")).thenReturn(Optional.of(eurCurrency));

            // Act
            BigDecimal rate = pricingService.getExchangeRate("EUR");

            // Assert
            assertThat(rate).isEqualByComparingTo(new BigDecimal("0.92"));
        }

        @Test
        @DisplayName("Should return 1 for USD exchange rate")
        void getExchangeRate_ForUsd_ShouldReturnOne() {
            // Arrange
            when(currencyRepository.findByCode("USD")).thenReturn(Optional.of(usdCurrency));

            // Act
            BigDecimal rate = pricingService.getExchangeRate("USD");

            // Assert
            assertThat(rate).isEqualByComparingTo(BigDecimal.ONE);
        }
    }
}
