package org.jeecg.modules.wms.analysis.controller;

import io.swagger.v3.oas.annotations.OpenAPI31;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.wms.analysis.service.BigscreenService;
import org.jeecg.modules.wms.analysis.vo.MonthlyTrend;
import org.jeecg.modules.wms.analysis.vo.OwnerShipmentRanking;
import org.jeecg.modules.wms.analysis.vo.TodoTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Tag(name = "首页大屏接口")
@RestController
@RequestMapping("/bigscreen")
@Slf4j
public class BigscreenController {

    @Autowired
    private BigscreenService bigscreenService;

    public BigscreenController(BigscreenService bigscreenService) {
        this.bigscreenService = bigscreenService;
    }

    @Operation(summary = "待办任务列表")
    @GetMapping(value = "/todo-tasks")
    public Result<List<TodoTask>> todoTaskList(HttpServletRequest request) {
        //模拟数据
//        List<TodoTask> todoTaskList = Arrays.asList(
//                new TodoTask("待收货任务", "visit-count|svg", 10, 5, "当天"),
//                new TodoTask("待上架任务", "total-sales|svg", 10, 5, "当天"),
//                new TodoTask("待拣货任务", "download-count|svg", 10, 5, "当天"),
//                new TodoTask("待发货任务", "download-count|svg", 10, 5, "当天")
//        );
        //当前仓库ID
        String warehouseId = request.getHeader("X-Warehouse-Id");
        List<TodoTask> todoTaskList = bigscreenService.findTodoTaskList(warehouseId);
        return Result.OK(todoTaskList);
    }

    @Operation(summary = "入库趋势")
    @GetMapping(value = "/inbound-trend")
    public Result<List<MonthlyTrend>> inboundTrend(HttpServletRequest request) {
        String warehouseId = request.getHeader("X-Warehouse-Id");
        List<MonthlyTrend> list = bigscreenService.findInboundTrend(warehouseId);
        return Result.OK(list);
    }

    @Operation(summary = "出库趋势")
    @GetMapping(value = "/outbound-trend")
    public Result<List<MonthlyTrend>> outboundTrend(HttpServletRequest request) {
        String warehouseId = request.getHeader("X-Warehouse-Id");
        List<MonthlyTrend> list = bigscreenService.findOutboundTrend(warehouseId);
        return Result.OK(list);
    }

    @Operation(summary = "货主出库排行 TOP5")
    @GetMapping(value = "/owner-shipment-ranking")
    public Result<List<OwnerShipmentRanking>> ownerShipmentRanking(HttpServletRequest request) {
        String warehouseId = request.getHeader("X-Warehouse-Id");
        List<OwnerShipmentRanking> list = bigscreenService.findOwnerShipmentRanking(warehouseId);
        return Result.OK(list);
    }
}
