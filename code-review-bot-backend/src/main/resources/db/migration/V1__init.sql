-- V1: Initial schema — review_task + review_issue tables

CREATE TABLE review_task (
    task_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    mode VARCHAR(16),
    code TEXT,
    error_message VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id)
);

CREATE INDEX idx_rt_user_id ON review_task(user_id);
CREATE INDEX idx_rt_code_hash ON review_task(code_hash);

CREATE TABLE review_issue (
    id BIGINT AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL,
    severity VARCHAR(16),
    line INT,
    title VARCHAR(255),
    description TEXT,
    suggestion TEXT,
    code_example TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_ri_task_id ON review_issue(task_id);
