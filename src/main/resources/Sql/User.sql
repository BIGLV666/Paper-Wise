CREATE TABLE user(
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE ,
    password VARCHAR(100) NOT NULL ,
    email VARCHAR(100)NOT NULL ,
    create_time DATETIME DEFAULT (CURRENT_TIMESTAMP)
);
create TABLE card(
    card_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL ,
    title VARCHAR(500)NOT NULL ,
    options JSON ,
    answer TEXT NOT NULL ,
    explanation TEXT,
    card_type VARCHAR(20)NOT NULL ,
    card_difficulty VARCHAR(20)NOT NULL ,
    card_mastery VARCHAR(20)NOT NULL ,
    create_time DATETIME DEFAULT (CURRENT_TIMESTAMP),
    question_type VARCHAR(50)NOT NULL ,
    INDEX idx_userid_type (user_id,question_type),
    FOREIGN KEY (user_id)REFERENCES user(user_id) ON DELETE CASCADE
);
create TABLE favorites(
    favorite_id BIGINT AUTO_INCREMENT PRIMARY KEY ,
    card_ids json,
    favorite_name VARCHAR(50)NOT NULL,
    user_id BIGINT NOT NULL ,
    like_count BIGINT NOT NULL ,
    share_id VARCHAR(50) ,
    is_public INTEGER not null ,
    create_time DATETime DEFAULT (CURRENT_TIMESTAMP),
    look_count BIGINT NOT NULL ,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
);
create TABLE share(
    uuid VARCHAR(50) PRIMARY KEY ,
    user_id BIGINT not null ,
    favorites_name VARCHAR(50) not null ,
    favorites_id BIGINT not null ,
    expire_time DATETIME not null ,
    FOREIGN KEY (user_id) REFERENCES user(user_id),
    FOREIGN KEY (favorites_id) REFERENCES favorites(favorite_id),
    INDEX idx_userid (user_id)
);
create TABLE favorites_like_record(
    like_id BIGINT AUTO_INCREMENT PRIMARY KEY ,
    user_id BIGINT not null ,
    favorites_id BIGINT not null,
    UNIQUE KEY uk_user_favorites (user_id,favorites_id),
    FOREIGN KEY (favorites_id) REFERENCES favorites(favorite_id)
);
create TABLE wrong_question
(
    wrong_question_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                 BIGINT      not null,
    card_id                 BIGINT      not null,
    wrong_count             INTEGER     not null,
    wrong_question_category VARCHAR(500),
    create_time             DATETIME DEFAULT (CURRENT_TIMESTAMP),
    UNIQUE KEY uk_user_card (user_id, card_id),
    FOREIGN KEY (card_id) REFERENCES card (card_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE
);
create TABLE wrong_review(
    wrong_review_id BIGINT AUTO_INCREMENT PRIMARY KEY ,
    card_id BIGINT not null ,
    user_id BIGINT not null ,
    wrong_question_id BIGINT not null ,
    next_review_time DATE not null ,
    last_review_time DATE not null ,
    stage INTEGER not null ,
    interval_days INTEGER not null ,
    easiness_factor DOUBLE not null default 2.5 ,
    UNIQUE KEY uk_user_card (user_id,card_id),
    INDEX idx_wrong_review (user_id,next_review_time),
    FOREIGN KEY (wrong_question_id) REFERENCES wrong_question (wrong_question_id) ON DELETE  CASCADE
);
create TABLE ai_generated_card(
    ai_generated_card_d BIGINT AUTO_INCREMENT PRIMARY Key,
    session_id VARCHAR(50) not null ,
    status VARCHAR(20) not null ,
    user_id BIGINT not null ,
    title VARCHAR(500)NOT NULL ,
    options JSON ,
    answer TEXT NOT NULL ,
    explanation TEXT,
    card_type VARCHAR(20)NOT NULL ,
    card_difficulty VARCHAR(20)NOT NULL ,
    card_mastery VARCHAR(20)NOT NULL ,
    question_type VARCHAR(50)NOT NULL ,
    name VARCHAR(50) not null ,
    create_time DATETIME DEFAULT (CURRENT_TIMESTAMP),
    INDEX idx_session(session_id),
    INDEX idx_user_id(user_id)

);
CREATE Table wrong_review_stats(
    wrong_review_stats_id BIGINT AUTO_INCREMENT PRIMARY KEY ,
    user_id BIGINT not null ,
    total BIGINT not null ,
    completed_count BIGINT not null,
    remain_count BIGINT not null ,
    day DATE DEFAULT (current_date),
    total_review_count BIGINT not null ,
    continuous_days INTEGER not null ,
    UNIQUE KEY uk_userid_day (user_id,day)

);
CREATE TABLE chat_message(
    chat_message_id BIGINT AUTO_INCREMENT PRIMARY KEY ,
    user_id BIGINT not null ,
    role VARCHAR(10) not null ,
    session_id VARCHAR(100) not null,
    content TEXT not null ,
    create_time DATETIME DEFAULT (CURRENT_TIMESTAMP),
    INDEX idx_session_id(session_id),
    INDEX idx_create_time(create_time)
);
CREATE TABLE chat_session(
    session_id VARCHAR(100) PRIMARY KEY ,
    user_id BIGINT not null ,
    title VARCHAR(100) not null ,
    create_time DATETIME DEFAULT (CURRENT_TIMESTAMP),
    update_time DATETIME DEFAULT (CURRENT_TIMESTAMP),
    INDEX idx_user_id(user_id),
    INDEX idx_update_time(update_time)
);

CREATE TABLE IF NOT EXISTS ai_provider_config(
    ai_provider_config_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    provider_name VARCHAR(50) NOT NULL DEFAULT 'OpenAI Compatible',
    base_url VARCHAR(500) NOT NULL,
    api_key TEXT NOT NULL,
    model VARCHAR(150) NOT NULL,
    temperature DOUBLE NOT NULL DEFAULT 0.7,
    max_tokens INTEGER NOT NULL DEFAULT 4096,
    enabled INTEGER NOT NULL DEFAULT 1,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS note(
    note_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content LONGTEXT NOT NULL,
    format VARCHAR(20) NOT NULL DEFAULT 'markdown',
    source_name VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_note_user_updated (user_id, updated_at),
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS note_ai_organization(
    organization_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    source_content LONGTEXT NOT NULL,
    organized_content LONGTEXT,
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_note_organization (user_id, note_id, created_at),
    FOREIGN KEY (note_id) REFERENCES note(note_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
);
CREATE TABLE card_in_favorites_record(
    card_in_favorites_record_id BIGINT AUTO_INCREMENT PRIMARY KEY ,
    card_id BIGINT not null ,
    favorites_id BIGINT NOT NULL ,
    UNIQUE key un_card_favorites(card_id,favorites_id),
    index idx_favorites_id(favorites_id)
);

CREATE TABLE IF NOT EXISTS study_set(
    study_set_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    card_ids JSON NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_study_set_user_updated (user_id, updated_at),
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
);
