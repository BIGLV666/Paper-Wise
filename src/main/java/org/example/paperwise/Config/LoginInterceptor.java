package org.example.paperwise.Config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.paperwise.Until.JwtUntil;
import org.example.paperwise.Until.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUntil jwtUntil;
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
        Long userid=jwtUntil.getUserIdFromToken(token);
        request.setAttribute("userid",userid);
        UserContext.setUserId(userid);
        return true;
    }
    @Override
    public void afterCompletion( HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
