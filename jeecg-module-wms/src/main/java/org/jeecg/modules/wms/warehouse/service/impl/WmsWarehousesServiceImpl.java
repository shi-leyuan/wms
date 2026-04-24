package org.jeecg.modules.wms.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.config.WarehouseDictEnum;
import org.jeecg.modules.wms.warehouse.entity.WmsWarehouses;
import org.jeecg.modules.wms.warehouse.mapper.WmsWarehousesMapper;
import org.jeecg.modules.wms.warehouse.service.IWmsWarehousesService;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @Description: 仓库表
 * @Author: jeecg-boot
 * @Date: 2026-04-23
 * @Version: V1.0
 */
@Service
public class WmsWarehousesServiceImpl extends ServiceImpl<WmsWarehousesMapper, WmsWarehouses> implements IWmsWarehousesService {

    /**
     * 新增仓库
     *
     * @param wmsWarehouses
     */
    public void add(WmsWarehouses wmsWarehouses) {
        String warehousecode = wmsWarehouses.getWarehouseCode();
        //添加时校验仓库代码是否已存在 sql select count(*) from wms_warehouses where warehouse_code = ?
        LambdaQueryWrapper<WmsWarehouses> eq = new LambdaQueryWrapper<WmsWarehouses>()
                .eq(WmsWarehouses::getWarehouseCode, warehousecode);
        long count = this.count(eq);
        if (count > 0) {
            throw new JeecgBootException("仓库代码已存在");
        }
        //初始状态为“创建”
        wmsWarehouses.setStatus(WarehouseDictEnum.STATUS_CREATED.getCode());
        this.save(wmsWarehouses);
    }

    /**
     * 修改仓库
     *
     * @param wmsWarehouses
     */
    public void edit(WmsWarehouses wmsWarehouses) {
        String warehousecode = wmsWarehouses.getWarehouseCode();
        //修改时校验仓库代码是否已存在 sql select count(*) from wms_warehouses where warehouse_code = ? and id != ?
        LambdaQueryWrapper<WmsWarehouses> eq = new LambdaQueryWrapper<WmsWarehouses>()
                .eq(WmsWarehouses::getWarehouseCode, warehousecode)
                .ne(WmsWarehouses::getId, wmsWarehouses.getId());
        long count = this.count(eq);
        if (count > 0) {
            throw new JeecgBootException("仓库代码已存在");
        }

        this.updateById(wmsWarehouses);
    }

    /**
     * 仓库启用
     *
     * @param id
     * @return
     */
    public void enable(String id) {
        //根据ID查询仓库
        WmsWarehouses wmsWarehouses = this.getById(id);
        //如果仓库不存在
        if (wmsWarehouses == null) {
            throw new JeecgBootException("仓库不存在");
        }
        //仓库状态为‘创建’或‘禁用’时才可以启用
        String status = wmsWarehouses.getStatus();
        if (!WarehouseDictEnum.STATUS_CREATED.getCode().equals(status) && !WarehouseDictEnum.STATUS_INACTIVE.getCode().equals(status)) {
            throw new JeecgBootException("仓库状态为‘创建’或‘禁用’时才可以启用");
        }
        //更新状态
        wmsWarehouses.setStatus(WarehouseDictEnum.STATUS_ACTIVE.getCode());
        this.updateById(wmsWarehouses);
    }

    /**
     * 仓库禁用
     *
     * @param id
     * @return
     */
    public void disable(String id) {
        //根据ID查询仓库
        WmsWarehouses wmsWarehouses = this.getById(id);
        //如果仓库不存在
        if (wmsWarehouses == null) {
            throw new JeecgBootException("仓库不存在");
        }
        //仓库状态为‘启用‘时才可以禁用
        String status = wmsWarehouses.getStatus();
        if (!status.equals(WarehouseDictEnum.STATUS_ACTIVE.getCode())) {
            throw new JeecgBootException("仓库状态为‘启用‘时才可以禁用");
        }
        //更新状态
        wmsWarehouses.setStatus(WarehouseDictEnum.STATUS_INACTIVE.getCode());
        this.updateById(wmsWarehouses);
    }
}
