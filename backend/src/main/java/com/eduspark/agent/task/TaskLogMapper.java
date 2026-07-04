package com.eduspark.agent.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Select;

public interface TaskLogMapper extends BaseMapper<EduTaskLog> {

  @Select("select * from edu_task_log where task_id = #{taskId} order by created_at asc")
  List<EduTaskLog> selectByTaskId(String taskId);
}
