CREATE TABLE IF NOT EXISTS review_task (
    task_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    mode VARCHAR(16),
    code CLOB,
    error_message VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_rt_user_id ON review_task(user_id);
CREATE INDEX IF NOT EXISTS idx_rt_code_hash ON review_task(code_hash);

CREATE TABLE IF NOT EXISTS review_issue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL,
    severity VARCHAR(16),
    line INT,
    title VARCHAR(255),
    description CLOB,
    suggestion CLOB,
    code_example CLOB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ri_task_id ON review_issue(task_id);
