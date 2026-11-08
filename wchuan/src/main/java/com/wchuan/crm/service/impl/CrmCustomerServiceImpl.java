package com.wchuan.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wchuan.common.exception.BusinessException;
import com.wchuan.crm.domain.dto.CustomerPageQuery;
import com.wchuan.crm.domain.dto.CustomerCreateDTO;
import com.wchuan.crm.domain.dto.FollowupCreateDTO;
import com.wchuan.crm.domain.entity.Customer;
import com.wchuan.crm.domain.entity.FollowupRecord;
import com.wchuan.crm.mapper.CustomerMapper;
import com.wchuan.crm.mapper.FollowupRecordMapper;
import com.wchuan.crm.service.ICrmCustomerService;
import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.domain.entity.SysUser;
import com.wchuan.system.domain.vo.LoginUser;
import com.wchuan.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CrmCustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer>
        implements ICrmCustomerService {

    private static final int PRIVATE_CUSTOMER_LIMIT = 50;
    private static final int PUBLIC_POOL = 0;
    private static final int PRIVATE_POOL = 1;
    private static final String ASSIGN_PERMISSION = "crm:customer:assign";
    private static final String SYSTEM_OPERATOR = "SYSTEM";
    private static final String SYSTEM_FOLLOWUP_TYPE = "系统";
    private static final String AUTO_POOL_CONTENT = "系统自动退回公海：客户超过7天未跟进";

    private final FollowupRecordMapper followupRecordMapper;
    private final SysUserMapper userMapper;

    @Override
    public IPage<Customer> selectPublicPoolPage(CustomerPageQuery query) {
        requireCurrentUser(getLoginUser());
        CustomerPageQuery pageQuery = query == null ? new CustomerPageQuery() : query;
        Page<Customer> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .eq(Customer::getPoolType, PUBLIC_POOL)
                .like(StringUtils.hasText(pageQuery.getCustomerName()),
                        Customer::getCustomerName, trim(pageQuery.getCustomerName()))
                .like(StringUtils.hasText(pageQuery.getPhone()),
                        Customer::getPhone, trim(pageQuery.getPhone()))
                .eq(StringUtils.hasText(pageQuery.getCustomerStage()),
                        Customer::getCustomerStage, trim(pageQuery.getCustomerStage()))
                .orderByDesc(Customer::getLastFollowupTime)
                .orderByDesc(Customer::getId);
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public IPage<Customer> selectPrivatePoolPage(CustomerPageQuery query) {
        requireCurrentUser(getLoginUser());
        CustomerPageQuery pageQuery = query == null ? new CustomerPageQuery() : query;
        Customer criteria = new Customer();
        criteria.setCustomerName(trim(pageQuery.getCustomerName()));
        criteria.setPhone(trim(pageQuery.getPhone()));
        criteria.setCustomerStage(trim(pageQuery.getCustomerStage()));
        return baseMapper.selectPrivateCustomerPage(
                criteria, new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<Customer> createCustomer(CustomerCreateDTO dto) {
        SysUser currentUser = requireCurrentUser(getLoginUser());
        Customer customer = new Customer();
        customer.setCustomerName(dto.getCustomerName().trim());
        customer.setPhone(trim(dto.getPhone()));
        customer.setWechat(trim(dto.getWechat()));
        customer.setCompanyName(trim(dto.getCompanyName()));
        customer.setCustomerStage(StringUtils.hasText(dto.getCustomerStage())
                ? dto.getCustomerStage() : "线索");
        customer.setPoolType(PRIVATE_POOL);
        customer.setOwnerUserId(currentUser.getId());
        customer.setOwnerUserName(getSalesName(currentUser));
        customer.setLockStatus(0);
        if (baseMapper.insert(customer) != 1) {
            throw new BusinessException("新增客户失败");
        }
        return ResponseResult.success("新增客户成功", customer);
    }

    @Override
    public List<SysUser> selectSalesOptions() {
        requireCurrentUser(getLoginUser());
        return baseMapper.selectSalesOptions(new Customer());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<Customer> claim(Long customerId) {
        requireId(customerId, "客户ID不能为空");
        SysUser currentUser = requireCurrentUser(getLoginUser());

        // Serialize claims by the same salesperson so concurrent claims cannot exceed the limit.
        SysUser lockedUser = userMapper.selectActiveUserForUpdate(currentUser.getId());
        if (lockedUser == null) {
            throw new BusinessException(403, "当前销售账号不可用");
        }

        Long privateCount = baseMapper.selectCount(new LambdaQueryWrapper<Customer>()
                .eq(Customer::getPoolType, PRIVATE_POOL)
                .eq(Customer::getOwnerUserId, currentUser.getId()));
        if (privateCount >= PRIVATE_CUSTOMER_LIMIT) {
            throw new BusinessException(409, "私海客户数量已达到上限 " + PRIVATE_CUSTOMER_LIMIT + " 个");
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = baseMapper.claimFromPool(
                customerId,
                currentUser.getId(),
                getSalesName(currentUser),
                now,
                currentUser.getUserName()
        );
        if (updated != 1) {
            throw new BusinessException(409, "客户不在公海或已被其他销售领取");
        }

        return ResponseResult.success("领取客户成功", requireCustomer(customerId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<Customer> assign(Long customerId, Long targetUserId) {
        LoginUser loginUser = getLoginUser();
        requirePermission(loginUser, ASSIGN_PERMISSION);
        SysUser currentUser = requireCurrentUser(loginUser);
        SysUser targetUser = requireTargetUser(targetUserId);
        Customer customer = requireCustomerForUpdate(customerId);

        updateOwner(customer, targetUser, currentUser.getUserName());
        return ResponseResult.success("分配客户成功", requireCustomer(customerId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<Customer> returnToPool(Long customerId) {
        SysUser currentUser = requireCurrentUser(getLoginUser());
        Customer customer = requireCustomerForUpdate(customerId);
        requirePrivateCustomerAccess(customer);

        Long ownerUserId = customer.getOwnerUserId();
        customer.setPoolType(PUBLIC_POOL);
        customer.setOwnerUserId(null);
        customer.setOwnerUserName(null);
        customer.setUpdateBy(currentUser.getUserName());
        customer.setUpdateTime(LocalDateTime.now());
        if (baseMapper.clearOwnerToPool(customer, ownerUserId) != 1) {
            throw new BusinessException(409, "客户状态已发生变化，请刷新后重试");
        }

        return ResponseResult.success("客户已退回公海", requireCustomer(customerId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<Customer> transfer(Long customerId, Long targetUserId) {
        SysUser currentUser = requireCurrentUser(getLoginUser());
        SysUser targetUser = requireTargetUser(targetUserId);
        if (Objects.equals(currentUser.getId(), targetUserId)) {
            throw new BusinessException(400, "客户不能移交给自己");
        }

        Customer customer = requireCustomerForUpdate(customerId);
        requirePrivateCustomerAccess(customer);
        updateOwner(customer, targetUser, currentUser.getUserName());
        return ResponseResult.success("移交客户成功", requireCustomer(customerId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseResult<FollowupRecord> addFollowup(FollowupCreateDTO dto) {
        FollowupRecord followupRecord = new FollowupRecord();
        BeanUtils.copyProperties(dto, followupRecord);
        validateFollowup(followupRecord);
        SysUser currentUser = requireCurrentUser(getLoginUser());
        Customer customer = requireCustomerForUpdate(followupRecord.getCustomerId());
        requirePrivateCustomerAccess(customer);

        LocalDateTime now = LocalDateTime.now();
        followupRecord.setId(null);
        followupRecord.setCreateBy(currentUser.getUserName());
        followupRecord.setUpdateBy(currentUser.getUserName());
        followupRecord.setCreateTime(now);
        followupRecord.setUpdateTime(now);
        if (followupRecordMapper.insert(followupRecord) != 1) {
            throw new BusinessException("新增跟进记录失败");
        }

        // 🔑 核心补全：如果本次写跟进时传了新的客户阶段，顺便推进阶段！
        if (StringUtils.hasText(followupRecord.getCustomerStage())) {
            customer.setCustomerStage(followupRecord.getCustomerStage().trim());
        }

        System.out.println("system" + followupRecord.getCustomerStage());
        System.out.println("crm" + customer.getCustomerStage());

        System.out.println("====== 准备更新客户，客户对象内容是：" + customer);
        System.out.println("====== 客户的 ID 是：" + customer.getId());

        customer.setLastFollowupTime(now);
        customer.setLastFollowupRecord(summarize(followupRecord.getContent()));
        customer.setUpdateBy(currentUser.getUserName());
        customer.setUpdateTime(now);
        customer.setCustomerStage(followupRecord.getCustomerStage());
        if (baseMapper.updateById(customer) != 1) {
            throw new BusinessException("更新客户最后跟进信息失败");
        }

        return ResponseResult.success("新增跟进记录成功", followupRecord);
    }

    @Override
    public List<FollowupRecord> selectFollowupList(Long customerId) {
        requireCurrentUser(getLoginUser());
        Customer customer = requireCustomer(customerId);
        requirePrivateCustomerAccess(customer);
        return followupRecordMapper.selectList(new LambdaQueryWrapper<FollowupRecord>()
                .eq(FollowupRecord::getCustomerId, customerId)
                .orderByDesc(FollowupRecord::getCreateTime)
                .orderByDesc(FollowupRecord::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int autoReturnToPool(LocalDateTime cutoffTime, int batchSize) {
        if (cutoffTime == null) {
            throw new IllegalArgumentException("最后跟进时间阈值不能为空");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("批处理数量必须大于0");
        }

        List<Customer> customers = baseMapper.selectAutoPoolCandidatesForUpdate(cutoffTime, batchSize);
        if (customers.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        List<Long> customerIds = customers.stream().map(Customer::getId).toList();
        int updated = baseMapper.batchAutoReturnToPool(
                customerIds, cutoffTime, now, SYSTEM_OPERATOR);
        if (updated != customers.size()) {
            throw new IllegalStateException("自动回收客户状态失败，预期更新 "
                    + customers.size() + " 条，实际更新 " + updated + " 条");
        }

        List<FollowupRecord> records = customers.stream().map(customer -> {
            FollowupRecord record = new FollowupRecord();
            record.setCustomerId(customer.getId());
            record.setFollowupType(SYSTEM_FOLLOWUP_TYPE);
            record.setContent(AUTO_POOL_CONTENT);
            record.setCreateBy(SYSTEM_OPERATOR);
            record.setUpdateBy(SYSTEM_OPERATOR);
            record.setCreateTime(now);
            record.setUpdateTime(now);
            return record;
        }).toList();
        if (followupRecordMapper.insertBatch(records) != records.size()) {
            throw new IllegalStateException("自动回收客户日志批量写入失败");
        }
        return customers.size();
    }

    private void updateOwner(Customer customer, SysUser targetUser, String operatorName) {
        customer.setPoolType(PRIVATE_POOL);
        customer.setOwnerUserId(targetUser.getId());
        customer.setOwnerUserName(getSalesName(targetUser));
        customer.setUpdateBy(operatorName);
        customer.setUpdateTime(LocalDateTime.now());
        if (baseMapper.updateOwner(customer) != 1) {
            throw new BusinessException(409, "客户状态已发生变化，请刷新后重试");
        }
    }

    private void requirePrivateCustomerAccess(Customer customer) {
        if (!Integer.valueOf(PRIVATE_POOL).equals(customer.getPoolType())) {
            throw new BusinessException(409, "客户当前不在私海");
        }
        Customer criteria = new Customer();
        criteria.setId(customer.getId());
        if (baseMapper.selectAccessiblePrivateCustomer(criteria) == null) {
            throw new BusinessException(403, "没有权限访问或操作该客户");
        }
    }

    private Customer requireCustomerForUpdate(Long customerId) {
        requireId(customerId, "客户ID不能为空");
        Customer customer = baseMapper.selectByIdForUpdate(customerId);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        return customer;
    }

    private Customer requireCustomer(Long customerId) {
        requireId(customerId, "客户ID不能为空");
        Customer customer = baseMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException(404, "客户不存在");
        }
        return customer;
    }

    private SysUser requireTargetUser(Long targetUserId) {
        requireId(targetUserId, "目标销售ID不能为空");
        SysUser user = userMapper.selectActiveUser(targetUserId);
        if (user == null) {
            throw new BusinessException(404, "目标销售不存在或账号不可用");
        }
        return user;
    }

    private void validateFollowup(FollowupRecord record) {
        if (record == null) {
            throw new BusinessException(400, "跟进记录不能为空");
        }
        requireId(record.getCustomerId(), "客户ID不能为空");
        if (!StringUtils.hasText(record.getFollowupType())) {
            throw new BusinessException(400, "跟进方式不能为空");
        }
        if (!"电话".equals(record.getFollowupType())
                && !"拜访".equals(record.getFollowupType())
                && !"微信".equals(record.getFollowupType())) {
            throw new BusinessException(400, "跟进方式只能是电话、拜访或微信");
        }
        if (!StringUtils.hasText(record.getContent())) {
            throw new BusinessException(400, "跟进内容不能为空");
        }
    }

    private String summarize(String content) {
        String normalized = content.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500);
    }

    private String getSalesName(SysUser user) {
        return StringUtils.hasText(user.getNickName()) ? user.getNickName() : user.getUserName();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new BusinessException(401, "登录状态已过期或未登录");
        }
        return loginUser;
    }

    private SysUser requireCurrentUser(LoginUser loginUser) {
        if (loginUser.getUser() == null || loginUser.getUser().getId() == null) {
            throw new BusinessException(401, "登录状态已过期或未登录");
        }
        return loginUser.getUser();
    }

    private void requirePermission(LoginUser loginUser, String permission) {
        if (!hasPermission(loginUser, permission)) {
            throw new BusinessException(403, "没有客户分配权限");
        }
    }

    private boolean hasPermission(LoginUser loginUser, String permission) {
        SysUser user = loginUser.getUser();
        boolean admin = user != null && (Long.valueOf(1L).equals(user.getId())
                || user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> "admin".equals(role.getRoleKey())));
        return admin || loginUser.getPermissions() != null
                && (loginUser.getPermissions().contains("*")
                || loginUser.getPermissions().contains(permission));
    }

    private void requireId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(400, message);
        }
    }


}
