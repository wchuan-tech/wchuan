package com.wchuan.crm.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wchuan.crm.domain.dto.CustomerPageQuery;
import com.wchuan.crm.domain.dto.CustomerCreateDTO;
import com.wchuan.crm.domain.dto.FollowupCreateDTO;
import com.wchuan.crm.domain.entity.Customer;
import com.wchuan.crm.domain.entity.FollowupRecord;
import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.domain.entity.SysUser;

import java.time.LocalDateTime;
import java.util.List;

public interface ICrmCustomerService extends IService<Customer> {

    /** 分页查询公海客户。 */
    IPage<Customer> selectPublicPoolPage(CustomerPageQuery query);

    /** 分页查询当前销售持有的私海客户。 */
    IPage<Customer> selectPrivatePoolPage(CustomerPageQuery query);

    /** 新增客户到当前销售私海。 */
    ResponseResult<Customer> createCustomer(CustomerCreateDTO dto);

    /** 查询当前用户数据范围内可选择的销售。 */
    List<SysUser> selectSalesOptions();

    /** 从公海领取客户。 */
    ResponseResult<Customer> claim(Long customerId);

    /** 将客户指派给指定销售。 */
    ResponseResult<Customer> assign(Long customerId, Long targetUserId);

    /** 将当前销售持有的客户退回公海。 */
    ResponseResult<Customer> returnToPool(Long customerId);

    /** 将当前销售持有的客户移交给同租户销售。 */
    ResponseResult<Customer> transfer(Long customerId, Long targetUserId);

    /** 新增跟进记录并同步客户最后跟进信息。 */
    ResponseResult<FollowupRecord> addFollowup(FollowupCreateDTO followupRecord);

    /** 查询有数据权限的指定客户历史跟进轨迹。 */
    List<FollowupRecord> selectFollowupList(Long customerId);

    /** 定时批量回收超过指定时间未跟进的未锁定私海客户。 */
    int autoReturnToPool(LocalDateTime cutoffTime, int batchSize);
}
