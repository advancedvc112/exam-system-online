package com.exam.userService.dto;

import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequestDTO {
    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}

