package com.qianfan.tag.mapper;

import com.qianfan.tag.domain.PersonTag;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/** 人员标签关系持久化接口。 */
public interface PersonTagMapper {
    int insert(PersonTag personTag);
    PersonTag find(@Param("personId") String personId, @Param("tagId") String tagId);
    List<PersonTag> findByPersonId(@Param("personId") String personId);
    int updateBinding(PersonTag personTag);
    int review(@Param("id") String id, @Param("status") String status,
               @Param("reviewer") String reviewer, @Param("reviewedAt") Date reviewedAt);
    int deleteManual(@Param("personId") String personId, @Param("tagId") String tagId);
    int deleteRemote(@Param("personId") String personId, @Param("tagId") String tagId);
    int deleteStaleRuleBindings(@Param("personId") String personId,
                                @Param("matchedTagIds") List<String> matchedTagIds);
}
