package org.jeecg.modules.wms.inorder.service.impl;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.DateUtils;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.wms.config.WarehouseDictEnum;
import org.jeecg.modules.wms.inorder.entity.WmsStockInOrders;
import org.jeecg.modules.wms.inorder.entity.WmsStockInOrderItems;
import org.jeecg.modules.wms.inorder.mapper.WmsStockInOrderItemsMapper;
import org.jeecg.modules.wms.inorder.mapper.WmsStockInOrdersMapper;
import org.jeecg.modules.wms.inorder.service.IWmsStockInOrderItemsService;
import org.jeecg.modules.wms.inorder.service.IWmsStockInOrdersService;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description: 入库单主表
 * @Author: jeecg-boot
 * @Date: 2026-04-27
 * @Version: V1.0
 */
@Service
public class WmsStockInOrdersServiceImpl extends ServiceImpl<WmsStockInOrdersMapper, WmsStockInOrders> implements IWmsStockInOrdersService {

    @Autowired
    private WmsStockInOrdersMapper wmsStockInOrdersMapper;
    @Autowired
    private WmsStockInOrderItemsMapper wmsStockInOrderItemsMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private IWmsStockInOrderItemsService wmsStockInOrderItemsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMain(WmsStockInOrders wmsStockInOrders, List<WmsStockInOrderItems> wmsStockInOrderItemsList) {
        wmsStockInOrdersMapper.insert(wmsStockInOrders);
        if (wmsStockInOrderItemsList != null && wmsStockInOrderItemsList.size() > 0) {
            for (WmsStockInOrderItems entity : wmsStockInOrderItemsList) {
                //外键设置
                entity.setOrderId(wmsStockInOrders.getId());
                wmsStockInOrderItemsMapper.insert(entity);
            }
        }
    }

//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public void updateMain(WmsStockInOrders wmsStockInOrders, List<WmsStockInOrderItems> wmsStockInOrderItemsList) {
//        wmsStockInOrdersMapper.updateById(wmsStockInOrders);
//
//        //1.先删除子表数据
//        wmsStockInOrderItemsMapper.deleteByMainId(wmsStockInOrders.getId());
//
//        //2.子表数据重新插入
//        if (wmsStockInOrderItemsList != null && wmsStockInOrderItemsList.size() > 0) {
//            for (WmsStockInOrderItems entity : wmsStockInOrderItemsList) {
//                //外键设置
//                entity.setOrderId(wmsStockInOrders.getId());
//                wmsStockInOrderItemsMapper.insert(entity);
//            }
//        }
//    }


    /**
     * 编辑入库单明细
     * 先删后插
     *
     * @param wmsStockInOrders
     * @param wmsStockInOrderItemsList
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMain(WmsStockInOrders wmsStockInOrders, List<WmsStockInOrderItems> wmsStockInOrderItemsList) {
        //如果入库单明细为空
        if (wmsStockInOrderItemsList == null || wmsStockInOrderItemsList.size() <= 0) {
            throw new JeecgBootException("请加入入库单明细");
        }
        //查询入库单
        WmsStockInOrders orders = wmsStockInOrdersMapper.selectById(wmsStockInOrders.getId());
        if (orders == null) {
            throw new JeecgBootException("入库单不存在");
        }
        //入库单状态
        String status = orders.getStatus();
        //初始状态、审核失败状态可以修改
        if (!(WarehouseDictEnum.INBOUND_INITIAL.getCode().equals(status) || WarehouseDictEnum.INBOUND_REJECTED.getCode().equals(status))) {
            throw new JeecgBootException("只有初始状态、审核失败状态可以修改");
        }
        // 先更新主表
        wmsStockInOrdersMapper.updateById(wmsStockInOrders);
        //删除入库单明细数据
        wmsStockInOrderItemsMapper.deleteByMainId(wmsStockInOrders.getId());
        //对子表数据按商品分组,使用Stream流实现
        Map<String, List<WmsStockInOrderItems>> collect = wmsStockInOrderItemsList.stream().collect(Collectors.groupingBy(WmsStockInOrderItems::getProductId));
        //合并后的结果
        List<WmsStockInOrderItems> mergeList = new ArrayList<>();
        //便利Map<String, List<WmsStockInOrderItems>>中的key，val对
        for (Map.Entry<String, List<WmsStockInOrderItems>> entry : collect.entrySet()) {
            List<WmsStockInOrderItems> list = entry.getValue();
            WmsStockInOrderItems wmsStockInOrderItems = list.get(0);
            //只有当list的size大于1，才需要求和
            if (list.size() > 1) {
                Integer sum = list.stream().mapToInt(WmsStockInOrderItems::getExpectedQuantity).sum();
                wmsStockInOrderItems.setExpectedQuantity(sum);
            }
            //设置入库单ID
            wmsStockInOrderItems.setOrderId(wmsStockInOrders.getId());
            mergeList.add(wmsStockInOrderItems);
        }
        //将mergeList添加到数据库
        boolean b = wmsStockInOrderItemsService.saveBatch(mergeList);
        if (!b) {
            throw new JeecgBootException("保存失败");
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delMain(String id) {
        wmsStockInOrderItemsMapper.deleteByMainId(id);
        wmsStockInOrdersMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delBatchMain(Collection<? extends Serializable> idList) {
        for (Serializable id : idList) {
            wmsStockInOrderItemsMapper.deleteByMainId(id.toString());
            wmsStockInOrdersMapper.deleteById(id);
        }
    }

    /**
     * 添加入库单
     *
     * @param wmsStockInOrders
     */
    public void add(WmsStockInOrders wmsStockInOrders) {
        //生成入库单号
        String orderNumber = generateOrderNumber();
        wmsStockInOrders.setOrderNumber(orderNumber);
        //默认状态为初始状态
        wmsStockInOrders.setStatus(WarehouseDictEnum.INBOUND_INITIAL.getCode());
        wmsStockInOrdersMapper.insert(wmsStockInOrders);
    }

    //生成入库单号
    public String generateOrderNumber() {
        //当前8位时间戳(年月日), DateUtils.now()结果示例2001-11-11
        String time = DateUtils.now().substring(0, 10).replace("-", "");
        //key
        String key = "wms:asm_number" + time;
        long incr = redisUtil.incr(key, 1);
        if (incr == 1) {
            //设置过期时间，设置24小时+10秒的目的是避免并发产生订单号重复
            redisUtil.expire(key, 24 * 60 * 60 + 60);
        }
        //将incr组成4位字符串
        String incrStr = String.format("%04d", incr);
        return "ASN" + time + incrStr;
    }

}
