package com.eduspark.agent.worksheet.attempt;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WorksheetAttemptMapper extends BaseMapper<EduWorksheetAttempt> {

  @Select(
      """
      select * from edu_worksheet_attempt
      where user_id = #{userId} and worksheet_id = #{worksheetId}
      order by created_at desc
      """)
  List<EduWorksheetAttempt> selectByUserIdAndWorksheetId(String userId, String worksheetId);
}
