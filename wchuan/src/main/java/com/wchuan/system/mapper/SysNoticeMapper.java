package com.wchuan.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wchuan.system.domain.entity.SysNotice;
import java.util.List;

public interface SysNoticeMapper extends BaseMapper<SysNotice> {
    List<SysNotice> selectNoticeList();

}
