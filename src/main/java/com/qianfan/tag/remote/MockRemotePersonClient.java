package com.qianfan.tag.remote;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

/** 本地演示数据，不包含任何真实人员信息。 */
@Component
@ConditionalOnProperty(name = "remote-person.mode", havingValue = "mock", matchIfMissing = true)
public class MockRemotePersonClient implements RemotePersonClient {
    @Override
    public RemotePersonPage fetchChanges(String cursor, int pageSize) {
        RemotePersonPage page = new RemotePersonPage();
        if (cursor != null && !cursor.isEmpty()) {
            page.setRecords(Collections.<RemotePerson>emptyList());
            page.setNextCursor(cursor);
            page.setHasMore(false);
            return page;
        }

        RemotePerson first = person("DEMO-001", "示例甲", "某物流企业", "长途货运司机", "长期从事跨区域货运");
        first.setTagCodes(Arrays.asList("EXTERNAL_FOCUS"));
        RemotePerson second = person("DEMO-002", "示例乙", "某科技企业", "软件工程师", "负责内部系统维护");
        page.setRecords(Arrays.asList(first, second));
        page.setNextCursor("MOCK-END");
        page.setHasMore(false);
        return page;
    }

    private RemotePerson person(String externalId, String name, String organization,
                                String occupation, String remark) {
        RemotePerson person = new RemotePerson();
        person.setExternalId(externalId);
        person.setName(name);
        person.setOrganization(organization);
        person.setOccupation(occupation);
        person.setRemark(remark);
        person.setUpdatedAt(new Date());
        return person;
    }
}
