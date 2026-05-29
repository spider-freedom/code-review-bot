package com.codereviewbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotBlank(message = "代码不能为空")
    @Size(max = 50_000, message = "代码长度不能超过 50000 字符")
    private String code;

    private String language;

    @Pattern(regexp = "code|diff", message = "mode 参数必须是 code 或 diff")
    private String mode;
}
