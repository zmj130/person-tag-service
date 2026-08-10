package com.qianfan.tag.mapper;

import com.qianfan.tag.domain.PersonTagEvidence;
import com.qianfan.tag.domain.RuleEvaluationBatch;
import com.qianfan.tag.domain.TagRuleCondition;
import com.qianfan.tag.domain.TagRuleSet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface StructuredRuleMapper {
    Integer findMaxVersion(@Param("tagId") String tagId);
    int insertRuleSet(TagRuleSet ruleSet);
    int insertCondition(TagRuleCondition condition);
    TagRuleSet findRuleSet(@Param("id") String id);
    List<TagRuleSet> findRuleSets();
    List<TagRuleSet> findPublishedRuleSets();
    List<TagRuleCondition> findConditions(@Param("ruleSetId") String ruleSetId);
    int disablePublishedByTag(@Param("tagId") String tagId, @Param("updatedAt") Date updatedAt);
    int publishRuleSet(@Param("id") String id, @Param("publishedAt") Date publishedAt);
    int deleteConditions(@Param("ruleSetId") String ruleSetId);
    int deleteDraftRuleSet(@Param("id") String id);
    RuleEvaluationBatch findBatchByNo(@Param("batchNo") String batchNo);
    int insertBatch(RuleEvaluationBatch batch);
    int updateBatchProgress(@Param("batchNo") String batchNo, @Param("scanned") int scanned,
                            @Param("matched") int matched, @Param("expired") int expired);
    int finishBatch(@Param("batchNo") String batchNo, @Param("status") String status,
                    @Param("errorMessage") String errorMessage, @Param("finishedAt") Date finishedAt);
    int restartBatch(@Param("batchNo") String batchNo, @Param("startedAt") Date startedAt);
    PersonTagEvidence findEvidence(@Param("personId") String personId, @Param("tagId") String tagId,
                                   @Param("ruleSetId") String ruleSetId, @Param("ruleVersion") int ruleVersion);
    int insertEvidence(PersonTagEvidence evidence);
    int updateEvidence(PersonTagEvidence evidence);
    int expireActiveEvidence(@Param("personId") String personId, @Param("tagId") String tagId,
                             @Param("ruleSetId") String ruleSetId, @Param("updatedAt") Date updatedAt);
    int countActiveRuleEvidence(@Param("personId") String personId, @Param("tagId") String tagId);
    int reviewActiveEvidence(@Param("personId") String personId, @Param("tagId") String tagId,
                             @Param("ruleSetId") String ruleSetId, @Param("reviewStatus") String reviewStatus,
                             @Param("reviewedBy") String reviewedBy, @Param("reviewedAt") Date reviewedAt);
    int deleteEvidenceForBinding(@Param("personId") String personId, @Param("tagId") String tagId,
                                 @Param("ruleSetId") String ruleSetId);
    int expireOtherRuleVersions(@Param("tagId") String tagId, @Param("activeRuleSetId") String activeRuleSetId,
                                @Param("updatedAt") Date updatedAt);
    int expireOrphanRuleBindings(@Param("tagId") String tagId, @Param("updatedAt") Date updatedAt);
}
