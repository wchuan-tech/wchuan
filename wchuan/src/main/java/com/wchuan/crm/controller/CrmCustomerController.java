package com.wchuan.crm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wchuan.crm.domain.dto.CustomerAssignDTO;
import com.wchuan.crm.domain.dto.CustomerCreateDTO;
import com.wchuan.crm.domain.dto.CustomerPageQuery;
import com.wchuan.crm.domain.dto.FollowupCreateDTO;
import com.wchuan.crm.domain.entity.Customer;
import com.wchuan.crm.domain.entity.FollowupRecord;
import com.wchuan.crm.service.ICrmCustomerService;
import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.domain.entity.SysUser;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** CRM 客户 Controller 控制层。 */
@Validated
@RestController
@RequestMapping("/crm/customer")
public class CrmCustomerController {

    @Resource
    private ICrmCustomerService crmCustomerService;

    /** 新增客户到当前销售私海。 */
    @PreAuthorize("@ss.hasPerm('crm:customer:add')")
    @PostMapping
    public ResponseResult<Customer> add(@Validated @RequestBody CustomerCreateDTO dto) {
        return crmCustomerService.createCustomer(dto);
    }

    /** 查询当前数据范围内可选择的销售。 */
    @PreAuthorize("@ss.hasPerm('crm:customer:list')")
    @GetMapping("/sales/options")
    public ResponseResult<List<SysUser>> salesOptions() {
        return ResponseResult.success(crmCustomerService.selectSalesOptions());
    }

    /** 查询公海客户。 */
    @PreAuthorize("@ss.hasPerm('crm:customer:list')")
    @GetMapping("/public-pool/list")
    public ResponseResult<IPage<Customer>> publicPoolList(@Validated CustomerPageQuery query) {
        return ResponseResult.success(crmCustomerService.selectPublicPoolPage(query));
    }

    /** 查询当前销售持有的私海客户。 */
    @PreAuthorize("@ss.hasPerm('crm:customer:list')")
    @GetMapping("/private-pool/list")
    public ResponseResult<IPage<Customer>> privatePoolList(@Validated CustomerPageQuery query) {
        return ResponseResult.success(crmCustomerService.selectPrivatePoolPage(query));
    }

    /** 从公海领取客户。 */
    @PreAuthorize("@ss.hasPerm('crm:customer:list')")
    @PostMapping("/claim/{id}")
    public ResponseResult<Customer> claim(
            @PathVariable @Positive(message = "客户ID必须大于0") Long id) {
        return crmCustomerService.claim(id);
    }

    /** 管理员或主管指派客户。 */
    @PreAuthorize("@ss.hasPerm('crm:customer:assign')")
    @PostMapping("/assign")
    public ResponseResult<Customer> assign(@Validated @RequestBody CustomerAssignDTO dto) {
        return crmCustomerService.assign(dto.getCustomerId(), dto.getTargetUserId());
    }

    /** 将客户移交给其他销售。 */
    @PreAuthorize("@ss.hasPerm('crm:customer:transfer')")
    @PostMapping("/transfer")
    public ResponseResult<Customer> transfer(@Validated @RequestBody CustomerAssignDTO dto) {
        return crmCustomerService.transfer(dto.getCustomerId(), dto.getTargetUserId());
    }

    /** 将当前销售持有的客户退回公海。 */
    @PreAuthorize("@ss.hasPerm('crm:customer:return')")
    @PostMapping("/return/{id}")
    public ResponseResult<Customer> returnToPool(
            @PathVariable @Positive(message = "客户ID必须大于0") Long id) {
        return crmCustomerService.returnToPool(id);
    }

    /** 添加客户跟进记录。 */
    @PreAuthorize("@ss.hasPerm('crm:customer:followup:add')")
    @PostMapping("/followup")
    public ResponseResult<FollowupRecord> addFollowup(
            @Validated @RequestBody FollowupCreateDTO dto) {
        return crmCustomerService.addFollowup(dto);
    }

    /** 查询指定客户的历史跟进轨迹。 */
    @PreAuthorize("@ss.hasPerm('crm:customer:list')")
    @GetMapping("/followup/list/{customerId}")
    public ResponseResult<List<FollowupRecord>> followupList(
            @PathVariable @Positive(message = "客户ID必须大于0") Long customerId) {
        return ResponseResult.success(crmCustomerService.selectFollowupList(customerId));
    }

}
