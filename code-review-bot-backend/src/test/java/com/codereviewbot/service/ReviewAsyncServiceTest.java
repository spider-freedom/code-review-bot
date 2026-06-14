package com.codereviewbot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codereviewbot.entity.ReviewIssue;
import com.codereviewbot.entity.ReviewTask;
import com.codereviewbot.mapper.ReviewIssueMapper;
import com.codereviewbot.mapper.ReviewTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewAsyncService")
class ReviewAsyncServiceTest {

    @Mock private ReviewTaskMapper taskMapper;
    @Mock private ReviewIssueMapper issueMapper;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private ReviewAsyncService service;

    @BeforeEach
    void setUp() {
        service = new ReviewAsyncService(taskMapper, issueMapper, redisTemplate);
        // Inject @Value fields since we're not using Spring context
        ReflectionTestUtils.setField(service, "apiUrl", "https://api.deepseek.com/v1/chat/completions");
        ReflectionTestUtils.setField(service, "apiKey", "sk-test-key");
        ReflectionTestUtils.setField(service, "model", "deepseek-chat");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Nested
    @DisplayName("submit() — task submission")
    class Submit {

        @Test
        @DisplayName("should persist task with PENDING status and return taskId")
        void shouldPersistPendingTask() {
            // Given
            String userId = "user-1";
            String code = "public class Test {}";
            String mode = "code";

            ArgumentCaptor<ReviewTask> taskCaptor = ArgumentCaptor.forClass(ReviewTask.class);

            // When
            ReviewTask result = service.submit(userId, code, mode);

            // Then
            verify(taskMapper).insert(taskCaptor.capture());
            ReviewTask persisted = taskCaptor.getValue();
            assertThat(persisted.getUserId()).isEqualTo(userId);
            assertThat(persisted.getStatus()).isEqualTo("PENDING");
            assertThat(persisted.getMode()).isEqualTo(mode);
            assertThat(persisted.getCodeHash()).isNotNull();
            assertThat(persisted.getCreateTime()).isNotNull();

            // Returned task is the same object
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getStatus()).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("should generate deterministic MD5 hash for code dedup")
        void shouldGenerateDeterministicHash() {
            // Same code → same hash
            ReviewTask t1 = service.submit("u1", "hello world", "code");
            ReviewTask t2 = service.submit("u2", "hello world", "code");
            assertThat(t1.getCodeHash()).isEqualTo(t2.getCodeHash());

            // Different code → different hash
            ReviewTask t3 = service.submit("u1", "different", "code");
            assertThat(t1.getCodeHash()).isNotEqualTo(t3.getCodeHash());
        }

        @Test
        @DisplayName("should truncate code > 10K chars to avoid DB bloat")
        void shouldTruncateLargeCode() {
            String largeCode = "x".repeat(15_000);
            ReviewTask task = service.submit("u1", largeCode, "diff");
            assertThat(task.getCode()).hasSize(10_000);
        }
    }

    @Nested
    @DisplayName("getTask() — task query")
    class GetTask {

        @Test
        @DisplayName("should return task when found")
        void shouldReturnTaskWhenFound() {
            ReviewTask mockTask = new ReviewTask();
            mockTask.setTaskId("abc-123");
            mockTask.setStatus("COMPLETED");
            when(taskMapper.selectById("abc-123")).thenReturn(mockTask);

            ReviewTask result = service.getTask("abc-123");

            assertThat(result).isNotNull();
            assertThat(result.getTaskId()).isEqualTo("abc-123");
            assertThat(result.getStatus()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("should return null when task not found")
        void shouldReturnNullWhenNotFound() {
            when(taskMapper.selectById("nonexistent")).thenReturn(null);

            ReviewTask result = service.getTask("nonexistent");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getIssues() — paginated issue query")
    class GetIssues {

        @Test
        @DisplayName("should query with default pagination (page=1, size=50)")
        @SuppressWarnings("unchecked")
        void shouldUseDefaultPagination() {
            String taskId = "task-1";
            when(issueMapper.selectPage(any(), any())).thenReturn(new Page<>());

            service.getIssues(taskId);

            ArgumentCaptor<Page<ReviewIssue>> pageCaptor = ArgumentCaptor.forClass(Page.class);
            verify(issueMapper).selectPage(pageCaptor.capture(), any());
            assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
            assertThat(pageCaptor.getValue().getSize()).isEqualTo(50);
        }

        @Test
        @DisplayName("should query with custom pagination")
        @SuppressWarnings("unchecked")
        void shouldUseCustomPagination() {
            String taskId = "task-2";
            when(issueMapper.selectPage(any(), any())).thenReturn(new Page<>());

            service.getIssues(taskId, 2, 10);

            ArgumentCaptor<Page<ReviewIssue>> pageCaptor = ArgumentCaptor.forClass(Page.class);
            verify(issueMapper).selectPage(pageCaptor.capture(), any());
            assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(2);
            assertThat(pageCaptor.getValue().getSize()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("validateConfig() — startup safety check")
    class ConfigValidation {

        @Test
        @DisplayName("should pass when apiKey is configured")
        void shouldPassWithValidConfig() {
            // setUp already sets apiKey = "sk-test-key"
            // This should not throw
            service.validateConfig();
        }

        @Test
        @DisplayName("should throw when apiKey is null")
        void shouldThrowWhenApiKeyNull() {
            ReflectionTestUtils.setField(service, "apiKey", null);

            assertThatThrownBy(() -> service.validateConfig())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DEEPSEEK_API_KEY");
        }

        @Test
        @DisplayName("should throw when apiKey is blank")
        void shouldThrowWhenApiKeyBlank() {
            ReflectionTestUtils.setField(service, "apiKey", "   ");

            assertThatThrownBy(() -> service.validateConfig())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DEEPSEEK_API_KEY");
        }
    }
}
