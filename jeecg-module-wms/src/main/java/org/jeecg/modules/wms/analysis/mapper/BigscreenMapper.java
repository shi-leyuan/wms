package org.jeecg.modules.wms.analysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.analysis.vo.MonthlyTrend;
import org.jeecg.modules.wms.analysis.vo.OwnerShipmentRanking;
import org.jeecg.modules.wms.wmstask.entity.WmsTasks;

import java.util.List;

/**
 * @Description: 大屏mapper
 */
public interface BigscreenMapper extends BaseMapper<WmsTasks> {

    /**
     * 统计任务
     */
    List<WmsTasks> countTaskList(WmsTasks wmsTasks);

    /**
     * 统计待发货包裹数：CREATED + PACKED
     */
    Integer countTodoShipment(String warehouseId);

    /**
     * 统计已发货包裹数：SHIPPED
     */
    Integer countShippedShipment(String warehouseId);

    /**
     * 入库趋势
     */
    List<MonthlyTrend> countInboundTrend(@Param("warehouseId") String warehouseId,
                                         @Param("startTime") String startTime,
                                         @Param("endTime") String endTime);
    /**
     * 出库趋势
     */
    List<MonthlyTrend> countOutboundTrend(@Param("warehouseId") String warehouseId,
                                          @Param("startTime") String startTime,
                                          @Param("endTime") String endTime);

    /**
     * 货主出库排行 TOP5
     */
    List<OwnerShipmentRanking> countOwnerShipmentRanking(@Param("warehouseId") String warehouseId);

}