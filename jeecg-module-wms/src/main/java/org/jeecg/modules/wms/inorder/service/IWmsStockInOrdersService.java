package org.jeecg.modules.wms.inorder.service;

import org.jeecg.modules.wms.inorder.entity.WmsStockInOrderItems;
import org.jeecg.modules.wms.inorder.entity.WmsStockInOrders;
import com.baomidou.mybatisplus.extension.service.IService;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @Description: 入库单主表
 * @Author: jeecg-boot
 * @Date:   2026-04-27
 * @Version: V1.0
 */
public interface IWmsStockInOrdersService extends IService<WmsStockInOrders> {

	/**
	 * 添加一对多
	 *
	 * @param wmsStockInOrders
	 * @param wmsStockInOrderItemsList
	 */
	public void saveMain(WmsStockInOrders wmsStockInOrders,List<WmsStockInOrderItems> wmsStockInOrderItemsList) ;
	
	/**
	 * 修改一对多
	 *
   * @param wmsStockInOrders
   * @param wmsStockInOrderItemsList
	 */
	public void updateMain(WmsStockInOrders wmsStockInOrders,List<WmsStockInOrderItems> wmsStockInOrderItemsList);
	
	/**
	 * 删除一对多
	 *
	 * @param id
	 */
	public void delMain (String id);
	
	/**
	 * 批量删除一对多
	 *
	 * @param idList
	 */
	public void delBatchMain (Collection<? extends Serializable> idList);

	/**
	 * 添加入库单
	 * @param wmsStockInOrders
	 */
    void add(WmsStockInOrders wmsStockInOrders);
}
