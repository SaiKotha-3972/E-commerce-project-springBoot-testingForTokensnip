package com.jtspringproject.JtSpringProject.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global controller advice that exposes the store's display currency to all view models.
 * The currency code is injected as a model attribute named "storeCurrency" available in all JSP views.
 *
 * Note: This feature displays currency labels only (e.g., "USD 19"). No conversion is performed.
 */
@ControllerAdvice
public class StoreCurrencyAdvice {

    private final StoreCurrencyConfig storeCurrencyConfig;

    @Autowired
    public StoreCurrencyAdvice(StoreCurrencyConfig storeCurrencyConfig) {
        this.storeCurrencyConfig = storeCurrencyConfig;
    }

    /**
     * Exposes the store's currency code as a model attribute to all views.
     *
     * @return the ISO 4217 currency code (e.g., "USD", "EUR", "GBP")
     */
    @ModelAttribute("storeCurrency")
    public String storeCurrency() {
        return storeCurrencyConfig.getCurrencyCode();
    }
}
