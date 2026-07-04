package com.eduspark.agent.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<EduUser> {

  @Select("select * from edu_user where username = #{username}")
  EduUser selectByUsername(@Param("username") String username);
}
