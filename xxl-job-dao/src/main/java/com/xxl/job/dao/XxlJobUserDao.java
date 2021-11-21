package com.xxl.job.dao;

import com.xxl.job.dao.model.XxlJobUser;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author xuxueli 2019-05-04 16:44:59
 */
@Repository
public interface XxlJobUserDao {

    List<XxlJobUser> pageList(@Param("offset") int offset,
                              @Param("pagesize") int pagesize,
                              @Param("username") String username,
                              @Param("role") int role);

    int pageListCount(@Param("offset") int offset,
                      @Param("pagesize") int pagesize,
                      @Param("username") String username,
                      @Param("role") int role);

    XxlJobUser loadByUserName(@Param("username") String username);

    int save(XxlJobUser xxlJobUser);

    int update(XxlJobUser xxlJobUser);

    int delete(@Param("id") int id);

}
