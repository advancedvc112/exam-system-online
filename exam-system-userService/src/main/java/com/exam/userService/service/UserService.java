package com.exam.userService.service;

import com.exam.userService.config.JwtUtil;
import com.exam.userService.dal.dataobject.UserDO;
import com.exam.userService.dal.mysqlmapper.UserMapper;
import com.exam.userService.dto.LoginRequestDTO;
import com.exam.userService.dto.LoginResponseDTO;
import com.exam.userService.dto.RegisterRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

/**
 * 用户服务类
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户登录
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        // 根据用户名查询用户
        UserDO user = userMapper.selectByUsername(loginRequest.getUsername());
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 验证用户状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("用户已被禁用");
        }

        // 验证密码（这里使用MD5加密，实际项目中建议使用BCrypt）
        String encryptedPassword = DigestUtils.md5DigestAsHex(loginRequest.getPassword().getBytes());
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 生成JWT令牌
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 构建响应
        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());

        return response;
    }

    /**
     * 用户注册（指定角色：admin / teacher / student）
     * @param registerRequest 注册请求
     * @return 登录响应（注册后直接登录）
     */
    public LoginResponseDTO register(RegisterRequestDTO registerRequest) {
        if (!StringUtils.hasText(registerRequest.getUsername()) ||
                !StringUtils.hasText(registerRequest.getPassword())) {
            throw new RuntimeException("用户名和密码不能为空");
        }

        if (!StringUtils.hasText(registerRequest.getRole())) {
            throw new RuntimeException("角色不能为空");
        }

        String role = registerRequest.getRole().trim();
        if (!"admin".equals(role) && !"teacher".equals(role) && !"student".equals(role)) {
            throw new RuntimeException("非法角色类型");
        }

        String username = registerRequest.getUsername().trim();
        if (userMapper.selectByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }

        String encryptedPassword = DigestUtils.md5DigestAsHex(registerRequest.getPassword().getBytes());

        UserDO user = new UserDO();
        user.setUsername(username);
        user.setPassword(encryptedPassword);
        user.setRole(role);
        user.setStatus(1);
        userMapper.insert(user);

        LoginResponseDTO response = new LoginResponseDTO();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getRole());
        response.setToken(jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole()));
        return response;
    }

    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    public UserDO getUserById(Long userId) {
        return userMapper.selectById(userId);
    }
}

