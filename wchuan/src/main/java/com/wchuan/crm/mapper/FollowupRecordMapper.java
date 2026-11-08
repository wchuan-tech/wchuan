package com.wchuan.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wchuan.crm.domain.entity.FollowupRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FollowupRecordMapper extends BaseMapper<FollowupRecord> {

    int insertBatch(@Param("records") List<FollowupRecord> records);
}
