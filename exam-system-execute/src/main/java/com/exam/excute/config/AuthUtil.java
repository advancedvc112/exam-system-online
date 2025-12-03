package com.exam.excute.config;

import com.exam.userService.config.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 考试执行模块的权限工具类：校验学生身份
 */
@Component
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
     * 校验当前登录用户是否为学生
     */
    public Long checkStudent(HttpServletRequest request) {
        String token = extractToken(request);
        if (!jwtUtil.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "令牌无效或已过期");
        }
        String role = jwtUtil.getRoleFromToken(token);
        if (!"student".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅学生可以进行考试");
        }
        return jwtUtil.getUserIdFromToken(token);
    }
}


