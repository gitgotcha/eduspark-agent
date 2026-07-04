package com.eduspark.agent.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TaskMapper extends BaseMapper<EduTask> {

  @Select("select * from edu_task where id = #{taskId} and user_id = #{userId}")
  EduTask selectByIdAndUserId(@Param("taskId") String taskId, @Param("userId") String userId);

  @Select(
      """
      select * from edu_task
      where user_id = #{userId}
      order by created_at desc
      limit #{limit}
      """)
  List<EduTask> selectByUserIdOrderByCreatedAtDesc(
      @Param("userId") String userId, @Param("limit") int limit);
}
