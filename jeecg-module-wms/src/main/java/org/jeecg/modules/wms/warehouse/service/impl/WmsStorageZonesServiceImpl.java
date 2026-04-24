package org.jeecg.modules.wms.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.config.WarehouseDictEnum;
import org.jeecg.modules.wms.warehouse.entity.WmsStorageZones;
import org.jeecg.modules.wms.warehouse.entity.WmsWarehouses;
import org.jeecg.modules.wms.warehouse.mapper.WmsStorageZonesMapper;
import org.jeecg.modules.wms.warehouse.service.IWmsStorageZonesService;
import org.jeecg.modules.wms.warehouse.service.IWmsWarehousesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



@Service
public class WmsStorageZonesServiceImpl extends ServiceImpl<WmsStorageZonesMapper, WmsStorageZones> implements IWmsStorageZonesService {

    @Autowired
    private IWmsWarehousesService wmsWarehousesService;

    /**
     * 启用储区
     */
    @Override
    public void enable(String id) {
        WmsStorageZones wmsStorageZones = this.getById(id);
        if (wmsStorageZones == null) {
            throw new JeecgBootException("储区不存在");
        }

        String status = wmsStorageZones.getStatus();
        if (!WarehouseDictEnum.STATUS_CREATED.getCode().equals(status)
                && !WarehouseDictEnum.STATUS_INACTIVE.getCode().equals(status)) {
            throw new JeecgBootException("储区状态为‘创建’或‘禁用’时才可以启用");
        }

        String warehouseId = wmsStorageZones.getWarehouseId();
        if (warehouseId == null || "".equals(warehouseId.trim())) {
            throw new JeecgBootException("所属仓库不能为空");
        }

        WmsWarehouses warehouse = wmsWarehousesService.getById(warehouseId);
        if (warehouse == null) {
            throw new JeecgBootException("仓库不存在");
        }

        wmsStorageZones.setStatus(WarehouseDictEnum.STATUS_ACTIVE.getCode());
        this.updateById(wmsStorageZones);
    }

    /**
     * 禁用储区
     */
    @Override
    public void disable(String id) {
        WmsStorageZones wmsStorageZones = this.getById(id);
        if (wmsStorageZones == null) {
            throw new JeecgBootException("储区不存在");
        }

        String status = wmsStorageZones.getStatus();
        if (!WarehouseDictEnum.STATUS_ACTIVE.getCode().equals(status)) {
            throw new JeecgBootException("储区状态为‘启用’时才可以禁用");
        }

        wmsStorageZones.setStatus(WarehouseDictEnum.STATUS_INACTIVE.getCode());
        this.updateById(wmsStorageZones);
    }

    /**
     * 新增储区
     */
    @Override
    public void add(WmsStorageZones wmsStorageZones) {
        String zoneCode = wmsStorageZones.getZoneCode();
        LambdaQueryWrapper<WmsStorageZones> eq = new LambdaQueryWrapper<WmsStorageZones>()
                .eq(WmsStorageZones::getZoneCode, zoneCode);
        long count = this.count(eq);
        if (count > 0) {
            throw new JeecgBootException("储区编码已存在");
        }

        String warehouseId = wmsStorageZones.getWarehouseId();
        if (warehouseId == null || "".equals(warehouseId.trim())) {
            throw new JeecgBootException("所属仓库不能为空");
        }

        WmsWarehouses warehouse = wmsWarehousesService.getById(warehouseId);
        if (warehouse == null) {
            throw new JeecgBootException("仓库不存在");
        }

        wmsStorageZones.setStatus(WarehouseDictEnum.STATUS_CREATED.getCode());
        this.save(wmsStorageZones);
    }

    /**
     * 修改储区
     */
    @Override
    public void edit(WmsStorageZones wmsStorageZones) {
        String zoneCode = wmsStorageZones.getZoneCode();
        String id = wmsStorageZones.getId();

        LambdaQueryWrapper<WmsStorageZones> eq = new LambdaQueryWrapper<WmsStorageZones>()
                .eq(WmsStorageZones::getZoneCode, zoneCode)
                .ne(WmsStorageZones::getId, id);
        long count = this.count(eq);
        if (count > 0) {
            throw new JeecgBootException("储区编码已存在");
        }

        String warehouseId = wmsStorageZones.getWarehouseId();
        if (warehouseId == null || "".equals(warehouseId.trim())) {
            throw new JeecgBootException("所属仓库不能为空");
        }

        WmsWarehouses warehouse = wmsWarehousesService.getById(warehouseId);
        if (warehouse == null) {
            throw new JeecgBootException("仓库不存在");
        }

        this.updateById(wmsStorageZones);
    }
}