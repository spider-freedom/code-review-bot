package com.codereviewbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereviewbot.entity.ReviewTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReviewTaskMapper extends BaseMapper<ReviewTask> {
}
