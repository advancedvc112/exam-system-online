package com.exam.manage.dal.mysqlmapper;

import com.exam.manage.dal.dataobject.ExamDO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试安排Mapper接口
 */
@Mapper
public interface ExamMapper {

    /**
     * 插入考试安排
     */
    @Insert("INSERT INTO exam (name, description, paper_id, start_time, end_time, duration, status, create_user_id, allow_view_answer, allow_retake, create_time, update_time) " +
            "VALUES (#{name}, #{description}, #{paperId}, #{startTime}, #{endTime}, #{duration}, #{status}, #{createUserId}, #{allowViewAnswer}, #{allowRetake}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExamDO exam);

    /**
     * 根据ID查询考试安排
     */
    @Select("SELECT id, name, description, paper_id, start_time, end_time, duration, status, create_user_id, allow_view_answer, allow_retake, create_time, update_time " +
            "FROM exam WHERE id = #{id}")
    ExamDO selectById(@Param("id") Long id);

    /**
     * 更新考试安排
     */
    @Update("UPDATE exam SET name = #{name}, description = #{description}, paper_id = #{paperId}, " +
            "start_time = #{startTime}, end_time = #{endTime}, duration = #{duration}, status = #{status}, " +
            "allow_view_answer = #{allowViewAnswer}, allow_retake = #{allowRetake}, update_time = NOW() " +
            "WHERE id = #{id}")
    int update(ExamDO exam);

    /**
     * 删除考试安排（逻辑删除，通过状态）
     */
    @Update("UPDATE exam SET status = 'cancelled', update_time = NOW() WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    /**
     * 分页查询考试安排列表
     */
    @Select("<script>" +
            "SELECT id, name, description, paper_id, start_time, end_time, duration, status, create_user_id, allow_view_answer, allow_retake, create_time, update_time " +
            "FROM exam WHERE 1=1 " +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "<if test='createUserId != null'> AND create_user_id = #{createUserId} </if>" +
            "ORDER BY start_time DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<ExamDO> selectList(@Param("status") String status,
                             @Param("createUserId") Long createUserId,
                             @Param("offset") Integer offset,
                             @Param("limit") Integer limit);

    /**
     * 统计考试安排总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM exam WHERE 1=1 " +
            "<if test='status != null and status != \"\"'> AND status = #{status} </if>" +
            "<if test='createUserId != null'> AND create_user_id = #{createUserId} </if>" +
            "</script>")
    Long count(@Param("status") String status, @Param("createUserId") Long createUserId);

    /**
     * 更新考试状态（根据时间自动更新）
     */
    @Update("UPDATE exam SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 查询需要更新状态的考试（用于定时任务）
     */
    @Select("SELECT id, name, description, paper_id, start_time, end_time, duration, status, create_user_id, allow_view_answer, allow_retake, create_time, update_time " +
            "FROM exam WHERE status IN ('not_started', 'in_progress')")
    List<ExamDO> selectExamsNeedStatusUpdate();
}

