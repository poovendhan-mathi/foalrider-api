package com.foalrider.modules.review.entity;

/**
 * Review status enum for moderation workflow.
 */
public enum ReviewStatus {
    PENDING,    // Awaiting moderation
    APPROVED,   // Approved and visible
    REJECTED,   // Rejected by moderator
    FLAGGED     // Flagged for further review
}
