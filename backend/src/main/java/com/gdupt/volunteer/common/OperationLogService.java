package com.gdupt.volunteer.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdupt.volunteer.entity.OperationLog;
import com.gdupt.volunteer.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 操作审计日志公共服务：各业务模块统一落库 operation_log。
 * detail 列类型为 JSON，写入前统一包装为合法 JSON（{"note":"..."}），避免非法 JSON 导致插入失败。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    public void record(Long userId, String module, String action, String targetType, Long targetId, String ip, String detail) {
        OperationLog entry = new OperationLog();
        entry.setUserId(userId);
        entry.setModule(module);
        entry.setAction(action);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        entry.setDetail(toJson(detail));
        entry.setIp(ip);
        operationLogMapper.insert(entry);
    }

    private String toJson(String detail) {
        if (detail == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(Map.of("note", detail));
        } catch (JsonProcessingException e) {
            log.warn("操作日志 detail 序列化失败，写入空对象：{}", e.getMessage());
            return "{}";
        }
    }
}
