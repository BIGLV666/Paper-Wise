package org.example.paperwise.entry;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FavoritesLikeRecord {
    Long likeId;
    Long userId;
    Long favoritesId;

    public FavoritesLikeRecord( Long userId, Long favoritesId) {
    this.userId = userId;
    this.favoritesId = favoritesId;
}}