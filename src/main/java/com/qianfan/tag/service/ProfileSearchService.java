package com.qianfan.tag.service;

import com.qianfan.tag.common.BusinessException;
import com.qianfan.tag.config.ProfileSearchProperties;
import com.qianfan.tag.domain.IndicatorDefinition;
import com.qianfan.tag.domain.PersonIndicatorValue;
import com.qianfan.tag.domain.PersonRecord;
import com.qianfan.tag.domain.PersonTag;
import com.qianfan.tag.domain.TagDefinition;
import com.qianfan.tag.dto.ProfileIndexStatus;
import com.qianfan.tag.dto.ProfileRequests;
import com.qianfan.tag.dto.ProfileSearchResult;
import com.qianfan.tag.mapper.IndicatorMapper;
import com.qianfan.tag.mapper.PersonMapper;
import com.qianfan.tag.mapper.PersonTagMapper;
import com.qianfan.tag.mapper.TagMapper;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

@Service
public class ProfileSearchService {
    private static final String INDEX_PREFIX = "person_tag_resume_";
    private static final int PAGE_SIZE = 500;

    private final ProfileSearchProperties properties;
    private final ObjectProvider<RestHighLevelClient> clientProvider;
    private final PersonMapper personMapper;
    private final PersonTagMapper personTagMapper;
    private final IndicatorMapper indicatorMapper;
    private final TagMapper tagMapper;
    private final IndicatorService indicatorService;

    public ProfileSearchService(ProfileSearchProperties properties,
                                ObjectProvider<RestHighLevelClient> clientProvider,
                                PersonMapper personMapper,
                                PersonTagMapper personTagMapper,
                                IndicatorMapper indicatorMapper,
                                TagMapper tagMapper,
                                IndicatorService indicatorService) {
        this.properties = properties;
        this.clientProvider = clientProvider;
        this.personMapper = personMapper;
        this.personTagMapper = personTagMapper;
        this.indicatorMapper = indicatorMapper;
        this.tagMapper = tagMapper;
        this.indicatorService = indicatorService;
    }

    public ProfileIndexStatus status() {
        if (!properties.isEnabled()) {
            return new ProfileIndexStatus(false, false, properties.getIndex(), 0);
        }
        validateIndexName();
        try {
            RestHighLevelClient client = requireClient();
            boolean exists = client.indices().exists(new GetIndexRequest(properties.getIndex()), RequestOptions.DEFAULT);
            long count = exists ? client.count(new CountRequest(properties.getIndex()), RequestOptions.DEFAULT).getCount() : 0;
            return new ProfileIndexStatus(true, exists, properties.getIndex(), count);
        } catch (IOException ex) {
            throw unavailable(ex);
        }
    }

    public ProfileIndexStatus rebuild() {
        validateEnabled();
        validateIndexName();

        List<PersonRecord> persons = loadWithinLimit();
        Map<String, IndicatorDefinition> indicators = indicatorMap();
        Map<String, TagDefinition> tags = tagMap();
        try {
            RestHighLevelClient client = requireClient();
            GetIndexRequest get = new GetIndexRequest(properties.getIndex());
            if (client.indices().exists(get, RequestOptions.DEFAULT)) {
                client.indices().delete(new DeleteIndexRequest(properties.getIndex()), RequestOptions.DEFAULT);
            }
            createIndex(client);
            bulkIndex(client, persons, indicators, tags);
            client.indices().refresh(new RefreshRequest(properties.getIndex()), RequestOptions.DEFAULT);
            return new ProfileIndexStatus(true, true, properties.getIndex(), persons.size());
        } catch (IOException ex) {
            throw unavailable(ex);
        }
    }

