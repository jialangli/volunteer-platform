package com.gdupt.volunteer.stats;

import com.gdupt.volunteer.mapper.StatsMapper;
import com.gdupt.volunteer.stats.dto.MonthlyTrendItem;
import com.gdupt.volunteer.stats.dto.OverviewResponse;
import com.gdupt.volunteer.stats.dto.TopActivityItem;
import com.gdupt.volunteer.stats.dto.TopVolunteerItem;
import com.gdupt.volunteer.stats.dto.TypeDistItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 统计可视化：平台总览、活动类型分布、月度趋势、热门活动、志愿者 TOP。
 * 口径：有效服务人次与总工时仅统计 status=CONFIRMED 的工时流水。
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsMapper statsMapper;

    public OverviewResponse overview() {
        BigDecimal totalHours = statsMapper.totalHours();
        return OverviewResponse.builder()
                .activityCount(nvl(statsMapper.totalActivities()))
                .volunteerCount(nvl(statsMapper.totalVolunteers()))
                .totalServiceCount(nvl(statsMapper.totalServiceCount()))
                .totalHours(totalHours == null ? BigDecimal.ZERO : totalHours)
                .monthActivityCount(nvl(statsMapper.monthActivityCount()))
                .build();
    }

    public List<TypeDistItem> typeDistribution() {
        List<TypeDistItem> items = new ArrayList<>();
        for (Map<String, Object> row : statsMapper.typeDistribution()) {
            items.add(TypeDistItem.builder()
                    .typeId(toLong(row.get("typeId")))
                    .typeName(str(row.get("typeName")))
                    .activityCount(toLong(row.get("activityCount")))
                    .registrationCount(toLong(row.get("registrationCount")))
                    .build());
        }
        return items;
    }

    public List<MonthlyTrendItem> monthlyTrend(int months) {
        months = Math.min(24, Math.max(1, months));
        List<MonthlyTrendItem> items = new ArrayList<>();
        for (Map<String, Object> row : statsMapper.monthlyTrend(months)) {
            items.add(MonthlyTrendItem.builder()
                    .month(str(row.get("month")))
                    .activityCount(toLong(row.get("activityCount")))
                    .totalHours(toBigDecimal(row.get("totalHours")))
                    .build());
        }
        return items;
    }

    public List<TopActivityItem> topActivities(int limit) {
        limit = Math.min(50, Math.max(1, limit));
        List<TopActivityItem> items = new ArrayList<>();
        for (Map<String, Object> row : statsMapper.topActivities(limit)) {
            items.add(TopActivityItem.builder()
                    .activityId(toLong(row.get("activityId")))
                    .title(str(row.get("title")))
                    .regCount(toLong(row.get("regCount")))
                    .build());
        }
        return items;
    }

    public List<TopVolunteerItem> topVolunteers(int limit) {
        limit = Math.min(100, Math.max(1, limit));
        List<TopVolunteerItem> items = new ArrayList<>();
        for (Map<String, Object> row : statsMapper.topVolunteers(limit)) {
            items.add(TopVolunteerItem.builder()
                    .userId(toLong(row.get("userId")))
                    .realName(str(row.get("realName")))
                    .studentNo(str(row.get("studentNo")))
                    .totalHours(toBigDecimal(row.get("totalHours")))
                    .serviceCount(toLong(row.get("serviceCount")))
                    .activityCount(toLong(row.get("activityCount")))
                    .build());
        }
        return items;
    }

    // ---------- 转换 ----------

    private Long nvl(Long v) {
        return v == null ? 0L : v;
    }

    private Long toLong(Object v) {
        if (v == null) {
            return 0L;
        }
        return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString());
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        return v instanceof BigDecimal ? (BigDecimal) v : new BigDecimal(v.toString());
    }

    private String str(Object v) {
        return v == null ? null : v.toString();
    }
}
