package com.search.query.rerank;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loader for sort rules
 *
 * Loads and caches sort rules for different apps.
 * In production, this would load from a database or config service.
 */
@Component
public class SortRuleLoader {

    private static final Logger log = LoggerFactory.getLogger(SortRuleLoader.class);

    private final Map<String, SortRule> rules = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public SortRuleLoader() {
        this.mapper = new ObjectMapper();
        loadDefaultRules();
    }

    /**
     * Get sort rule for an app
     *
     * @param appKey the application key
     * @return the sort rule, or null if not found
     */
    public SortRule getRule(String appKey) {
        if (appKey == null) {
            return getDefaultRule();
        }

        SortRule rule = rules.get(appKey);
        if (rule == null) {
            log.debug("No rule found for appKey: {}, using default", appKey);
            return getDefaultRule();
        }

        return rule.isEnabled() ? rule : null;
    }

    /**
     * Get rule by ID
     *
     * @param ruleId the rule ID
     * @return the sort rule, or null if not found
     */
    public SortRule getRuleById(String ruleId) {
        return rules.values().stream()
                .filter(r -> r.getRuleId().equals(ruleId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Add or update a rule
     *
     * @param rule the sort rule
     */
    public void addRule(SortRule rule) {
        if (rule != null && rule.getRuleId() != null) {
            rules.put(rule.getAppKey(), rule);
            log.info("Added/updated sort rule: ruleId={}, appKey={}", rule.getRuleId(), rule.getAppKey());
        }
    }

    /**
     * Load default rules
     */
    private void loadDefaultRules() {
        // Default rule for ecommerce
        SortRule ecommerceRule = new SortRule();
        ecommerceRule.setRuleId("ecommerce-default");
        ecommerceRule.setAppKey("ecommerce");
        ecommerceRule.setEnabled(true);

        SortRule.Factor salesFactor = new SortRule.Factor();
        salesFactor.setField("sales");
        salesFactor.setWeight(0.4);
        salesFactor.setMode("log");

        SortRule.Factor freshnessFactor = new SortRule.Factor();
        freshnessFactor.setField("created_at");
        freshnessFactor.setWeight(0.3);
        freshnessFactor.setMode("linear");

        SortRule.Factor ratingFactor = new SortRule.Factor();
        ratingFactor.setField("rating");
        ratingFactor.setWeight(0.3);
        ratingFactor.setMode("linear");

        ecommerceRule.setFactors(java.util.List.of(salesFactor, freshnessFactor, ratingFactor));

        rules.put("ecommerce", ecommerceRule);
        log.info("Loaded default sort rules");
    }

    /**
     * Get default rule
     */
    private SortRule getDefaultRule() {
        SortRule defaultRule = new SortRule();
        defaultRule.setRuleId("default");
        defaultRule.setAppKey("default");
        defaultRule.setEnabled(true);

        // Simple default: just use the relevance score
        SortRule.Factor scoreFactor = new SortRule.Factor();
        scoreFactor.setField("_score");
        scoreFactor.setWeight(1.0);
        scoreFactor.setMode("linear");

        defaultRule.setFactors(java.util.List.of(scoreFactor));

        return defaultRule;
    }
}
