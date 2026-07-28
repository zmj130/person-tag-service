package com.qianfan.tag.service;

import com.qianfan.tag.common.BusinessException;
import com.qianfan.tag.common.Ids;
import com.qianfan.tag.config.RemotePersonProperties;
import com.qianfan.tag.domain.SyncBatch;
import com.qianfan.tag.dto.SyncRequest;
import com.qianfan.tag.dto.PageResult;
import com.qianfan.tag.mapper.SyncMapper;
import com.qianfan.tag.remote.RemotePersonClient;
import com.qianfan.tag.remote.RemotePersonPage;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Collections;
import java.util.List;

/** DolphinScheduler 调用的增量同步编排服务。 */
@Service
public class PersonSyncService {
    private static final String TASK_CODE = "PERSON_INCREMENTAL";
    private final RemotePersonClient remotePersonClient;
    private final RemotePersonProperties properties;
    private final SyncMapper syncMapper;
    private final SyncPageProcessor pageProcessor;

    public PersonSyncService(RemotePersonClient remotePersonClient, RemotePersonProperties properties,
                             SyncMapper syncMapper, SyncPageProcessor pageProcessor) {
        this.remotePersonClient = remotePersonClient;
        this.properties = properties;
        this.syncMapper = syncMapper;
        this.pageProcessor = pageProcessor;
    }

    /** synchronized 只解决单实例并发；生产调度还应把工作流最大并行度设置为 1。 */
    public synchronized SyncBatch synchronize(SyncRequest request) {
        SyncBatch existing = syncMapper.findBatchByNo(request.getBatchNo());
        if (existing != null && "SUCCESS".equals(existing.getStatus())) {
            return existing;
        }
        if (existing != null && "RUNNING".equals(existing.getStatus())) {
            throw new BusinessException("BATCH_RUNNING", "该同步批次正在执行");
        }

        String cursor = request.getStartCursor();
        if (cursor == null || cursor.trim().isEmpty()) {
            cursor = syncMapper.findCursor(TASK_CODE);
        }
        if (existing == null) {
            existing = newBatch(request.getBatchNo(), cursor);
            syncMapper.insertBatch(existing);
        } else {
            syncMapper.markBatchRunning(request.getBatchNo(), new Date());
        }

        try {
            boolean hasMore;
            do {
                RemotePersonPage page = remotePersonClient.fetchChanges(cursor, properties.getPageSize());
                String nextCursor = page.getNextCursor() == null ? cursor : page.getNextCursor();
                pageProcessor.process(request.getBatchNo(), page.getRecords(), nextCursor);
                cursor = nextCursor;
                hasMore = page.isHasMore();
                if (hasMore && (page.getRecords() == null || page.getRecords().isEmpty())) {
                    throw new BusinessException("REMOTE_CURSOR_STALLED", "远程接口声明还有数据，但当前页为空");
                }
            } while (hasMore);
            syncMapper.finishBatch(request.getBatchNo(), "SUCCESS", null, new Date());
        } catch (RuntimeException ex) {
            syncMapper.finishBatch(request.getBatchNo(), "FAILED", abbreviate(ex.getMessage()), new Date());
            throw ex;
        }
        return syncMapper.findBatchByNo(request.getBatchNo());
    }

    public PageResult<SyncBatch> listBatches(String status, int pageNo, int pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 200) {
            throw new BusinessException("INVALID_PAGE", "页码必须大于 0，分页大小范围为 1 到 200");
        }
        if (status != null && !"RUNNING".equals(status) && !"SUCCESS".equals(status)
                && !"FAILED".equals(status)) {
            throw new BusinessException("INVALID_SYNC_STATUS", "同步状态不合法");
        }
        int offset = (pageNo - 1) * pageSize;
        long total = syncMapper.countBatches(status);
        List<SyncBatch> records = total == 0 ? Collections.<SyncBatch>emptyList()
                : syncMapper.findBatches(status, offset, offset + pageSize);
        return new PageResult<SyncBatch>(total, pageNo, pageSize, records);
    }

    private SyncBatch newBatch(String batchNo, String cursor) {
        SyncBatch batch = new SyncBatch();
        batch.setId(Ids.uuid());
        batch.setBatchNo(batchNo);
        batch.setSyncType("INCREMENTAL");
        batch.setStatus("RUNNING");
        batch.setCursorBefore(cursor);
        batch.setCursorAfter(cursor);
        batch.setReceivedCount(0);
        batch.setSuccessCount(0);
        batch.setFailureCount(0);
        batch.setStartedAt(new Date());
        return batch;
    }

    private String abbreviate(String message) {
        if (message == null) {
            return "未知错误";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
