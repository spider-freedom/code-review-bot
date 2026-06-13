package com.codereviewbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Async code review task — persisted to survive restarts.
 */
@Data
@TableName("review_task")
public class ReviewTask {

    @TableId(type = IdType.ASSIGN_UUID)
    private String taskId;
    private String userId;       // API Key hash used as tenant identifier
    private String codeHash;     // MD5 of code content for result dedup
    private String status;       // PENDING | PROCESSING | COMPLETED | FAILED
    private String mode;         // "code" or "diff"
    private String code;         // Original code/diff content (truncated)
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
