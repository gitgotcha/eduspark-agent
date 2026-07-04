package com.eduspark.agent.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Select;

public interface TaskArtifactMapper extends BaseMapper<EduTaskArtifact> {

  @Select("select * from edu_task_artifact where task_id = #{taskId} order by created_at asc")
  List<EduTaskArtifact> selectByTaskId(String taskId);
}
