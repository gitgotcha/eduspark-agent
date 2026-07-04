package com.eduspark.agent.document;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DocumentMapper extends BaseMapper<EduDocument> {

  @Update("update edu_document set task_id = #{taskId} where id = #{documentId}")
  int attachToTask(@Param("documentId") String documentId, @Param("taskId") String taskId);

  @Update(
      "update edu_document set task_id = #{taskId} where id = #{documentId} and user_id = #{userId}")
  int attachToTaskForUser(
      @Param("documentId") String documentId,
      @Param("taskId") String taskId,
      @Param("userId") String userId);

  @Select("select * from edu_document where task_id = #{taskId} order by created_at asc")
  List<EduDocument> selectByTaskId(@Param("taskId") String taskId);

  @Select("select * from edu_document where id = #{documentId} and user_id = #{userId}")
  EduDocument selectByIdAndUserId(
      @Param("documentId") String documentId, @Param("userId") String userId);

  @Select("select * from edu_document where user_id = #{userId} order by created_at desc")
  List<EduDocument> selectByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);

  @Delete("delete from edu_document where id = #{documentId} and user_id = #{userId}")
  int deleteByIdAndUserId(
      @Param("documentId") String documentId, @Param("userId") String userId);

  @Select(
      "select * from edu_document where task_id = #{taskId} and user_id = #{userId} order by created_at asc")
  List<EduDocument> selectByTaskIdAndUserId(
      @Param("taskId") String taskId, @Param("userId") String userId);
}
