package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Share {
    @TableId
    private String uuid;
    private Long userid;
    private Long favoriteId;
    private String favoritesName;
    private LocalDateTime expireTime;
    public Share() {}
    public Share(String uuid,Long userid, Long favoriteId,String favoritesName,  LocalDateTime expireTime) {
        this.uuid = uuid;
        this.userid = userid;
        this.favoritesName = favoritesName;
        this.expireTime = expireTime;
        this.favoriteId = favoriteId;
    }
    public Share(String uuid,Long userid, String favoritesName,Long favoriteId){
        this.uuid = uuid;
        this.userid = userid;
        this.favoritesName = favoritesName;
        this.expireTime = LocalDateTime.now().plusDays(7);
        this.favoriteId = favoriteId;
    }
}
