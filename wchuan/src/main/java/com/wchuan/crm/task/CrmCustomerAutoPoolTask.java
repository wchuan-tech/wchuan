package com.wchuan.crm.task;

import com.wchuan.crm.service.ICrmCustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrmCustomerAutoPoolTask {

    private static final int INACTIVE_DAYS = 7;
    private static final int BATCH_SIZE = 200;

    private final ICrmCustomerService crmCustomerService;

    /** 每天凌晨 2 点回收超过 7 天未跟进且未锁定的私海客户。 */
    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Shanghai")
    public void autoReturnInactiveCustomers() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(INACTIVE_DAYS);
        int total = 0;
        int processed;

        log.info("开始执行 CRM 私海客户自动回收，最后跟进时间阈值: {}", cutoffTime);
        do {
            processed = crmCustomerService.autoReturnToPool(cutoffTime, BATCH_SIZE);
            total += processed;
        } while (processed == BATCH_SIZE);
        log.info("CRM 私海客户自动回收完成，共回收 {} 个客户", total);
    }
}
