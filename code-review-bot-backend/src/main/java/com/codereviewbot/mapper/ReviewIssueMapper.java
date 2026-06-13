package com.codereviewbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codereviewbot.entity.ReviewIssue;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReviewIssueMapper extends BaseMapper<ReviewIssue> {
}
