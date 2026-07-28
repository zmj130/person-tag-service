package com.qianfan.tag.mapper;

import com.qianfan.tag.domain.ImportBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface ImportMapper {
    ImportBatch findByBatchNo(@Param("batchNo") String batchNo);
    int insert(ImportBatch batch);
    int restart(@Param("batchNo") String batchNo, @Param("fileName") String fileName,
                @Param("startedAt") Date startedAt);
    int finish(@Param("batchNo") String batchNo, @Param("status") String status,
               @Param("totalCount") int totalCount, @Param("successCount") int successCount,
               @Param("errorMessage") String errorMessage, @Param("finishedAt") Date finishedAt);
    List<ImportBatch> findRecent(@Param("limit") int limit);
}
