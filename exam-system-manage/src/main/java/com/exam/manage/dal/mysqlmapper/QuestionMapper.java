package com.exam.manage.dal.mysqlmapper;

import com.exam.manage.dal.dataobject.QuestionDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 题目Mapper接口
 */
@Mapper
public interface QuestionMapper {

    /**
     * 插入题目
     */
    @Insert("INSERT INTO question (content, type, difficulty, options, answer, score, category, create_user_id, status, create_time, update_time) " +
            "VALUES (#{content}, #{type}, #{difficulty}, #{options}, #{answer}, #{score}, #{category}, #{createUserId}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionDO question);

    /**
     * 根据ID查询题目
     */
    @Select("SELECT id, content, type, difficulty, options, answer, score, category, create_user_id, status, create_time, update_time " +
            "FROM question WHERE id = #{id}")
    QuestionDO selectById(@Param("id") Long id);

    /**
     * 更新题目
     */
    @Update("UPDATE question SET content = #{content}, type = #{type}, difficulty = #{difficulty}, " +
            "options = #{options}, answer = #{answer}, score = #{score}, category = #{category}, " +
            "status = #{status}, update_time = NOW() WHERE id = #{id}")
    int update(QuestionDO question);

    /**
     * 删除题目（逻辑删除）
     */
    @Update("UPDATE question SET status = 0, update_time = NOW() WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    /**
     * 分页查询题目列表
     */
    @Select("<script>" +
            "SELECT id, content, type, difficulty, options, answer, score, category, create_user_id, status, create_time, update_time " +
            "FROM question WHERE status = 1 " +
            "<if test='type != null and type != \"\"'> AND type = #{type} </if>" +
            "<if test='difficulty != null and difficulty != \"\"'> AND difficulty = #{difficulty} </if>" +
            "<if test='category != null and category != \"\"'> AND category LIKE CONCAT('%', #{category}, '%') </if>" +
            "ORDER BY create_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<QuestionDO> selectList(@Param("type") String type, 
                                 @Param("difficulty") String difficulty,
                                 @Param("category") String category,
                                 @Param("offset") Integer offset,
                                 @Param("limit") Integer limit);

    /**
     * 统计题目总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM question WHERE status = 1 " +
            "<if test='type != null and type != \"\"'> AND type = #{type} </if>" +
            "<if test='difficulty != null and difficulty != \"\"'> AND difficulty = #{difficulty} </if>" +
            "<if test='category != null and category != \"\"'> AND category LIKE CONCAT('%', #{category}, '%') </if>" +
            "</script>")
    Long count(@Param("type") String type,
               @Param("difficulty") String difficulty,
               @Param("category") String category);

    /**
     * 随机查询题目（用于随机组卷）
     */
    @Select("<script>" +
            "SELECT id, content, type, difficulty, options, answer, score, category, create_user_id, status, create_time, update_time " +
            "FROM question WHERE status = 1 " +
            "<if test='type != null and type != \"\"'> AND type = #{type} </if>" +
            "<if test='difficulty != null and difficulty != \"\"'> AND difficulty = #{difficulty} </if>" +
            "<if test='category != null and category != \"\"'> AND category LIKE CONCAT('%', #{category}, '%') </if>" +
            "ORDER BY RAND() LIMIT #{limit}" +
            "</script>")
    List<QuestionDO> selectRandom(@Param("type") String type,
                                  @Param("difficulty") String difficulty,
                                  @Param("category") String category,
                                  @Param("limit") Integer limit);
}

