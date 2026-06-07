package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class CardInFavoritesRecord {
    @TableId(type = IdType.AUTO)
    private Long cardInFavoritesRecordId;
    private Long cardId;
    private Long favoritesId;

    public CardInFavoritesRecord( Long cardId, Long favoritesId) {
        this.cardId = cardId;
        this.favoritesId = favoritesId;
    }
}
