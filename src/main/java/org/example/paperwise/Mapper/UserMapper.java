package org.example.paperwise.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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

    User getUserById(Long userid);


}
