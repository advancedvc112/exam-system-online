package com.exam.excute.dal.mysqlmapper;

import com.exam.excute.dal.dataobject.AnswerRecordDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 答题记录Mapper接口
 */
@Mapper
public interface AnswerRecordMapper {

    /**
     * 插入答题记录
     */
    @Insert("INSERT INTO answer_record (exam_record_id, question_id, student_answer, is_correct, score, answer_time, create_time, update_time) " +
            "VALUES (#{examRecordId}, #{questionId}, #{studentAnswer}, #{isCorrect}, #{score}, #{answerTime}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AnswerRecordDO answerRecord);

    /**
     * 更新答题记录
     */
    @Update("UPDATE answer_record SET student_answer = #{studentAnswer}, is_correct = #{isCorrect}, " +
            "score = #{score}, answer_time = #{answerTime}, update_time = NOW() " +
            "WHERE exam_record_id = #{examRecordId} AND question_id = #{questionId}")
    int update(AnswerRecordDO answerRecord);

    /**
     * 根据考试记录ID和题目ID查询
     */
    @Select("SELECT id, exam_record_id, question_id, student_answer, is_correct, score, answer_time, create_time, update_time " +
            "FROM answer_record WHERE exam_record_id = #{examRecordId} AND question_id = #{questionId}")
    AnswerRecordDO selectByExamRecordIdAndQuestionId(@Param("examRecordId") Long examRecordId, @Param("questionId") Long questionId);

    /**
     * 根据考试记录ID查询所有答题记录
     */
    @Select("SELECT id, exam_record_id, question_id, student_answer, is_correct, score, answer_time, create_time, update_time " +
            "FROM answer_record WHERE exam_record_id = #{examRecordId} ORDER BY question_id")
    List<AnswerRecordDO> selectByExamRecordId(@Param("examRecordId") Long examRecordId);

    /**
     * 批量插入答题记录
     */
    @Insert("<script>" +
            "INSERT INTO answer_record (exam_record_id, question_id, student_answer, is_correct, score, answer_time, create_time, update_time) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.examRecordId}, #{item.questionId}, #{item.studentAnswer}, #{item.isCorrect}, #{item.score}, #{item.answerTime}, NOW(), NOW())" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<AnswerRecordDO> answerRecordList);
}