    public ProfileSearchResult search(ProfileRequests.Search request) {
        validateEnabled();
        validateIndexName();
        int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null ? 20 : request.getPageSize();
        if (pageNo < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException("INVALID_PAGE", "页码必须大于 0，且每页不能超过 100 条");
        }

        BoolQueryBuilder query = QueryBuilders.boolQuery();
        if (hasText(request.getKeyword())) {
            query.must(QueryBuilders.multiMatchQuery(request.getKeyword().trim(),
                    "name", "externalId", "organization", "occupation", "address", "remark")
                    .type(MultiMatchQueryBuilder.Type.PHRASE));
        }
        addTagQuery(query, request);
        if (request.getIndicators() != null) {
            for (ProfileRequests.IndicatorFilter filter : request.getIndicators()) {
                query.must(indicatorQuery(filter));
            }
        }

        SearchSourceBuilder source = new SearchSourceBuilder()
                .query(query)
                .from((pageNo - 1) * pageSize)
                .size(pageSize)
                .trackTotalHits(true)
                .aggregation(AggregationBuilders.terms("tags").field("tagNames").size(30))
                .aggregation(AggregationBuilders.terms("genders").field("gender").size(10))
                .aggregation(AggregationBuilders.terms("occupations").field("occupation.keyword").size(30));
        try {
            SearchResponse response = requireClient().search(
                    new SearchRequest(properties.getIndex()).source(source), RequestOptions.DEFAULT);
            ProfileSearchResult result = new ProfileSearchResult();
            result.setTotal(response.getHits().getTotalHits() == null ? 0 : response.getHits().getTotalHits().value);
            result.setPageNo(pageNo);
            result.setPageSize(pageSize);
            List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
            for (SearchHit hit : response.getHits().getHits()) {
                records.add(hit.getSourceAsMap());
            }
            result.setRecords(records);
            Map<String, List<ProfileSearchResult.Bucket>> aggregations = new LinkedHashMap<String, List<ProfileSearchResult.Bucket>>();
            aggregations.put("tags", buckets(response, "tags"));
            aggregations.put("genders", buckets(response, "genders"));
            aggregations.put("occupations", buckets(response, "occupations"));
            result.setAggregations(aggregations);
            return result;
        } catch (IOException ex) {
            throw unavailable(ex);
        }
    }

    private void addTagQuery(BoolQueryBuilder query, ProfileRequests.Search request) {
        if (request.getTagIds() == null || request.getTagIds().isEmpty()) return;
        if ("OR".equalsIgnoreCase(request.getTagOperator())) {
            query.must(QueryBuilders.termsQuery("tagIds", request.getTagIds()));
            return;
        }
        if (!"AND".equalsIgnoreCase(request.getTagOperator())) {
            throw new BusinessException("INVALID_TAG_OPERATOR", "标签关系只支持 AND 或 OR");
        }
        for (String tagId : request.getTagIds()) {
            query.must(QueryBuilders.termQuery("tagIds", tagId));
        }
    }

