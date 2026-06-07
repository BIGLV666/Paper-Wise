package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDate;


@Data
public class WrongReviewStats {
    @TableId(type = IdType.AUTO,value = "wrong_review_stats_id")
    private Long wrongReviewStatsId;
    @TableField(value = "user_id")
    private Long userId;
    private Long total;
    private Long completedCount;
    private Long remainCount;
    private LocalDate day;
    private Long totalReviewCount;
    private Integer continuousDays;
}
