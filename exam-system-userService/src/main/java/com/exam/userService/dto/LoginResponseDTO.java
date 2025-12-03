package com.exam.userService.dto;

import lombok.Data;

/**
 * 登录响应DTO
 */
@Data
public class LoginResponseDTO {
    /**
     * 访问令牌
     */
    private String token;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户角色
     */
    private String role;
}

