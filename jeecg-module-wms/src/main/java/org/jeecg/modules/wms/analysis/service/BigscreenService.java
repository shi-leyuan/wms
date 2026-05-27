package org.jeecg.modules.wms.analysis.service;

import org.jeecg.modules.wms.analysis.vo.MonthlyTrend;
import org.jeecg.modules.wms.analysis.vo.OwnerShipmentRanking;
import org.jeecg.modules.wms.analysis.vo.TodoTask;

import java.util.List;

/**
 * @description 首页大屏接口
 */
public interface BigscreenService {

    /**
     * 查询待办任务
     */
    public List<TodoTask> findTodoTaskList(String warehouseId);

    /**
     * 入库趋势
     */
    public List<MonthlyTrend> findInboundTrend(String warehouseId);

    /**
     * 出库趋势
     */
    List<MonthlyTrend> findOutboundTrend(String warehouseId);

    /**
     * 货主出库排行 TOP5
     */
    List<OwnerShipmentRanking> findOwnerShipmentRanking(String warehouseId);
}
