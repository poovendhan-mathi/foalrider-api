package com.foalrider.audit;

import com.foalrider.config.*;
import com.foalrider.modules.auth.controller.AuthController;
import com.foalrider.modules.auth.service.AuthService;
import com.foalrider.modules.order.entity.*;
import com.foalrider.modules.order.repository.OrderRepository;
import com.foalrider.modules.payment.controller.*;
import com.foalrider.modules.payment.service.PaymentServiceImpl;
import com.foalrider.modules.user.entity.*;
import com.foalrider.modules.user.repository.UserRepository;
import com.foalrider.security.*;
import com.foalrider.security.jwt.*;
import com.foalrider.shared.exception.GlobalExceptionHandler;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Opt-in audit acceptance probes: failures identify OPEN findings, not harness errors.
 * Uses the real security filter chain, JWT provider, controllers and payment service.
 * All persistence is mocked. No Boot profiles, database, email or provider calls.
 * Run explicitly with -Dtest=SecurityBoundaryAuditProbe (see docs/audit/TESTING.md).
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = SecurityBoundaryAuditProbe.Config.class)
class SecurityBoundaryAuditProbe {
    private static final byte[] KEY = "local-audit-key-only-never-use-in-deployment-0123456789-abcdefghijk".getBytes(StandardCharsets.UTF_8);
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired OrderRepository orders;
    @Autowired JwtTokenProvider tokens;
    @Autowired AuthService auth;
    private MockMvc mvc;
    private User user;
    private Order order;

    @BeforeEach void setup() {
        reset(users, orders, auth);
        SecurityContextHolder.clearContext();
        user = User.builder().email("customer@example.invalid").firstName("Audit").lastName("Customer")
                .passwordHash("unused").role(Role.builder().name("ROLE_CUSTOMER").build()).isActive(true).build();
        user.setId(USER_ID);
        order = Order.builder().user(user).orderNumber("AUDIT-1").totalAmount(new BigDecimal("100.00"))
                .paymentIntentId("pi_audit_unpaid").build();
        order.setId(UUID.fromString("20000000-0000-0000-0000-000000000001"));
        when(users.findById(USER_ID)).thenReturn(Optional.of(user));
        when(orders.findByPaymentIntentId(order.getPaymentIntentId())).thenReturn(Optional.of(order));
        when(orders.save(any())).thenAnswer(call -> call.getArgument(0));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    private String bearer() { return "Bearer " + tokens.generateAccessToken(new CustomUserDetails(user)); }

    @Test void anonymousPaymentConfirmationIsRejected() throws Exception {
        mvc.perform(post("/payments/confirm/pi_audit_unpaid")).andExpect(status().isUnauthorized());
        verify(orders, never()).save(any());
    }

    @Test void customerCannotUseAdminRefundEndpoint() throws Exception {
        mvc.perform(post("/payments/refund/" + order.getId()).header("Authorization", bearer()))
                .andExpect(status().isForbidden());
        verify(orders, never()).save(any());
    }

    @Test void validCustomerTokenCanReachAuthenticatedEndpoint() throws Exception {
        mvc.perform(post("/auth/logout-all").header("Authorization", bearer())).andExpect(status().isOk());
        verify(auth).logoutAll();
    }

    @Test void unpaidIntentMustNotBecomePaidByClientAssertion() throws Exception {
        mvc.perform(post("/payments/confirm/pi_audit_unpaid").header("Authorization", bearer()));
        assertThat(order.getPaymentStatus()).as("SEC-001: require authoritative Stripe success").isNotEqualTo(PaymentStatus.PAID);
    }

    @Test void customerMustNotConfirmAnotherCustomersPayment() throws Exception {
        User other = User.builder().build();
        other.setId(UUID.randomUUID());
        order.setUser(other);
        mvc.perform(post("/payments/confirm/pi_audit_unpaid").header("Authorization", bearer()));
        assertThat(order.getPaymentStatus()).as("SEC-001: order ownership").isNotEqualTo(PaymentStatus.PAID);
    }

    @ParameterizedTest @ValueSource(strings = {"CANCELLED", "REFUNDED", "DELIVERED"})
    void confirmationMustNotReopenTerminalOrFulfilledOrder(String state) throws Exception {
        order.setStatus(OrderStatus.valueOf(state));
        mvc.perform(post("/payments/confirm/pi_audit_unpaid").header("Authorization", bearer()));
        assertThat(order.getStatus()).as("SEC-001: lifecycle cannot regress").isEqualTo(OrderStatus.valueOf(state));
    }

    @Test void deactivatedUserMustNotAuthenticateWithPreviouslyIssuedJwt() throws Exception {
        String token = bearer();
        user.setIsActive(false);
        mvc.perform(post("/auth/logout-all").header("Authorization", token));
        verify(auth, never()).logoutAll();
    }

    @Test void wrongIssuerMustBeRejected() {
        String token = Jwts.builder().subject(USER_ID.toString()).issuer("other-application")
                .expiration(Date.from(Instant.now().plusSeconds(60))).signWith(Keys.hmacShaKeyFor(KEY)).compact();
        assertThat(tokens.validateToken(token)).as("SEC-006: issuer isolation").isFalse();
    }

    @Test void missingExpirationMustBeRejected() {
        String token = Jwts.builder().subject(USER_ID.toString()).issuer("foalrider-api")
                .signWith(Keys.hmacShaKeyFor(KEY)).compact();
        assertThat(tokens.validateToken(token)).as("SEC-006: tokens require expiry").isFalse();
    }

    @Test void expiredJwtIsRejected() {
        String token = Jwts.builder().subject(USER_ID.toString()).issuer("foalrider-api")
                .expiration(Date.from(Instant.now().minusSeconds(60))).signWith(Keys.hmacShaKeyFor(KEY)).compact();
        assertThat(tokens.validateToken(token)).isFalse();
    }

    @Test void wrongSignatureIsRejected() {
        String token = Jwts.builder().subject(USER_ID.toString()).expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Jwts.SIG.HS256.key().build()).compact();
        assertThat(tokens.validateToken(token)).isFalse();
    }

