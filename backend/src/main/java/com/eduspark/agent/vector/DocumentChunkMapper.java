package com.eduspark.agent.vector;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

  @Select(
      "select * from edu_document_chunk where document_id = #{documentId} and user_id = #{userId} order by chunk_index asc")
  List<DocumentChunk> selectByDocumentIdAndUserId(
      @Param("documentId") String documentId, @Param("userId") String userId);

  default List<DocumentChunk> selectByUserIdAndDocumentIds(String userId, List<String> documentIds) {
    if (documentIds == null || documentIds.isEmpty()) {
      return List.of();
    }
    return selectByUserIdAndDocumentIdsInternal(userId, documentIds);
  }

  @Select(
      """
      <script>
      select * from edu_document_chunk
      where user_id = #{userId}
        and document_id in
        <foreach collection="documentIds" item="documentId" open="(" separator="," close=")">
          #{documentId}
        </foreach>
      order by document_id asc, chunk_index asc
      </script>
      """)
  List<DocumentChunk> selectByUserIdAndDocumentIdsInternal(
      @Param("userId") String userId, @Param("documentIds") List<String> documentIds);
}
