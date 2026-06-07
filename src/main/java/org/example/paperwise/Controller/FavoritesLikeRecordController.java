package org.example.paperwise.Controller;


import io.swagger.v3.oas.annotations.Operation;
import org.example.paperwise.Dto.LikeFavoritesDto;
import org.example.paperwise.Dto.Result;
import org.example.paperwise.Service.FavoritesRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/paperwise/favoriteslikerecord")
public class FavoritesLikeRecordController {
    @Autowired
    private FavoritesRecordService favoritesRecordService;

    /**
     * @param userid 自动传入
     * @return 返回Dto列表
     */
    @Operation(summary = "获得用户所有的喜欢列表，需要在点击展开后调用查收藏夹接口")
    @GetMapping("/getalllikefavoritesdto")
    public Result<List<LikeFavoritesDto>> getAllLikeFavoritesDto(@RequestAttribute Long userid) {
        return Result.success(favoritesRecordService.getAllLikeFavoritesDto(userid));
    }
    //取消点赞
    @Operation(summary = "取消点赞，需要favoritesId")
    @PostMapping("/deletelike")
    public Result<String> deleteLike(@RequestAttribute Long userid, @RequestParam Long favoritesId) {
        favoritesRecordService.deleteLike(userid, favoritesId);
         return Result.success("取消成功");

    }

    /**
     * 给收藏夹点赞
     * 先判断是否点过赞
     * @param userid Long
     * @param favoritesId Long 收藏夹Id
     * @return 点赞成功
     */
    @PostMapping("/uplikecount")
    public Result<String>upLikeCount(@RequestParam Long favoritesId,@RequestAttribute Long userid) {
        favoritesRecordService.upLikeCount(userid,favoritesId);
        return Result.success("点赞成功");
    }
    //检查点赞状态
    @GetMapping("/checklikestatus")
    public Result<Boolean>checkLikeStatus(@RequestParam Long favoritesId,@RequestAttribute Long userid) {
        boolean checkLikeStatus = favoritesRecordService.checkLikeStatus(userid,favoritesId);
            System.out.println(checkLikeStatus);
            return Result.success(!checkLikeStatus);

    }

}
