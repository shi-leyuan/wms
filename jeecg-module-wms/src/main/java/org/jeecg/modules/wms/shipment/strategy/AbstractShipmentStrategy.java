package org.jeecg.modules.wms.shipment.strategy;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.config.WarehouseDictEnum;
import org.jeecg.modules.wms.goods.entity.WmsProducts;
import org.jeecg.modules.wms.goods.service.IWmsProductsService;
import org.jeecg.modules.wms.outorder.entity.WmsOutOrders;
import org.jeecg.modules.wms.outorder.entity.WmsOutOrdersItems;
import org.jeecg.modules.wms.shipment.entity.WmsShipment;
import org.jeecg.modules.wms.shipment.entity.WmsShipmentDetail;
import org.jeecg.modules.wms.shipment.service.IWmsShipmentService;
import org.jeecg.modules.wms.shipment.vo.ShipmentGenerationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2025/6/17 15:34
 */
public abstract class AbstractShipmentStrategy implements ShipmentGenerationStrategy {

    protected IWmsShipmentService shipmentService;

    protected IWmsProductsService productsService;

    public AbstractShipmentStrategy(IWmsShipmentService shipmentService, IWmsProductsService productsService) {
        this.shipmentService = shipmentService;
        this.productsService = productsService;
    }

    @Override
    public abstract List<ShipmentGenerationResult> generateShipments(WmsOutOrders order, List<WmsOutOrdersItems> items);

    @Override
    public abstract boolean supports(WmsOutOrders order);

    @Override
    public abstract String getStrategyName();

    protected WmsShipment createBaseShipment(WmsOutOrders order) {
        // 生成包裹号
        String shipmentNumber = shipmentService.generateShipmentNumber();
        WmsShipment shipment = new WmsShipment();
        //主键用雪花算法
        shipment.setId(String.valueOf(IdWorker.getId()));
        shipment.setShipmentNo(shipmentNumber);
        shipment.setOrderId(order.getId());
        shipment.setWaveId(order.getWaveId());
        shipment.setStatus(WarehouseDictEnum.PACKAGE_STATUS_CREATED.getCode());//已创建
        return shipment;
    }
    protected Double getItemWeight(String skuId) {
        WmsProducts products = productsService.getById(skuId);
        if (products == null) {
            throw new JeecgBootException("商品不存在，商品ID：" + skuId);
        }

        Double netWeight = products.getNetWeight();
        if (netWeight == null || netWeight <= 0) {
            throw new JeecgBootException("商品【" + products.getProductName() + "】净重未维护，无法按重量拆包");
        }

        return netWeight;
    }
    /**
     * 计算总重量
     * @param items
     * @return
     */
    protected double calculateTotalWeight(List<WmsOutOrdersItems> items) {
        return items.stream().mapToDouble(item -> {
            double weight = getItemWeight(item.getSkuId());
            int quantity = item.getPickedQuantity() == null ? 0 : item.getPickedQuantity();
            return weight * quantity;
        }).sum();
    }

    /**
     * 生成包裹明细
     */
    protected List<WmsShipmentDetail> createShipmentDetail(WmsShipment shipment,List<WmsOutOrdersItems> outOrdersItems) {
        List<WmsShipmentDetail> allDetails = new ArrayList<>();
        List<WmsOutOrdersItems> items = new ArrayList<>();
            for (WmsOutOrdersItems item : outOrdersItems) {
                WmsShipmentDetail detail = convertToDetail(shipment, item);
                allDetails.add(detail);

                // 出库单明细设置打包数量
                item.setPackedQuantity(item.getPickedQuantity());
                // 更新出库单明细状态为已打包
                item.setStatus(WarehouseDictEnum.OUTBOUND_DETAIL_PACKED.getCode());
                items.add(item);
            }
        return allDetails;
    }
    private WmsShipmentDetail convertToDetail(WmsShipment shipment, WmsOutOrdersItems item) {
        WmsShipmentDetail detail = new WmsShipmentDetail();
        detail.setShipmentId(shipment.getId());
        detail.setOrderId(shipment.getOrderId());
        detail.setOrderItemId(item.getId());
        detail.setSkuId(item.getSkuId());
        detail.setQuantity(item.getPickedQuantity());
        return detail;
    }
}
