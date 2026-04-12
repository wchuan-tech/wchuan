package com.wchuan.system.controller;

import com.wchuan.common.annotation.Log;

import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.domain.entity.SysNotice;
import com.wchuan.system.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dev/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /**
     * 获取公告列表
     * 逻辑：查询 [本租户公告] + [全平台公告]
     */
    @GetMapping("/list")
    public ResponseResult<List<SysNotice>> list() {
        return noticeService.list();
    }

    /**
     * 新增公告
     * 逻辑：租户管理员只能发“内部”公告，只有超级管理员(ID=1)能发“全平台”公告
     */
    @Log(title = "发布公告", businessType = 1)
    @PreAuthorize("@ss.hasPerm('dev:notice:add')")
    @PostMapping("/add")
    public ResponseResult<?> add(@RequestBody SysNotice notice) {
        return noticeService.add(notice);
    }

    /**
     * 删除公告
     * 逻辑：多租户插件会自动生效，确保 A 租户删不掉 B 租户的公告
     */
    @Log(title = "删除公告", businessType = 3)
    @PreAuthorize("@ss.hasPerm('dev:notice:remove')")
    @DeleteMapping("/{id}")
    public ResponseResult<?> remove(@PathVariable Long id) {
        return noticeService.remove(id);
    }

}