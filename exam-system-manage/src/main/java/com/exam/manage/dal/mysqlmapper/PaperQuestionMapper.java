package com.exam.manage.dal.mysqlmapper;

import com.exam.manage.dal.dataobject.PaperQuestionDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 试卷题目关联Mapper接口
 */
@Mapper
public interface PaperQuestionMapper {

    /**
     * 插入试卷题目关联
     */
    @Insert("INSERT INTO paper_question (paper_id, question_id, order_num, score) " +
            "VALUES (#{paperId}, #{questionId}, #{orderNum}, #{score})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PaperQuestionDO paperQuestion);

    /**
     * 批量插入试卷题目关联
     */
    @Insert("<script>" +
            "INSERT INTO paper_question (paper_id, question_id, order_num, score) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.paperId}, #{item.questionId}, #{item.orderNum}, #{item.score})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<PaperQuestionDO> paperQuestionList);

    /**
     * 根据试卷ID查询所有题目关联
     */
    @Select("SELECT id, paper_id, question_id, order_num, score " +
            "FROM paper_question WHERE paper_id = #{paperId} ORDER BY order_num")
    List<PaperQuestionDO> selectByPaperId(@Param("paperId") Long paperId);

    /**
     * 删除试卷的所有题目关联
     */
    @Delete("DELETE FROM paper_question WHERE paper_id = #{paperId}")
    int deleteByPaperId(@Param("paperId") Long paperId);

    /**
     * 根据试卷ID和题目ID查询
     */
    @Select("SELECT id, paper_id, question_id, order_num, score " +
            "FROM paper_question WHERE paper_id = #{paperId} AND question_id = #{questionId}")
    PaperQuestionDO selectByPaperIdAndQuestionId(@Param("paperId") Long paperId, @Param("questionId") Long questionId);
}

