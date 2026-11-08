package com.wchuan.crm.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.wchuan.system.domain.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_customer")
public class Customer extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String customerName;

    private String phone;

    private String wechat;

    private String companyName;

    /** 公私海状态：0-公海，1-私海。 */
    private Integer poolType;

    /** 归属销售人员ID，公海客户为空。 */
    private Long ownerUserId;

    private String ownerUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastFollowupTime;

    private String lastFollowupRecord;

    /** 锁定状态：0-未锁定，1-已锁定。 */
    private Integer lockStatus;

    /** 客户阶段：线索、初访、意向、报价、签约。 */
    private String customerStage;
}
