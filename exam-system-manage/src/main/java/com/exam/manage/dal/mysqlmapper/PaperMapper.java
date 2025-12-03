package com.exam.manage.dal.mysqlmapper;

import com.exam.manage.dal.dataobject.PaperDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 试卷Mapper接口
 */
@Mapper
public interface PaperMapper {

    /**
     * 插入试卷
     */
    @Insert("INSERT INTO paper (name, description, type, total_score, duration, create_user_id, status, create_time, update_time) " +
            "VALUES (#{name}, #{description}, #{type}, #{totalScore}, #{duration}, #{createUserId}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PaperDO paper);

    /**
     * 根据ID查询试卷
     */
    @Select("SELECT id, name, description, type, total_score, duration, create_user_id, status, create_time, update_time " +
            "FROM paper WHERE id = #{id}")
    PaperDO selectById(@Param("id") Long id);

    /**
     * 更新试卷
     */
    @Update("UPDATE paper SET name = #{name}, description = #{description}, type = #{type}, " +
            "total_score = #{totalScore}, duration = #{duration}, status = #{status}, update_time = NOW() " +
            "WHERE id = #{id}")
    int update(PaperDO paper);

    /**
     * 删除试卷（逻辑删除）
     */
    @Update("UPDATE paper SET status = 0, update_time = NOW() WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    /**
     * 分页查询试卷列表
     */
    @Select("SELECT id, name, description, type, total_score, duration, create_user_id, status, create_time, update_time " +
            "FROM paper WHERE status = 1 ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<PaperDO> selectList(@Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * 统计试卷总数
     */
    @Select("SELECT COUNT(*) FROM paper WHERE status = 1")
    Long count();
}

