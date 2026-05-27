package org.example.paperwise.Controller;

import org.example.paperwise.Dto.Result;
import org.example.paperwise.Dto.ShareFavoritesDto;
import org.example.paperwise.Service.ShareService;
import org.example.paperwise.entry.Share;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/paperwise/share")
public class ShareController {
    @Autowired
    private ShareService shareService;
    @GetMapping("getallshare")
    public Result<List<Share>> getAllShare(@RequestParam int page, @RequestParam int size) {
        return Result.success(shareService.getAllShare(size,page));
    }

    //根据链接返回收藏夹
    @GetMapping("getsharefavorites")
    public Result<ShareFavoritesDto> getShareFavorites(@RequestParam String shareId) {
         ShareFavoritesDto shareFavoritesDto=shareService.getShareFavorites(shareId);
         shareService.upLookCount(shareFavoritesDto.getFavoritesId());
         return Result.success(shareFavoritesDto);
    }


}
