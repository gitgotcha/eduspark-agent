package com.eduspark.agent.knowledge;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KnowledgeGraphMapper extends BaseMapper<EduKnowledgeGraph> {

  @Select("select * from edu_knowledge_graph where id = #{graphId} and user_id = #{userId}")
  EduKnowledgeGraph selectByIdAndUserId(
      @Param("graphId") String graphId, @Param("userId") String userId);

  @Select("select * from edu_knowledge_graph where user_id = #{userId} order by created_at desc")
  List<EduKnowledgeGraph> selectByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);

  @Delete("delete from edu_knowledge_graph where id = #{graphId} and user_id = #{userId}")
  int deleteByIdAndUserId(@Param("graphId") String graphId, @Param("userId") String userId);
}
