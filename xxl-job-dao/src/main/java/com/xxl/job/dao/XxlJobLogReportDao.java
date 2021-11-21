package com.xxl.job.dao;

import com.xxl.job.dao.model.XxlJobLogReport;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * job log
 *
 * @author xuxueli 2019-11-22
 */
@Repository
public interface XxlJobLogReportDao {

    int save(XxlJobLogReport xxlJobLogReport);

    int update(XxlJobLogReport xxlJobLogReport);

    List<XxlJobLogReport> queryLogReport(@Param("triggerDayFrom") Date triggerDayFrom,
                                                @Param("triggerDayTo") Date triggerDayTo);

    XxlJobLogReport queryLogReportTotal();

}
