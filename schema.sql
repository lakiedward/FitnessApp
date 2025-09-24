
-- Performance metrics tables
CREATE TABLE IF NOT EXISTS metric_jobs (
    job_id CHAR(36) PRIMARY KEY,
    user_id INT NOT NULL,
    metric VARCHAR(32) NOT NULL,
    status ENUM('queued','running','done','error','cooldown') NOT NULL DEFAULT 'queued',
    message TEXT NULL,
    last_computed_at DATETIME NULL,
    cooldown_until DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_metric_jobs_user_metric (user_id, metric, created_at)
);

CREATE TABLE IF NOT EXISTS performance_metrics (
    user_id INT NOT NULL,
    metric VARCHAR(32) NOT NULL,
    value VARCHAR(32) NULL,
    unit VARCHAR(16) NULL,
    last_computed_at DATETIME NULL,
    window_days INT NULL,
    source ENUM('strava','health_connect','manual') NULL,
    status ENUM('ok','insufficient','cooldown','error') NOT NULL DEFAULT 'ok',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, metric),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS performance_metrics_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    metric VARCHAR(32) NOT NULL,
    value VARCHAR(32) NULL,
    unit VARCHAR(16) NULL,
    source ENUM('strava','health_connect','manual') NULL,
    status ENUM('ok','insufficient','cooldown','error') NOT NULL,
    window_days INT NULL,
    last_computed_at DATETIME NULL,
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_perf_history_user_metric (user_id, metric, recorded_at)
);

