/**
 * 用户管理控制器
 * <p>
 * 提供用户注册、登录、密码管理、账户激活等RESTful API接口
 * </p>
 *
 * @author PaperWise Team
 * @version 1.0
 * @since 2024-01-01
 */
package org.example.paperwise.Controller;

import io.github.biglv666.authkit.AuthKit;
import io.github.biglv666.authkit.model.DeviceType;
import lombok.extern.slf4j.Slf4j;
import org.example.paperwise.Dto.Result;
import org.example.paperwise.Dto.UserStatusDto;
import org.example.paperwise.Interface.RateLimit;
import org.example.paperwise.Service.UserService;
import org.example.paperwise.Until.JwtUntil;
import org.example.paperwise.entry.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器类
 * <p>
 * 处理所有与用户相关的HTTP请求，包括：
 * <ul>
 *   <li>用户登录认证</li>
 *   <li>用户注册</li>
 *   <li>账户激活</li>
 *   <li>密码更新（通过旧密码或邮箱验证码）</li>
 *   <li>获取用户信息</li>
 * </ul>
 * </p>
 *
 * @Slf4j 自动生成日志对象
 * @RestController 组合@RestController和@ResponseBody，返回JSON格式数据
 * @RequestMapping("/paperwise/user") 统一前缀路径
 */
@Slf4j
@RestController
@RequestMapping("/paperwise/user")
public class UserController {

    /** 用户服务层依赖 */
    @Autowired
    private UserService userService;

    /** JWT工具类依赖，用于生成和验证Token */
    @Autowired
    private JwtUntil jwtUntil;

