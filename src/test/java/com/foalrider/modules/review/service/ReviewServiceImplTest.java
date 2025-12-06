package com.foalrider.modules.review.service;

import com.foalrider.modules.order.repository.OrderRepository;
import com.foalrider.modules.product.entity.Product;
import com.foalrider.modules.product.repository.ProductRepository;
import com.foalrider.modules.review.dto.*;
import com.foalrider.modules.review.entity.*;
import com.foalrider.modules.review.repository.ReviewImageRepository;
import com.foalrider.modules.review.repository.ReviewRepository;
import com.foalrider.modules.review.repository.ReviewVoteRepository;
import com.foalrider.modules.user.entity.Role;
import com.foalrider.modules.user.entity.User;
import com.foalrider.modules.user.repository.UserRepository;
import com.foalrider.shared.dto.PagedResponse;
import com.foalrider.shared.exception.BadRequestException;
import com.foalrider.shared.exception.ResourceNotFoundException;
import com.foalrider.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReviewServiceImpl.
 * Tests review creation, retrieval, moderation, and voting.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Tests")
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewImageRepository reviewImageRepository;

    @Mock
    private ReviewVoteRepository reviewVoteRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User testUser;
    private Product testProduct;
    private Review testReview;
    private CreateReviewRequest createReviewRequest;
    private UUID userId;
    private UUID productId;
    private UUID reviewId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        reviewId = UUID.randomUUID();

        // Setup test user
        Role customerRole = Role.builder()
                .id(UUID.randomUUID())
                .name("CUSTOMER")
                .build();

        testUser = User.builder()
                .id(userId)
                .email("customer@test.com")
                .firstName("John")
                .lastName("Doe")
                .role(customerRole)
                .isActive(true)
                .build();

        // Setup test product
        testProduct = Product.builder()
                .id(productId)
                .name("Test Product")
                .slug("test-product")
                .sku("TST-001")
                .basePrice(new BigDecimal("29.99"))
                .isActive(true)
                .images(new ArrayList<>())
                .build();

        // Setup test review
        testReview = Review.builder()
                .id(reviewId)
                .product(testProduct)
                .user(testUser)
                .rating(5)
                .title("Great product!")
                .content("Really loved this product. Highly recommend!")
                .pros(Arrays.asList("Good quality", "Fast delivery"))
                .cons(Arrays.asList("Slightly expensive"))
                .isVerifiedPurchase(true)
                .status(ReviewStatus.APPROVED)
                .helpfulCount(10)
                .notHelpfulCount(2)
                .images(new ArrayList<>())
                .build();

        // Setup create review request
        createReviewRequest = CreateReviewRequest.builder()
                .productId(productId)
                .rating(5)
                .title("Excellent!")
                .content("This product exceeded my expectations!")
                .pros(Arrays.asList("High quality", "Great fit"))
                .cons(Arrays.asList("None"))
                .build();
    }

    @Nested
    @DisplayName("Create Review Tests")
    class CreateReviewTests {

        @Test
        @DisplayName("Should create review successfully")
        void createReview_WithValidRequest_ShouldSucceed() {
            // Arrange
            when(reviewRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(orderRepository.existsByUserIdAndProductIdAndStatus(eq(userId), eq(productId), any()))
                    .thenReturn(true); // Verified purchase
            when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
                Review review = invocation.getArgument(0);
                review.setId(reviewId);
                return review;
            });

            // Act
            ReviewResponse response = reviewService.createReview(createReviewRequest, userId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getRating()).isEqualTo(5);
            assertThat(response.getTitle()).isEqualTo("Excellent!");
            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("Should throw exception when user already reviewed product")
        void createReview_WithExistingReview_ShouldThrowException() {
            // Arrange
            when(reviewRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> reviewService.createReview(createReviewRequest, userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already reviewed");

            verify(reviewRepository, never()).save(any(Review.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void createReview_WithInvalidUser_ShouldThrowException() {
            // Arrange
            when(reviewRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> reviewService.createReview(createReviewRequest, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when product not found")
        void createReview_WithInvalidProduct_ShouldThrowException() {
            // Arrange
            when(reviewRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> reviewService.createReview(createReviewRequest, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should mark review as verified purchase when applicable")
        void createReview_WithVerifiedPurchase_ShouldMarkAsVerified() {
            // Arrange
            when(reviewRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
            when(orderRepository.existsByUserIdAndProductIdAndStatus(eq(userId), eq(productId), any()))
                    .thenReturn(true);
            when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
                Review review = invocation.getArgument(0);
                review.setId(reviewId);
                return review;
            });

            // Act
            ReviewResponse response = reviewService.createReview(createReviewRequest, userId);

            // Assert
            assertThat(response).isNotNull();
            verify(reviewRepository).save(argThat(review -> review.getIsVerifiedPurchase()));
        }
    }

    @Nested
    @DisplayName("Get Reviews Tests")
    class GetReviewsTests {

        @Test
        @DisplayName("Should get product reviews successfully")
        void getProductReviews_WithValidProduct_ShouldReturnReviews() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> reviewPage = new PageImpl<>(Arrays.asList(testReview), pageable, 1);
            
            when(reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.APPROVED, pageable))
                    .thenReturn(reviewPage);

            // Act
            PagedResponse<ReviewResponse> response = reviewService.getProductReviews(productId, pageable, null);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should get user reviews successfully")
        void getUserReviews_WithValidUser_ShouldReturnReviews() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> reviewPage = new PageImpl<>(Arrays.asList(testReview), pageable, 1);
            
            when(reviewRepository.findByUserId(userId, pageable)).thenReturn(reviewPage);

            // Act
            PagedResponse<ReviewResponse> response = reviewService.getUserReviews(userId, pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should get review by ID successfully")
        void getReviewById_WithValidId_ShouldReturnReview() {
            // Arrange
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));

            // Act
            ReviewResponse response = reviewService.getReviewById(reviewId, null);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(reviewId);
            assertThat(response.getRating()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should throw exception when review not found")
        void getReviewById_WithInvalidId_ShouldThrowException() {
            // Arrange
            UUID invalidId = UUID.randomUUID();
            when(reviewRepository.findById(invalidId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> reviewService.getReviewById(invalidId, null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update Review Tests")
    class UpdateReviewTests {

        @Test
        @DisplayName("Should update review successfully")
        void updateReview_WithValidRequest_ShouldSucceed() {
            // Arrange
            UpdateReviewRequest updateRequest = UpdateReviewRequest.builder()
                    .rating(4)
                    .title("Updated title")
                    .content("Updated content")
                    .build();

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
            when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

            // Act
            ReviewResponse response = reviewService.updateReview(reviewId, updateRequest, userId);

            // Assert
            assertThat(response).isNotNull();
            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("Should throw exception when updating another user's review")
        void updateReview_WithWrongUser_ShouldThrowException() {
            // Arrange
            UUID anotherUserId = UUID.randomUUID();
            UpdateReviewRequest updateRequest = UpdateReviewRequest.builder()
                    .rating(4)
                    .build();

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));

            // Act & Assert
            assertThatThrownBy(() -> reviewService.updateReview(reviewId, updateRequest, anotherUserId))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("Delete Review Tests")
    class DeleteReviewTests {

        @Test
        @DisplayName("Should delete review successfully")
        void deleteReview_WithValidId_ShouldSucceed() {
            // Arrange
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
            doNothing().when(reviewRepository).delete(any(Review.class));

            // Act
            reviewService.deleteReview(reviewId, userId);

            // Assert
            verify(reviewRepository).delete(testReview);
        }

        @Test
        @DisplayName("Should throw exception when deleting another user's review")
        void deleteReview_WithWrongUser_ShouldThrowException() {
            // Arrange
            UUID anotherUserId = UUID.randomUUID();
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));

            // Act & Assert
            assertThatThrownBy(() -> reviewService.deleteReview(reviewId, anotherUserId))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("Vote Review Tests")
    class VoteReviewTests {

        @Test
        @DisplayName("Should add helpful vote successfully")
        void voteReview_WithHelpfulVote_ShouldSucceed() {
            // Arrange
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
            when(reviewVoteRepository.existsByReviewIdAndUserId(reviewId, userId)).thenReturn(false);
            when(reviewVoteRepository.save(any(ReviewVote.class))).thenAnswer(invocation -> {
                ReviewVote vote = invocation.getArgument(0);
                vote.setId(UUID.randomUUID());
                return vote;
            });
            when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

            // Act
            ReviewResponse response = reviewService.voteReview(reviewId, true, userId);

            // Assert
            assertThat(response).isNotNull();
            verify(reviewVoteRepository).save(any(ReviewVote.class));
        }

        @Test
        @DisplayName("Should throw exception when user already voted")
        void voteReview_WithExistingVote_ShouldThrowException() {
            // Arrange
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
            when(reviewVoteRepository.existsByReviewIdAndUserId(reviewId, userId)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> reviewService.voteReview(reviewId, true, userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already voted");
        }

        @Test
        @DisplayName("Should prevent voting on own review")
        void voteReview_OnOwnReview_ShouldThrowException() {
            // Arrange - testReview already has testUser as author
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));

            // Act & Assert
            assertThatThrownBy(() -> reviewService.voteReview(reviewId, true, userId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("cannot vote on your own review");
        }
    }

    @Nested
    @DisplayName("Moderation Tests (Admin)")
    class ModerationTests {

        @Test
        @DisplayName("Should approve review successfully")
        void moderateReview_Approve_ShouldSucceed() {
            // Arrange
            testReview.setStatus(ReviewStatus.PENDING);
            ModerateReviewRequest request = ModerateReviewRequest.builder()
                    .status(ReviewStatus.APPROVED)
                    .build();

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
            when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

            // Act
            ReviewResponse response = reviewService.moderateReview(reviewId, request);

            // Assert
            assertThat(response).isNotNull();
            verify(reviewRepository).save(argThat(review -> 
                    review.getStatus() == ReviewStatus.APPROVED));
        }

        @Test
        @DisplayName("Should reject review with reason")
        void moderateReview_Reject_ShouldSucceed() {
            // Arrange
            testReview.setStatus(ReviewStatus.PENDING);
            ModerateReviewRequest request = ModerateReviewRequest.builder()
                    .status(ReviewStatus.REJECTED)
                    .moderationNotes("Inappropriate content")
                    .build();

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(testReview));
            when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

            // Act
            ReviewResponse response = reviewService.moderateReview(reviewId, request);

            // Assert
            assertThat(response).isNotNull();
            verify(reviewRepository).save(argThat(review -> 
                    review.getStatus() == ReviewStatus.REJECTED));
        }

        @Test
        @DisplayName("Should get pending reviews for moderation")
        void getPendingReviews_ShouldReturnPendingReviews() {
            // Arrange
            testReview.setStatus(ReviewStatus.PENDING);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> reviewPage = new PageImpl<>(Arrays.asList(testReview), pageable, 1);
            
            when(reviewRepository.findByStatus(ReviewStatus.PENDING, pageable))
                    .thenReturn(reviewPage);

            // Act
            PagedResponse<ReviewResponse> response = reviewService.getPendingReviews(pageable);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Product Rating Statistics Tests")
    class RatingStatisticsTests {

        @Test
        @DisplayName("Should get product rating statistics")
        void getProductRatingStats_ShouldReturnStatistics() {
            // Arrange
            when(reviewRepository.getAverageRatingByProductId(productId))
                    .thenReturn(Optional.of(4.5));
            when(reviewRepository.countByProductIdAndStatus(productId, ReviewStatus.APPROVED))
                    .thenReturn(25L);
            when(reviewRepository.countByProductIdAndStatusAndRating(productId, ReviewStatus.APPROVED, 5))
                    .thenReturn(15L);
            when(reviewRepository.countByProductIdAndStatusAndRating(productId, ReviewStatus.APPROVED, 4))
                    .thenReturn(7L);
            when(reviewRepository.countByProductIdAndStatusAndRating(productId, ReviewStatus.APPROVED, 3))
                    .thenReturn(2L);
            when(reviewRepository.countByProductIdAndStatusAndRating(productId, ReviewStatus.APPROVED, 2))
                    .thenReturn(1L);
            when(reviewRepository.countByProductIdAndStatusAndRating(productId, ReviewStatus.APPROVED, 1))
                    .thenReturn(0L);

            // Act
            ProductRatingStatsResponse response = reviewService.getProductRatingStats(productId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAverageRating()).isEqualTo(4.5);
            assertThat(response.getTotalReviews()).isEqualTo(25);
        }
    }
}
