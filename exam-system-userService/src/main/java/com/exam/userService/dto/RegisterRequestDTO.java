package com.exam.userService.dto;

import lombok.Data;

/**
 * 注册请求DTO
 */
@Data
public class RegisterRequestDTO {
    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 角色：admin / teacher / student
     */
    private String role;
}


