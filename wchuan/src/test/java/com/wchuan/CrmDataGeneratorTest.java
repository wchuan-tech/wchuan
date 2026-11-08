package com.wchuan;

import com.wchuan.crm.domain.entity.Customer;
import com.wchuan.crm.service.ICrmCustomerService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SpringBootTest
public class CrmDataGeneratorTest {

    @Resource
    private ICrmCustomerService customerService; // 注入你的 CRM Service

    // 随机公司名称词库
    private final String[] CITIES = {"深圳", "北京", "上海", "广州", "长沙", "杭州", "武汉", "成都"};
    private final String[] INDUSTRIES = {"智能硬件", "互联网", "新能源", "生物医药", "重工业", "跨境电商", "数字科技"};
    private final String[] SUFFIXES = {"科技有限公司", "制造基地", "信息技术有限公司", "数字科技集团", "供应链中心"};

    // 随机联系人词库
    private final String[] SURNAMES = {"赵", "钱", "孙", "李", "周", "吴", "郑", "王", "张", "陈", "刘", "杨"};
    private final String[] TITLES = {"总", "经理", "主管", "总监", "工", "主任", "处长"};

    // 客户阶段词库
    private final String[] STAGES = {"线索", "初访", "意向", "报价", "签约"};

    // 模拟真实的跟进记录词库
    private final String[] FOLLOW_RECORDS = {
            "电话沟通，客户对新系统非常感兴趣，约定下周二演示 Demo。",
            "已添加微信，发送了产品报价方案初稿，等待客户内部评审。",
            "现场拜访技术负责人，解答了私有化部署和数据安全相关的疑问。",
            "与采购总监商务洽谈，对方要求争取 5% 的折扣，已反馈给领导批示。",
            "电话未接通，发送了跟进短信提醒。"
    };

    // 对应之前建立好的销售人员：{userId, 用户名}
    private static class SalesUser {
        Long id;
        String name;
        SalesUser(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private final SalesUser[] SALES_TEAM = {
            new SalesUser(5L, "销售小王"),
            new SalesUser(6L, "销售小李"),
            new SalesUser(9L, "长沙小赵")
    };

    /**
     * 批量生成 10,000 条贴合实体类结构的真实业务数据
     */
    @Test
    public void generateMassiveCustomers() {
        System.out.println("===== 开始执行生成客户数据 =====");

        int totalCount = 10000;
        int batchSize = 1000;
        Random random = new Random();

        List<Customer> customerList = new ArrayList<>(batchSize);
        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= totalCount; i++) {
            Customer customer = new Customer();

            // 1. 公司名称与联系人姓名
            String compName = CITIES[random.nextInt(CITIES.length)] +
                    INDUSTRIES[random.nextInt(INDUSTRIES.length)] +
                    "第" + (1000 + i) + "分公司" +
                    SUFFIXES[random.nextInt(SUFFIXES.length)];
            String contact = SURNAMES[random.nextInt(SURNAMES.length)] + TITLES[random.nextInt(TITLES.length)];
            String phoneNum = "13" + String.format("%09d", random.nextInt(1000000000));

            customer.setCompanyName(compName);
            customer.setCustomerName(contact);
            customer.setPhone(phoneNum);
            customer.setWechat("wx_" + phoneNum); // 自动生成微信号

            // 2. 公海 / 私海划分 (按实体类注释：0-公海，1-私海)
            boolean isPublicPool = random.nextInt(10) < 3; // 30% 概率进入公海

            if (isPublicPool) {
                // =============== 公海客户 ===============
                customer.setPoolType(0);              // 0-公海
                customer.setOwnerUserId(null);        // 无归属销售
                customer.setOwnerUserName(null);
                customer.setLockStatus(0);            // 未锁定
                customer.setCustomerStage("线索");     // 公海多为初步线索
                customer.setLastFollowupTime(null);
                customer.setLastFollowupRecord("待分配公海池资源，等待销售认领。");
            } else {
                // =============== 私海客户 ===============
                customer.setPoolType(1);              // 1-私海
                SalesUser sales = SALES_TEAM[random.nextInt(SALES_TEAM.length)];
                customer.setOwnerUserId(sales.id);      // 绑定销售人员ID
                customer.setOwnerUserName(sales.name);  // 冗余销售人员姓名

                // 锁定状态 (90% 未锁定，10% 锁定防止被自动回收)
                customer.setLockStatus(random.nextInt(10) == 0 ? 1 : 0);

                // 随机阶段
                customer.setCustomerStage(STAGES[random.nextInt(STAGES.length)]);

                // 模拟最后跟进时间 (0 ~ 15 天前，>7天的用于后续测试自动退回公海定时器)
                int daysAgo = random.nextInt(16);
                customer.setLastFollowupTime(LocalDateTime.now().minusDays(daysAgo));
                customer.setLastFollowupRecord(FOLLOW_RECORDS[random.nextInt(FOLLOW_RECORDS.length)]);
            }

            customerList.add(customer);

            // 每凑满 batchSize 批量入库一次
            if (customerList.size() >= batchSize) {
                customerService.saveBatch(customerList);
                customerList.clear();
            }
        }

        // 插入剩余不足一个 batch 的数据
        if (!customerList.isEmpty()) {
            customerService.saveBatch(customerList);
            customerList.clear();
        }

        long costTime = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("🎉 成功生成 " + totalCount + " 条真实客户数据！耗时: " + costTime + " 秒");
    }
}