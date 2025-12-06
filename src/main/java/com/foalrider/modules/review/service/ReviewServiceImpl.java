package com.foalrider.modules.review.service;

import com.foalrider.modules.order.entity.OrderStatus;
import com.foalrider.modules.order.repository.OrderRepository;
import com.foalrider.modules.product.entity.Product;
import com.foalrider.modules.product.repository.ProductRepository;
import com.foalrider.modules.review.dto.*;
import com.foalrider.modules.review.entity.*;
import com.foalrider.modules.review.repository.ReviewImageRepository;
import com.foalrider.modules.review.repository.ReviewRepository;
import com.foalrider.modules.review.repository.ReviewVoteRepository;
import com.foalrider.modules.user.entity.User;
import com.foalrider.modules.user.repository.UserRepository;
import com.foalrider.shared.dto.PagedResponse;
import com.foalrider.shared.exception.BadRequestException;
import com.foalrider.shared.exception.ResourceNotFoundException;
import com.foalrider.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ReviewService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewVoteRepository reviewVoteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    public ReviewResponse createReview(CreateReviewRequest request, UUID userId) {
        // Check if user already reviewed this product
        if (reviewRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            throw new BadRequestException("You have already reviewed this product");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Check if user has purchased this product (verified purchase)
        boolean isVerifiedPurchase = hasUserPurchasedProduct(userId, request.getProductId());

        Review review = Review.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .title(request.getTitle())
                .content(request.getContent())
                .pros(request.getPros() != null ? request.getPros() : new ArrayList<>())
                .cons(request.getCons() != null ? request.getCons() : new ArrayList<>())
                .fitFeedback(request.getFitFeedback())
                .isVerifiedPurchase(isVerifiedPurchase)
                .status(ReviewStatus.PENDING) // Requires moderation
                .helpfulCount(0)
                .notHelpfulCount(0)
                .build();

        // Add images
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            int sortOrder = 0;
            for (ReviewImageRequest imageRequest : request.getImages()) {
                ReviewImage image = ReviewImage.builder()
                        .url(imageRequest.getUrl())
                        .altText(imageRequest.getAltText())
                        .sortOrder(imageRequest.getSortOrder() != null ? imageRequest.getSortOrder() : sortOrder++)
                        .build();
                review.addImage(image);
            }
        }

        review = reviewRepository.save(review);
        log.info("Review created for product {} by user {}", product.getId(), userId);

        return mapToResponse(review, userId);
    }

    @Override
    public ReviewResponse updateReview(UUID reviewId, UpdateReviewRequest request, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Check ownership
        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only update your own reviews");
        }

        // Update fields
        if (request.getRating() != null) {
            review.setRating(request.getRating());
        }
        if (request.getTitle() != null) {
            review.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            review.setContent(request.getContent());
        }
        if (request.getPros() != null) {
            review.setPros(request.getPros());
        }
        if (request.getCons() != null) {
            review.setCons(request.getCons());
        }
        if (request.getFitFeedback() != null) {
            review.setFitFeedback(request.getFitFeedback());
        }

        // Update images if provided
        if (request.getImages() != null) {
            // Clear existing images
            review.getImages().clear();
            
            // Add new images
            int sortOrder = 0;
            for (ReviewImageRequest imageRequest : request.getImages()) {
                ReviewImage image = ReviewImage.builder()
                        .url(imageRequest.getUrl())
                        .altText(imageRequest.getAltText())
                        .sortOrder(imageRequest.getSortOrder() != null ? imageRequest.getSortOrder() : sortOrder++)
                        .build();
                review.addImage(image);
            }
        }

        // Reset to pending if previously approved (needs re-moderation)
        if (review.getStatus() == ReviewStatus.APPROVED) {
            review.setStatus(ReviewStatus.PENDING);
        }

        review = reviewRepository.save(review);
        log.info("Review {} updated by user {}", reviewId, userId);

        return mapToResponse(review, userId);
    }

    @Override
    public void deleteReview(UUID reviewId, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Check ownership
        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
        
        // Update product rating
        updateProductRating(review.getProduct().getId());
        
        log.info("Review {} deleted by user {}", reviewId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReview(UUID reviewId, UUID currentUserId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        return mapToResponse(review, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getProductReviews(UUID productId, UUID currentUserId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByProductIdAndStatus(
                productId, ReviewStatus.APPROVED, pageable);

        return mapToPagedResponse(reviews, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getVerifiedReviews(UUID productId, UUID currentUserId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByProductIdAndStatusAndIsVerifiedPurchase(
                productId, ReviewStatus.APPROVED, true, pageable);

        return mapToPagedResponse(reviews, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getUserReviews(UUID userId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByUserId(userId, pageable);
        return mapToPagedResponse(reviews, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getMyReviews(UUID userId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByUserId(userId, pageable);
        return mapToPagedResponse(reviews, userId);
    }

    @Override
    public ReviewResponse voteReview(UUID reviewId, ReviewVoteRequest request, UUID userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Can't vote on own review
        if (review.getUser().getId().equals(userId)) {
            throw new BadRequestException("You cannot vote on your own review");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check for existing vote
        Optional<ReviewVote> existingVote = reviewVoteRepository.findByUserIdAndReviewId(userId, reviewId);

        if (existingVote.isPresent()) {
            ReviewVote vote = existingVote.get();
            
            // If same vote type, do nothing
            if (vote.getIsHelpful().equals(request.getIsHelpful())) {
                return mapToResponse(review, userId);
            }

            // Update vote and counts
            if (vote.getIsHelpful()) {
                review.decrementHelpfulCount();
            } else {
                review.decrementNotHelpfulCount();
            }

            vote.setIsHelpful(request.getIsHelpful());
            reviewVoteRepository.save(vote);

            if (request.getIsHelpful()) {
                review.incrementHelpfulCount();
            } else {
                review.incrementNotHelpfulCount();
            }
        } else {
            // Create new vote
            ReviewVote vote = ReviewVote.builder()
                    .review(review)
                    .user(user)
                    .isHelpful(request.getIsHelpful())
                    .build();
            reviewVoteRepository.save(vote);

            if (request.getIsHelpful()) {
                review.incrementHelpfulCount();
            } else {
                review.incrementNotHelpfulCount();
            }
        }

        review = reviewRepository.save(review);
        return mapToResponse(review, userId);
    }

    @Override
    public void removeVote(UUID reviewId, UUID userId) {
        Optional<ReviewVote> vote = reviewVoteRepository.findByUserIdAndReviewId(userId, reviewId);
        
        if (vote.isPresent()) {
            Review review = vote.get().getReview();
            
            if (vote.get().getIsHelpful()) {
                review.decrementHelpfulCount();
            } else {
                review.decrementNotHelpfulCount();
            }
            
            reviewRepository.save(review);
            reviewVoteRepository.delete(vote.get());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewStatsResponse getReviewStats(UUID productId) {
        Double avgRating = reviewRepository.calculateAverageRating(productId);
        Long totalReviews = reviewRepository.countApprovedReviews(productId);
        List<Object[]> distribution = reviewRepository.getRatingDistribution(productId);

        Map<Integer, Long> ratingDistribution = new HashMap<>();
        // Initialize with zeros
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(i, 0L);
        }
        // Fill with actual values
        for (Object[] row : distribution) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            ratingDistribution.put(rating, count);
        }

        // Count verified purchases
        Long verifiedCount = reviewRepository.findByProductIdAndStatusAndIsVerifiedPurchase(
                productId, ReviewStatus.APPROVED, true, Pageable.unpaged()).getTotalElements();

        return ReviewStatsResponse.builder()
                .productId(productId)
                .averageRating(avgRating != null ? BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .totalReviews(totalReviews)
                .ratingDistribution(ratingDistribution)
                .verifiedPurchaseCount(verifiedCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserReviewProduct(UUID userId, UUID productId) {
        // User can review if they haven't already reviewed
        return !reviewRepository.existsByUserIdAndProductId(userId, productId);
    }

    // Admin methods

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getPendingReviews(Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByStatus(ReviewStatus.PENDING, pageable);
        return mapToPagedResponse(reviews, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponse> getAllReviews(Pageable pageable) {
        Page<Review> reviews = reviewRepository.findAll(pageable);
        return mapToPagedResponse(reviews, null);
    }

    @Override
    public ReviewResponse moderateReview(UUID reviewId, ModerateReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        ReviewStatus previousStatus = review.getStatus();
        review.setStatus(request.getStatus());
        
        if (request.getAdminResponse() != null) {
            review.setAdminResponse(request.getAdminResponse());
        }

        review = reviewRepository.save(review);
        
        // Update product rating if review was approved
        if (request.getStatus() == ReviewStatus.APPROVED && previousStatus != ReviewStatus.APPROVED) {
            updateProductRating(review.getProduct().getId());
        } else if (previousStatus == ReviewStatus.APPROVED && request.getStatus() != ReviewStatus.APPROVED) {
            updateProductRating(review.getProduct().getId());
        }

        log.info("Review {} moderated to status {}", reviewId, request.getStatus());

        return mapToResponse(review, null);
    }

    @Override
    public ReviewResponse addAdminResponse(UUID reviewId, String response) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        review.setAdminResponse(response);
        review = reviewRepository.save(review);

        log.info("Admin response added to review {}", reviewId);

        return mapToResponse(review, null);
    }

    // Helper methods

    private boolean hasUserPurchasedProduct(UUID userId, UUID productId) {
        // Check if user has any delivered/completed order containing this product
        return orderRepository.findByUserId(userId, Pageable.unpaged()).stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED || 
                                 order.getStatus() == OrderStatus.COMPLETED)
                .flatMap(order -> order.getItems().stream())
                .anyMatch(item -> item.getProduct().getId().equals(productId));
    }

    private void updateProductRating(UUID productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return;

        Double avgRating = reviewRepository.calculateAverageRating(productId);
        Long reviewCount = reviewRepository.countApprovedReviews(productId);

        product.setAvgRating(avgRating != null ? 
                BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        product.setReviewCount(reviewCount != null ? reviewCount.intValue() : 0);

        productRepository.save(product);
        log.debug("Updated product {} rating to {} with {} reviews", productId, avgRating, reviewCount);
    }

    private ReviewResponse mapToResponse(Review review, UUID currentUserId) {
        // Get user's vote if logged in
        Boolean userVotedHelpful = null;
        if (currentUserId != null) {
            Optional<ReviewVote> vote = reviewVoteRepository.findByUserIdAndReviewId(currentUserId, review.getId());
            userVotedHelpful = vote.map(ReviewVote::getIsHelpful).orElse(null);
        }

        List<ReviewImageResponse> imageResponses = review.getImages().stream()
                .map(img -> ReviewImageResponse.builder()
                        .id(img.getId())
                        .url(img.getUrl())
                        .altText(img.getAltText())
                        .sortOrder(img.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .productSlug(review.getProduct().getSlug())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFullName())
                .userAvatarUrl(review.getUser().getAvatarUrl())
                .rating(review.getRating())
                .title(review.getTitle())
                .content(review.getContent())
                .pros(review.getPros())
                .cons(review.getCons())
                .fitFeedback(review.getFitFeedback())
                .isVerifiedPurchase(review.getIsVerifiedPurchase())
                .status(review.getStatus())
                .helpfulCount(review.getHelpfulCount())
                .notHelpfulCount(review.getNotHelpfulCount())
                .adminResponse(review.getAdminResponse())
                .images(imageResponses)
                .userVotedHelpful(userVotedHelpful)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private PagedResponse<ReviewResponse> mapToPagedResponse(Page<Review> page, UUID currentUserId) {
        List<ReviewResponse> content = page.getContent().stream()
                .map(review -> mapToResponse(review, currentUserId))
                .collect(Collectors.toList());

        return PagedResponse.<ReviewResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
