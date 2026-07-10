package com.codereviewbot.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewTaskResponse {
    private String taskId;
    private String status;
    private String errorMessage;
    private String code;
    private String mode;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
