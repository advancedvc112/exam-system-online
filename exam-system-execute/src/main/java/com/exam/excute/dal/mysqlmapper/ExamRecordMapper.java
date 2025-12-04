package com.exam.excute.dal.mysqlmapper;

import com.exam.excute.dal.dataobject.ExamRecordDO;
import org.apache.ibatis.annotations.*;

/**
 * 考试记录Mapper接口
 */
@Mapper
public interface ExamRecordMapper {

    /**
     * 插入考试记录
     */
    @Insert("INSERT INTO exam_record (exam_id, student_id, paper_id, start_time, status, switch_count, is_cheating, create_time, update_time) " +
            "VALUES (#{examId}, #{studentId}, #{paperId}, NOW(), #{status}, 0, 0, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ExamRecordDO examRecord);

    /**
     * 根据ID查询考试记录
     */
    @Select("SELECT id, exam_id, student_id, paper_id, start_time, submit_time, total_score, score, status, switch_count, is_cheating, cheating_reason, create_time, update_time " +
            "FROM exam_record WHERE id = #{id}")
    ExamRecordDO selectById(@Param("id") Long id);

    /**
     * 根据考试ID和学生ID查询考试记录
     */
    @Select("SELECT id, exam_id, student_id, paper_id, start_time, submit_time, total_score, score, status, switch_count, is_cheating, cheating_reason, create_time, update_time " +
            "FROM exam_record WHERE exam_id = #{examId} AND student_id = #{studentId}")
    ExamRecordDO selectByExamIdAndStudentId(@Param("examId") Long examId, @Param("studentId") Long studentId);

    /**
     * 更新考试记录
     */
    @Update("UPDATE exam_record SET submit_time = #{submitTime}, total_score = #{totalScore}, score = #{score}, " +
            "status = #{status}, switch_count = #{switchCount}, is_cheating = #{isCheating}, " +
            "cheating_reason = #{cheatingReason}, update_time = NOW() WHERE id = #{id}")
    int update(ExamRecordDO examRecord);

    /**
     * 更新切屏次数
     */
    @Update("UPDATE exam_record SET switch_count = switch_count + 1, update_time = NOW() WHERE id = #{id}")
    int incrementSwitchCount(@Param("id") Long id);

    /**
     * 标记作弊
     */
    @Update("UPDATE exam_record SET is_cheating = 1, cheating_reason = #{reason}, update_time = NOW() WHERE id = #{id}")
    int markCheating(@Param("id") Long id, @Param("reason") String reason);

    /**
     * 根据考试ID查询所有进行中的考试记录
     */
    @Select("SELECT id, exam_id, student_id, paper_id, start_time, submit_time, total_score, score, status, switch_count, is_cheating, cheating_reason, create_time, update_time " +
            "FROM exam_record WHERE exam_id = #{examId} AND status = 'in_progress'")
    java.util.List<ExamRecordDO> selectInProgressByExamId(@Param("examId") Long examId);
}

