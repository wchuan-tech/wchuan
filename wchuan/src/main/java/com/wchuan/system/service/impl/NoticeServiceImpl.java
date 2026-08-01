package com.wchuan.system.service.impl;


import com.wchuan.common.utils.SecurityUtils;
import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.domain.entity.SysNotice;
import com.wchuan.system.domain.entity.User;
import com.wchuan.system.domain.vo.LoginUser;
import com.wchuan.system.mapper.NoticeMapper;
import com.wchuan.system.mapper.TenantMapper;
import com.wchuan.system.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;

    @Override
    public ResponseResult<List<SysNotice>> list() {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long tenantId = loginUser.getUser().getTenantId();

        List<SysNotice> list = noticeMapper.selectNoticeMixed(tenantId);

        for(SysNotice sysNotice:list){
            System.out.println();
            System.out.println("timeNow" + new Date());
            System.out.println("loginUserName: "+ loginUser.getUser().getUserName());
            System.out.println("Authorities: " + loginUser.getAuthorities());
        }
        return new ResponseResult<>(200, "查询成功", list);
    }

    @Override
    public ResponseResult<?> add(SysNotice notice) {
        // 获取当前用户
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 安全校验：非超级管理员强制将类型设为 '0' (租户内部)
        if (loginUser.getUser().getId() != 1L) {
            notice.setType("0");
        }

        // 获取当前在线用户信息
        User user = loginUser.getUser();


        // 设置公告创建时间
        notice.setCreateTime(LocalDateTime.now());
        // 插入数据库
        // 注意：tenant_id 会由我们之前写的 MyMetaObjectHandler 自动填充
        noticeMapper.insert(notice);

        return new ResponseResult<>(200, "公告发布成功");
    }

    @Override
    public ResponseResult<?> removeBatch(List<Long> ids) {
        // 1. 先查出这些公告
        List<SysNotice> list = noticeMapper.selectBatchIds(ids);

        for (SysNotice notice : list) {
            // 2. 只有超级管理员能删全平台(Type 1)
            // 普通租户管理员只能删租户内(Type 0)
            if ("1".equals(notice.getType()) && !SecurityUtils.isAdmin()) {
                // 直接返回 ResponseResult 的错误状态，不抛异常
                return new ResponseResult<>(403,"无权删除全平台公共公告");
            }
        }

        // 3. 执行删除（此时多租户插件会拼接 WHERE tenant_id = ?）
        noticeMapper.deleteBatchIds(ids);
        return new ResponseResult<>(200,"删除成功");
    }

}
