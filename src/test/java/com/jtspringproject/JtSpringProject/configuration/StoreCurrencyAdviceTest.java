package com.jtspringproject.JtSpringProject.configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Database-free tests of property binding, MVC exposure, and JSP label wiring. */
class StoreCurrencyAdviceTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> {
                context.getEnvironment().getPropertySources()
                        .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                context.getEnvironment().getPropertySources()
                        .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
            })
            .withUserConfiguration(StoreCurrencyConfig.class, StoreCurrencyAdvice.class);

    @Test
    void defaultsToUsdWithoutConfiguration() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(StoreCurrencyConfig.class).getCurrencyCode()).isEqualTo("USD");
            assertThat(context.getBean(StoreCurrencyAdvice.class).storeCurrency()).isEqualTo("USD");
        });
    }

    @Test
    void bindsCurrencyFromEnvironmentVariable() {
        runner.withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                new SystemEnvironmentPropertySource("testEnvironment",
                        Collections.singletonMap("STORE_CURRENCY", "EUR"))))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(StoreCurrencyConfig.class).getCurrencyCode()).isEqualTo("EUR");
                });
    }

    @Test
    void exposesConfiguredCurrencyToMvcViews() {
        runner.withPropertyValues("store.currency=EUR").run(context -> {
            assertThat(context).hasNotFailed();
            MockMvcBuilders.standaloneSetup(new CurrencyTestController())
                    .setControllerAdvice(context.getBean(StoreCurrencyAdvice.class))
                    .build().perform(get("/currency-check"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("currency-view"))
                    .andExpect(model().attribute("storeCurrency", "EUR"));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"NOTACURRENCY", "usd", "", " "})
    void invalidConfigurationFailsStartup(String code) {
        runner.withPropertyValues("store.currency=" + code).run(context ->
                assertThat(context).hasFailed().getFailure()
                        .hasRootCauseInstanceOf(IllegalArgumentException.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"usd", "NOTACURRENCY", " "})
    void invalidConstructorInputFailsExplicitly(String code) {
        assertThatThrownBy(() -> new StoreCurrencyConfig(code))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USD", "EUR", "GBP", "JPY", "INR", "CAD", "AUD"})
    void acceptsValidIsoCurrencyCodes(String code) {
        assertThat(new StoreCurrencyConfig(code).getCurrencyCode()).isEqualTo(code);
    }

    @ParameterizedTest
    @ValueSource(strings = {"index.jsp", "products.jsp", "productsAdd.jsp",
            "productsUpdate.jsp", "uproduct.jsp", "cartproduct.jsp"})
    void priceViewsDeclareJstlAndEscapeCurrency(String filename) throws Exception {
        String source = Files.readString(Path.of("src/main/webapp/views", filename));
        assertThat(source).as(filename)
                .contains("uri=\"http://java.sun.com/jsp/jstl/core\"")
                .contains("<c:out value=\"${storeCurrency}\"/>");
    }

    /** Minimal controller that avoids booting the application's database beans. */
    @Controller
    static class CurrencyTestController {
        @GetMapping("/currency-check")
        ModelAndView page() {
            return new ModelAndView("currency-view");
        }
    }
}
