package com.xxl.job.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xxl.job.dao.entity.XxlJobGroup;
import org.springframework.stereotype.Repository;

@Repository
public interface XxlJobGroupMapper extends BaseMapper<XxlJobGroup> {

    /**
     * select by primary key
     * @param id primary key
     * @return object by primary key
     */
    XxlJobGroup selectByPrimaryKey(Integer id);

    /**
     * insert record to table selective
     * @param record the record
     * @return insert count
     */
    int insertSelective(XxlJobGroup record);

    /**
     * update record selective
     * @param record the updated record
     * @return update count
     */
    int updateByPrimaryKeySelective(XxlJobGroup record);

    /**
     * delete by primary key
     * @param id primaryKey
     * @return deleteCount
     */
    int deleteByPrimaryKey(Integer id);

}