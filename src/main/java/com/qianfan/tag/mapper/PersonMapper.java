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
                     @Param("andMode") boolean andMode, @Param("tagCount") int tagCount,
                     @Param("includeDeleted") boolean includeDeleted);
    List<PersonRecord> search(@Param("keyword") String keyword, @Param("tagIds") List<String> tagIds,
                              @Param("andMode") boolean andMode, @Param("tagCount") int tagCount,
                              @Param("offset") int offset, @Param("endRow") int endRow,
                              @Param("includeDeleted") boolean includeDeleted);
    List<PersonRecord> findActivePage(@Param("offset") int offset, @Param("endRow") int endRow);
    int updateDeleted(@Param("id") String id, @Param("deleted") int deleted,
                      @Param("updatedAt") java.util.Date updatedAt);
}
