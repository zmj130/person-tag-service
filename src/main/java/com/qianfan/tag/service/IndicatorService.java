package com.qianfan.tag.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qianfan.tag.common.BusinessException;
import com.qianfan.tag.common.Ids;
import com.qianfan.tag.domain.IndicatorDefinition;
import com.qianfan.tag.domain.IndicatorOption;
import com.qianfan.tag.domain.PersonIndicatorValue;
import com.qianfan.tag.dto.IndicatorDetail;
import com.qianfan.tag.dto.IndicatorRequests;
import com.qianfan.tag.dto.PersonIndicatorItem;
import com.qianfan.tag.mapper.IndicatorMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class IndicatorService {
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{1,63}");
    private static final Set<String> TYPES = new HashSet<String>(
            Arrays.asList("TEXT", "NUMBER", "DATE", "DATETIME", "BOOLEAN", "ENUM"));
    private static final Set<String> SOURCES = new HashSet<String>(
            Arrays.asList("IMPORT", "REMOTE", "DERIVED"));
    private final IndicatorMapper mapper;
    private final ObjectMapper objectMapper;

    public IndicatorService(IndicatorMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IndicatorDefinition create(IndicatorRequests.Create request) {
        String code = request.getCode().trim().toUpperCase(Locale.ROOT);
        String type = request.getDataType().trim().toUpperCase(Locale.ROOT);
        String source = request.getSourceType().trim().toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new BusinessException("INVALID_INDICATOR_CODE", "指标编码必须为大写字母、数字和下划线，且以字母开头");
        }
        if (!TYPES.contains(type)) {
            throw new BusinessException("INVALID_INDICATOR_TYPE", "不支持的指标数据类型");
        }
        if (!SOURCES.contains(source)) {
            throw new BusinessException("INVALID_INDICATOR_SOURCE", "自定义指标来源只能是 IMPORT、REMOTE 或 DERIVED");
        }
        if (mapper.findDefinitionByCode(code) != null) {
            throw new BusinessException("INDICATOR_CODE_EXISTS", "指标编码已经存在");
        }
        Date now = new Date();
        IndicatorDefinition definition = new IndicatorDefinition();
        definition.setId(Ids.uuid());
        definition.setCode(code);
        definition.setName(request.getName().trim());
        definition.setDataType(type);
        definition.setSourceType(source);
        definition.setUnit(trimToNull(request.getUnit()));
        definition.setStatus(1);
        definition.setCreatedAt(now);
        definition.setUpdatedAt(now);
        mapper.insertDefinition(definition);
        return definition;
    }

    public List<IndicatorDetail> list(boolean enabledOnly) {
        List<IndicatorDetail> result = new ArrayList<IndicatorDetail>();
        for (IndicatorDefinition definition : mapper.findDefinitions(enabledOnly)) {
            List<IndicatorOption> options = "ENUM".equals(definition.getDataType())
                    ? mapper.findOptions(definition.getId(), enabledOnly) : Collections.<IndicatorOption>emptyList();
            result.add(new IndicatorDetail(definition, options, operatorsFor(definition.getDataType())));
        }
        return result;
    }

    public IndicatorDefinition require(String id) {
        IndicatorDefinition definition = mapper.findDefinitionById(id);
        if (definition == null) {
            throw new BusinessException("INDICATOR_NOT_FOUND", "指标不存在");
        }
        return definition;
    }

    public IndicatorDefinition findByCode(String code) {
        return mapper.findDefinitionByCode(code);
    }

    public List<IndicatorDefinition> importDefinitions() {
        List<IndicatorDefinition> result = new ArrayList<IndicatorDefinition>();
        for (IndicatorDefinition definition : mapper.findDefinitions(true)) {
            if ("IMPORT".equals(definition.getSourceType())) result.add(definition);
        }
        return result;
    }

    public List<PersonIndicatorItem> listPersonValues(String personId) {
        Map<String, IndicatorDefinition> definitions = new LinkedHashMap<String, IndicatorDefinition>();
        for (IndicatorDefinition definition : mapper.findDefinitions(false)) {
            definitions.put(definition.getId(), definition);
        }
        List<PersonIndicatorItem> result = new ArrayList<PersonIndicatorItem>();
        for (PersonIndicatorValue value : mapper.findPersonValues(personId)) {
            IndicatorDefinition definition = definitions.get(value.getIndicatorId());
            if (definition == null) {
                throw new BusinessException("INDICATOR_DEFINITION_MISSING", "人员指标引用的指标定义不存在");
            }
            result.add(new PersonIndicatorItem(definition.getId(), definition.getCode(), definition.getName(),
                    definition.getDataType(), definition.getUnit(), displayValue(definition, value),
                    value.getSourceType(), value.getSourceUpdatedAt()));
        }
        Collections.sort(result, (left, right) -> left.getCode().compareTo(right.getCode()));
        return result;
    }

    public List<IndicatorOption> enabledOptions(String indicatorId) {
        return mapper.findOptions(indicatorId, true);
    }

    public void validateRawValue(IndicatorDefinition definition, String raw) {
        validateScalar(definition, raw);
    }

    @Transactional
    public IndicatorOption addOption(String indicatorId, IndicatorRequests.AddOption request) {
        IndicatorDefinition definition = require(indicatorId);
        if (!"ENUM".equals(definition.getDataType())) {
            throw new BusinessException("INDICATOR_NOT_ENUM", "只有枚举指标可以配置固定选项");
        }
        String code = request.getCode().trim();
        if (mapper.findOption(indicatorId, code) != null) {
            throw new BusinessException("INDICATOR_OPTION_EXISTS", "枚举选项编码已经存在");
        }
        Date now = new Date();
        IndicatorOption option = new IndicatorOption();
        option.setId(Ids.uuid());
        option.setIndicatorId(indicatorId);
        option.setCode(code);
        option.setLabel(request.getLabel().trim());
        option.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        option.setStatus(1);
        option.setCreatedAt(now);
        option.setUpdatedAt(now);
        mapper.insertOption(option);
        return option;
    }

    @Transactional
    public void changeStatus(String id, boolean enabled) {
        require(id);
        mapper.updateDefinitionStatus(id, enabled ? 1 : 0, new Date());
    }

    public String validateAndSerializeExpected(IndicatorDefinition definition, String operator, List<String> values) {
        String op = operator.toUpperCase(Locale.ROOT);
        if (!operatorsFor(definition.getDataType()).contains(op)) {
            throw new BusinessException("INVALID_RULE_OPERATOR", "运算符不适用于指标“" + definition.getName() + "”");
        }
        List<String> actual = values == null ? Collections.<String>emptyList() : values;
        int expectedCount = expectedValueCount(op);
        if (expectedCount >= 0 && actual.size() != expectedCount) {
            throw new BusinessException("INVALID_RULE_VALUE_COUNT", "运算符“" + op + "”需要" + expectedCount + "个比较值");
        }
        if (("IN".equals(op) || "NOT_IN".equals(op)) && actual.isEmpty()) {
            throw new BusinessException("INVALID_RULE_VALUE_COUNT", "IN 运算符至少需要一个比较值");
        }
        for (String value : actual) {
            validateScalar(definition, value);
        }
        try {
            return objectMapper.writeValueAsString(actual);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("RULE_VALUE_SERIALIZE_FAILED", "规则比较值序列化失败");
        }
    }

    @Transactional
    public PersonIndicatorValue saveImportedValue(String personId, IndicatorDefinition definition,
                                                  String rawValue, String batchNo, Date sourceUpdatedAt) {
        validateScalar(definition, rawValue);
        PersonIndicatorValue value = mapper.findPersonValue(personId, definition.getId());
        Date now = new Date();
        if (value == null) {
            value = new PersonIndicatorValue();
            value.setId(Ids.uuid());
            value.setPersonId(personId);
            value.setIndicatorId(definition.getId());
            value.setCreatedAt(now);
        }
        clearTypedValues(value);
        assignTypedValue(value, definition, rawValue);
        value.setSourceType("IMPORT");
        value.setImportBatchNo(batchNo);
        value.setSourceUpdatedAt(sourceUpdatedAt);
        value.setUpdatedAt(now);
        if (mapper.findPersonValue(personId, definition.getId()) == null) {
            mapper.insertPersonValue(value);
        } else {
            mapper.updatePersonValue(value);
        }
        return value;
    }

    public Object readActualValue(com.qianfan.tag.domain.PersonRecord person, IndicatorDefinition definition) {
        if ("PERSON_FIELD".equals(definition.getSourceType())) {
            return readPersonField(person, definition.getPersonField());
        }
        PersonIndicatorValue value = mapper.findPersonValue(person.getId(), definition.getId());
        if (value == null) {
            return null;
        }
        if ("NUMBER".equals(definition.getDataType())) return value.getNumberValue();
        if ("DATE".equals(definition.getDataType()) || "DATETIME".equals(definition.getDataType())) return value.getDateValue();
        if ("BOOLEAN".equals(definition.getDataType())) return value.getBooleanValue() == null ? null : value.getBooleanValue() == 1;
        if ("ENUM".equals(definition.getDataType())) return value.getOptionCode();
        return value.getStringValue();
    }

    public List<String> parseExpected(String json) {
        try {
            return objectMapper.readValue(json == null ? "[]" : json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception ex) {
            throw new BusinessException("RULE_VALUE_INVALID", "规则比较值格式损坏");
        }
    }

    public Object parseComparable(IndicatorDefinition definition, String raw) {
        String value = raw == null ? "" : raw.trim();
        try {
            if ("NUMBER".equals(definition.getDataType())) return new BigDecimal(value);
            if ("DATE".equals(definition.getDataType())) return parseDate(value, "yyyy-MM-dd");
            if ("DATETIME".equals(definition.getDataType())) return parseDate(value, "yyyy-MM-dd HH:mm:ss");
            if ("BOOLEAN".equals(definition.getDataType())) return parseBoolean(value);
            return value;
        } catch (RuntimeException ex) {
            throw new BusinessException("INDICATOR_VALUE_INVALID", "指标“" + definition.getName() + "”的值格式不正确：" + raw);
        }
    }

    private void validateScalar(IndicatorDefinition definition, String raw) {
        Object parsed = parseComparable(definition, raw);
        if ("ENUM".equals(definition.getDataType())) {
            IndicatorOption option = mapper.findOption(definition.getId(), String.valueOf(parsed));
            if (option == null || option.getStatus() == null || option.getStatus() != 1) {
                throw new BusinessException("INDICATOR_OPTION_INVALID", "指标“" + definition.getName() + "”不存在启用选项：" + raw);
            }
        }
    }

    private void assignTypedValue(PersonIndicatorValue value, IndicatorDefinition definition, String raw) {
        Object parsed = parseComparable(definition, raw);
        if ("NUMBER".equals(definition.getDataType())) value.setNumberValue((BigDecimal) parsed);
        else if ("DATE".equals(definition.getDataType()) || "DATETIME".equals(definition.getDataType())) value.setDateValue((Date) parsed);
        else if ("BOOLEAN".equals(definition.getDataType())) value.setBooleanValue(Boolean.TRUE.equals(parsed) ? 1 : 0);
        else if ("ENUM".equals(definition.getDataType())) value.setOptionCode(String.valueOf(parsed));
        else value.setStringValue(String.valueOf(parsed));
    }

    private Object readPersonField(com.qianfan.tag.domain.PersonRecord person, String field) {
        if ("gender".equals(field)) return person.getGender();
        if ("organization".equals(field)) return person.getOrganization();
        if ("occupation".equals(field)) return person.getOccupation();
        if ("address".equals(field)) return person.getAddress();
        if ("remark".equals(field)) return person.getRemark();
        throw new BusinessException("PERSON_FIELD_UNSUPPORTED", "人员基础字段映射不受支持：" + field);
    }

    private List<String> operatorsFor(String type) {
        if ("NUMBER".equals(type) || "DATE".equals(type) || "DATETIME".equals(type)) {
            return Arrays.asList("EQ", "NE", "GT", "GE", "LT", "LE", "BETWEEN", "IS_NULL", "IS_NOT_NULL");
        }
        if ("ENUM".equals(type)) return Arrays.asList("EQ", "NE", "IN", "NOT_IN", "IS_NULL", "IS_NOT_NULL");
        if ("BOOLEAN".equals(type)) return Arrays.asList("EQ", "NE", "IS_NULL", "IS_NOT_NULL");
        return Arrays.asList("EQ", "NE", "CONTAINS", "NOT_CONTAINS", "IS_NULL", "IS_NOT_NULL");
    }

    private int expectedValueCount(String operator) {
        if ("IS_NULL".equals(operator) || "IS_NOT_NULL".equals(operator)) return 0;
        if ("BETWEEN".equals(operator)) return 2;
        if ("IN".equals(operator) || "NOT_IN".equals(operator)) return -1;
        return 1;
    }

    private Date parseDate(String value, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        format.setLenient(false);
        try {
            return format.parse(value);
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private Boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value) || "1".equals(value) || "是".equals(value)) return true;
        if ("false".equalsIgnoreCase(value) || "0".equals(value) || "否".equals(value)) return false;
        throw new IllegalArgumentException("invalid boolean");
    }

    private void clearTypedValues(PersonIndicatorValue value) {
        value.setStringValue(null);
        value.setNumberValue(null);
        value.setDateValue(null);
        value.setBooleanValue(null);
        value.setOptionCode(null);
    }

    private String displayValue(IndicatorDefinition definition, PersonIndicatorValue value) {
        String type = definition.getDataType();
        if ("NUMBER".equals(type)) {
            String number = value.getNumberValue().stripTrailingZeros().toPlainString();
            return definition.getUnit() == null || definition.getUnit().trim().isEmpty()
                    ? number : number + " " + definition.getUnit().trim();
        }
        if ("DATE".equals(type)) return new SimpleDateFormat("yyyy-MM-dd").format(value.getDateValue());
        if ("DATETIME".equals(type)) return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(value.getDateValue());
        if ("BOOLEAN".equals(type)) return Integer.valueOf(1).equals(value.getBooleanValue()) ? "是" : "否";
        if ("ENUM".equals(type)) {
            IndicatorOption option = mapper.findOption(definition.getId(), value.getOptionCode());
            if (option == null) {
                throw new BusinessException("INDICATOR_OPTION_MISSING", "人员枚举指标引用的选项不存在");
            }
            return option.getLabel().equals(option.getCode())
                    ? option.getLabel() : option.getLabel() + "（" + option.getCode() + "）";
        }
        return value.getStringValue();
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }
}
