package org.jeecg.modules.wms.inorder.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.wms.inorder.entity.WmsStockInOrders;

/**
 * @Description: 入库单主表
 * @Author: jeecg-boot
 * @Date: 2026-04-27
 * @Version: V1.0
 */
public interface WmsStockInOrdersMapper extends BaseMapper<WmsStockInOrders> {

    IPage<WmsStockInOrders> queryPageList(
            Page<WmsStockInOrders> page,
            @Param(Constants.WRAPPER) QueryWrapper<WmsStockInOrders> queryWrapper
    );

}