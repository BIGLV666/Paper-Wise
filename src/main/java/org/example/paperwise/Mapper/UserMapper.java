package org.example.paperwise.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.biglv666.cachekit.annotation.CachedQuery;
import org.apache.ibatis.annotations.Mapper;
import org.example.paperwise.entry.User;

import java.util.List;
@Mapper
public interface UserMapper extends BaseMapper<User> {
    //
    List<User> getAllUsers();

    //添加用户

    int addUser(User user);


    User getUserByUserName(String username);

    /** 用户公开信息按 ID 读取频繁（个人主页等），走三级缓存；updateById 时自动失效 */
    @CachedQuery
    User getUserById(Long userid);


}
