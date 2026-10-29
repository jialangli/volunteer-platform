package com.gdupt.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("certification")
public class Certification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 证明周期，如 2025-2026 学年 */
    private String periodLabel;

    /** 周期内累计工时 */
    private BigDecimal totalHours;

    /** 证明编号（防伪） */
    private String serialNo;

    /** GENERATED/INVALID */
    private String status;

    /** 证明 PDF 地址（后续生成） */
    private String pdfUrl;

    private LocalDateTime createdAt;
}
