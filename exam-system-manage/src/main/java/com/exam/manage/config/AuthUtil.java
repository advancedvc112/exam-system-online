package com.exam.manage.config;

import com.exam.userService.config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 简单权限工具类：用于管理端接口的角色校验
 */
@Component("manageAuthUtil")
public class AuthUtil {

    @Autowired
    private JwtUtil jwtUtil;

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或令牌缺失");
        }
        return authHeader.substring(7);
    }

    /**
     * 校验当前登录用户是否为管理员或教师
     */
    public void checkAdminOrTeacher(HttpServletRequest request) {
        String token = extractToken(request);
        if (!jwtUtil.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "令牌无效或已过期");
        }
        String role = jwtUtil.getRoleFromToken(token);
        if (!"admin".equals(role) && !"teacher".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权限执行该操作");
        }
    }
}


