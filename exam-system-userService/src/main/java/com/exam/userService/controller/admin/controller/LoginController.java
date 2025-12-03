package com.exam.userService.controller.admin.controller;

import com.exam.userService.dto.LoginRequestDTO;
import com.exam.userService.dto.LoginResponseDTO;
import com.exam.userService.dto.RegisterRequestDTO;
import com.exam.userService.dto.Result;
import com.exam.userService.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 登录控制器
 */
@RestController
@RequestMapping("/exam-online/auth")
public class LoginController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest) {
        LoginResponseDTO response = userService.login(loginRequest);
        return Result.success("登录成功", response);
    }

    /**
     * 用户注册（默认学生角色）
     * @param registerRequest 注册请求
     * @return 注册后直接返回登录信息
     */
    @PostMapping("/register")
    public Result<LoginResponseDTO> register(@RequestBody RegisterRequestDTO registerRequest) {
        LoginResponseDTO response = userService.register(registerRequest);
        return Result.success("注册成功，已默认为学生身份", response);
    }
}

