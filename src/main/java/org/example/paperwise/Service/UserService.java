package org.example.paperwise.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.paperwise.Dto.UserStatusDto;
import org.example.paperwise.Mapper.CardMapper;
import org.example.paperwise.Mapper.UserMapper;
import org.example.paperwise.Mapper.WrongReviewMapper;
import org.example.paperwise.Mapper.WrongReviewStatsMapper;
import org.example.paperwise.Until.EmailUntil;
import org.example.paperwise.Until.JwtUntil;
import org.example.paperwise.Until.PasswordEncoder;
import org.example.paperwise.entry.User;
import org.example.paperwise.entry.WrongReviewStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    @Autowired
    private JwtUntil jwtUntil;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private EmailUntil emailUntil;
    @Autowired
    private WrongReviewMapper wrongReviewMapper;
    @Autowired
    private CardMapper cardMapper;
    @Autowired
    private WrongReviewStatsMapper wrongReviewStatsMapper;

    private static final String USER_REGISTER_KEY="user_register";
    private static final String USER_UPDATE_PASSWORD_EMAIL_KEY="user_update_password_email";

    private String getRegisterCode(){
        Random random = new Random();
        return String.format("%06d", random.nextInt(10000000));
    }






    public User login(String username, String password) {
        if(username == null || password == null) {
            throw new RuntimeException("账户或密码不为空");
        }
        User user=userMapper.getUserByUserName(username);

        if(user==null) {
            throw new RuntimeException("not find user");
        }
        if(PasswordEncoder.matches(password,user.getPassword())) {
            System.out.println(user);
            return user;
        }
        throw new RuntimeException("password error");
    }

    public boolean register(String username, String password,String email) {
        if(username == null || password == null||email == null) {
            throw new RuntimeException("信息不能为空");
        }
        User user=userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if(user!=null){
            throw new RuntimeException("username exist");
        }
        user=new User();
        user.setUsername(username);
        user.setPassword(PasswordEncoder.encode(password));
        user.setEmail(email);
        Map<String,Object> map=new HashMap<>();
        String code=getRegisterCode();
        map.put("code",code);
        map.put("user",user);
        redisTemplate.opsForValue().set(USER_REGISTER_KEY+"--"+"email"+email,map,5, TimeUnit.MINUTES);
        String context="您的验证码为"+code+"有效期5分钟";
        emailUntil.sendActivationEmail(email,"邮箱验证码",context);
        return true;

    }
    public boolean activation(String email,String code){
        Map map= (Map) redisTemplate.opsForValue().get(USER_REGISTER_KEY+"--"+"email"+email);
        if(map==null) {
            throw new RuntimeException("not find user");
        }
        if(map.get("code").equals(code)) {
            User user=(User) map.get("user");
            userMapper.addUser(user);
            redisTemplate.delete(USER_REGISTER_KEY+"--"+"email"+email);
            return true;
        }
        return false;
    }
    // 旧密码修改
    public boolean updatePassword(Long userid, String password,String newPassword) {
        if(userid == null || password == null||newPassword == null) {
            throw new RuntimeException("password or new password error");
        }
        User user=userMapper.getUserById(userid);
        if(user==null) {
            throw new RuntimeException("not find user");
        }
        if(!PasswordEncoder.matches(password,user.getPassword())) {
            throw new RuntimeException("password error");
        }
        user.setPassword(newPassword);
        int r= userMapper.updateById(user);
        return r == 1;
    }
    //email 找回密码
    public  boolean updatePassword(Long userid, String email) {
        if(userid == null || email== null) {
            throw new RuntimeException("userid or email error");
        }
        User user=userMapper.getUserById(userid);
        if(user==null) {
            throw new RuntimeException("not find user");
        }
        if(!user.getEmail().equals(email)) {
            throw new RuntimeException("email error");
        }

        user.setPassword(PasswordEncoder.encode(email));
        String code=getRegisterCode();
        Map<String,Object> map=new HashMap<>();
        map.put("user",user);
        map.put("code",code);
        redisTemplate.opsForValue().set(USER_UPDATE_PASSWORD_EMAIL_KEY+"--"+"userid"+userid,map,5, TimeUnit.MINUTES);

        String context="您的验证码为"+code+"有效期5分钟";
        emailUntil.sendActivationEmail(email,"邮箱验证码",context);
        return true;
    }
    @SuppressWarnings("unchecked")
    public  boolean updatePasswordEmail( Long userid,String code) {
        if( userid == null) {
            throw new RuntimeException("email error");
        }
        Map<String,Object> map=(Map<String, Object>) redisTemplate.opsForValue().get(USER_UPDATE_PASSWORD_EMAIL_KEY+"--"+"userid"+userid);
        if(map==null) {
            throw new RuntimeException("not find user");
        }
        if(!map.get("code").equals(code)) {
            throw new RuntimeException("code error");
        }
        User user=(User) map.get("user");
        int r= userMapper.updateById(user);
        return r == 1;
    }
    public UserStatusDto getUserById(Long userid) {
        User user=userMapper.getUserById(userid);
        if(user==null) {
            throw new RuntimeException("用户不存在");
        }
        Long masterCount=wrongReviewMapper.getMasteredCount(userid);
        QueryWrapper<WrongReviewStats> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("user_id",userid).eq("day", LocalDate.now());
        WrongReviewStats wrongReviewStats=wrongReviewStatsMapper.selectOne(queryWrapper);
        Long ReviewCount;
        if(wrongReviewStats == null) {
            ReviewCount=0L;
        }
        else {
            ReviewCount=wrongReviewStats.getTotalReviewCount();
        }
        Long CardCount=cardMapper.getCardCount(userid);
        UserStatusDto userStatusDto=new UserStatusDto();
        userStatusDto.setCardCount(CardCount);
        userStatusDto.setMasteredCount(masterCount);
        userStatusDto.setUserid(userid);
        userStatusDto.setUsername(user.getUsername());
        userStatusDto.setEmail(user.getEmail());
        userStatusDto.setReviewCount(ReviewCount);
        return userStatusDto;
    }

}
