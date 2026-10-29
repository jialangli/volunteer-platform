package com.gdupt.volunteer.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdupt.volunteer.common.ApiResponse;
import com.gdupt.volunteer.common.BusinessException;
import com.gdupt.volunteer.common.ErrorCode;
import com.gdupt.volunteer.entity.Announcement;
import com.gdupt.volunteer.mapper.AnnouncementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 公开端：已发布公告列表与详情（status=1 且置顶优先） */
@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementMapper announcementMapper;

    @GetMapping
    public ApiResponse<List<Announcement>> published(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = Math.max(1, page);
        size = Math.min(50, Math.max(1, size));
        int offset = (page - 1) * size;
        List<Announcement> list = announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1)
                .orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getPublishTime)
                .last("LIMIT " + offset + "," + size));
        return ApiResponse.ok(list);
    }

    @GetMapping("/{id}")
    public ApiResponse<Announcement> detail(@PathVariable Long id) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null) {
            throw new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND);
        }
        if (a.getStatus() == null || a.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ANNOUNCEMENT_NOT_FOUND, "公告未发布");
        }
        return ApiResponse.ok(a);
    }
}
