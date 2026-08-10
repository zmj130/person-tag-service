package com.qianfan.tag.service;

import com.qianfan.tag.common.BusinessException;
import com.qianfan.tag.common.Ids;
import com.qianfan.tag.domain.PersonRecord;
import com.qianfan.tag.dto.PageResult;
import com.qianfan.tag.dto.PersonRequests;
import com.qianfan.tag.mapper.PersonMapper;
import com.qianfan.tag.remote.RemotePerson;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/** 人员资料写入和基于标签的组合检索。 */
@Service
public class PersonService {
    private final PersonMapper personMapper;
    private final PersonTagService personTagService;
    private final StructuredRuleService structuredRuleService;
    private final ApplicationEventPublisher eventPublisher;

    public PersonService(PersonMapper personMapper, PersonTagService personTagService,
                         StructuredRuleService structuredRuleService,
                         ApplicationEventPublisher eventPublisher) {
        this.personMapper = personMapper;
        this.personTagService = personTagService;
        this.structuredRuleService = structuredRuleService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PersonRecord upsert(PersonRequests.UpsertPerson request) {
        RemotePerson remote = new RemotePerson();
        remote.setExternalId(request.getExternalId());
        remote.setName(request.getName());
        remote.setGender(request.getGender());
        remote.setOrganization(request.getOrganization());
        remote.setOccupation(request.getOccupation());
        remote.setAddress(request.getAddress());
        remote.setRemark(request.getRemark());
        // 人工维护是一次新的本地变更，不能沿用页面回传的旧上游版本时间。
        remote.setUpdatedAt(new Date());
        PersonRecord person = upsertRemote(remote);
        personTagService.applyRules(person, "MANUAL_UPSERT");
        structuredRuleService.evaluatePublishedForPerson(person, "MANUAL_UPSERT");
        publishProfileRefresh(person.getId());
        return person;
    }

    public PersonRecord upsertRemote(RemotePerson remote) {
        if (remote.getExternalId() == null || remote.getExternalId().trim().isEmpty()) {
            throw new BusinessException("REMOTE_PERSON_INVALID", "远程人员唯一编码不能为空");
        }
        Date now = new Date();
        PersonRecord person = personMapper.findByExternalId(remote.getExternalId());
        if (person == null) {
            person = new PersonRecord();
            person.setId(Ids.uuid());
            person.setExternalId(remote.getExternalId());
            person.setCreatedAt(now);
        } else if (person.getSourceUpdatedAt() != null && remote.getUpdatedAt() != null
                && person.getSourceUpdatedAt().after(remote.getUpdatedAt())) {
            // 远程乱序数据不得覆盖本地较新的版本。
            return person;
        }
        person.setName(remote.getName());
        person.setGender(remote.getGender());
        person.setOrganization(remote.getOrganization());
        person.setOccupation(remote.getOccupation());
        person.setAddress(remote.getAddress());
        person.setRemark(remote.getRemark());
        person.setSourceUpdatedAt(remote.getUpdatedAt());
        person.setDeleted(Boolean.TRUE.equals(remote.getDeleted()) ? 1 : 0);
        person.setUpdatedAt(now);
        if (personMapper.findById(person.getId()) == null) {
            personMapper.insert(person);
        } else {
            personMapper.update(person);
        }
        publishProfileRefresh(person.getId());
        return person;
    }

    public PersonRecord requirePerson(String id) {
        PersonRecord person = personMapper.findById(id);
        if (person == null) {
            throw new BusinessException("PERSON_NOT_FOUND", "人员不存在");
        }
        return person;
    }

    @Transactional
    public void changeDeleted(String id, boolean deleted) {
        requirePerson(id);
        if (personMapper.updateDeleted(id, deleted ? 1 : 0, new Date()) == 0) {
            throw new BusinessException("PERSON_STATUS_UPDATE_FAILED", "人员删除状态更新失败");
        }
        publishProfileRefresh(id);
    }

    public PageResult<PersonRecord> search(PersonRequests.Search request) {
        int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null ? 20 : request.getPageSize();
        if (pageNo < 1 || pageSize < 1 || pageSize > 200) {
            throw new BusinessException("INVALID_PAGE", "页码必须大于 0，分页大小范围为 1 到 200");
        }
        List<String> tagIds = request.getTagIds() == null
                ? Collections.<String>emptyList() : request.getTagIds();
        boolean andMode = !"OR".equalsIgnoreCase(request.getTagOperator());
        boolean includeDeleted = Boolean.TRUE.equals(request.getIncludeDeleted());
        int offset = (pageNo - 1) * pageSize;
        long total = personMapper.countSearch(request.getKeyword(), tagIds, andMode, tagIds.size(), includeDeleted);
        List<PersonRecord> records = total == 0 ? Collections.<PersonRecord>emptyList()
                : personMapper.search(request.getKeyword(), tagIds, andMode, tagIds.size(),
                        offset, offset + pageSize, includeDeleted);
        return new PageResult<PersonRecord>(total, pageNo, pageSize, records);
    }

    private void publishProfileRefresh(String personId) {
        eventPublisher.publishEvent(new ProfileRefreshEvent(personId));
    }
}
