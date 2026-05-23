package org.example.paperwise.Controller;

import org.example.paperwise.Dto.Result;
import org.example.paperwise.Service.UserService;
import org.example.paperwise.Until.JwtUntil;
import org.example.paperwise.entry.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/paperwise/user")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUntil jwtUntil;
    Logger logger = LoggerFactory.getLogger(UserController.class);
    @PostMapping("/login")
    public Result<Map<String,Object>> login(@RequestParam String username, @RequestParam String password) {
        try{
            logger.info("xiangmukaishi-----------------------");
            System.out.println("_------------------------------");
            User user = userService.login(username, password);
            logger.info("xiangmukaishi----------------------");
            String Token= jwtUntil.generateToken(user.getUserid(),username);
            Map<String,Object> map = new HashMap<>();
            map.put("token",Token);
            map.put("userid",user.getUserid());
            map.put("username",user.getUsername());
            logger.info(Token);
            System.out.println(Token);
            logger.info(map.toString());
            return Result.success(map);
        }catch (Exception e){
            e.printStackTrace();
            logger.info("异常----------------------"+e);
            return Result.error(e.getMessage());
        }
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
            return Result.error(e.getMessage());
        }
    }
}
