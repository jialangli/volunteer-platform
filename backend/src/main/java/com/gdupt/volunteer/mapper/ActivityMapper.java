package com.gdupt.volunteer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gdupt.volunteer.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {

    /**
     * 原子扣减/回补剩余名额（防超卖核心）。
     * delta > 0 表示扣减（报名），delta < 0 表示回补（取消/审核拒绝）。
     * 扣减场景下条件 status='RECRUITING' AND capacity_remaining >= delta 保证不会超卖。
     */
    @Update("UPDATE activity SET capacity_remaining = capacity_remaining - #{delta}, version = version + 1 " +
            "WHERE id = #{activityId} AND deleted_at IS NULL " +
            "AND ( #{delta} < 0 OR (status = 'RECRUITING' AND capacity_remaining >= #{delta}) )")
    int updateRemaining(@Param("activityId") Long activityId, @Param("delta") int delta);
}
