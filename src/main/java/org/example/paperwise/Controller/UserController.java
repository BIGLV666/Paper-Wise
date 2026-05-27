package org.example.paperwise.Controller;

import lombok.extern.slf4j.Slf4j;
import org.example.paperwise.Dto.Result;
import org.example.paperwise.Service.UserService;
import org.example.paperwise.Until.JwtUntil;
import org.example.paperwise.entry.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/paperwise/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUntil jwtUntil;
    @PostMapping("/login")
    public Result<Map<String,Object>> login(@RequestParam String username, @RequestParam String password) {


            User user = userService.login(username, password);
            String Token= jwtUntil.generateToken(user.getUserid(),username);
            Map<String,Object> map = new HashMap<>();
            map.put("token",Token);
            map.put("userid",user.getUserid());
            map.put("username",user.getUsername());
            System.out.println(Token);
            return Result.success(map);

    }

    @PostMapping("/register")
    public Result<String> register(@RequestParam String username,@RequestParam String password,@RequestParam String email){
        try{
            boolean f= userService.register(username,password,email);
            if(f){
                return Result.success("success");
            }
            return Result.error("error");
        }catch (Exception e){
            log.error("UserController+register{}", e.getMessage()+ Arrays.toString(e.getStackTrace()));
            return Result.error(e.getMessage());
        }
    }
    @PostMapping("/activation")
    public Result<String> activation(@RequestParam String email,@RequestParam String code){
        try{
            boolean f= userService.activation(email,code);
            if(f){
                return Result.success("success");
            }
            return Result.error("error");
        }catch (Exception e){
            log.error("UserController+activation{}", e.getMessage()+ Arrays.toString(e.getStackTrace()));
            return Result.error(e.getMessage());
        }
    }
    @PostMapping("/updatepasswordforpassword")
    public Result<String>updatePassword(@RequestAttribute Long userid, @RequestParam String oldPassword, @RequestParam String newpassword){
        try{
            boolean f= userService.updatePassword(userid,oldPassword,newpassword);
            if(f){
            return Result.success("success");
            }
            return Result.error("请稍后重试");
        }catch (Exception e){
            log.error("UserController+updatePassword{}", e.getMessage()+ Arrays.toString(e.getStackTrace()));
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("updatepasswordforemail")
    public Result<String>updatePasswordForEmail(@RequestAttribute Long userid,@RequestParam String newpassword,@RequestParam String email){
        try {
            boolean f= userService.updatePassword(userid,newpassword,email);
            if(f){
                return Result.success("success");
            }
            return Result.error("please check your email");
        }catch (Exception e){
            log.error("UserController+updatePasswordForEmail{}", e.getMessage()+ Arrays.toString(e.getStackTrace()));
            return Result.error(e.getMessage());
        }
    }
    @PostMapping("updatepasswordcode")
    public Result<String>updatePasswordCode(@RequestAttribute Long userid,@RequestParam String code){
        try {
            boolean f= userService.updatePasswordEmail(userid,code);
            if(f){
                return Result.success("success");
            }
            return Result.error("please check your code");
        }catch (Exception e){
            log.error("UserController+updatePasswordCode{}", e.getMessage()+ Arrays.toString(e.getStackTrace()));
            return Result.error(e.getMessage());
        }
    }
}
