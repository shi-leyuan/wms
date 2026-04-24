package org.jeecg.modules.wms.warehouse.service;

import org.jeecg.modules.wms.warehouse.entity.WmsStorageZones;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @Description: 储区表
 * @Author: jeecg-boot
 * @Date: 2026-04-23
 * @Version: V1.0
 */
public interface IWmsStorageZonesService extends IService<WmsStorageZones> {

    /**
     * 启用储区
     *
     * @param id
     */
    void enable(String id);


    /**
     * 禁用储区
     *
     * @param id
     */
    void disable(String id);

    /**
     * 新增储区
     *
     * @param wmsStorageZones
     */
    void add(WmsStorageZones wmsStorageZones);

    /**
     * 修改储区
     *
     * @param wmsStorageZones
     */
    void edit(WmsStorageZones wmsStorageZones);
}
