CREATE TABLE user(
    userid BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE ,
    password VARCHAR(100) NOT NULL ,
    email VARCHAR(100)NOT NULL ,
    createTime DATETIME DEFAULT (CURRENT_TIMESTAMP)
);
create TABLE card(
    card_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    userid BIGINT NOT NULL ,
    title VARCHAR(500)NOT NULL ,
    options JSON ,
    answer TEXT NOT NULL ,
    explanation TEXT,
    card_type VARCHAR(20)NOT NULL ,
    card_difficulty VARCHAR(20)NOT NULL ,
    card_mastery VARCHAR(20)NOT NULL ,
    review_count INT DEFAULT 0,
    create_time DATETIME DEFAULT (CURRENT_TIMESTAMP),
    next_review_date DATE,
    question_type VARCHAR(50)NOT NULL ,
    FOREIGN KEY (userid)REFERENCES user(userid) ON DELETE CASCADE
);
create TABLE favorites(
    favorite_id BIGINT AUTO_INCREMENT PRIMARY KEY ,
    card_id json,
    favorite_name VARCHAR(50)NOT NULL,
    user_id BIGINT NOT NULL ,
    like_count BIGINT NOT NULL ,
    shar_id VARCHAR(50) ,
    is_public INTEGER not null ,
    create_time DATE DEFAULT (CURRENT_DATE),
    look_count BIGINT NOT NULL ,
    FOREIGN KEY (user_id) REFERENCES user(userid) ON DELETE CASCADE
);
create TABLE share(
    uuid VARCHAR(50) PRIMARY KEY ,
    userid BIGINT not null ,
    favorites_name VARCHAR(50) not null ,
    favorites_id BIGINT not null ,
    expire_time DATETIME not null ,
    FOREIGN KEY (userid) REFERENCES user(userid),
    FOREIGN KEY (favorites_id) REFERENCES favorites(favorite_id),
    INDEX idx_userid (userid)
);
create TABLE favorites_like_record(
    like_id BIGINT AUTO_INCREMENT PRIMARY KEY ,
    userid BIGINT not null ,
    favorites_id BIGINT not null,
    FOREIGN KEY (favorites_id) REFERENCES favorites(favorite_id)
);
create TABLE wrong_question(
    wrong_question_id BIGINT AUTO_INCREMENT PRIMARY KEY ,
    user_id BIGINT not null ,
    card_id BIGINT not null ,
    wrong_count INTEGER not null ,
    last_wrong_time DATETIME not null ,
    wrong_question_category VARCHAR(500),
    status VARCHAR(10) not null ,
    create_time DATETIME DEFAULT (CURRENT_TIMESTAMP),
    UNIQUE KEY uk_user_card(user_id,card_id),
    FOREIGN KEY (card_id) REFERENCES card(card_id) ON DELETE CASCADE ,
    FOREIGN KEY (user_id) REFERENCES user(userid) ON DELETE CASCADE
);