package org.example.paperwise.Config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.github.biglv666.authkit.AuthKit;
import io.github.biglv666.authkit.exception.NotLoginException;
import io.github.biglv666.authkit.model.AuthSession;
import org.example.paperwise.Until.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    // 原 JwtUntil JWT 校验已由 auth-kit 会话校验替代
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");

        if (token==null||token.isEmpty()){
            response.sendError(401);
            response.getWriter().write("未登录");
            return false;
        }
        if (token.startsWith("Bearer ")){
            token = token.substring(7);
        }
        // 委托 auth-kit：会话校验 + 活跃续期；被踢/被顶/过期有专用错误语义
        AuthSession session;
        try {
            session = AuthKit.getAuthManager().checkLogin(token);
        } catch (NotLoginException e) {
            // 不能先 sendError：会提交响应导致后续 write 被吞，401 响应体变空
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(e.getMessage());
            return false;
        }
        Long userid = Long.parseLong(session.getUserId());
        request.setAttribute("userid",userid);
        UserContext.setUserId(userid);
        return true;
    }
    @Override
    public void afterCompletion( HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
