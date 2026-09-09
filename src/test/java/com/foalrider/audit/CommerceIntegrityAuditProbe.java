package com.foalrider.audit;

import com.foalrider.config.StripeConfig;
import com.foalrider.modules.cart.entity.*;
import com.foalrider.modules.cart.repository.CartRepository;
import com.foalrider.modules.order.dto.*;
import com.foalrider.modules.order.entity.*;
import com.foalrider.modules.order.repository.OrderRepository;
import com.foalrider.modules.order.service.OrderServiceImpl;
import com.foalrider.modules.payment.service.PaymentServiceImpl;
import com.foalrider.modules.pricing.entity.Currency;
import com.foalrider.modules.pricing.entity.*;
import com.foalrider.modules.pricing.repository.*;
import com.foalrider.modules.pricing.service.PricingService;
import com.foalrider.modules.product.entity.*;
import com.foalrider.modules.product.repository.*;
import com.foalrider.modules.review.entity.*;
import com.foalrider.modules.review.repository.*;
import com.foalrider.modules.review.service.ReviewServiceImpl;
import com.foalrider.modules.user.entity.*;
import com.foalrider.modules.user.repository.UserRepository;
import com.foalrider.security.CustomUserDetails;
import com.stripe.Stripe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Opt-in secure-behavior assertions against real services with mocked persistence. */
class CommerceIntegrityAuditProbe {
    private OrderRepository orders;
    private UserRepository users;
    private CartRepository carts;
    private ProductVariantRepository variants;
    private PaymentServiceImpl payments;
    private OrderServiceImpl checkout;
    private Order order;
    private User user;
    private Product product;
    private Cart cart;
    private static final String WEBHOOK_SECRET = "whsec_local_audit_only";

