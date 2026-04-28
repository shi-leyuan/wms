package org.jeecg.modules.wms.inorder.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.wms.wmstask.entity.WmsTasks;
import org.jeecg.modules.wms.wmstask.entity.WmsTasksRecords;
import org.jeecg.modules.wms.wmstask.service.IWmsTasksRecordsService;
import org.jeecg.modules.wms.wmstask.service.IWmsTasksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;

/**
 * @Description: 收货任务接口类
 * @Author: jeecg-boot
 * @Date: 2026-04-28
 * @Version: V1.0
 */
@Tag(name = "任务表")
@RestController
@RequestMapping("/inorder/receiveTasks")
@Slf4j
public class ReceiveTasksController {
    @Autowired
    private IWmsTasksService wmsTasksService;

    @Autowired
    private IWmsTasksRecordsService wmsTasksRecordsService;

    /**
     * 待收货任务查询
     *
     * @param wmsTasks
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @Operation(summary = "待收货任务查询")
    @GetMapping(value = "/list")
    public Result<IPage<WmsTasks>> queryPageList(WmsTasks wmsTasks,
                                                 @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                 @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                 HttpServletRequest req) {
        QueryWrapper<WmsTasks> queryWrapper = QueryGenerator.initQueryWrapper(wmsTasks, req.getParameterMap());
        Page<WmsTasks> page = new Page<WmsTasks>(pageNo, pageSize);
        IPage<WmsTasks> pageList = wmsTasksService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    @Operation(summary = "收货记录查询")
    @GetMapping(value = "/records")
    public Result<IPage<WmsTasksRecords>> records(WmsTasksRecords wmsTasksRecords,
                                                  @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                  @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                  HttpServletRequest req) {
        QueryWrapper<WmsTasksRecords> queryWrapper = QueryGenerator.initQueryWrapper(wmsTasksRecords, req.getParameterMap());
        Page<WmsTasksRecords> page = new Page<WmsTasksRecords>(pageNo, pageSize);
        IPage<WmsTasksRecords> pageList = wmsTasksRecordsService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

//    /**
//     * 创建收货任务
//     * 参数： 入库单id,执行人
//     */
//    @AutoLog(value = "创建收货任务")
//    @Operation(summary="创建收货任务")
//    @RequiresPermissions("inorder:receive_task:add")
//    @PostMapping(value = "/add")
//    public Result<String> add(String orderIds, String operator){
//
//        return Result.OK("创建成功！");
//    }

    /**
     * 创建收货任务
     * @param orderIds
     * @param operator
     * @return
     */
    @AutoLog(value = "创建收货任务")
    @Operation(summary = "创建收货任务")
    @RequiresPermissions("inorder:receive_task:add")
    @PostMapping("/add")
    public Result<String> add(String orderIds, String operator) {
        if (StringUtils.isEmpty(operator) || StringUtils.isEmpty(operator)) {
            return Result.error("请选择入库单及执行人！");
        }
        String[] orderIdArray = orderIds.split(",");
        for (String orderId : orderIdArray) {
            wmsTasksService.createReceiveTask(orderId, operator);
        }
        return Result.OK("创建成功！");
    }

    /**
     * 收货
     *
     * @param wmsTasksRecords
     * @return
     */
    @AutoLog(value = "收货")
    @Operation(summary = "收货")
    @RequiresPermissions("inorder:receive_task:addRecords")
    @PostMapping(value = "/addRecords")
    public Result<String> addRecords(@RequestBody WmsTasksRecords wmsTasksRecords) {
        return Result.OK("添加成功！");
    }

    /**
     * 通过id查询
     *
     * @param id
     * @return
     */
    //@AutoLog(value = "任务表-通过id查询")
    @Operation(summary = "任务表-通过id查询")
    @GetMapping(value = "/queryById")
    public Result<WmsTasks> queryById(@RequestParam(name = "id", required = true) String id) {
        WmsTasks wmsTasks = wmsTasksService.getById(id);
        if (wmsTasks == null) {
            return Result.error("未找到对应数据");
        }
        return Result.OK(wmsTasks);
    }


}