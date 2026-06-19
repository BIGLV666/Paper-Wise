/**
 * 社区控制器
 * <p>提供社区内容浏览、排行榜等功能</p>
 *
 * @author PaperWise Team
 */
package org.example.paperwise.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.paperwise.Dto.Result;
import org.example.paperwise.Service.CommunityService;
import org.example.paperwise.entry.Favorites;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 社区控制器
 * <p>处理社区内容浏览、热门排行等功能</p>
 */
@RestController
@RequestMapping("/paperwise/community")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    /**
     * 获取热门收藏夹TOP10
     * @return 收藏夹列表（按浏览量排序）
     */
    @GetMapping("/getallfavoritestop")
    public Result<List<Favorites>> getAllFavoritesTop() {
        return Result.success(communityService.getAllFavoritesTop());
    }

    /**
     * 分页获取社区收藏夹列表
     * @param page 页码
     * @param size 每页数量
     * @param category 分类筛选
     * @return 分页收藏夹列表
     */
    @PostMapping("/getallfavorites")
    public Result<Page<Favorites>> getAllFavorites(@RequestParam int page, @RequestParam int size, @RequestParam String category) {
        return Result.success(communityService.getAllFavorites(page, size, category));
    }
}
