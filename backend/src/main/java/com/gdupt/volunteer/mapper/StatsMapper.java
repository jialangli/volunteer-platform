package com.gdupt.volunteer.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 统计聚合 SQL（口径：仅 CONFIRMED 工时计入有效服务） */
@Mapper
public interface StatsMapper {

    @Select("SELECT COALESCE(SUM(hours),0) FROM service_hours WHERE status='CONFIRMED'")
    BigDecimal totalHours();

    @Select("SELECT COUNT(*) FROM service_hours WHERE status='CONFIRMED'")
    Long totalServiceCount();

    @Select("SELECT COUNT(*) FROM activity WHERE deleted_at IS NULL")
    Long totalActivities();

    @Select("SELECT COUNT(*) FROM activity WHERE deleted_at IS NULL "
            + "AND DATE_FORMAT(start_time,'%Y-%m') = DATE_FORMAT(CURDATE(),'%Y-%m')")
    Long monthActivityCount();

    @Select("SELECT COUNT(DISTINCT ur.user_id) FROM user_roles ur JOIN roles r ON ur.role_id=r.id WHERE r.code='VOLUNTEER'")
    Long totalVolunteers();

    @Select("SELECT at.id AS typeId, at.name AS typeName, COUNT(a.id) AS activityCount, COUNT(r.id) AS registrationCount "
            + "FROM activity_type at "
            + "LEFT JOIN activity a ON a.type_id = at.id AND a.deleted_at IS NULL "
            + "LEFT JOIN registration r ON r.activity_id = a.id "
            + "GROUP BY at.id, at.name ORDER BY activityCount DESC, at.sort ASC")
    List<Map<String, Object>> typeDistribution();

    @Select("SELECT DATE_FORMAT(a.start_time,'%Y-%m') AS month, COUNT(DISTINCT a.id) AS activityCount, "
            + "COALESCE(SUM(CASE WHEN h.status='CONFIRMED' THEN h.hours ELSE 0 END),0) AS totalHours "
            + "FROM activity a LEFT JOIN service_hours h ON h.activity_id = a.id "
            + "WHERE a.deleted_at IS NULL AND a.start_time >= DATE_SUB(CURDATE(), INTERVAL #{months} MONTH) "
            + "GROUP BY month ORDER BY month")
    List<Map<String, Object>> monthlyTrend(@Param("months") int months);

    @Select("SELECT a.id AS activityId, a.title AS title, COUNT(r.id) AS regCount "
            + "FROM activity a JOIN registration r ON r.activity_id = a.id "
            + "WHERE a.deleted_at IS NULL GROUP BY a.id, a.title ORDER BY regCount DESC LIMIT #{limit}")
    List<Map<String, Object>> topActivities(@Param("limit") int limit);

    @Select("SELECT u.id AS userId, u.real_name AS realName, u.student_no AS studentNo, "
            + "COALESCE(SUM(h.hours),0) AS totalHours, COUNT(h.id) AS serviceCount, COUNT(DISTINCT h.activity_id) AS activityCount "
            + "FROM users u JOIN service_hours h ON h.user_id = u.id AND h.status='CONFIRMED' "
            + "WHERE u.deleted_at IS NULL GROUP BY u.id, u.real_name, u.student_no "
            + "ORDER BY totalHours DESC LIMIT #{limit}")
    List<Map<String, Object>> topVolunteers(@Param("limit") int limit);
}
