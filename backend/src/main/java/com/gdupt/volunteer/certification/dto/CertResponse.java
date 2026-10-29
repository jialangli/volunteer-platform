package com.gdupt.volunteer.certification.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CertResponse {

    private Long id;

    /** 防伪编号 */
    private String serialNo;

    private String periodLabel;

    private BigDecimal totalHours;

    private String status;

    private String pdfUrl;

    private Long userId;

    private String realName;

    private String studentNo;

    private LocalDateTime createdAt;
}
