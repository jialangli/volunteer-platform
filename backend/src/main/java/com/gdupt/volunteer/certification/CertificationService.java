package com.gdupt.volunteer.certification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdupt.volunteer.certification.dto.CertResponse;
import com.gdupt.volunteer.certification.dto.IssueCertRequest;
import com.gdupt.volunteer.common.BusinessException;
import com.gdupt.volunteer.common.ErrorCode;
import com.gdupt.volunteer.common.FileStorageService;
import com.gdupt.volunteer.common.OperationLogService;
import com.gdupt.volunteer.common.PageResult;
import com.gdupt.volunteer.common.PdfService;
import com.gdupt.volunteer.entity.Certification;
import com.gdupt.volunteer.entity.Role;
import com.gdupt.volunteer.entity.ServiceHours;
import com.gdupt.volunteer.entity.User;
import com.gdupt.volunteer.entity.UserRole;
import com.gdupt.volunteer.mapper.CertificationMapper;
import com.gdupt.volunteer.mapper.RoleMapper;
import com.gdupt.volunteer.mapper.ServiceHoursMapper;
import com.gdupt.volunteer.mapper.UserMapper;
import com.gdupt.volunteer.mapper.UserRoleMapper;
import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 志愿服务证明（周期证明）：组织者/管理员为志愿者按周期（如学年）签发，
 * 汇总周期内全部 CONFIRMED 工时，serial_no 防伪编号全局唯一；同一用户同一周期仅可签发一次。
 */
@Service
@RequiredArgsConstructor
public class CertificationService {

    private final CertificationMapper certificationMapper;
    private final UserMapper userMapper;
    private final ServiceHoursMapper serviceHoursMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final OperationLogService operationLogService;
    private final FileStorageService fileStorageService;
    private final PdfService pdfService;

