package org.jeecg.modules.wms.inventory.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.inventory.entity.WmsInventory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * @Description: 库存表
 * @Author: jeecg-boot
 * @Date:   2026-05-06
 * @Version: V1.0
 */
public interface WmsInventoryMapper extends BaseMapper<WmsInventory> {

    IPage<WmsInventory> queryPageList(
            Page<WmsInventory> page,
            @Param(Constants.WRAPPER) QueryWrapper<WmsInventory> queryWrapper
    );

}
