package org.example.paperwise.entry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
public class User {
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userid;
    private String username;
    private String password;
    private String email;
    private LocalDateTime createTime;
    public User(){}
}
