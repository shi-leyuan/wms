package org.jeecg.modules.wms.shipment.strategy;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.wms.config.WarehouseDictEnum;
import org.jeecg.modules.wms.goods.service.IWmsProductsService;
import org.jeecg.modules.wms.outorder.entity.WmsOutOrders;
import org.jeecg.modules.wms.outorder.entity.WmsOutOrdersItems;
import org.jeecg.modules.wms.shipment.entity.WmsShipment;
import org.jeecg.modules.wms.shipment.entity.WmsShipmentDetail;
import org.jeecg.modules.wms.shipment.service.IWmsShipmentService;
import org.jeecg.modules.wms.shipment.vo.ShipmentGenerationResult;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(300)
public class SplitByWeightShipmentStrategy extends AbstractShipmentStrategy {

    @Autowired
    public SplitByWeightShipmentStrategy(IWmsShipmentService shipmentService, IWmsProductsService productsService) {
        super(shipmentService, productsService);
    }

    private static final double MAX_WEIGHT = 20.0;

    @Override
    public List<ShipmentGenerationResult> generateShipments(WmsOutOrders order, List<WmsOutOrdersItems> items) {
        List<ShipmentGenerationResult> results = new ArrayList<>();

        List<WmsOutOrdersItems> currentBatch = new ArrayList<>();
        double currentWeight = 0D;
        int batchNumber = 1;

        for (WmsOutOrdersItems item : items) {
            int pickedQuantity = ObjectUtil.defaultIfNull(item.getPickedQuantity(), 0);
            int packedQuantity = ObjectUtil.defaultIfNull(item.getPackedQuantity(), 0);
            int surplusQuantity = pickedQuantity - packedQuantity;

            if (surplusQuantity <= 0) {
                continue;
            }

            double unitWeight = getItemWeight(item.getSkuId());

            // 这里必须拦截，否则 splitSingleItem 会 maxQuantity = 0，进入死循环
            if (unitWeight > MAX_WEIGHT) {
                throw new JeecgBootException("商品单件净重超过最大包裹重量，无法按重量拆包，商品ID：" + item.getSkuId());
            }

            WmsOutOrdersItems packItem = copyItemWithQuantity(item, surplusQuantity);
            packItem.setPackedQuantity(0);

            double totalItemWeight = unitWeight * surplusQuantity;

            if (totalItemWeight > MAX_WEIGHT) {
                if (!currentBatch.isEmpty()) {
                    results.add(createBatchResult(order, currentBatch, batchNumber++));
                    currentBatch = new ArrayList<>();
                    currentWeight = 0D;
                }

                List<ShipmentGenerationResult> splitResults = splitSingleItem(order, packItem, unitWeight, batchNumber);
                results.addAll(splitResults);
                batchNumber += splitResults.size();
            } else {
                if (currentWeight + totalItemWeight > MAX_WEIGHT && !currentBatch.isEmpty()) {
                    results.add(createBatchResult(order, currentBatch, batchNumber++));
                    currentBatch = new ArrayList<>();
                    currentWeight = 0D;
                }

                currentBatch.add(packItem);
                currentWeight += totalItemWeight;
            }
        }

        if (!currentBatch.isEmpty()) {
            results.add(createBatchResult(order, currentBatch, batchNumber));
        }

        return results;
    }

    private List<ShipmentGenerationResult> splitSingleItem(WmsOutOrders order,
                                                           WmsOutOrdersItems item,
                                                           double unitWeight,
                                                           int startBatchNumber) {
        List<ShipmentGenerationResult> results = new ArrayList<>();

        int remainingQuantity = ObjectUtil.defaultIfNull(item.getPickedQuantity(), 0);
        int batchNum = startBatchNumber;

        int maxQuantity = (int) Math.floor(MAX_WEIGHT / unitWeight);
        if (maxQuantity <= 0) {
            throw new JeecgBootException("商品单件重量超过最大包裹重量，无法生成包裹，商品ID：" + item.getSkuId());
        }

        while (remainingQuantity > 0) {
            int quantityInBatch = Math.min(maxQuantity, remainingQuantity);

            WmsOutOrdersItems splitItem = copyItemWithQuantity(item, quantityInBatch);
            splitItem.setPackedQuantity(0);

            List<WmsOutOrdersItems> batch = new ArrayList<>();
            batch.add(splitItem);

            results.add(createBatchResult(order, batch, batchNum++));

            // 这一句必须保证每轮减少，否则接口会超时
            remainingQuantity -= quantityInBatch;
        }

        return results;
    }

    private WmsOutOrdersItems copyItemWithQuantity(WmsOutOrdersItems original, int quantity) {
        WmsOutOrdersItems copy = new WmsOutOrdersItems();
        BeanUtils.copyProperties(original, copy);
        copy.setPickedQuantity(quantity);
        return copy;
    }

    private ShipmentGenerationResult createBatchResult(WmsOutOrders order,
                                                       List<WmsOutOrdersItems> batch,
                                                       int batchNo) {
        WmsShipment shipment = createBaseShipment(order);
        shipment.setShipmentType(WarehouseDictEnum.PACKAGE_TYPE_STANDARD.getCode());

        double batchWeight = calculateTotalWeight(batch);
        shipment.setTotalWeight(batchWeight);
        shipment.setPackageCount(batch.size());

        List<WmsShipmentDetail> shipmentDetail = createShipmentDetail(shipment, batch);
        return new ShipmentGenerationResult(shipment, batch, shipmentDetail);
    }

    @Override
    public boolean supports(WmsOutOrders order) {
        return "SPLIT_BY_WEIGHT_STRATEGY".equals(order.getShipmentStrategy());
    }

    @Override
    public String getStrategyName() {
        return "SPLIT_BY_WEIGHT_STRATEGY";
    }
}