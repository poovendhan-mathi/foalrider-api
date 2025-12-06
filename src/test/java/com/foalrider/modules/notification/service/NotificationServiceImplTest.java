package com.foalrider.modules.notification.service;

import com.foalrider.modules.notification.dto.*;
import com.foalrider.modules.notification.entity.Notification;
import com.foalrider.modules.notification.entity.NotificationChannel;
import com.foalrider.modules.notification.entity.NotificationPreference;
import com.foalrider.modules.notification.entity.NotificationType;
import com.foalrider.modules.notification.repository.NotificationPreferenceRepository;
import com.foalrider.modules.notification.repository.NotificationRepository;
import com.foalrider.modules.user.entity.User;
import com.foalrider.modules.user.repository.UserRepository;
import com.foalrider.shared.dto.PagedResponse;
import com.foalrider.shared.exception.ResourceNotFoundException;
import com.foalrider.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationServiceImpl.
 * Tests notification CRUD operations, read status management, and preferences.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Tests")
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User testUser;
    private UUID userId;
    private UUID notificationId;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();

        testUser = User.builder()
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();
        testUser.setId(userId);

        testNotification = Notification.builder()
                .user(testUser)
                .type(NotificationType.ORDER_CONFIRMED)
                .title("Order Confirmed")
                .message("Your order #12345 has been confirmed")
                .channel(NotificationChannel.IN_APP)
                .isRead(false)
                .sentAt(Instant.now())
                .build();
        testNotification.setId(notificationId);
    }

    @Nested
    @DisplayName("Create Notification Tests")
    class CreateNotificationTests {

        @Test
        @DisplayName("Should create notification successfully")
        void createNotification_WithValidRequest_ShouldSucceed() {
            // Arrange
            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .userId(userId)
                    .type(NotificationType.ORDER_CONFIRMED)
                    .title("Order Confirmed")
                    .message("Your order has been confirmed")
                    .actionUrl("/orders/123")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

            // Act
            NotificationResponse response = notificationService.createNotification(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getType()).isEqualTo(NotificationType.ORDER_CONFIRMED);
            assertThat(response.getTitle()).isEqualTo("Order Confirmed");
            verify(notificationRepository).save(any(Notification.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void createNotification_WithInvalidUser_ShouldThrowException() {
            // Arrange
            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .userId(UUID.randomUUID())
                    .type(NotificationType.ORDER_CONFIRMED)
                    .title("Order Confirmed")
                    .message("Your order has been confirmed")
                    .build();

            when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> notificationService.createNotification(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("Should create notification with metadata")
        void createNotification_WithMetadata_ShouldSucceed() {
            // Arrange
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("orderId", "12345");
            metadata.put("amount", 99.99);

            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .userId(userId)
                    .type(NotificationType.ORDER_SHIPPED)
                    .title("Order Shipped")
                    .message("Your order is on the way")
                    .metadata(metadata)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            NotificationResponse response = notificationService.createNotification(request);

            // Assert
            assertThat(response).isNotNull();
            verify(notificationRepository).save(argThat(notification ->
                    notification.getMetadata() != null &&
                    notification.getMetadata().containsKey("orderId")));
        }
    }

    @Nested
    @DisplayName("Send Notification Tests")
    class SendNotificationTests {

        @Test
        @DisplayName("Should send simple notification successfully")
        void sendNotification_WithBasicParams_ShouldSucceed() {
            // Arrange
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

            // Act
            NotificationResponse response = notificationService.sendNotification(
                    userId,
                    NotificationType.ORDER_CONFIRMED,
                    "Order Confirmed",
                    "Your order has been confirmed"
            );

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getType()).isEqualTo(NotificationType.ORDER_CONFIRMED);
        }

        @Test
        @DisplayName("Should send notification with action URL and metadata")
        void sendNotification_WithFullParams_ShouldSucceed() {
            // Arrange
            Map<String, Object> metadata = Map.of("key", "value");
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

            // Act
            NotificationResponse response = notificationService.sendNotification(
                    userId,
                    NotificationType.PROMOTION,
                    "Special Offer",
                    "Check out our new deals",
                    "/promotions",
                    metadata
            );

            // Assert
            assertThat(response).isNotNull();
            verify(notificationRepository).save(any(Notification.class));
        }
    }

    @Nested
    @DisplayName("Get User Notifications Tests")
    class GetUserNotificationsTests {

        @Test
        @DisplayName("Should get user notifications with pagination")
        void getUserNotifications_ShouldReturnPagedResponse() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            List<Notification> notifications = Arrays.asList(testNotification);
            Page<Notification> page = new PageImpl<>(notifications, pageable, 1);

            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
                    .thenReturn(page);

            // Act
            PagedResponse<NotificationResponse> response = notificationService.getUserNotifications(userId, pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should get unread notifications only")
        void getUnreadNotifications_ShouldReturnOnlyUnread() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            List<Notification> notifications = Arrays.asList(testNotification);
            Page<Notification> page = new PageImpl<>(notifications, pageable, 1);

            when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable))
                    .thenReturn(page);

            // Act
            PagedResponse<NotificationResponse> response = notificationService.getUnreadNotifications(userId, pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should get recent notifications (top 10)")
        void getRecentNotifications_ShouldReturnTop10() {
            // Arrange
            List<Notification> notifications = Arrays.asList(testNotification);
            when(notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId))
                    .thenReturn(notifications);

            // Act
            List<NotificationResponse> response = notificationService.getRecentNotifications(userId);

            // Assert
            assertThat(response).hasSize(1);
            verify(notificationRepository).findTop10ByUserIdOrderByCreatedAtDesc(userId);
        }
    }

    @Nested
    @DisplayName("Unread Count Tests")
    class UnreadCountTests {

        @Test
        @DisplayName("Should return correct unread count")
        void getUnreadCount_ShouldReturnCount() {
            // Arrange
            when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(5L);

            // Act
            NotificationCountResponse response = notificationService.getUnreadCount(userId);

            // Assert
            assertThat(response.getUnreadCount()).isEqualTo(5);
            assertThat(response.getHasUnread()).isTrue();
        }

        @Test
        @DisplayName("Should return zero when no unread notifications")
        void getUnreadCount_WithNoUnread_ShouldReturnZero() {
            // Arrange
            when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(0L);

            // Act
            NotificationCountResponse response = notificationService.getUnreadCount(userId);

            // Assert
            assertThat(response.getUnreadCount()).isZero();
            assertThat(response.getHasUnread()).isFalse();
        }
    }

    @Nested
    @DisplayName("Mark As Read Tests")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark single notification as read")
        void markAsRead_SingleNotification_ShouldSucceed() {
            // Arrange
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(testNotification));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            NotificationResponse response = notificationService.markAsRead(notificationId, userId);

            // Assert
            assertThat(response).isNotNull();
            verify(notificationRepository).save(any(Notification.class));
        }

        @Test
        @DisplayName("Should throw exception when notification not found")
        void markAsRead_NotFound_ShouldThrowException() {
            // Arrange
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> notificationService.markAsRead(notificationId, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Notification not found");
        }

        @Test
        @DisplayName("Should throw exception when marking another user's notification")
        void markAsRead_WrongUser_ShouldThrowException() {
            // Arrange
            UUID anotherUserId = UUID.randomUUID();
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(testNotification));

            // Act & Assert
            assertThatThrownBy(() -> notificationService.markAsRead(notificationId, anotherUserId))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("your own notifications");
        }

        @Test
        @DisplayName("Should mark multiple notifications as read")
        void markAsRead_MultipleNotifications_ShouldSucceed() {
            // Arrange
            List<UUID> notificationIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
            when(notificationRepository.markAsRead(eq(notificationIds), eq(userId), any(Instant.class)))
                    .thenReturn(2);

            // Act
            int count = notificationService.markAsRead(notificationIds, userId);

            // Assert
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("Should mark all notifications as read")
        void markAllAsRead_ShouldSucceed() {
            // Arrange
            when(notificationRepository.markAllAsRead(eq(userId), any(Instant.class))).thenReturn(10);

            // Act
            int count = notificationService.markAllAsRead(userId);

            // Assert
            assertThat(count).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Delete Notification Tests")
    class DeleteNotificationTests {

        @Test
        @DisplayName("Should delete notification successfully")
        void deleteNotification_ByOwner_ShouldSucceed() {
            // Arrange
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(testNotification));
            doNothing().when(notificationRepository).delete(testNotification);

            // Act
            notificationService.deleteNotification(notificationId, userId);

            // Assert
            verify(notificationRepository).delete(testNotification);
        }

        @Test
        @DisplayName("Should throw exception when notification not found")
        void deleteNotification_NotFound_ShouldThrowException() {
            // Arrange
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> notificationService.deleteNotification(notificationId, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Notification not found");
        }

        @Test
        @DisplayName("Should throw exception when deleting another user's notification")
        void deleteNotification_WrongUser_ShouldThrowException() {
            // Arrange
            UUID anotherUserId = UUID.randomUUID();
            when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(testNotification));

            // Act & Assert
            assertThatThrownBy(() -> notificationService.deleteNotification(notificationId, anotherUserId))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("your own notifications");
        }
    }

    @Nested
    @DisplayName("Notification Preferences Tests")
    class NotificationPreferencesTests {

        private NotificationPreference testPreference;

        @BeforeEach
        void setUp() {
            testPreference = NotificationPreference.builder()
                    .user(testUser)
                    .orderUpdatesEmail(true)
                    .orderUpdatesPush(true)
                    .orderUpdatesSms(false)
                    .promotionsEmail(true)
                    .promotionsPush(false)
                    .priceAlertsEmail(true)
                    .priceAlertsPush(true)
                    .stockAlertsEmail(true)
                    .stockAlertsPush(true)
                    .reviewUpdatesEmail(true)
                    .reviewUpdatesPush(false)
                    .securityAlertsEmail(true)
                    .newsletterEmail(false)
                    .build();
            testPreference.setId(UUID.randomUUID());
        }

        @Test
        @DisplayName("Should get existing preferences")
        void getPreferences_Existing_ShouldReturnPreferences() {
            // Arrange
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(testPreference));

            // Act
            NotificationPreferenceResponse response = notificationService.getPreferences(userId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getOrderUpdatesEmail()).isTrue();
            assertThat(response.getPromotionsEmail()).isTrue();
        }

        @Test
        @DisplayName("Should update preferences successfully")
        void updatePreferences_ShouldUpdateFields() {
            // Arrange
            UpdateNotificationPreferenceRequest request = UpdateNotificationPreferenceRequest.builder()
                    .orderUpdatesEmail(false)
                    .promotionsEmail(false)
                    .build();

            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(testPreference));
            when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            NotificationPreferenceResponse response = notificationService.updatePreferences(userId, request);

            // Assert
            assertThat(response).isNotNull();
            verify(preferenceRepository).save(any(NotificationPreference.class));
        }

        @Test
        @DisplayName("Should update only specified preference fields")
        void updatePreferences_PartialUpdate_ShouldOnlyUpdateSpecified() {
            // Arrange
            UpdateNotificationPreferenceRequest request = UpdateNotificationPreferenceRequest.builder()
                    .promotionsEmail(false)  // Only updating this field
                    .build();

            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(testPreference));
            when(preferenceRepository.save(any(NotificationPreference.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            notificationService.updatePreferences(userId, request);

            // Assert
            verify(preferenceRepository).save(argThat(pref ->
                    !pref.getPromotionsEmail() && pref.getOrderUpdatesEmail()));
        }
    }

    @Nested
    @DisplayName("Notification Type Tests")
    class NotificationTypeTests {

        @Test
        @DisplayName("Should create ORDER_PLACED notification")
        void createNotification_OrderPlaced_ShouldSucceed() {
            // Arrange
            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .userId(userId)
                    .type(NotificationType.ORDER_PLACED)
                    .title("Order Placed")
                    .message("Your order #12345 has been placed successfully")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
                Notification n = invocation.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            // Act
            NotificationResponse response = notificationService.createNotification(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getType()).isEqualTo(NotificationType.ORDER_PLACED);
        }

        @Test
        @DisplayName("Should create PRICE_DROP notification")
        void createNotification_PriceDrop_ShouldSucceed() {
            // Arrange
            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .userId(userId)
                    .type(NotificationType.PRICE_DROP)
                    .title("Price Drop Alert")
                    .message("A product in your wishlist has dropped in price!")
                    .actionUrl("/products/123")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
                Notification n = invocation.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            // Act
            NotificationResponse response = notificationService.createNotification(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getType()).isEqualTo(NotificationType.PRICE_DROP);
        }

        @Test
        @DisplayName("Should create BACK_IN_STOCK notification")
        void createNotification_BackInStock_ShouldSucceed() {
            // Arrange
            CreateNotificationRequest request = CreateNotificationRequest.builder()
                    .userId(userId)
                    .type(NotificationType.BACK_IN_STOCK)
                    .title("Back in Stock")
                    .message("Your wishlist item is now available!")
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
                Notification n = invocation.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            // Act
            NotificationResponse response = notificationService.createNotification(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getType()).isEqualTo(NotificationType.BACK_IN_STOCK);
        }
    }
}
