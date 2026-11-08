package com.wchuan.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wchuan.common.annotation.DataScope;
import com.wchuan.crm.domain.entity.Customer;
import com.wchuan.system.domain.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerMapper extends BaseMapper<Customer> {

    @DataScope(deptAlias = "d", userAlias = "u")
    IPage<Customer> selectPrivateCustomerPage(@Param("customer") Customer customer,
                                              Page<Customer> page);

    @DataScope(deptAlias = "d", userAlias = "u")
    Customer selectAccessiblePrivateCustomer(@Param("customer") Customer customer);

    @DataScope(deptAlias = "d", userAlias = "u")
    List<SysUser> selectSalesOptions(@Param("customer") Customer customer);

    @Select("SELECT * FROM crm_customer WHERE id = #{customerId} FOR UPDATE")
    Customer selectByIdForUpdate(@Param("customerId") Long customerId);

    @Update("""
            UPDATE crm_customer
            SET pool_type = 1,
                owner_user_id = #{ownerUserId},
                owner_user_name = #{ownerUserName},
                last_followup_time = #{claimTime},
                update_by = #{updateBy},
                update_time = #{claimTime}
            WHERE id = #{customerId}
              AND pool_type = 0
              AND owner_user_id IS NULL
            """)
    int claimFromPool(@Param("customerId") Long customerId,
                      @Param("ownerUserId") Long ownerUserId,
                      @Param("ownerUserName") String ownerUserName,
                      @Param("claimTime") LocalDateTime claimTime,
                      @Param("updateBy") String updateBy);

    @Update("""
            UPDATE crm_customer
            SET pool_type = #{poolType},
                owner_user_id = #{ownerUserId},
                owner_user_name = #{ownerUserName},
                update_by = #{updateBy},
                update_time = #{updateTime}
            WHERE id = #{id}
            """)
    int updateOwner(Customer customer);

    @Update("""
            UPDATE crm_customer
            SET pool_type = #{customer.poolType},
                owner_user_id = NULL,
                owner_user_name = NULL,
                update_by = #{customer.updateBy},
                update_time = #{customer.updateTime}
            WHERE id = #{customer.id}
              AND pool_type = 1
              AND owner_user_id = #{originalOwnerUserId}
            """)
    int clearOwnerToPool(@Param("customer") Customer customer,
                         @Param("originalOwnerUserId") Long originalOwnerUserId);

    @Update("""
            UPDATE crm_customer
            SET last_followup_time = #{lastFollowupTime},
                last_followup_record = #{lastFollowupRecord},
                update_by = #{updateBy},
                update_time = #{updateTime}
            WHERE id = #{id}
            """)
    int updateLastFollowup(Customer customer);

    @Select("""
            SELECT id
            FROM crm_customer
            WHERE pool_type = 1
              AND lock_status = 0
              AND last_followup_time < #{cutoffTime}
            ORDER BY last_followup_time, id
            LIMIT #{batchSize}
            FOR UPDATE
            """)
    List<Customer> selectAutoPoolCandidatesForUpdate(@Param("cutoffTime") LocalDateTime cutoffTime,
                                                      @Param("batchSize") int batchSize);

    int batchAutoReturnToPool(@Param("customerIds") List<Long> customerIds,
                              @Param("cutoffTime") LocalDateTime cutoffTime,
                              @Param("updateTime") LocalDateTime updateTime,
                              @Param("updateBy") String updateBy);
}
