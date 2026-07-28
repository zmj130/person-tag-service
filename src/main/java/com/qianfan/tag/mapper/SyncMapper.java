package com.qianfan.tag.mapper;

import com.qianfan.tag.domain.SyncBatch;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/** 同步批次和增量游标持久化接口。 */
public interface SyncMapper {
    SyncBatch findBatchByNo(@Param("batchNo") String batchNo);
    long countBatches(@Param("status") String status);
    List<SyncBatch> findBatches(@Param("status") String status,
                                @Param("offset") int offset, @Param("endRow") int endRow);
    int insertBatch(SyncBatch batch);
    int markBatchRunning(@Param("batchNo") String batchNo, @Param("startedAt") Date startedAt);
    int updateProgress(@Param("batchNo") String batchNo, @Param("cursorAfter") String cursorAfter,
                       @Param("received") int received, @Param("success") int success,
                       @Param("failure") int failure);
    int finishBatch(@Param("batchNo") String batchNo, @Param("status") String status,
                    @Param("errorMessage") String errorMessage, @Param("finishedAt") Date finishedAt);
    String findCursor(@Param("taskCode") String taskCode);
    int updateCursor(@Param("taskCode") String taskCode, @Param("cursor") String cursor,
                     @Param("updatedAt") Date updatedAt);
}
