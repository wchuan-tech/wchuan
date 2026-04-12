package com.wchuan.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wchuan.system.domain.entity.SysNotice;
import org.apache.ibatis.annotations.Param;


import java.util.List;

public interface NoticeMapper extends BaseMapper<SysNotice> {
    /**
     * @InterceptorIgnore(tenantLine = "true") 表示此方法不被多租户插件自动修改 SQL
     */
    @InterceptorIgnore(tenantLine = "true")
    List<SysNotice> selectNoticeMixed(@Param("tenantId") Long tenantId);

    @InterceptorIgnore(tenantLine = "true")
    String selectTenantNameById(@Param("tenantId") Long tenantId);
}
