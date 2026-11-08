package com.wchuan.system.controller;

import com.wchuan.system.service.ISysDeptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SysDeptControllerMappingTest {

    @Mock
    private ISysDeptService deptService;

    @InjectMocks
    private SysDeptController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(deptService.selectDeptList(any())).thenReturn(List.of());
        when(deptService.buildDeptTreeSelect(any())).thenReturn(List.of());
    }

    @Test
    void treeselectMustUseStaticRouteInsteadOfDepartmentIdRoute() throws Exception {
        mockMvc.perform(get("/system/dept/treeselect"))
                .andExpect(status().isOk());

        verify(deptService).selectDeptList(any());
        verify(deptService).buildDeptTreeSelect(any());
    }
}