    @BeforeEach void setup() {
        orders = mock(OrderRepository.class);
        users = mock(UserRepository.class);
        carts = mock(CartRepository.class);
        variants = mock(ProductVariantRepository.class);
        StripeConfig stripe = new StripeConfig();
        stripe.setWebhookSecret(WEBHOOK_SECRET);
        payments = new PaymentServiceImpl(orders, stripe);
        checkout = new OrderServiceImpl(orders, carts, users, variants);
        user = User.builder().email("audit@example.invalid").firstName("Audit").lastName("User")
                .role(Role.builder().name("ROLE_CUSTOMER").build()).build();
        user.setId(UUID.randomUUID());
        CustomUserDetails principal = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        order = Order.builder().user(user).orderNumber("AUDIT-1").totalAmount(new BigDecimal("100.00"))
                .paymentIntentId("pi_current_audit").build();
        order.setId(UUID.randomUUID());
        product = Product.builder().name("Audit shirt").sku("AUDIT-SKU").slug("audit-shirt")
                .basePrice(new BigDecimal("100.00")).build();
        product.setId(UUID.randomUUID());
        cart = Cart.builder().user(user).build();
        cart.setId(UUID.randomUUID());
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));
        when(orders.findByIdWithItems(order.getId())).thenReturn(Optional.of(order));
        when(orders.save(any())).thenAnswer(call -> call.getArgument(0));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(carts.findByUserIdWithItems(user.getId())).thenReturn(Optional.of(cart));
    }

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    private String event(String type, String intent, long amount, String currency) {
        return """
            {"id":"evt_local_audit","object":"event","api_version":"%s","type":"%s",
             "data":{"object":{"id":"%s","object":"payment_intent","amount":%d,"amount_received":%d,
             "currency":"%s","status":"%s","metadata":{"order_id":"%s"}}}}
            """.formatted(Stripe.API_VERSION, type, intent, amount, amount, currency,
                type.endsWith("succeeded") ? "succeeded" : "requires_payment_method", order.getId());
    }

    private void deliver(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(hmac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
        payments.handleWebhookEvent(payload, "t=" + timestamp + ",v1=" + signature);
    }

    @Test void signedMatchingSuccessCanMarkOrderPaid() throws Exception {
        deliver(event("payment_intent.succeeded", "pi_current_audit", 10000, "usd"));
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test void duplicateSuccessDoesNotRepeatStatusHistory() throws Exception {
        String payload = event("payment_intent.succeeded", "pi_current_audit", 10000, "usd");
        deliver(payload);
        deliver(payload);
        assertThat(order.getStatusHistory()).hasSize(1);
    }

    @Test void oldIntentMustNotPayCurrentOrder() throws Exception {
        deliver(event("payment_intent.succeeded", "pi_obsolete_audit", 10000, "usd"));
        assertThat(order.getPaymentStatus()).as("PAY-001: stored intent binding").isNotEqualTo(PaymentStatus.PAID);
    }

    @Test void insufficientAmountMustNotPayOrder() throws Exception {
        deliver(event("payment_intent.succeeded", "pi_current_audit", 1, "usd"));
        assertThat(order.getPaymentStatus()).as("PAY-001: expected amount binding").isNotEqualTo(PaymentStatus.PAID);
    }

    @Test void wrongCurrencyMustNotPayOrder() throws Exception {
        deliver(event("payment_intent.succeeded", "pi_current_audit", 10000, "jpy"));
        assertThat(order.getPaymentStatus()).as("PAY-001: expected currency binding").isNotEqualTo(PaymentStatus.PAID);
    }

    @Test void delayedFailureMustNotUndoSuccessfulPayment() throws Exception {
        deliver(event("payment_intent.succeeded", "pi_current_audit", 10000, "usd"));
        deliver(event("payment_intent.payment_failed", "pi_current_audit", 10000, "usd"));
        assertThat(order.getPaymentStatus()).as("PAY-001: monotonic lifecycle").isEqualTo(PaymentStatus.PAID);
    }

    @Test void webhookMustNotReopenCancelledOrder() throws Exception {
        order.setStatus(OrderStatus.CANCELLED);
        deliver(event("payment_intent.succeeded", "pi_current_audit", 10000, "usd"));
        assertThat(order.getStatus()).as("PAY-001: reconcile late success").isEqualTo(OrderStatus.CANCELLED);
    }

    @Test void webhookStorageFailureMustPropagateForRetry() {
        when(orders.save(any())).thenThrow(new IllegalStateException("simulated transient database outage"));
        assertThatThrownBy(() -> deliver(event("payment_intent.succeeded", "pi_current_audit", 10000, "usd")))
                .as("PAY-002: do not acknowledge lost event").isInstanceOf(Exception.class);
    }

    private CreateOrderRequest request() {
        return CreateOrderRequest.builder().shippingAddress(ShippingAddressRequest.builder()
                .name("Audit User").addressLine1("1 Test Road").city("Test City")
                .postalCode("12345").country("US").build()).build();
    }

    private void addCartItem(ProductVariant variant, BigDecimal price) {
        CartItem item = CartItem.builder().product(product).variant(variant).quantity(1).unitPrice(price).totalPrice(price).build();
        cart.addItem(item);
    }

    @Test void variantStockShortageRejectsCheckout() {
        ProductVariant variant = ProductVariant.builder().product(product).name("M").stockQuantity(0).build();
        addCartItem(variant, new BigDecimal("100.00"));
        assertThatThrownBy(() -> checkout.createOrder(request())).hasMessageContaining("Insufficient stock");
        verify(orders, never()).save(any());
    }

    @Test void checkoutMustRequireVariantForVariantManagedProduct() {
        product.getVariants().add(ProductVariant.builder().product(product).stockQuantity(0).build());
        addCartItem(null, new BigDecimal("100.00"));
        assertThatThrownBy(() -> checkout.createOrder(request())).as("ORD-001: cannot bypass inventory by omitting variant");
    }

    @Test void inactiveProductMustBeRejectedAtCheckout() {
        product.setIsActive(false);
        addCartItem(null, new BigDecimal("100.00"));
        assertThatThrownBy(() -> checkout.createOrder(request())).as("ORD-003: revalidate saleability");
    }

    @Test void checkoutMustRepriceOrRejectStaleCartPrice() {
        addCartItem(null, new BigDecimal("1.00"));
        OrderResponse response = checkout.createOrder(request());
        assertThat(response.getSubtotal()).as("PRICE-001: authoritative current price").isEqualByComparingTo("100.00");
    }

    @Test void customerCannotReadAnotherCustomersOrder() {
        User other = User.builder().build();
        other.setId(UUID.randomUUID());
        order.setUser(other);
        assertThatThrownBy(() -> checkout.getOrderById(order.getId())).hasMessage("Access denied");
    }

    @Test void adminStatusEditMustNotInventRefundWithoutProviderOperation() {
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaymentStatus(PaymentStatus.PAID);
        checkout.updateOrderStatus(order.getId(), UpdateOrderStatusRequest.builder().status(OrderStatus.REFUNDED).build());
        assertThat(order.getPaymentStatus()).as("PAY-004: refund requires provider result").isNotEqualTo(PaymentStatus.REFUNDED);
    }

    @Test void publicReviewLookupMustHidePendingContent() {
        ReviewRepository reviews = mock(ReviewRepository.class);
        Review review = Review.builder().product(product).user(user).rating(1).status(ReviewStatus.PENDING).build();
        review.setId(UUID.randomUUID());
        when(reviews.findById(review.getId())).thenReturn(Optional.of(review));
        ReviewServiceImpl service = new ReviewServiceImpl(reviews, mock(ReviewImageRepository.class),
                mock(ReviewVoteRepository.class), mock(ProductRepository.class), users, orders);
        assertThatThrownBy(() -> service.getReview(review.getId(), null)).as("SEC-009: pending reviews are private");
    }

    @Test void inclusiveTaxMustNotBeAddedTwice() {
        CurrencyRepository currencies = mock(CurrencyRepository.class);
        RegionRepository regions = mock(RegionRepository.class);
        TaxRateRepository taxes = mock(TaxRateRepository.class);
        ShippingRateRepository shipping = mock(ShippingRateRepository.class);
        RegionalPriceRepository regionalPrices = mock(RegionalPriceRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        Currency currency = Currency.builder().code("GBP").symbol("GBP ").build();
        Region region = Region.builder().code("GB").name("Test Region").defaultCurrency(currency).build();
        product.setBasePrice(new BigDecimal("120.00"));
        when(regions.findByCode("GB")).thenReturn(Optional.of(region));
        when(products.findById(product.getId())).thenReturn(Optional.of(product));
        when(taxes.findActiveByRegionCode("GB")).thenReturn(List.of(TaxRate.builder()
                .name("Synthetic inclusive tax").rate(new BigDecimal("0.20")).isInclusive(true).build()));
        when(shipping.findByRegionCodeAndMethod("GB", "STANDARD")).thenReturn(Optional.of(
                ShippingRate.builder().shippingMethod("STANDARD").baseCost(BigDecimal.ZERO).build()));
        PricingService service = new PricingService(currencies, regions, taxes, shipping, regionalPrices, products);
        assertThat(service.calculateFullPricing(List.of(new PricingService.CartItem(product.getId(), 1)), "GB", "STANDARD", null)
                .getGrandTotal()).as("PRICE-002: 120 inclusive stays 120 with free shipping").isEqualByComparingTo("120.00");
    }
}
