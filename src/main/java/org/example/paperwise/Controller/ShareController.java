/**
 * 分享管理控制器
 * <p>提供公开分享列表查询、分享内容获取等功能</p>
 *
 * @author PaperWise Team
 */
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

/**
 * 分享控制器
 * <p>处理分享内容的查询和浏览</p>
 */
@RestController
@RequestMapping("/paperwise/share")
public class ShareController {

    @Autowired
    private ShareService shareService;

    /**
     * 分页获取所有公开分享
     * @param page 页码
     * @param size 每页数量
     * @return 分享列表
     */
    @GetMapping("getallshare")
    public Result<List<Share>> getAllShare(@RequestParam int page, @RequestParam int size) {
        return Result.success(shareService.getAllShare(size, page));
    }

    /**
     * 根据分享ID获取收藏夹内容
     * @param shareId 分享ID
     * @return 收藏夹信息及卡片内容
     */
    @GetMapping("getsharefavorites")
    public Result<ShareFavoritesDto> getShareFavorites(@RequestParam String shareId) {
        ShareFavoritesDto shareFavoritesDto = shareService.getShareFavorites(shareId);
        shareService.upLookCount(shareFavoritesDto.getFavoritesId());
        return Result.success(shareFavoritesDto);
    }
}
