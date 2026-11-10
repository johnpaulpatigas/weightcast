package com.johnpaulpatigas.weightcast.core.util

/**
 * Authoritative centralized constants for weight measurement validation.
 *
 * Note: These represent product-level validation limits for the WeightCast application
 * to filter out corrupted database records, extreme scale artifacts, or data entry errors.
 * They are not universal physiological limits.
 */
object WeightValidationConstants {
    const val MIN_PLAUSIBLE_WEIGHT_KG = 2.0
    const val MAX_PLAUSIBLE_WEIGHT_KG = 500.0
}
