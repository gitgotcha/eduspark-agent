package com.eduspark.agent.worksheet.wrong;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WrongQuestionMapper extends BaseMapper<WrongQuestion> {

  @Select(
      """
      select * from edu_wrong_question
      where user_id = #{userId} and resolved = 0
      order by created_at desc
      """)
  List<WrongQuestion> selectOpenByUserId(@Param("userId") String userId);

  @Select(
      """
      select * from edu_wrong_question
      where user_id = #{userId} and worksheet_id = #{worksheetId}
      order by created_at desc
      """)
  List<WrongQuestion> selectByWorksheetId(
      @Param("userId") String userId, @Param("worksheetId") String worksheetId);

  @Select("select * from edu_wrong_question where id = #{wrongQuestionId} and user_id = #{userId}")
  WrongQuestion selectByIdAndUserId(
      @Param("userId") String userId, @Param("wrongQuestionId") String wrongQuestionId);

  @Update(
      """
      delete from edu_wrong_question
      where id = #{wrongQuestionId} and user_id = #{userId}
      """)
  int deleteByIdAndUserId(
      @Param("userId") String userId,
      @Param("wrongQuestionId") String wrongQuestionId);

  @Update(
      """
      <script>
      update edu_wrong_question
      set retry_worksheet_id = #{retryWorksheetId}, updated_at = #{updatedAt}
      where user_id = #{userId}
        and id in
        <foreach collection="wrongQuestionIds" item="wrongQuestionId" open="(" separator="," close=")">
          #{wrongQuestionId}
        </foreach>
      </script>
      """)
  int attachRetryWorksheet(
      @Param("userId") String userId,
      @Param("wrongQuestionIds") List<String> wrongQuestionIds,
      @Param("retryWorksheetId") String retryWorksheetId,
      @Param("updatedAt") LocalDateTime updatedAt);
}
