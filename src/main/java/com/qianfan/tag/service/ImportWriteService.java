package com.qianfan.tag.service;

import com.qianfan.tag.domain.IndicatorDefinition;
import com.qianfan.tag.domain.PersonRecord;
import com.qianfan.tag.dto.PersonImportRow;
import com.qianfan.tag.remote.RemotePerson;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class ImportWriteService {
    private final PersonService personService;
    private final PersonTagService personTagService;
    private final IndicatorService indicatorService;
    private final StructuredRuleService ruleService;

    public ImportWriteService(PersonService personService, PersonTagService personTagService,
                              IndicatorService indicatorService, StructuredRuleService ruleService) {
        this.personService = personService;
        this.personTagService = personTagService;
        this.indicatorService = indicatorService;
        this.ruleService = ruleService;
    }

    @Transactional
    public int write(String batchNo, List<PersonImportRow> rows) {
        Date sourceUpdatedAt = new Date();
        for (PersonImportRow row : rows) {
            RemotePerson remote = new RemotePerson();
            remote.setExternalId(row.getExternalId());
            remote.setName(row.getName());
            remote.setGender(row.getGender());
            remote.setOrganization(row.getOrganization());
            remote.setOccupation(row.getOccupation());
            remote.setAddress(row.getAddress());
            remote.setRemark(row.getRemark());
            remote.setDeleted(row.isDeleted());
            remote.setUpdatedAt(sourceUpdatedAt);
            PersonRecord person = personService.upsertRemote(remote);
            if (row.isDeleted()) continue;
            for (Map.Entry<String, String> item : row.getIndicators().entrySet()) {
                IndicatorDefinition definition = indicatorService.findByCode(item.getKey());
                indicatorService.saveImportedValue(person.getId(), definition, item.getValue(), batchNo, sourceUpdatedAt);
            }
            personTagService.applyRules(person, batchNo);
            ruleService.evaluatePublishedForPerson(person, batchNo);
        }
        return rows.size();
    }
}
