package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FavoritesLikeRecord {
    Long likeId;
    @TableField(value = "user_id")
    Long userId;
    Long favoritesId;

    public FavoritesLikeRecord( Long userId, Long favoritesId) {
    this.userId = userId;
    this.favoritesId = favoritesId;
}}