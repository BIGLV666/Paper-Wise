package org.example.paperwise.Task;

import lombok.extern.slf4j.Slf4j;
import org.example.paperwise.Mapper.WrongReviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WrongReviewTask {
    @Autowired
    private WrongReviewMapper wrongReviewMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final static String WRONG_REVIEW_KEY = "wrong_review_key";


}