    private QueryBuilder indicatorQuery(ProfileRequests.IndicatorFilter filter) {
        if (filter == null || !hasText(filter.getIndicatorCode()) || !hasText(filter.getOperator())) {
            throw new BusinessException("INVALID_INDICATOR_FILTER", "指标编码和运算符不能为空");
        }
        List<String> values = filter.getValues() == null ? Collections.<String>emptyList() : filter.getValues();
        String code = filter.getIndicatorCode().trim().toUpperCase(Locale.ROOT);
        IndicatorDefinition definition = indicatorMapper.findDefinitionByCode(code);
        if (definition == null || !Integer.valueOf(1).equals(definition.getStatus())) {
            throw new BusinessException("INDICATOR_NOT_FOUND", "画像查询指标不存在或已停用: " + code);
        }
        String operator = filter.getOperator().trim().toUpperCase(Locale.ROOT);
        indicatorService.validateAndSerializeExpected(definition, operator, values);
        BoolQueryBuilder nested = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("indicators.code", code));
        if ("IS_NULL".equals(operator)) {
            return QueryBuilders.boolQuery().mustNot(QueryBuilders.nestedQuery("indicators", nested,
                    org.apache.lucene.search.join.ScoreMode.None));
        }
        if ("IS_NOT_NULL".equals(operator)) {
            return QueryBuilders.nestedQuery("indicators", nested, org.apache.lucene.search.join.ScoreMode.None);
        }
        if (values.isEmpty()) {
            throw new BusinessException("INVALID_INDICATOR_FILTER", "该运算符至少需要一个比较值");
        }
        String field = indicatorField(definition);
        List<Object> comparable = comparableValues(definition, values);
        if ("EQ".equals(operator)) nested.must(QueryBuilders.termQuery(field, comparable.get(0)));
        else if ("NE".equals(operator)) nested.mustNot(QueryBuilders.termQuery(field, comparable.get(0)));
        else if ("IN".equals(operator)) nested.must(QueryBuilders.termsQuery(field, comparable));
        else if ("NOT_IN".equals(operator)) nested.mustNot(QueryBuilders.termsQuery(field, comparable));
        else if ("CONTAINS".equals(operator)) nested.must(QueryBuilders.wildcardQuery(field, "*" + values.get(0).toLowerCase(Locale.ROOT) + "*"));
        else if ("NOT_CONTAINS".equals(operator)) nested.mustNot(QueryBuilders.wildcardQuery(field, "*" + values.get(0).toLowerCase(Locale.ROOT) + "*"));
        else if ("GT".equals(operator)) nested.must(QueryBuilders.rangeQuery(field).gt(comparable.get(0)));
        else if ("GE".equals(operator)) nested.must(QueryBuilders.rangeQuery(field).gte(comparable.get(0)));
        else if ("LT".equals(operator)) nested.must(QueryBuilders.rangeQuery(field).lt(comparable.get(0)));
        else if ("LE".equals(operator)) nested.must(QueryBuilders.rangeQuery(field).lte(comparable.get(0)));
        else if ("BETWEEN".equals(operator) && values.size() >= 2) {
            nested.must(QueryBuilders.rangeQuery(field).gte(comparable.get(0)).lte(comparable.get(1)));
        } else {
            throw new BusinessException("INVALID_INDICATOR_OPERATOR", "画像查询暂不支持运算符: " + operator);
        }
        return QueryBuilders.nestedQuery("indicators", nested, org.apache.lucene.search.join.ScoreMode.None);
    }

    private String indicatorField(IndicatorDefinition definition) {
        if ("NUMBER".equals(definition.getDataType())) return "indicators.numberValue";
        if ("DATE".equals(definition.getDataType()) || "DATETIME".equals(definition.getDataType())) return "indicators.dateValue";
        if ("BOOLEAN".equals(definition.getDataType())) return "indicators.booleanValue";
        return "indicators.exactValue";
    }

    private List<Object> comparableValues(IndicatorDefinition definition, List<String> values) {
        List<Object> result = new ArrayList<Object>();
        for (String value : values) {
            Object parsed = indicatorService.parseComparable(definition, value);
            if (parsed instanceof Date) result.add(date((Date) parsed));
            else if (parsed instanceof String) result.add(((String) parsed).toLowerCase(Locale.ROOT));
            else result.add(parsed);
        }
        return result;
    }

    private List<PersonRecord> loadWithinLimit() {
        List<PersonRecord> result = new ArrayList<PersonRecord>();
        int offset = 0;
        while (true) {
            List<PersonRecord> page = personMapper.findActivePage(offset, offset + PAGE_SIZE);
            if (page.isEmpty()) break;
            result.addAll(page);
            if (result.size() > properties.getMaxDocuments()) {
                throw new BusinessException("ES_DOCUMENT_LIMIT", "画像索引最多允许 " + properties.getMaxDocuments() + " 人，请缩小演示数据范围");
            }
            if (page.size() < PAGE_SIZE) break;
            offset += PAGE_SIZE;
        }
        return result;
    }

    private void createIndex(RestHighLevelClient client) throws IOException {
        CreateIndexRequest request = new CreateIndexRequest(properties.getIndex());
        request.settings(Settings.builder()
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0)
                .put("index.refresh_interval", "30s"));
        request.mapping(indexMapping());
        client.indices().create(request, RequestOptions.DEFAULT);
    }

    private Map<String, Object> indexMapping() {
        Map<String, Object> keyword = field("keyword");
        Map<String, Object> textWithKeyword = field("text");
        textWithKeyword.put("fields", Collections.singletonMap("keyword", field("keyword")));
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        props.put("id", keyword);
        props.put("externalId", keyword);
        props.put("name", textWithKeyword);
        props.put("gender", keyword);
        props.put("organization", copy(textWithKeyword));
        props.put("occupation", copy(textWithKeyword));
        props.put("address", field("text"));
        props.put("remark", field("text"));
        props.put("tagIds", keyword);
        props.put("tagNames", keyword);
        props.put("updatedAt", field("date"));

        Map<String, Object> indicatorProps = new LinkedHashMap<String, Object>();
        indicatorProps.put("code", keyword);
        indicatorProps.put("name", keyword);
        indicatorProps.put("type", keyword);
        indicatorProps.put("exactValue", keyword);
        indicatorProps.put("textValue", field("text"));
        indicatorProps.put("numberValue", field("double"));
        indicatorProps.put("dateValue", field("date"));
        indicatorProps.put("booleanValue", field("boolean"));
        Map<String, Object> nested = new LinkedHashMap<String, Object>();
        nested.put("type", "nested");
        nested.put("properties", indicatorProps);
        props.put("indicators", nested);

        Map<String, Object> mapping = new LinkedHashMap<String, Object>();
        mapping.put("dynamic", "strict");
        mapping.put("properties", props);
        return mapping;
    }

    private void bulkIndex(RestHighLevelClient client, List<PersonRecord> persons,
                           Map<String, IndicatorDefinition> indicators,
                           Map<String, TagDefinition> tags) throws IOException {
        for (int start = 0; start < persons.size(); start += PAGE_SIZE) {
            BulkRequest bulk = new BulkRequest();
            int end = Math.min(start + PAGE_SIZE, persons.size());
            for (PersonRecord person : persons.subList(start, end)) {
                bulk.add(new IndexRequest(properties.getIndex()).id(person.getId())
                        .source(document(person, indicators, tags)));
            }
            BulkResponse response = client.bulk(bulk, RequestOptions.DEFAULT);
            if (response.hasFailures()) {
                throw new BusinessException("ES_BULK_FAILED", "画像索引写入失败: " + response.buildFailureMessage());
            }
        }
    }

    private Map<String, Object> document(PersonRecord person,
                                         Map<String, IndicatorDefinition> definitions,
                                         Map<String, TagDefinition> tagDefinitions) {
        Map<String, Object> doc = new LinkedHashMap<String, Object>();
        put(doc, "id", person.getId());
        put(doc, "externalId", person.getExternalId());
        put(doc, "name", person.getName());
        put(doc, "gender", person.getGender());
        put(doc, "organization", person.getOrganization());
        put(doc, "occupation", person.getOccupation());
        put(doc, "address", person.getAddress());
        put(doc, "remark", person.getRemark());
        put(doc, "updatedAt", date(person.getUpdatedAt()));

        List<String> tagIds = new ArrayList<String>();
        List<String> tagNames = new ArrayList<String>();
        for (PersonTag binding : personTagMapper.findByPersonId(person.getId())) {
            if (!"APPROVED".equals(binding.getStatus())) continue;
            TagDefinition tag = tagDefinitions.get(binding.getTagId());
            if (tag != null && Integer.valueOf(1).equals(tag.getStatus())) {
                tagIds.add(tag.getId());
                tagNames.add(tag.getName());
            }
        }
        doc.put("tagIds", tagIds);
        doc.put("tagNames", tagNames);

        List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
        for (PersonIndicatorValue value : indicatorMapper.findPersonValues(person.getId())) {
            IndicatorDefinition definition = definitions.get(value.getIndicatorId());
            if (definition == null || !Integer.valueOf(1).equals(definition.getStatus())) continue;
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("code", definition.getCode());
            item.put("name", definition.getName());
            item.put("type", definition.getDataType());
            addIndicatorValue(item, value);
            values.add(item);
        }
        addBuiltinIndicators(values, person);
        doc.put("indicators", values);
        return doc;
    }

    private void addIndicatorValue(Map<String, Object> item, PersonIndicatorValue value) {
        if (value.getNumberValue() != null) {
            item.put("numberValue", value.getNumberValue());
            item.put("exactValue", value.getNumberValue().stripTrailingZeros().toPlainString());
        } else if (value.getDateValue() != null) {
            item.put("dateValue", date(value.getDateValue()));
            item.put("exactValue", date(value.getDateValue()));
        } else if (value.getBooleanValue() != null) {
            item.put("booleanValue", value.getBooleanValue() == 1);
            item.put("exactValue", String.valueOf(value.getBooleanValue() == 1));
        } else {
            String text = value.getOptionCode() != null ? value.getOptionCode() : value.getStringValue();
            if (text != null) {
                item.put("exactValue", text.toLowerCase(Locale.ROOT));
                item.put("textValue", text);
            }
        }
    }

    private void addBuiltinIndicators(List<Map<String, Object>> values, PersonRecord person) {
        addBuiltin(values, "GENDER", "性别", person.getGender());
        addBuiltin(values, "ORGANIZATION", "单位", person.getOrganization());
        addBuiltin(values, "OCCUPATION", "职业", person.getOccupation());
        addBuiltin(values, "ADDRESS", "地址", person.getAddress());
        addBuiltin(values, "REMARK", "备注", person.getRemark());
    }

    private void addBuiltin(List<Map<String, Object>> values, String code, String name, String value) {
        if (!hasText(value)) return;
        for (Map<String, Object> existing : values) {
            if (code.equals(existing.get("code"))) return;
        }
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("code", code);
        item.put("name", name);
        item.put("type", "TEXT");
        item.put("exactValue", value.toLowerCase(Locale.ROOT));
        item.put("textValue", value);
        values.add(item);
    }

    private Map<String, IndicatorDefinition> indicatorMap() {
        Map<String, IndicatorDefinition> result = new HashMap<String, IndicatorDefinition>();
        for (IndicatorDefinition definition : indicatorMapper.findDefinitions(true)) result.put(definition.getId(), definition);
        return result;
    }

    private Map<String, TagDefinition> tagMap() {
        Map<String, TagDefinition> result = new HashMap<String, TagDefinition>();
        for (TagDefinition tag : tagMapper.findAllTags()) result.put(tag.getId(), tag);
        return result;
    }

    private List<ProfileSearchResult.Bucket> buckets(SearchResponse response, String name) {
        ParsedStringTerms terms = response.getAggregations().get(name);
        List<ProfileSearchResult.Bucket> result = new ArrayList<ProfileSearchResult.Bucket>();
        if (terms == null) return result;
        for (Terms.Bucket bucket : terms.getBuckets()) {
            result.add(new ProfileSearchResult.Bucket(bucket.getKeyAsString(), bucket.getDocCount()));
        }
        return result;
    }

    private RestHighLevelClient requireClient() {
        RestHighLevelClient client = clientProvider.getIfAvailable();
        if (client == null) throw new BusinessException("ES_DISABLED", "画像检索未启用，请设置 ES_ENABLED=true");
        return client;
    }

    private void validateEnabled() {
        if (!properties.isEnabled()) throw new BusinessException("ES_DISABLED", "画像检索未启用，请设置 ES_ENABLED=true");
    }

    private void validateIndexName() {
        String index = properties.getIndex();
        if (index == null || !index.matches("person_tag_resume_[a-z0-9_-]+")) {
            throw new BusinessException("UNSAFE_ES_INDEX", "ES 索引名必须以 " + INDEX_PREFIX + " 开头且只能包含小写字母、数字、下划线或横线");
        }
    }

    private BusinessException unavailable(Exception ex) {
        return new BusinessException("ES_UNAVAILABLE", "画像检索服务不可用: " + ex.getMessage());
    }

    private Map<String, Object> field(String type) {
        Map<String, Object> field = new LinkedHashMap<String, Object>();
        field.put("type", type);
        return field;
    }

    private Map<String, Object> copy(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.putAll(source);
        return result;
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (value != null) target.put(key, value);
    }

    private String date(Date value) {
        if (value == null) return null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(value);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
