package com.qianfan.tag.service;

import com.qianfan.tag.domain.PersonRecord;
import com.qianfan.tag.mapper.SyncMapper;
import com.qianfan.tag.remote.RemotePerson;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/** 每一页同步数据使用独立事务，失败时整页回滚，游标也不会前移。 */
@Service
public class SyncPageProcessor {
    private static final String TASK_CODE = "PERSON_INCREMENTAL";
    private final PersonService personService;
    private final PersonTagService personTagService;
    private final SyncMapper syncMapper;

    public SyncPageProcessor(PersonService personService, PersonTagService personTagService, SyncMapper syncMapper) {
        this.personService = personService;
        this.personTagService = personTagService;
        this.syncMapper = syncMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int process(String batchNo, List<RemotePerson> records, String nextCursor) {
        int success = 0;
        for (RemotePerson remote : records) {
            PersonRecord person = personService.upsertRemote(remote);
            if (!Boolean.TRUE.equals(remote.getDeleted())) {
                personTagService.bindRemoteTags(person, remote.getTagCodes(), batchNo);
                personTagService.removeRemoteTags(person, remote.getRemovedTagCodes());
                personTagService.applyRules(person, batchNo);
            }
            success++;
        }
        syncMapper.updateCursor(TASK_CODE, nextCursor, new Date());
        syncMapper.updateProgress(batchNo, nextCursor, records.size(), success, 0);
        return success;
    }
}
