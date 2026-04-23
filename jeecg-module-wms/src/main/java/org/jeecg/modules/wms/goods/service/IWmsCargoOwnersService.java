package org.jeecg.modules.wms.goods.service;

import org.jeecg.modules.wms.goods.entity.WmsCargoOwners;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @Description: 货主表
 * @Author: jeecg-boot
 * @Date:   2026-04-21
 * @Version: V1.0
 */
public interface IWmsCargoOwnersService extends IService<WmsCargoOwners> {

    /**
     *   添加
     *
     * @param wmsCargoOwners
     * @return
     */
    void add(WmsCargoOwners wmsCargoOwners);


}