    /**
     * 用户登录接口
     * <p>
     * 根据用户名和密码验证用户身份，验证成功后生成JWT令牌返回给客户端
     * </p>
     *
     * @param username 用户名
     * @param password 密码（已加密存储）
     * @return 包含Token、用户ID和用户名的结果对象
     * @apiEndpoint POST /paperwise/user/login
     * @apiParams username=String, password=String
     */
    @PostMapping("/login")
    public Result<Map<String,Object>> login(@RequestParam String username, @RequestParam String password) {
        try {
            // 调用服务层验证用户登录
            User user = userService.login(username, password);

            // auth-kit：签发不透明 token 并写入 Redis 会话（同端互斥：旧会话被顶下线）
            String Token = AuthKit.login(user.getUserid(), DeviceType.WEB);

            // 封装返回数据
            Map<String, Object> map = new HashMap<>();
            map.put("token", Token);
            map.put("userid", user.getUserid());
            map.put("username", user.getUsername());

            System.out.println(Token);
            return Result.success(map);
        } catch (Exception e) {
            log.error("用户登录失败: username={}, 错误信息: {}", username, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登出接口：销毁 auth-kit Redis 会话（原方案无此接口，JWT 无状态无法登出）
     * @apiEndpoint POST /paperwise/user/logout
     */
    @PostMapping("/logout")
    public Result<String> logout() {
        AuthKit.logout();
        return Result.success("登出成功");
    }

    /**
     * 用户注册接口
     * <p>
     * 创建新用户账号，发送激活邮件到用户邮箱
     * </p>
     *
     * @param username 用户名（唯一标识）
     * @param password 密码
     * @param email 邮箱地址（用于激活账户和找回密码）
     * @return 注册结果
     * @apiEndpoint POST /paperwise/user/register
     * @apiParams username=String, password=String, email=String
     */
    @PostMapping("/register")
    public Result<String> register(@RequestParam String username,
                                   @RequestParam String password,
                                   @RequestParam String email) {
        try {
            boolean success = userService.register(username, password, email);
            if (success) {
                log.info("用户注册成功: username={}, email={}", username, email);
                return Result.success("注册成功，请查收激活邮件");
            }
            return Result.error("注册失败，请稍后重试");
        } catch (Exception e) {
            log.error("用户注册异常: username={}, 错误: {}", username, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 账户激活接口
     * <p>
     * 验证邮箱激活码，完成账户激活流程
     * </p>
     *
     * @param email 邮箱地址
     * @param code 激活验证码
     * @return 激活结果
     * @apiEndpoint POST /paperwise/user/activation
     * @apiParams email=String, code=String
     */
    @PostMapping("/activation")
    public Result<String> activation(@RequestParam String email, @RequestParam String code) {
        try {
            boolean success = userService.activation(email, code);
            if (success) {
                log.info("账户激活成功: email={}", email);
                return Result.success("激活成功");
            }
            return Result.error("激活码错误或已过期");
        } catch (Exception e) {
            log.error("账户激活异常: email={}, 错误: {}", email, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改密码（通过旧密码验证）
     * <p>
     * 用户需要提供旧密码进行身份验证后才能修改新密码
     * </p>
     *
     * @param userid 用户ID（从Token中提取）
     * @param oldPassword 旧密码
     * @param newpassword 新密码
     * @return 修改结果
     * @apiEndpoint POST /paperwise/user/updatepasswordforpassword
     * @apiParams userid=Long, oldPassword=String, newpassword=String
     */
    @PostMapping("/updatepasswordforpassword")
    public Result<String> updatePassword(@RequestAttribute Long userid,
                                          @RequestParam String oldPassword,
                                          @RequestParam String newpassword) {
        try {
            boolean success = userService.updatePassword(userid, oldPassword, newpassword);
            if (success) {
                log.info("密码修改成功（通过旧密码验证）: userid={}", userid);
                return Result.success("密码修改成功");
            }
            return Result.error("请稍后重试");
        } catch (Exception e) {
            log.error("密码修改异常（通过旧密码验证）: userid={}, 错误: {}", userid, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 修改密码（通过邮箱验证码）
     * <p>
     * 发送验证码到用户邮箱，验证通过后修改密码
     * 该接口有60秒内只能请求1次的限流保护
     * </p>
     *
     * @param userid 用户ID（从Token中提取）
     * @param newpassword 新密码
     * @param email 邮箱地址（接收验证码）
     * @return 修改结果
     * @RateLimit 限流注解：60秒内最多1次请求
     * @apiEndpoint POST /paperwise/user/updatepasswordforemail
     * @apiParams userid=Long, newpassword=String, email=String
     */
    @PostMapping("/updatepasswordforemail")
    @RateLimit(key = "updatePasswordForEmail", WindowsSeconds = 60, MaxRequests = 1)
    public Result<String> updatePasswordForEmail(@RequestAttribute Long userid,
                                                 @RequestParam String newpassword,
                                                 @RequestParam String email) {
        try {
            boolean success = userService.updatePassword(userid, newpassword, email);
            if (success) {
                log.info("密码修改成功（通过邮箱验证）: userid={}, email={}", userid, email);
                return Result.success("密码修改成功，请查收确认邮件");
            }
            return Result.error("请检查邮箱收取验证码");
        } catch (Exception e) {
            log.error("密码修改异常（通过邮箱验证）: userid={}, 错误: {}", userid, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 验证邮箱修改密码验证码
     * <p>
     * 验证通过邮箱发送的验证码，完成密码修改的最后一步
     * </p>
     *
     * @param userid 用户ID（从Token中提取）
     * @param code 验证码
     * @return 验证结果
     * @apiEndpoint POST /paperwise/user/updatepasswordcode
     * @apiParams userid=Long, code=String
     */
    @PostMapping("/updatepasswordcode")
    public Result<String> updatePasswordCode(@RequestAttribute Long userid,
                                              @RequestParam String code) {
        try {
            boolean success = userService.updatePasswordEmail(userid, code);
            if (success) {
                log.info("邮箱验证码验证成功: userid={}", userid);
                return Result.success("验证成功");
            }
            return Result.error("验证码错误或已过期");
        } catch (Exception e) {
            log.error("邮箱验证码验证异常: userid={}, 错误: {}", userid, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取当前用户信息
     * <p>
     * 根据Token中包含的用户ID查询并返回用户详细信息
     * </p>
     *
     * @param userid 用户ID（从Token中提取）
     * @return 用户状态信息
     * @apiEndpoint POST /paperwise/user/getuser
     * @apiParams userid=Long
     */
    @PostMapping("/getuser")
    public Result<UserStatusDto> getUser(@RequestAttribute Long userid) {
        try {
            return Result.success(userService.getUserById(userid));
        } catch (Exception e) {
            log.error("获取用户信息异常: userid={}, 错误: {}", userid, e.getMessage(), e);
            return Result.error(e.getMessage());
        }
    }
}
