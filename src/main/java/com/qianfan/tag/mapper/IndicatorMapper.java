package com.qianfan.tag.mapper;

import com.qianfan.tag.domain.IndicatorDefinition;
import com.qianfan.tag.domain.IndicatorOption;
import com.qianfan.tag.domain.PersonIndicatorValue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface IndicatorMapper {
    int insertDefinition(IndicatorDefinition definition);
    IndicatorDefinition findDefinitionById(@Param("id") String id);
    IndicatorDefinition findDefinitionByCode(@Param("code") String code);
    List<IndicatorDefinition> findDefinitions(@Param("enabledOnly") boolean enabledOnly);
    int updateDefinitionStatus(@Param("id") String id, @Param("status") int status, @Param("updatedAt") Date updatedAt);
    int insertOption(IndicatorOption option);
    IndicatorOption findOption(@Param("indicatorId") String indicatorId, @Param("code") String code);
    List<IndicatorOption> findOptions(@Param("indicatorId") String indicatorId, @Param("enabledOnly") boolean enabledOnly);
    int updateOptionStatus(@Param("id") String id, @Param("status") int status, @Param("updatedAt") Date updatedAt);
    PersonIndicatorValue findPersonValue(@Param("personId") String personId, @Param("indicatorId") String indicatorId);
    List<PersonIndicatorValue> findPersonValues(@Param("personId") String personId);
    int insertPersonValue(PersonIndicatorValue value);
    int updatePersonValue(PersonIndicatorValue value);
}
