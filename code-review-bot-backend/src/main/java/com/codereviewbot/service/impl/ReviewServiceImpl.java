package com.codereviewbot.service.impl;

import com.codereviewbot.dto.ReviewIssue;
import com.codereviewbot.dto.ReviewRequest;
import com.codereviewbot.service.ReviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_CODE_LENGTH = 50_000; // 50KB

    @Value("${deepseek.api-url}")
    private String apiUrl;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.model}")
    private String model;

    @Override
    public SseEmitter reviewStream(ReviewRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);

        CompletableFuture.runAsync(() -> {
            try {
                String code = request.getCode();
                if (code == null || code.isBlank()) {
                    sendDone(emitter, "代码为空，无需审查");
                    emitter.complete();
                    return;
                }

                if (code.length() > MAX_CODE_LENGTH) {
                    sendError(emitter, "代码过长，请限制在 50KB 以内");
                    emitter.complete();
                    return;
                }

                callDeepSeekApi(code, request.getMode(), emitter);
                emitter.complete();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                emitter.completeWithError(e);
            } catch (IOException e) {
                log.warn("SSE connection closed: {}", e.getMessage());
                try { emitter.complete(); } catch (Exception ignored) {}
            } catch (Exception e) {
                log.error("Review stream error", e);
                try {
                    sendError(emitter, "审查过程出错: " + e.getMessage());
                    emitter.complete();
                } catch (IOException ignored) {
                    try { emitter.complete(); } catch (Exception ignored2) {}
                }
            }
        });

        return emitter;
    }

    private void callDeepSeekApi(String code, String mode, SseEmitter emitter) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(300_000);

        String requestBody = buildRequestBody(code, mode);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            String errorBody = readErrorBody(conn);
            log.error("DeepSeek API error {}: {}", status, errorBody);
            sendError(emitter, "AI 服务返回错误 (HTTP " + status + ")");
            return;
        }

        parseStreamResponse(conn, emitter);
    }

    private void parseStreamResponse(HttpURLConnection conn, SseEmitter emitter) throws Exception {
        StringBuilder contentBuffer = new StringBuilder();
        List<ReviewIssue> allIssues = new ArrayList<>();
        int parseOffset = 0; // Track where we last finished parsing

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) continue;

                String data = line.substring(6).trim();
                if ("[DONE]".equals(data)) break;

                try {
                    JsonNode chunk = objectMapper.readTree(data);
                    JsonNode choices = chunk.get("choices");
                    if (choices != null && choices.isArray() && choices.size() > 0) {
                        JsonNode delta = choices.get(0).get("delta");
                        if (delta != null) {
                            JsonNode contentNode = delta.get("content");
                            if (contentNode != null && !contentNode.isNull()) {
                                String content = contentNode.asText();
                                contentBuffer.append(content);

                                // Only scan new content since last parse position
                                parseOffset = extractNewIssues(contentBuffer, parseOffset, emitter, allIssues);
                            }
                        }
                    }
                } catch (JsonProcessingException e) {
                    log.debug("Skipping non-JSON SSE line: {}", data);
                } catch (Exception e) {
                    log.warn("Error processing SSE chunk: {}", e.getMessage());
                }
            }
        }

        // Final pass: try to parse any remaining content
        extractNewIssues(contentBuffer, parseOffset, emitter, allIssues);

        int total = allIssues.size();
        int errorCount = (int) allIssues.stream().filter(i -> "error".equals(i.getSeverity())).count();
        int warningCount = (int) allIssues.stream().filter(i -> "warning".equals(i.getSeverity())).count();
        int infoCount = (int) allIssues.stream().filter(i -> "info".equals(i.getSeverity())).count();

        String summary;
        if (total == 0) {
            summary = "审查完成，未发现明显问题";
        } else {
            summary = String.format(
                "审查完成，共发现 %d 个问题（严重: %d, 建议: %d, 优化: %d）",
                total, errorCount, warningCount, infoCount
            );
        }
        sendDone(emitter, summary);
    }

    /**
     * Extract newly completed JSON objects from the buffer, starting from parseOffset.
     * Returns the new parse position (end of last successfully parsed content).
     */
    private int extractNewIssues(StringBuilder buffer, int parseOffset, SseEmitter emitter,
                                  List<ReviewIssue> allIssues) throws IOException {
        String content = buffer.toString();
        if (parseOffset >= content.length()) return parseOffset;

        // Find the JSON array wrapper, but only search from parseOffset
        int arrayStart = content.indexOf('[', parseOffset);
        // If no [ found in new content, check if there was one earlier
        if (arrayStart < 0) {
            arrayStart = content.indexOf('[');
            if (arrayStart < 0 || arrayStart >= parseOffset) return parseOffset;
        }

        int pos = Math.max(parseOffset, arrayStart + 1);
        int lastParsedEnd = parseOffset;

        while (pos < content.length()) {
            while (pos < content.length() && Character.isWhitespace(content.charAt(pos))) pos++;
            if (pos >= content.length() || content.charAt(pos) == ']') break;
            if (content.charAt(pos) == ',') { pos++; continue; }

            int objStart = content.indexOf('{', pos);
            if (objStart < 0) return lastParsedEnd;

            int objEnd = findClosingBrace(content, objStart);
            if (objEnd < 0) return lastParsedEnd; // Object not yet complete

            String objJson = content.substring(objStart, objEnd + 1);
            try {
                ReviewIssue issue = parseIssue(objectMapper.readTree(objJson));
                allIssues.add(issue);
                sendIssue(emitter, issue);
                lastParsedEnd = objEnd + 1;
            } catch (Exception e) {
                log.debug("Failed to parse issue JSON: {}", e.getMessage());
            }

            pos = objEnd + 1;
        }

        return lastParsedEnd;
    }

    private int findClosingBrace(String s, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) continue;

            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private ReviewIssue parseIssue(JsonNode node) {
        return ReviewIssue.builder()
            .severity(node.has("severity") ? node.get("severity").asText() : "info")
            .line(node.has("line") ? node.get("line").asInt() : 0)
            .title(node.has("title") ? node.get("title").asText() : "")
            .description(node.has("description") ? node.get("description").asText() : "")
            .suggestion(node.has("suggestion") ? node.get("suggestion").asText() : "")
            .codeExample(node.has("codeExample") ? node.get("codeExample").asText() : null)
            .build();
    }

    // ---- Prompt building ----

    private String buildRequestBody(String code, String mode) throws JsonProcessingException {
        String contentDescription = "code".equals(mode)
            ? "请审查以下代码："
            : "请审查以下 git diff 内容，重点关注变更引入的潜在问题：";

        String systemPrompt = """
            你是一个资深代码审查专家，请从以下维度进行分析：

            1. 潜在Bug：逻辑错误、空指针、异常未处理、边界条件遗漏
            2. 安全漏洞：注入风险、敏感信息泄露、权限校验缺失
            3. 性能问题：不必要的循环、资源未释放、低效算法
            4. 代码规范：命名不当、硬编码、重复代码、过度复杂

            ## 输出要求
            - 严格返回 JSON 数组，不要包含任何 markdown 标记或额外文字
            - 如果代码没有明显问题，返回空数组：[]
            - 每个数组元素包含以下字段：

            {
              "severity": "error|warning|info",
              "line": 行号数字,
              "title": "问题标题（简洁）",
              "description": "详细描述",
              "suggestion": "修改建议",
              "codeExample": "修复后的代码示例"
            }

            severity 说明：
            - error: 会导致程序错误或安全问题的严重缺陷
            - warning: 可能导致问题的隐患或违反最佳实践
            - info: 优化建议，代码可以工作但可改进
            """;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("stream", true);
        body.put("messages", List.of(
            Map.of("role", "system", "content", systemPrompt),
            Map.of("role", "user", "content", contentDescription + "\n\n" + code)
        ));
        body.put("temperature", 0.3);
        body.put("max_tokens", 8192);

        return objectMapper.writeValueAsString(body);
    }

    // ---- SSE output helpers ----

    private void sendIssue(SseEmitter emitter, ReviewIssue issue) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "issue");
        data.put("severity", issue.getSeverity());
        data.put("line", issue.getLine());
        data.put("title", issue.getTitle());
        data.put("description", issue.getDescription());
        data.put("suggestion", issue.getSuggestion());
        if (issue.getCodeExample() != null) {
            data.put("codeExample", issue.getCodeExample());
        }
        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(data)));
    }

    private void sendDone(SseEmitter emitter, String summary) throws IOException {
        Map<String, String> data = Map.of("type", "done", "summary", summary);
        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(data)));
    }

    private void sendError(SseEmitter emitter, String message) throws IOException {
        Map<String, String> data = Map.of("type", "error", "message", message);
        emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(data)));
    }

    private String readErrorBody(HttpURLConnection conn) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return "(unable to read error body)";
        }
    }
}
