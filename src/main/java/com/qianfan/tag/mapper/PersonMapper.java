package com.qianfan.tag.mapper;

import com.qianfan.tag.domain.PersonRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 人员基础资料持久化接口。 */
public interface PersonMapper {
    int insert(PersonRecord person);
    int update(PersonRecord person);
    PersonRecord findById(@Param("id") String id);
    PersonRecord findByExternalId(@Param("externalId") String externalId);
    long countSearch(@Param("keyword") String keyword, @Param("tagIds") List<String> tagIds,
                     @Param("andMode") boolean andMode, @Param("tagCount") int tagCount);
    List<PersonRecord> search(@Param("keyword") String keyword, @Param("tagIds") List<String> tagIds,
                              @Param("andMode") boolean andMode, @Param("tagCount") int tagCount,
                              @Param("offset") int offset, @Param("endRow") int endRow);
}

