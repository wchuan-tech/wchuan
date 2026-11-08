package com.wchuan.crm.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.wchuan.common.exception.BusinessException;
import com.wchuan.crm.domain.entity.Customer;
import com.wchuan.crm.mapper.CustomerMapper;
import com.wchuan.crm.mapper.FollowupRecordMapper;
import com.wchuan.system.domain.dto.ResponseResult;
import com.wchuan.system.domain.entity.SysUser;
import com.wchuan.system.domain.vo.LoginUser;
import com.wchuan.system.mapper.SysUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrmCustomerServiceImplTest {

    private static final Long USER_ID = 10L;
    private static final Long CUSTOMER_ID = 1000L;
    private static final String USER_NAME = "sales01";
    private static final String NICK_NAME = "销售一号";

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private FollowupRecordMapper followupRecordMapper;

    @Mock
    private SysUserMapper userMapper;

    private CrmCustomerServiceImpl service;
    private SysUser currentUser;

    @BeforeEach
    void setUp() {
        service = new CrmCustomerServiceImpl(followupRecordMapper, userMapper);
        ReflectionTestUtils.setField(service, "baseMapper", customerMapper);

        currentUser = new SysUser();
        currentUser.setId(USER_ID);
        currentUser.setUserName(USER_NAME);
        currentUser.setNickName(NICK_NAME);

        LoginUser loginUser = new LoginUser(currentUser, List.of("crm:customer:claim"));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("正常领取公海客户成功")
    void claimShouldSucceedForPublicPoolCustomer() {
        Customer claimedCustomer = claimedCustomer();
        when(userMapper.selectActiveUserForUpdate(USER_ID)).thenReturn(currentUser);
        when(customerMapper.selectCount(any(Wrapper.class))).thenReturn(12L);
        when(customerMapper.claimFromPool(
                eq(CUSTOMER_ID),
                eq(USER_ID),
                eq(NICK_NAME),
                any(LocalDateTime.class),
                eq(USER_NAME)
        )).thenReturn(1);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(claimedCustomer);

        ResponseResult<Customer> result = service.claim(CUSTOMER_ID);

        assertEquals(200, result.getCode());
        assertEquals("领取客户成功", result.getMsg());
        assertSame(claimedCustomer, result.getData());

        ArgumentCaptor<LocalDateTime> claimTimeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(customerMapper).claimFromPool(
                eq(CUSTOMER_ID),
                eq(USER_ID),
                eq(NICK_NAME),
                claimTimeCaptor.capture(),
                eq(USER_NAME)
        );
        assertNotNull(claimTimeCaptor.getValue());
        verify(customerMapper).selectById(CUSTOMER_ID);
        verifyNoInteractions(followupRecordMapper);
    }

    @Test
    @DisplayName("领取已被他人领取的客户时抛出业务异常")
    void claimShouldFailWhenCustomerWasClaimedByAnotherUser() {
        when(userMapper.selectActiveUserForUpdate(USER_ID)).thenReturn(currentUser);
        when(customerMapper.selectCount(any(Wrapper.class))).thenReturn(8L);
        when(customerMapper.claimFromPool(
                eq(CUSTOMER_ID),
                eq(USER_ID),
                eq(NICK_NAME),
                any(LocalDateTime.class),
                eq(USER_NAME)
        )).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.claim(CUSTOMER_ID)
        );

        assertEquals(409, exception.getCode());
        assertEquals("客户不在公海或已被其他销售领取", exception.getMessage());
        verify(customerMapper, never()).selectById(anyLong());
        verifyNoInteractions(followupRecordMapper);
    }

    @Test
    @DisplayName("销售名下私海客户已满时阻止领取")
    void claimShouldFailWhenPrivateCustomerLimitIsReached() {
        when(userMapper.selectActiveUserForUpdate(USER_ID)).thenReturn(currentUser);
        when(customerMapper.selectCount(any(Wrapper.class))).thenReturn(50L);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.claim(CUSTOMER_ID)
        );

        assertEquals(409, exception.getCode());
        assertTrue(exception.getMessage().contains("私海客户数量已达到上限 50 个"));
        verify(customerMapper, never()).claimFromPool(
                anyLong(), anyLong(), anyString(), any(LocalDateTime.class), anyString());
        verify(customerMapper, never()).selectById(anyLong());
        verifyNoInteractions(followupRecordMapper);
    }

    private Customer claimedCustomer() {
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setCustomerName("测试客户");
        customer.setPoolType(1);
        customer.setOwnerUserId(USER_ID);
        customer.setOwnerUserName(NICK_NAME);
        return customer;
    }
}
