package org.example.paperwise.Dto;

import lombok.Data;

import java.util.List;

@Data
public class ShareFavoritesDto {
    private String shareId;
    private Long userId;
    private String username;
    private String favoritesName;
    private Long favoritesId;
    private List<Long>cardIds;
    private Long lookCount;
    private Long likeCount;
    private String expireTime;
}