    @ParameterizedTest @ValueSource(strings = {"not-a-jwt", "eyJhbGciOiJub25lIn0.eyJzdWIiOiJhbm9ueW1vdXMifQ.", ""})
    void malformedOrUnsignedJwtIsRejected(String token) { assertThat(tokens.validateToken(token)).isFalse(); }

    @Test void validationResponseMustNotEchoPassword() throws Exception {
        String body = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"audit@example.invalid\",\"password\":\"tiny!\",\"firstName\":\"Audit\",\"lastName\":\"User\"}"))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();
        assertThat(body).as("SEC-005: credential redaction").doesNotContain("tiny!");
        verifyNoInteractions(auth);
    }

    @Test void malformedJsonMustReturn400() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest());
    }

    @Test void missingWebhookSignatureIsRejected() throws Exception {
        mvc.perform(post("/webhooks/stripe").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is4xxClientError());
        verify(orders, never()).save(any());
    }

    @Test void invalidWebhookSignatureIsRejected() throws Exception {
        mvc.perform(post("/webhooks/stripe").contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=0,v1=invalid").content("{}"))
                .andExpect(status().isBadRequest());
        verify(orders, never()).save(any());
    }

    @Configuration @EnableWebMvc
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthEntryPoint.class,
            CustomUserDetailsService.class, PaymentController.class, WebhookController.class,
            AuthController.class, GlobalExceptionHandler.class})
    static class Config {
        @Bean com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
            return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        }
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean OrderRepository orders() { return mock(OrderRepository.class); }
        @Bean AuthService auth() { return mock(AuthService.class); }
        @Bean JwtConfig jwtConfig() {
            JwtConfig config = new JwtConfig();
            config.setSecret(Base64.getEncoder().encodeToString(KEY));
            return config;
        }
        @Bean JwtTokenProvider tokens(JwtConfig config) { return new JwtTokenProvider(config); }
        @Bean StripeConfig stripe() {
            StripeConfig config = new StripeConfig();
            config.setApiKey("sk_test_audit_unused");
            config.setWebhookSecret("whsec_local_audit_only");
            return config;
        }
        @Bean PaymentServiceImpl payment(OrderRepository repo, StripeConfig config) { return new PaymentServiceImpl(repo, config); }
    }
}
