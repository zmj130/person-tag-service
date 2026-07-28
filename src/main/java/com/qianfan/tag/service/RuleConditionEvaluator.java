package com.qianfan.tag.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianfan.tag.common.BusinessException;
import com.qianfan.tag.domain.IndicatorDefinition;
import com.qianfan.tag.domain.PersonRecord;
import com.qianfan.tag.domain.TagRuleCondition;
import com.qianfan.tag.domain.TagRuleSet;
import com.qianfan.tag.mapper.IndicatorMapper;
import com.qianfan.tag.mapper.StructuredRuleMapper;
import com.qianfan.tag.trie.TextNormalizer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RuleConditionEvaluator {
    private final StructuredRuleMapper ruleMapper;
    private final IndicatorMapper indicatorMapper;
    private final IndicatorService indicatorService;
    private final TextNormalizer normalizer;
    private final ObjectMapper objectMapper;

    public RuleConditionEvaluator(StructuredRuleMapper ruleMapper, IndicatorMapper indicatorMapper,
                                  IndicatorService indicatorService, TextNormalizer normalizer,
                                  ObjectMapper objectMapper) {
        this.ruleMapper = ruleMapper;
        this.indicatorMapper = indicatorMapper;
        this.indicatorService = indicatorService;
        this.normalizer = normalizer;
        this.objectMapper = objectMapper;
    }

    public RuleEvaluationResult evaluate(PersonRecord person, TagRuleSet ruleSet) {
        List<TagRuleCondition> conditions = ruleSet.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            conditions = ruleMapper.findConditions(ruleSet.getId());
        }
        boolean all = "ALL".equals(ruleSet.getMatchMode());
        boolean finalMatch = all;
        List<Map<String, Object>> details = new ArrayList<Map<String, Object>>();
        for (TagRuleCondition condition : conditions) {
            IndicatorDefinition definition = indicatorMapper.findDefinitionById(condition.getIndicatorId());
            if (definition == null || definition.getStatus() == null || definition.getStatus() != 1) {
                throw new BusinessException("RULE_INDICATOR_UNAVAILABLE", "规则引用的指标不存在或已停用");
            }
            Object actual = indicatorService.readActualValue(person, definition);
            List<String> expected = indicatorService.parseExpected(condition.getExpectedValues());
            boolean matched = compare(definition, condition.getOperator(), actual, expected);
            Map<String, Object> detail = new LinkedHashMap<String, Object>();
            detail.put("indicatorCode", definition.getCode());
            detail.put("indicatorName", definition.getName());
            detail.put("operator", condition.getOperator());
            detail.put("expected", expected);
            detail.put("actual", actual);
            detail.put("matched", matched);
            details.add(detail);
            if (all && !matched) finalMatch = false;
            if (!all && matched) finalMatch = true;
        }
        try {
            return new RuleEvaluationResult(finalMatch, objectMapper.writeValueAsString(details));
        } catch (JsonProcessingException ex) {
            throw new BusinessException("RULE_DETAIL_SERIALIZE_FAILED", "规则命中证据序列化失败");
        }
    }

    private boolean compare(IndicatorDefinition definition, String operator, Object actual, List<String> expected) {
        if ("IS_NULL".equals(operator)) return actual == null;
        if ("IS_NOT_NULL".equals(operator)) return actual != null;
        if (actual == null) return false;
        if ("IN".equals(operator) || "NOT_IN".equals(operator)) {
            boolean contains = false;
            for (String item : expected) {
                if (equalValue(definition, actual, item)) {
                    contains = true;
                    break;
                }
            }
            return "IN".equals(operator) ? contains : !contains;
        }
        if ("CONTAINS".equals(operator) || "NOT_CONTAINS".equals(operator)) {
            String actualText = normalizer.normalize(String.valueOf(actual));
            String expectedText = normalizer.normalize(expected.get(0));
            boolean contains = !expectedText.isEmpty() && actualText.contains(expectedText);
            return "CONTAINS".equals(operator) ? contains : !contains;
        }
        if ("BETWEEN".equals(operator)) {
            Comparable<Object> value = comparable(actual);
            Object left = indicatorService.parseComparable(definition, expected.get(0));
            Object right = indicatorService.parseComparable(definition, expected.get(1));
            return value.compareTo(left) >= 0 && value.compareTo(right) <= 0;
        }
        Object expectedValue = indicatorService.parseComparable(definition, expected.get(0));
        int comparison = comparable(actual).compareTo(expectedValue);
        if ("EQ".equals(operator)) return comparison == 0;
        if ("NE".equals(operator)) return comparison != 0;
        if ("GT".equals(operator)) return comparison > 0;
        if ("GE".equals(operator)) return comparison >= 0;
        if ("LT".equals(operator)) return comparison < 0;
        if ("LE".equals(operator)) return comparison <= 0;
        throw new BusinessException("RULE_OPERATOR_UNSUPPORTED", "不支持的规则运算符：" + operator);
    }

    private boolean equalValue(IndicatorDefinition definition, Object actual, String expected) {
        Object expectedValue = indicatorService.parseComparable(definition, expected);
        return comparable(actual).compareTo(expectedValue) == 0;
    }

    @SuppressWarnings("unchecked")
    private Comparable<Object> comparable(Object value) {
        if (value instanceof BigDecimal) return (Comparable<Object>) (Comparable<?>) value;
        if (value instanceof Date) return (Comparable<Object>) (Comparable<?>) value;
        if (value instanceof Boolean) return (Comparable<Object>) (Comparable<?>) value;
        return (Comparable<Object>) (Comparable<?>) String.valueOf(value);
    }
}
