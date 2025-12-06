package com.foalrider.shared.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utility class for generating URL-friendly slugs from strings.
 */
public final class SlugUtils {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTIPLE_DASHES = Pattern.compile("-+");

    private SlugUtils() {
        // Prevent instantiation
    }

    /**
     * Generate a URL-friendly slug from a string.
     * 
     * @param input The input string
     * @return A lowercase, hyphenated slug
     */
    public static String toSlug(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // Normalize unicode characters
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        
        // Replace whitespace with hyphens
        String slug = WHITESPACE.matcher(normalized).replaceAll("-");
        
        // Remove non-latin characters
        slug = NON_LATIN.matcher(slug).replaceAll("");
        
        // Replace multiple dashes with single dash
        slug = MULTIPLE_DASHES.matcher(slug).replaceAll("-");
        
        // Convert to lowercase and trim dashes from ends
        slug = slug.toLowerCase(Locale.ENGLISH);
        slug = slug.replaceAll("^-+|-+$", "");
        
        return slug;
    }

    /**
     * Generate a unique slug by appending a suffix if needed.
     * 
     * @param baseSlug The base slug
     * @param suffix The suffix to append (e.g., a counter)
     * @return The unique slug
     */
    public static String toUniqueSlug(String baseSlug, int suffix) {
        if (suffix <= 0) {
            return baseSlug;
        }
        return baseSlug + "-" + suffix;
    }

    /**
     * Check if a string is a valid slug format.
     * 
     * @param slug The slug to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidSlug(String slug) {
        if (slug == null || slug.isEmpty()) {
            return false;
        }
        return slug.matches("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    }
}
