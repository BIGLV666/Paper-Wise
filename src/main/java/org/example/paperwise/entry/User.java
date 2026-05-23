package org.example.paperwise.entry;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Locked;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.time.LocalDateTime;


@Data
@AllArgsConstructor
public class User {
    private Long userid;
    private String username;
    private String password;
    private String email;
    private LocalDateTime createTime;
    public User(){}
}
