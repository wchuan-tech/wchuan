package com.wchuan.system.service.impl;


import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.domain.entity.SysNotice;
import com.wchuan.system.domain.entity.User;
import com.wchuan.system.domain.vo.LoginUser;
import com.wchuan.system.mapper.NoticeMapper;
import com.wchuan.system.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;

    @Override
    public ResponseResult<List<SysNotice>> list() {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long tenantId = loginUser.getUser().getTenantId();

        // 调用自定义 Mapper 方法，绕过插件的硬性隔离，实现手动混合查询
        List<SysNotice> list = noticeMapper.selectNoticeMixed(tenantId);

        for(SysNotice sysNotice:list){
            System.out.println("loginUserName: "+ loginUser.getUser().getUserName());
            System.out.println("Authorities: " + loginUser.getAuthorities());
            System.out.println("sysNotice.name: " + sysNotice.getTenantName());
        }
        return new ResponseResult<>(200, "查询成功", list);
    }

    @Override
    public ResponseResult<?> add(SysNotice notice) {
        LoginUser loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 安全校验：非超级管理员强制将类型设为 '0' (租户内部)
        if (loginUser.getUser().getId() != 1L) {
            notice.setType("0");
        }

        User user = loginUser.getUser();

        // ✅ 自动填充：发布人（昵称）
        notice.setCreateBy(user.getNickName());

        // ✅ 自动填充：租户ID
        notice.setTenantId(user.getTenantId());

        String tenantName = noticeMapper.selectTenantNameById(notice.getTenantId());

        notice.setTenantName(tenantName);
        // 设置创建者姓名（方便展示）
        notice.setCreateBy(loginUser.getUsername());

        // 插入数据库
        // 注意：tenant_id 会由我们之前写的 MyMetaObjectHandler 自动填充
        noticeMapper.insert(notice);

        return new ResponseResult<>(200, "公告发布成功");
    }

    @Override
    public ResponseResult<?> remove(Long id) {
        noticeMapper.deleteById(id);
        return new ResponseResult<>(200, "删除成功");
    }
}
