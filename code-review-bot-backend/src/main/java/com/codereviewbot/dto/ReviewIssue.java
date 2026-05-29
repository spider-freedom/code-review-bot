package com.codereviewbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewIssue {
    private String severity;  // "error", "warning", "info"
    private int line;
    private String title;
    private String description;
    private String suggestion;
    private String codeExample;
}