    @Transactional
    public Long issue(IssueCertRequest req, Long operatorId, String ip) {
        User volunteer = userMapper.selectById(req.getUserId());
        if (volunteer == null || volunteer.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        Certification existing = certificationMapper.selectOne(new LambdaQueryWrapper<Certification>()
                .eq(Certification::getUserId, req.getUserId())
                .eq(Certification::getPeriodLabel, req.getPeriodLabel()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.CERT_ALREADY_EXISTS);
        }
        // 组织者仅可为已参与的志愿者签发；管理员任意签发
        if (!isAdmin(operatorId) && !isOrganizer(operatorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        // 汇总该志愿者全部 CONFIRMED 工时
        List<ServiceHours> hoursList = serviceHoursMapper.selectList(new LambdaQueryWrapper<ServiceHours>()
                .eq(ServiceHours::getUserId, req.getUserId())
                .eq(ServiceHours::getStatus, "CONFIRMED"));
        BigDecimal total = BigDecimal.ZERO;
        for (ServiceHours h : hoursList) {
            total = total.add(h.getHours() == null ? BigDecimal.ZERO : h.getHours());
        }
        Certification cert = new Certification();
        cert.setUserId(req.getUserId());
        cert.setPeriodLabel(req.getPeriodLabel());
        cert.setTotalHours(total);
        cert.setSerialNo(nextSerialNo());
        cert.setStatus("GENERATED");
        certificationMapper.insert(cert);
        // 同步生成证明 PDF（签发即有电子版），失败则回滚整笔签发
        try {
            byte[] pdf = pdfService.generateCert(new PdfService.CertData(
                    cert.getSerialNo(),
                    volunteer.getRealName() == null ? volunteer.getUsername() : volunteer.getRealName(),
                    volunteer.getStudentNo() == null ? "" : volunteer.getStudentNo(),
                    req.getPeriodLabel(),
                    total.stripTrailingZeros().toPlainString(),
                    PdfService.todayText(),
                    "志愿服务管理平台"));
            String url = fileStorageService.write("certs/cert-" + cert.getSerialNo() + ".pdf", pdf);
            cert.setPdfUrl(url);
            certificationMapper.updateById(cert);
        } catch (DocumentException | IOException e) {
            throw new BusinessException(ErrorCode.FILE_WRITE_FAILED, "证明 PDF 生成失败：" + e.getMessage());
        }
        operationLogService.record(operatorId, "certification", "ISSUE", "USER", req.getUserId(), ip,
                "serialNo=" + cert.getSerialNo() + ",period=" + req.getPeriodLabel() + ",hours=" + total
                        + ",pdf=" + cert.getPdfUrl());
        return cert.getId();
    }

    /** 下载证明 PDF：本人/管理员/组织者，Content-Disposition 使用 ASCII 文件名避免乱码 */
    public ResponseEntity<byte[]> downloadPdf(Long id, Long operatorId) {
        Certification cert = certificationMapper.selectById(id);
        if (cert == null) {
            throw new BusinessException(ErrorCode.CERTIFICATE_NOT_FOUND);
        }
        if (!cert.getUserId().equals(operatorId) && !isAdmin(operatorId) && !isOrganizer(operatorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        byte[] data = fileStorageService.read(cert.getPdfUrl());
        String filename = "cert-" + cert.getSerialNo() + ".pdf";
        String disposition = "attachment; filename=\"" + filename + "\"";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    /** 我的证明列表 */
    public PageResult<CertResponse> listMine(Long userId, int page, int size) {
        return list(userId, page, size);
    }

    /** 查看某志愿者的证明（ADMIN/ORGANIZER） */
    public PageResult<CertResponse> listByVolunteer(Long userId, int page, int size) {
        return list(userId, page, size);
    }

    /** 证明详情：本人或管理员/组织者 */
    public CertResponse detail(Long id, Long operatorId) {
        Certification cert = certificationMapper.selectById(id);
        if (cert == null) {
            throw new BusinessException(ErrorCode.CERTIFICATE_NOT_FOUND);
        }
        if (!cert.getUserId().equals(operatorId) && !isAdmin(operatorId) && !isOrganizer(operatorId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return toResponse(cert);
    }

    // ---------- 私有 ----------

    private PageResult<CertResponse> list(Long userId, int page, int size) {
        page = Math.max(1, page);
        size = Math.min(100, Math.max(1, size));
        Page<Certification> p = certificationMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Certification>()
                        .eq(Certification::getUserId, userId)
                        .orderByDesc(Certification::getId));
        List<CertResponse> items = new ArrayList<>(p.getRecords().size());
        for (Certification c : p.getRecords()) {
            items.add(toResponse(c));
        }
        return PageResult.of(items, p.getTotal(), page, size);
    }

    private CertResponse toResponse(Certification cert) {
        User u = userMapper.selectById(cert.getUserId());
        return CertResponse.builder()
                .id(cert.getId())
                .serialNo(cert.getSerialNo())
                .periodLabel(cert.getPeriodLabel())
                .totalHours(cert.getTotalHours())
                .status(cert.getStatus())
                .pdfUrl(cert.getPdfUrl())
                .userId(cert.getUserId())
                .realName(u == null ? null : (u.getRealName() == null ? u.getUsername() : u.getRealName()))
                .studentNo(u == null ? null : u.getStudentNo())
                .createdAt(cert.getCreatedAt())
                .build();
    }

    /** 编号：CERT-YYYYMMDD-当日序号（>=1，补 0 到 4 位） */
    private synchronized String nextSerialNo() {
        String prefix = "CERT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        Long count = certificationMapper.selectCount(new LambdaQueryWrapper<Certification>()
                .likeRight(Certification::getSerialNo, prefix));
        return prefix + String.format("%04d", (count == null ? 0 : count) + 1);
    }

    private boolean hasRole(Long userId, String code) {
        List<UserRole> links = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (links.isEmpty()) {
            return false;
        }
        List<Long> roleIds = links.stream().map(UserRole::getRoleId).toList();
        return roleMapper.selectBatchIds(roleIds).stream().anyMatch(r -> code.equals(r.getCode()));
    }

    private boolean isAdmin(Long userId) {
        return hasRole(userId, "ADMIN");
    }

    private boolean isOrganizer(Long userId) {
        return hasRole(userId, "ORGANIZER");
    }
}
