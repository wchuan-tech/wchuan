package com.wchuan.system.service;

import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.domain.entity.SysNotice;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public interface NoticeService {
    ResponseResult<List<SysNotice>> list();
    ResponseResult<?> add(SysNotice notice);
    ResponseResult<?> remove(Long id);
}
