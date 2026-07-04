package com.eduspark.agent.worksheet;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface WorksheetMapper extends BaseMapper<EduWorksheet> {

  @Select("select * from edu_worksheet where id = #{worksheetId} and user_id = #{userId}")
  EduWorksheet selectByIdAndUserId(
      @Param("worksheetId") String worksheetId, @Param("userId") String userId);

  @Select(
      """
      select * from edu_worksheet
      where user_id = #{userId}
      order by created_at desc, id desc
      limit #{limit}
      """)
  List<EduWorksheet> selectByUserIdOrderByCreatedAtDesc(
      @Param("userId") String userId, @Param("limit") int limit);
}
