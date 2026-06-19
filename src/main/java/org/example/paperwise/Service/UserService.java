/**
 * 用户服务层
 * <p>处理用户登录、注册、密码管理等核心业务逻辑</p>
 *
 * @author PaperWise Team
 */
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

/**
 * 用户服务
 * <p>提供用户认证、账户管理、密码找回等功能</p>
 */
@Service
public class UserService {

    @Autowired
    private JwtUntil jwtUntil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private EmailUntil emailUntil;

    @Autowired
    private WrongReviewMapper wrongReviewMapper;

    @Autowired
    private CardMapper cardMapper;

    @Autowired
    private WrongReviewStatsMapper wrongReviewStatsMapper;

    /** Redis注册验证码key前缀 */
    private static final String USER_REGISTER_KEY = "user_register";

    /** Redis邮箱修改密码验证码key前缀 */
    private static final String USER_UPDATE_PASSWORD_EMAIL_KEY = "user_update_password_email";

    /**
     * 生成6位随机验证码
     */
    private String getRegisterCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(10000000));
    }

    /**
     * 用户登录验证
     * @param username 用户名
     * @param password 密码
     * @return 用户对象
     */
    public User login(String username, String password) {
        if (username == null || password == null) {
            throw new RuntimeException("账户或密码不为空");
        }
        User user = userMapper.getUserByUserName(username);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (PasswordEncoder.matches(password, user.getPassword())) {
            return user;
        }
        throw new RuntimeException("密码错误");
    }

    /**
     * 用户注册
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱
     * @return 是否成功
     */
    public boolean register(String username, String password, String email) {
        if (username == null || password == null || email == null) {
            throw new RuntimeException("信息不能为空");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user != null) {
            throw new RuntimeException("用户名已存在");
        }

        user = new User();
        user.setUsername(username);
        user.setPassword(PasswordEncoder.encode(password));
        user.setEmail(email);

        Map<String, Object> map = new HashMap<>();
        String code = getRegisterCode();
        map.put("code", code);
        map.put("user", user);

        // 验证码5分钟有效期
        redisTemplate.opsForValue().set(USER_REGISTER_KEY + "--" + "email" + email, map, 5, TimeUnit.MINUTES);

        String context = "您的验证码为" + code + "，有效期5分钟";
        emailUntil.sendActivationEmail(email, "邮箱验证码", context);
        return true;
    }

    /**
     * 邮箱激活验证
     * @param email 邮箱
     * @param code 验证码
     * @return 是否成功
     */
    public boolean activation(String email, String code) {
        Map map = (Map) redisTemplate.opsForValue().get(USER_REGISTER_KEY + "--" + "email" + email);
        if (map == null) {
            throw new RuntimeException("验证码已过期");
        }
        if (map.get("code").equals(code)) {
            User user = (User) map.get("user");
            userMapper.addUser(user);
            redisTemplate.delete(USER_REGISTER_KEY + "--" + "email" + email);
            return true;
        }
        return false;
    }

    /**
     * 通过旧密码修改密码
     * @param userid 用户ID
     * @param password 旧密码
     * @param newPassword 新密码
     * @return 是否成功
     */
    public boolean updatePassword(Long userid, String password, String newPassword) {
        if (userid == null || password == null || newPassword == null) {
            throw new RuntimeException("密码不能为空");
        }
        User user = userMapper.getUserById(userid);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!PasswordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        user.setPassword(PasswordEncoder.encode(newPassword));
        int r = userMapper.updateById(user);
        return r == 1;
    }

    /**
     * 通过邮箱发送验证码
     * @param userid 用户ID
     * @param email 邮箱
     * @return 是否成功
     */
    public boolean updatePassword(Long userid, String email) {
        if (userid == null || email == null) {
            throw new RuntimeException("参数错误");
        }
        User user = userMapper.getUserById(userid);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!user.getEmail().equals(email)) {
            throw new RuntimeException("邮箱不匹配");
        }

        String code = getRegisterCode();
        Map<String, Object> map = new HashMap<>();
        map.put("user", user);
        map.put("code", code);

        // 验证码5分钟有效期
        redisTemplate.opsForValue().set(USER_UPDATE_PASSWORD_EMAIL_KEY + "--" + "userid" + userid, map, 5, TimeUnit.MINUTES);

        String context = "您的验证码为" + code + "，有效期5分钟";
        emailUntil.sendActivationEmail(email, "邮箱验证码", context);
        return true;
    }

    /**
     * 验证邮箱验证码并更新密码
     * @param userid 用户ID
     * @param code 验证码
     * @return 是否成功
     */
    @SuppressWarnings("unchecked")
    public boolean updatePasswordEmail(Long userid, String code) {
        if (userid == null) {
            throw new RuntimeException("参数错误");
        }
        Map<String, Object> map = (Map<String, Object>) redisTemplate.opsForValue()
                .get(USER_UPDATE_PASSWORD_EMAIL_KEY + "--" + "userid" + userid);
        if (map == null) {
            throw new RuntimeException("验证码已过期");
        }
        if (!map.get("code").equals(code)) {
            throw new RuntimeException("验证码错误");
        }
        User user = (User) map.get("user");
        int r = userMapper.updateById(user);
        return r == 1;
    }

    /**
     * 获取用户状态信息
     * @param userid 用户ID
     * @return 用户状态DTO
     */
    public UserStatusDto getUserById(Long userid) {
        User user = userMapper.getUserById(userid);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        Long masterCount = wrongReviewMapper.getMasteredCount(userid);

        QueryWrapper<WrongReviewStats> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userid).eq("day", LocalDate.now());
        WrongReviewStats wrongReviewStats = wrongReviewStatsMapper.selectOne(queryWrapper);

        Long reviewCount;
        if (wrongReviewStats == null) {
            reviewCount = 0L;
        } else {
            reviewCount = wrongReviewStats.getTotalReviewCount();
        }

        Long cardCount = cardMapper.getCardCount(userid);

        UserStatusDto userStatusDto = new UserStatusDto();
        userStatusDto.setCardCount(cardCount);
        userStatusDto.setMasteredCount(masterCount);
        userStatusDto.setUserid(userid);
        userStatusDto.setUsername(user.getUsername());
        userStatusDto.setEmail(user.getEmail());
        userStatusDto.setReviewCount(reviewCount);
        return userStatusDto;
    }
}
