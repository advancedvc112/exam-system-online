package com.exam.userService.dal.mysqlmapper;

import com.exam.userService.dal.dataobject.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT id, username, password, role, status, create_time, update_time " +
            "FROM user WHERE username = #{username}")
    UserDO selectByUsername(@Param("username") String username);

    /**
     * 根据用户ID查询用户
     * @param id 用户ID
     * @return 用户信息
     */
    @Select("SELECT id, username, password, role, status, create_time, update_time " +
            "FROM user WHERE id = #{id}")
    UserDO selectById(@Param("id") Long id);
}

