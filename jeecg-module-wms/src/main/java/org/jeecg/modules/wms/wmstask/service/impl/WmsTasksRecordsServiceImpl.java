package org.jeecg.modules.wms.wmstask.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.wms.wmstask.entity.WmsTasksRecords;
import org.jeecg.modules.wms.wmstask.mapper.WmsTasksRecordsMapper;
import org.jeecg.modules.wms.wmstask.service.IWmsTasksRecordsService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * @Description: 任务执行记录表
 * @Author: jeecg-boot
 * @Date:   2026-04-28
 * @Version: V1.0
 */
@Service
public class WmsTasksRecordsServiceImpl extends ServiceImpl<WmsTasksRecordsMapper, WmsTasksRecords> implements IWmsTasksRecordsService {

    @Override
    public IPage<WmsTasksRecords> pageList(WmsTasksRecords wmsTasksRecords,
                                           Integer pageNo,
                                           Integer pageSize) {
        Page<WmsTasksRecords> page = new Page<>(pageNo, pageSize);

        QueryWrapper<WmsTasksRecords> queryWrapper = new QueryWrapper<>();

        if (wmsTasksRecords != null) {
            if (wmsTasksRecords.getTaskType() != null && !"".equals(wmsTasksRecords.getTaskType())) {
                queryWrapper.eq("r.task_type", wmsTasksRecords.getTaskType());
            }

            if (wmsTasksRecords.getTaskNumber() != null && !"".equals(wmsTasksRecords.getTaskNumber())) {
                queryWrapper.eq("r.task_number", wmsTasksRecords.getTaskNumber());
            }

            if (wmsTasksRecords.getTaskId() != null && !"".equals(wmsTasksRecords.getTaskId())) {
                queryWrapper.eq("r.task_id", wmsTasksRecords.getTaskId());
            }

            if (wmsTasksRecords.getOperator() != null && !"".equals(wmsTasksRecords.getOperator())) {
                queryWrapper.eq("r.operator", wmsTasksRecords.getOperator());
            }
        }

        queryWrapper.orderByDesc("r.create_time");

        return baseMapper.queryPageList(page, queryWrapper);
    }
}
