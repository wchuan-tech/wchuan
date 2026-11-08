package com.wchuan.system.service.impl;

import com.wchuan.system.domain.entity.SysOperLog;
import com.wchuan.system.mapper.SysLogMapper;
import com.wchuan.system.service.AsyncLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AsyncLogServiceImpl implements AsyncLogService {

    private final SysLogMapper logMapper;

    /**
     * 异步保存操作日志
     * 当这个方法被调用时，Spring 会从线程池中启动一个新线程来执行它，
     * 主业务线程（Controller）会立即返回，不会等待数据库写入完成。
     */
    @Override
    @Async("taskExecutor")
    public void saveLog(SysOperLog operLog) {
        logMapper.insert(operLog);
    }

}