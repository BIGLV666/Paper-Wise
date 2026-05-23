CREATE TABLE user(
    userid BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE ,
    password VARCHAR(100) NOT NULL ,
    email VARCHAR(100)NOT NULL ,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP
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
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    next_review_date DATE,
    question_type VARCHAR(50)NOT NULL ,
    FOREIGN KEY (userid)REFERENCES user(userid)
);