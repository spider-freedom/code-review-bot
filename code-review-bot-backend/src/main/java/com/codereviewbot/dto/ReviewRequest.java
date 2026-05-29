package com.codereviewbot.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private String code;
    private String language;
    private String mode;  // "code" or "diff"
}
