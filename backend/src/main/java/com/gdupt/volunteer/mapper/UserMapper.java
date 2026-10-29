package com.gdupt.volunteer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gdupt.volunteer.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
