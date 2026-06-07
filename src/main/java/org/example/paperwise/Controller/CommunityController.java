package org.example.paperwise.Controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.paperwise.Dto.Result;
import org.example.paperwise.Service.CommunityService;
import org.example.paperwise.entry.Favorites;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/paperwise/community")
public class CommunityController {
    @Autowired
    private CommunityService communityService;

    //获取前十排行
    @GetMapping("/getallfavoritestop")
    public Result<List<Favorites>> getAllFavoritesTop(){
        return Result.success(communityService.getAllFavoritesTop());
    }
    @PostMapping("/getallfavorites")
    public Result<Page<Favorites>> getAllFavorites(@RequestParam int page, @RequestParam int size, @RequestParam String category){
        return Result.success(communityService.getAllFavorites(page,size,category));
    }
}
