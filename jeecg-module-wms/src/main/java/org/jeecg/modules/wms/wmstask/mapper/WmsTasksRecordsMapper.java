package org.jeecg.modules.wms.wmstask.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.wmstask.entity.WmsTasksRecords;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @Description: 任务执行记录表
 * @Author: jeecg-boot
 * @Date:   2026-04-28
 * @Version: V1.0
 */
public interface WmsTasksRecordsMapper extends BaseMapper<WmsTasksRecords> {

    IPage<WmsTasksRecords> queryPageList(
            Page<WmsTasksRecords> page,
            @Param(Constants.WRAPPER) QueryWrapper<WmsTasksRecords> queryWrapper
    );
}
