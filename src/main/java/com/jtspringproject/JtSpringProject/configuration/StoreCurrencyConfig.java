package com.jtspringproject.JtSpringProject.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Currency;

/**
 * Immutable configuration holder for the store's display currency.
 * Validates the ISO 4217 currency code at construction time.
 * Supports override via the "store.currency" Spring property (defaults to USD).
 *
 * Note: This is a label/display feature only. No currency conversion is performed.
 * All prices in the database remain as integers without currency metadata.
 */
@Component
public class StoreCurrencyConfig {

    private final String currencyCode;

    /**
     * Constructor with property injection.
     * Validates the currency code immediately; invalid codes trigger IllegalArgumentException.
     *
     * @param currencyCode the ISO 4217 currency code (e.g., USD, EUR, GBP), defaults to USD
     * @throws IllegalArgumentException if currencyCode is null, empty, or not a valid ISO 4217 code
     */
    public StoreCurrencyConfig(@Value("${store.currency:USD}") String currencyCode) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "store.currency property must not be null or empty. " +
                    "Please set 'store.currency' to a valid ISO 4217 currency code (e.g., USD, EUR, GBP).");
        }

        this.currencyCode = currencyCode;
        validateCurrencyCode();
    }

    /**
     * Returns the validated ISO 4217 currency code.
     *
     * @return the currency code (e.g., "USD", "EUR", "GBP")
     */
    public String getCurrencyCode() {
        return currencyCode;
    }

    /**
     * Validates that the currency code is a valid ISO 4217 code.
     * Throws IllegalArgumentException if invalid.
     *
     * @throws IllegalArgumentException if the code is not a valid ISO 4217 code
     */
    private void validateCurrencyCode() {
        try {
            Currency.getInstance(currencyCode);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid currency code configured: " + currencyCode +
                    ". Please set 'store.currency' to a valid ISO 4217 currency code (e.g., USD, EUR, GBP). " +
                    "Note: currency codes are case-sensitive.", e);
        }
    }
}
