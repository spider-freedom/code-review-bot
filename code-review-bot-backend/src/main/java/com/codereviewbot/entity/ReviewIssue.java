package com.codereviewbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Persisted review issue — linked to ReviewTask via taskId.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("review_issue")
public class ReviewIssue {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String severity;
    private Integer line;
    private String title;
    private String description;
    private String suggestion;
    private String codeExample;
    private LocalDateTime createTime;
}
