package com.qianfan.tag.mapper;

import com.qianfan.tag.domain.PersonTag;
import com.qianfan.tag.dto.ReviewItem;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/** 人员标签关系持久化接口。 */
public interface PersonTagMapper {
    int insert(PersonTag personTag);
    PersonTag find(@Param("personId") String personId, @Param("tagId") String tagId);
    PersonTag findBySource(@Param("personId") String personId, @Param("tagId") String tagId,
                           @Param("sourceRef") String sourceRef);
    PersonTag findById(@Param("id") String id);
    List<PersonTag> findByPersonId(@Param("personId") String personId);
    List<String> findPersonIdsByTag(@Param("tagId") String tagId);
    List<String> findPersonIdsByRule(@Param("ruleId") String ruleId);
    long countReviews(@Param("status") String status);
    List<ReviewItem> findReviews(@Param("status") String status,
                                 @Param("offset") int offset, @Param("endRow") int endRow);
    int updateBinding(PersonTag personTag);
    int review(@Param("id") String id, @Param("status") String status,
               @Param("reviewer") String reviewer, @Param("reviewedAt") Date reviewedAt);
    int deleteManual(@Param("personId") String personId, @Param("tagId") String tagId);
    int deleteRemote(@Param("personId") String personId, @Param("tagId") String tagId);
    int deleteStaleRuleBindings(@Param("personId") String personId,
                                @Param("matchedRuleIds") List<String> matchedRuleIds);
    int deleteKeywordRuleBindings(@Param("ruleId") String ruleId);
    int deleteRuleBindingById(@Param("id") String id);
    int expireRuleBinding(@Param("personId") String personId, @Param("tagId") String tagId,
                          @Param("sourceRef") String sourceRef,
                          @Param("updatedAt") java.util.Date updatedAt);
}
