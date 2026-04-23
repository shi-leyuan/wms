package org.jeecg.modules.wms.goods.service.impl;

import org.jeecg.common.exception.JeecgBoot401Exception;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.wms.goods.entity.WmsCargoOwners;
import org.jeecg.modules.wms.goods.mapper.WmsCargoOwnersMapper;
import org.jeecg.modules.wms.goods.service.IWmsCargoOwnersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @Description: 货主表
 * @Author: jeecg-boot
 * @Date: 2026-04-21
 * @Version: V1.0
 */
@Service
public class WmsCargoOwnersServiceImpl extends ServiceImpl<WmsCargoOwnersMapper, WmsCargoOwners> implements IWmsCargoOwnersService {

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 添加
     *
     * @param wmsCargoOwners
     * @return
     */
    public void add(WmsCargoOwners wmsCargoOwners) {
        wmsCargoOwners.setOwnerCode(gengrateOwnerCode());
        save(wmsCargoOwners);
    }

    /**
     * 生成货主编码
     */
    public String gengrateOwnerCode() {
        //编码规则：C+5位序号，序号使用redis自增序号实现
        //调用redis的incr函数
        long incr = 0;
        try {
            incr = redisUtil.incr("WMS_CARGO_OWNERS_CODE", 1);
        } catch (Exception e) {
            throw new JeecgBootException("生成货主编码错误");
        }
        String code = "C" + String.format("%05d", incr);
        return code;
    }
}
