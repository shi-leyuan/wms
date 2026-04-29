package org.jeecg.modules.wms.wmstask.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.wms.wmstask.entity.WmsTasks;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @Description: 任务表
 * @Author: jeecg-boot
 * @Date: 2026-04-28
 * @Version: V1.0
 */
public interface IWmsTasksService extends IService<WmsTasks> {

    /**
     * 创建收货任务
     *
     * @param
     * @param
     * @return
     */
    void createReceiveTask(String orderId, String operator);

    /**
     * 查询待办理任务列表
     * @param wmsTasks
     * @return
     */
    IPage<WmsTasks> list(WmsTasks wmsTasks, Integer pageNo, Integer pageSize);
}
