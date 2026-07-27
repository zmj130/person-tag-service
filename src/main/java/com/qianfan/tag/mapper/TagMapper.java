package com.qianfan.tag.mapper;

import com.qianfan.tag.domain.TagDefinition;
import com.qianfan.tag.domain.TagRule;
import com.qianfan.tag.trie.RuleMatch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 标签和规则持久化接口。 */
public interface TagMapper {
    int insertTag(TagDefinition tag);
    TagDefinition findTagById(@Param("id") String id);
    TagDefinition findTagByCode(@Param("code") String code);
    List<TagDefinition> findAllTags();
    int updateTag(TagDefinition tag);
    int updateTagStatus(@Param("id") String id, @Param("status") int status,
                        @Param("updatedAt") java.util.Date updatedAt);
    int insertRule(TagRule rule);
    TagRule findRule(@Param("tagId") String tagId, @Param("normalizedKeyword") String normalizedKeyword);
    TagRule findRuleById(@Param("id") String id);
    int updateRuleStatus(@Param("id") String id, @Param("status") int status,
                         @Param("updatedAt") java.util.Date updatedAt);
    List<TagRule> findRulesByTagId(@Param("tagId") String tagId);
    List<RuleMatch> findEnabledRuleMatches();
}
