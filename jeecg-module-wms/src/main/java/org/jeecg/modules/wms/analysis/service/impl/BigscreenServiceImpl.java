package org.jeecg.modules.wms.analysis.service.impl;

import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.DateUtils;
import org.jeecg.modules.wms.analysis.mapper.BigscreenMapper;
import org.jeecg.modules.wms.analysis.service.BigscreenService;
import org.jeecg.modules.wms.analysis.vo.MonthlyTrend;
import org.jeecg.modules.wms.analysis.vo.OwnerShipmentRanking;
import org.jeecg.modules.wms.analysis.vo.TodoTask;
import org.jeecg.modules.wms.config.WarehouseDictEnum;
import org.jeecg.modules.wms.wmstask.entity.WmsTasks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @description 首页大屏接口实现类
 */
@Service
@Slf4j
public class BigscreenServiceImpl implements BigscreenService {

    @Autowired
    private BigscreenMapper bigscreenMapper;


    @Override
    @Cacheable(value = "sys:cache:bigscreen:todotasklist", key = "#warehouseId")
    public List<TodoTask> findTodoTaskList(String warehouseId) {
        //所有待收货任务数量
        int totalReceivingTaskCount = 0;
        //所以待上架任务数量
        int totalPutawayTaskCount = 0;
        //所有待拣货任务数量
        int totalPickingTaskCount = 0;


        //已完成收货任务数量
        int completedReceivingTaskCount = 0;
        //已完成上架任务数量
        int completedPutawayTaskCount = 0;
        //已完成拣货任务数量
        int completedPickingTaskCount = 0;


        //当前时间(年-月-日)
        String date = DateUtils.formatDate(new Date(), "yyyy-MM-dd");
        WmsTasks wmsTasks = new WmsTasks();
        //仓库id
        wmsTasks.setTargetWarehouseId(warehouseId);

        List<WmsTasks> allTodoTaskList = bigscreenMapper.countTaskList(wmsTasks);
        for (WmsTasks task : allTodoTaskList) {
            switch (task.getTaskType()) {
                case "RECEIVING_TASK":
                    totalReceivingTaskCount = task.getTaskCount();
                    break;
                case "PUTAWAY_TASK":
                    totalPutawayTaskCount = task.getTaskCount();
                    break;
                case "PICKING_TASK":
                    totalPickingTaskCount = task.getTaskCount();
                    break;
            }
        }
        wmsTasks.setTaskStatus(WarehouseDictEnum.TASK_STATUS_COMPLETED.getCode());
        //统计已完成的任务
        List<WmsTasks> completedTodoTaskList = bigscreenMapper.countTaskList(wmsTasks);
        for (WmsTasks task : completedTodoTaskList) {
            switch (task.getTaskType()) {
                case "RECEIVING_TASK":
                    completedReceivingTaskCount = task.getTaskCount();
                    break;
                case "PUTAWAY_TASK":
                    completedPutawayTaskCount = task.getTaskCount();
                    break;
                case "PICKING_TASK":
                    completedPickingTaskCount = task.getTaskCount();
                    break;
            }
        }
        //待收货任务数量
        int todoReceivingTaskCount = totalReceivingTaskCount - completedReceivingTaskCount;
        //待上架任务数量
        int todoPutawayTaskCount = totalPutawayTaskCount - completedPutawayTaskCount;
        //待拣货任务数量
        int todoPickingTaskCount = totalPickingTaskCount - completedPickingTaskCount;
        //待发货数量
        Integer todoShipmentCount = bigscreenMapper.countTodoShipment(warehouseId);
        //已发货数量
        Integer shippedShipmentCount = bigscreenMapper.countShippedShipment(warehouseId);

        if (todoShipmentCount == null) {
            todoShipmentCount = 0;
        }
        if (shippedShipmentCount == null) {
            shippedShipmentCount = 0;
        }

        //构建List<TodoTask>
        List<TodoTask> todoTaskList = new ArrayList<>();
        todoTaskList.add(new TodoTask("待收货任务", "visit-count|svg", todoReceivingTaskCount, completedReceivingTaskCount, "当天"));
        todoTaskList.add(new TodoTask("待上架任务", "total-sales|svg", todoPutawayTaskCount, completedPutawayTaskCount, "当天"));
        todoTaskList.add(new TodoTask("待拣货任务", "download-count|svg", todoPickingTaskCount, completedPickingTaskCount, "当天"));
        todoTaskList.add(new TodoTask("待发货数量", "transaction|svg", todoShipmentCount, shippedShipmentCount, "当天"));

        return todoTaskList;
    }

    @Override
    @Cacheable(value = "sys:cache:bigscreen:inboundtrend", key = "#warehouseId")
    public List<MonthlyTrend> findInboundTrend(String warehouseId) {
        LocalDate now = LocalDate.now();
        // 当前年份 1 月 1 日
        LocalDate startDate = LocalDate.of(now.getYear(), 1, 1);

        // 下一年 1 月 1 日，方便 SQL 使用 < endTime
        LocalDate endDate = startDate.plusYears(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<MonthlyTrend> dbList = bigscreenMapper.countInboundTrend(
                warehouseId,
                startDate.format(formatter),
                endDate.format(formatter)
        );
        return fillMonthZero(dbList);
    }

    @Override
    @Cacheable(value = "sys:cache:bigscreen:outboundtrend", key = "#warehouseId")
    public List<MonthlyTrend> findOutboundTrend(String warehouseId) {
        LocalDate now = LocalDate.now();

        LocalDate startDate = LocalDate.of(now.getYear(), 1, 1);
        LocalDate endDate = startDate.plusYears(1);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<MonthlyTrend> dbList = bigscreenMapper.countOutboundTrend(
                warehouseId,
                startDate.format(formatter),
                endDate.format(formatter)
        );

        return fillMonthZero(dbList);
    }

    @Override
    @Cacheable(value = "sys:cache:bigscreen:ownershipmentranking", key = "#warehouseId")
    public List<OwnerShipmentRanking> findOwnerShipmentRanking(String warehouseId) {
        return bigscreenMapper.countOwnerShipmentRanking(warehouseId);
    }

    /**
     * 补齐 1-12 月，没有数据的月份补 0
     */
    public List<MonthlyTrend> fillMonthZero(List<MonthlyTrend> dbList) {
        Map<Integer, Integer> monthCountMap = dbList.stream()
                .collect(Collectors.toMap(
                        MonthlyTrend::getMonth,
                        MonthlyTrend::getCount,
                        (oldValue, newValue) -> newValue
                ));
        List<MonthlyTrend> result = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            Integer count = monthCountMap.getOrDefault(month, 0);
            result.add(new MonthlyTrend(month, count));
        }
        return result;
    }
}
