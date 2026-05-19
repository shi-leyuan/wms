package org.jeecg.modules.wms.shipment.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.DateUtils;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.wms.config.WarehouseDictEnum;
import org.jeecg.modules.wms.goods.entity.WmsProducts;
import org.jeecg.modules.wms.goods.service.IWmsProductsService;
import org.jeecg.modules.wms.outorder.entity.WmsOutOrders;
import org.jeecg.modules.wms.outorder.entity.WmsOutOrdersItems;
import org.jeecg.modules.wms.outorder.service.IWmsOutOrdersItemsService;
import org.jeecg.modules.wms.outorder.service.IWmsOutOrdersService;
import org.jeecg.modules.wms.shipment.entity.WmsShipment;
import org.jeecg.modules.wms.shipment.entity.WmsShipmentDetail;
import org.jeecg.modules.wms.waybill.service.IWmsSfService;
import org.jeecg.modules.wms.wave.entity.WmsWaveMaster;
import org.jeecg.modules.wms.shipment.mapper.WmsShipmentDetailMapper;
import org.jeecg.modules.wms.shipment.mapper.WmsShipmentMapper;
import org.jeecg.modules.wms.shipment.service.IWmsShipmentDetailService;
import org.jeecg.modules.wms.shipment.service.IWmsShipmentService;
import org.jeecg.modules.wms.wave.service.IWmsWaveMasterService;
import org.jeecg.modules.wms.shipment.strategy.ShipmentGenerationStrategy;
import org.jeecg.modules.wms.shipment.strategy.ShipmentStrategyFactory;
import org.jeecg.modules.wms.shipment.vo.ShipmentGenerationResult;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description: 包裹主表
 * @Author: jeecg-boot
 * @Date:   2025-06-13
 * @Version: V1.0
 */
@Service
@Slf4j
public class WmsShipmentServiceImpl extends ServiceImpl<WmsShipmentMapper, WmsShipment> implements IWmsShipmentService {

	@Autowired
	private WmsShipmentMapper wmsShipmentMapper;
	@Autowired
	private WmsShipmentDetailMapper wmsShipmentDetailMapper;
	@Autowired
	private IWmsShipmentDetailService wmsShipmentDetailService;

	@Autowired
	private IWmsProductsService wmsProductsService;

	@Autowired
	private WmsShipmentServiceImpl owner;

	@Autowired
	private IWmsOutOrdersService wmsOutOrdersService;
	@Autowired
	private IWmsOutOrdersItemsService wmsOutOrdersItemsService;
	@Autowired
	private IWmsWaveMasterService wmsWaveMasterService;

	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private ShipmentStrategyFactory shipmentStrategyFactory;


	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveMain(WmsShipment wmsShipment, List<WmsShipmentDetail> wmsShipmentDetailList) {
		//生成包裹编号
		String shipmentNumber = generateShipmentNumber();
		wmsShipment.setId(String.valueOf(IdWorker.getId()));
		wmsShipment.setShipmentNo(shipmentNumber);
		//获取出库单
		WmsOutOrders outOrder = wmsOutOrdersService.getById(wmsShipment.getOrderId());
		//波次信息
		wmsShipment.setWaveId(outOrder.getWaveId());

		//对wmsShipmentDetailList中按出库单明细id进行分组
		Map<String, List<WmsShipmentDetail>> map = wmsShipmentDetailList.stream().collect(Collectors.groupingBy(WmsShipmentDetail::getOrderItemId));

		wmsShipmentMapper.insert(wmsShipment);
		//2.子表数据重新插入
		for (Map.Entry<String, List<WmsShipmentDetail>> entry : map.entrySet()) {
			List<WmsShipmentDetail> list = entry.getValue();
			WmsShipmentDetail wmsShipmentDetail = list.get(0);
			//打包数量汇总
			int packedQuantity = list.stream().mapToInt(WmsShipmentDetail::getQuantity).sum();
			//打包数量
			wmsShipmentDetail.setQuantity(packedQuantity);
			//外键设置
			wmsShipmentDetail.setShipmentId(wmsShipment.getId());
			wmsShipmentDetail.setOrderId(wmsShipment.getOrderId());
			wmsShipmentDetailMapper.insert(wmsShipmentDetail);
			updateOutItemPackedQuantity(entry.getKey());
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateMain(WmsShipment wmsShipment,List<WmsShipmentDetail> wmsShipmentDetailList) {
		//包裹状态为已创建可以修改
		if(!WarehouseDictEnum.PACKAGE_STATUS_CREATED.getCode().equals(wmsShipment.getStatus())){
			throw new JeecgBootException("非已创建状态包裹不允许修改");
		}

		//对wmsShipmentDetailList中按出库单明细id进行分组
		Map<String, List<WmsShipmentDetail>> map = wmsShipmentDetailList.stream().collect(Collectors.groupingBy(WmsShipmentDetail::getOrderItemId));

		wmsShipmentMapper.updateById(wmsShipment);

		//1.先删除子表数据
		wmsShipmentDetailMapper.deleteByMainId(wmsShipment.getId());

		//2.子表数据重新插入
		for (Map.Entry<String, List<WmsShipmentDetail>> entry : map.entrySet()) {
			List<WmsShipmentDetail> list = entry.getValue();
			WmsShipmentDetail wmsShipmentDetail = list.get(0);
			//打包数量汇总
			int packedQuantity = list.stream().mapToInt(WmsShipmentDetail::getQuantity).sum();
			//打包数量
			wmsShipmentDetail.setQuantity(packedQuantity);
			//外键设置
			wmsShipmentDetail.setShipmentId(wmsShipment.getId());
			wmsShipmentDetail.setOrderId(wmsShipment.getOrderId());
			wmsShipmentDetailMapper.insert(wmsShipmentDetail);
			//更新出库单明细打包数量
			updateOutItemPackedQuantity(entry.getKey());
		}

	}

	/**
	 * 更新出库单明细打包数量
	 * @param outItemId
	 */
	@Transactional(rollbackFor = Exception.class)
	public void updateOutItemPackedQuantity(String outItemId) {
		//总打包数量
		int packedQuantityByOrderItemId = getPackedQuantityByOrderItemId(outItemId);
		//查询出库单明细
		WmsOutOrdersItems item = wmsOutOrdersItemsService.getById(outItemId);
		 //商品
		WmsProducts wmsProducts = wmsProductsService.getById(item.getSkuId());
		//打包数量不能大于拣货数量
		if(packedQuantityByOrderItemId > item.getPickedQuantity()){
			throw new JeecgBootException(wmsProducts.getProductName()+" 的总打包数量不能大于拣货数量!");
		}
		LambdaUpdateWrapper<WmsOutOrdersItems> le = new LambdaUpdateWrapper<WmsOutOrdersItems>()
				.eq(WmsOutOrdersItems::getId, outItemId)
				.set(WmsOutOrdersItems::getPackedQuantity, packedQuantityByOrderItemId)
				.ge(WmsOutOrdersItems::getPickedQuantity, packedQuantityByOrderItemId);
		boolean update = wmsOutOrdersItemsService.update(null,le);
		if(!update){
			throw new JeecgBootException("更新出库单明细打包数量失败!");
		}
	}

	/**
	 * 校验包裹明细打包数量是否大于拣货数量
	 * @param   shipmentId	包裹id
	 * @param  outItemId
	 */
	@Transactional(rollbackFor = Exception.class)
	public void checkPackedQuantity(String shipmentId,String outItemId) {
		//获取出库单明细
		WmsOutOrdersItems outItem = wmsOutOrdersItemsService.getById(outItemId);
		//获取拣货数量
		int pickedQuantity = outItem.getPickedQuantity();
		//根据出库单明细id查询包裹明细表并汇总打包数量
		int packedQuantityByOrderItemId = getPackedQuantityByOrderItemId(outItemId);
		//判断打包数量不能大于拣货数量
		if(packedQuantityByOrderItemId > pickedQuantity){
			throw new JeecgBootException("打包数量不能大于拣货数量");
		}
	}

	/**
	 * 根据出库单明细id查询包裹明细表并汇总打包数量
	 * @param outItemId
	 */
	public int getPackedQuantityByOrderItemId(String outItemId) {
		LambdaQueryWrapper<WmsShipmentDetail> eq = new LambdaQueryWrapper<WmsShipmentDetail>()
				.eq(WmsShipmentDetail::getOrderItemId, outItemId);
		List<WmsShipmentDetail> wmsShipmentDetails = wmsShipmentDetailMapper.selectList(eq);
		if(wmsShipmentDetails == null|| wmsShipmentDetails.size() == 0){
			return 0;
		}
		int packedQuantitySum = wmsShipmentDetails.stream().mapToInt(WmsShipmentDetail::getQuantity).sum();
		return packedQuantitySum;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delMain(String id) {
		//包裹
		WmsShipment wmsShipment = wmsShipmentMapper.selectById(id);
		//如果包裹状态为已创建可以删除
		if(!WarehouseDictEnum.PACKAGE_STATUS_CREATED.getCode().equals(wmsShipment.getStatus())){
			throw new JeecgBootException("非已创建状态包裹不允许删除");
		}
		//查询包裹明细
		List<WmsShipmentDetail> wmsShipmentDetails = wmsShipmentDetailMapper.selectByMainId(id);
		//提取出库单明细id
		List<String> outItemIds = wmsShipmentDetails.stream().map(WmsShipmentDetail::getOrderItemId).collect(Collectors.toList());
		wmsShipmentDetailMapper.deleteByMainId(id);
		wmsShipmentMapper.deleteById(id);
		for (String outItemId : outItemIds) {
			//更新出库单明细打包数量
			updateOutItemPackedQuantity(outItemId);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delBatchMain(Collection<? extends String> idList) {
		for(String id:idList) {
			delMain(id);
		}
	}

	/**
	 * 根据波次创建包裹
	 * @param waveId 波次id
	 * @return 包裹数量
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public int createShipment(String waveId) {
		WmsWaveMaster wave = wmsWaveMasterService.getById(waveId);
		if (wave == null) {
			throw new JeecgBootException("波次" + waveId + "不存在");
		}

		// 严格按你的流程：波次必须拣货完成
		if (!WarehouseDictEnum.WAVE_PICKED.getCode().equals(wave.getStatus())) {
			throw new JeecgBootException("波次未拣货完成，不能创建包裹");
		}

		List<WmsOutOrders> orders = wmsOutOrdersService.selectByWaveId(waveId);
		if (orders == null || orders.isEmpty()) {
			throw new JeecgBootException("该波次下没有出库单");
		}

		int createdCount = 0;

		for (WmsOutOrders order : orders) {
			// 严格按你的流程：只处理拣货完成的出库单
			if (!WarehouseDictEnum.OUTBOUND_PICKED.getCode().equals(order.getStatus())
					&& !WarehouseDictEnum.OUTBOUND_PACKED.getCode().equals(order.getStatus())) {
				throw new JeecgBootException("出库单【" + order.getOrderNo() + "】未拣货完成，不能创建包裹");
			}

			if (WarehouseDictEnum.OUTBOUND_PACKED.getCode().equals(order.getStatus())) {
				continue;
			}

			int count = owner.generateForSingleOrder(order);
			createdCount += count;

			// 如果该出库单所有明细都已经打包完成，则更新订单、明细、包裹状态
			if (isOrderPackQuantityComplete(order.getId())) {
				completePackByOrder(order);
			}
		}

		// 如果该波次下所有出库单都已打包，则更新波次状态为已打包
		if (allOrdersPacked(waveId)) {
			updateWavePacked(waveId);
		}

		return createdCount;
	}

	/**
	 * 判断波次下所有出库单是否都已打包
	 *
	 * @param waveId 波次ID
	 * @return true-全部已打包 false-未全部打包
	 */
	private boolean allOrdersPacked(String waveId) {
		if (waveId == null || waveId.trim().isEmpty()) {
			return false;
		}

		List<WmsOutOrders> orders = wmsOutOrdersService.selectByWaveId(waveId);

		if (orders == null || orders.isEmpty()) {
			return false;
		}

		return orders.stream().allMatch(order ->
				WarehouseDictEnum.OUTBOUND_PACKED.getCode().equals(order.getStatus())
		);
	}
	/**
	 * 判断某个出库单是否已全部完成打包
	 */
	private boolean isOrderPackQuantityComplete(String orderId) {
		List<WmsOutOrdersItems> items = wmsOutOrdersItemsService.selectByMainId(orderId);
		if (items == null || items.isEmpty()) {
			return false;
		}

		return items.stream().allMatch(item -> {
			int pickedQuantity = ObjectUtil.defaultIfNull(item.getPickedQuantity(), 0);
			int packedQuantity = ObjectUtil.defaultIfNull(item.getPackedQuantity(), 0);
			return pickedQuantity - packedQuantity == 0;
		});
	}

	/**
	 * 更新波次状态为已打包
	 */
	private void updateWavePacked(String waveId) {
		LambdaUpdateWrapper<WmsWaveMaster> update = new LambdaUpdateWrapper<WmsWaveMaster>()
				.eq(WmsWaveMaster::getId, waveId)
				.set(WmsWaveMaster::getStatus, WarehouseDictEnum.WAVE_PACKED.getCode());

		boolean result = wmsWaveMasterService.update(null, update);
		if (!result) {
			throw new JeecgBootException("更新波次打包状态失败");
		}
	}

	/**
	 * 单个订单生成包裹
	 * @param order
	 * @return 创建的包裹数量
	 */
	@Transactional(rollbackFor = Exception.class)
	public int generateForSingleOrder(WmsOutOrders order) {
		if (!WarehouseDictEnum.OUTBOUND_PICKED.getCode().equals(order.getStatus())) {
			throw new JeecgBootException("订单" + order.getOrderNo() + "未完成拣货");
		}

		List<WmsOutOrdersItems> items = wmsOutOrdersItemsService.selectByMainId(order.getId());
		if (items == null || items.isEmpty()) {
			throw new JeecgBootException("订单" + order.getOrderNo() + "没有出库单明细");
		}

		// 严格按你的流程：存在未拣货完成明细则不能创建包裹
		boolean hasUnPickedItem = items.stream().anyMatch(item ->
				!WarehouseDictEnum.OUTBOUND_DETAIL_PICKED.getCode().equals(item.getStatus())
						&& !WarehouseDictEnum.OUTBOUND_DETAIL_PACKED.getCode().equals(item.getStatus())
		);

		if (hasUnPickedItem) {
			throw new JeecgBootException("订单" + order.getOrderNo() + "存在未完成拣货的出库单明细");
		}

		// 只处理剩余打包数量 > 0 的明细
		List<WmsOutOrdersItems> needPackItems = items.stream()
				.filter(item -> {
					int pickedQuantity = ObjectUtil.defaultIfNull(item.getPickedQuantity(), 0);
					int packedQuantity = ObjectUtil.defaultIfNull(item.getPackedQuantity(), 0);
					return pickedQuantity - packedQuantity > 0;
				})
				.collect(Collectors.toList());

		// 没有剩余打包数量，说明该订单已经不需要再生成包裹
		if (needPackItems.isEmpty()) {
			return 0;
		}

		ShipmentGenerationStrategy strategy = shipmentStrategyFactory.getStrategy(order);

		List<ShipmentGenerationResult> results = strategy.generateShipments(order, needPackItems);
		if (results == null || results.isEmpty()) {
			return 0;
		}

		List<WmsShipment> shipments = results.stream()
				.map(ShipmentGenerationResult::getShipment)
				.collect(Collectors.toList());

		boolean saveShipment = saveBatch(shipments);
		if (!saveShipment) {
			throw new JeecgBootException("包裹主表保存失败");
		}

		List<WmsShipmentDetail> allDetails = results.stream()
				.map(ShipmentGenerationResult::getShipmentDetail)
				.flatMap(Collection::stream)
				.collect(Collectors.toList());

		boolean saveDetail = wmsShipmentDetailService.saveBatch(allDetails);
		if (!saveDetail) {
			throw new JeecgBootException("包裹明细保存失败");
		}

		// 更新出库单明细打包数量
		List<String> orderItemIds = allDetails.stream()
				.map(WmsShipmentDetail::getOrderItemId)
				.filter(Objects::nonNull)
				.distinct()
				.collect(Collectors.toList());

		for (String orderItemId : orderItemIds) {
			updateOutItemPackedQuantity(orderItemId);
		}

		return results.size();
	}

	@Override
	public IPage<WmsShipment> queryList(WmsShipment wmsShipment, Integer pageNo, Integer pageSize) {
		//开启分页查询,当执行查询时，插件进行相关的sql拦截进行分页操作，返回一个page对象
		Page<WmsShipment> page = PageHelper.startPage(pageNo, pageSize);
		List<WmsShipment> list = wmsShipmentMapper.queryShipmentList(wmsShipment);
		PageDTO<WmsShipment> wmsShipmentPageDTO = new PageDTO<>();
		wmsShipmentPageDTO.setRecords(list);
		wmsShipmentPageDTO.setTotal(page.getTotal());
		wmsShipmentPageDTO.setSize(page.getPageSize());
		wmsShipmentPageDTO.setCurrent(page.getPageNum());
		wmsShipmentPageDTO.setPages(page.getPages());
		return wmsShipmentPageDTO;
	}
	/**
	 * 完成某订单的打包
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public void completePackByOrder(WmsOutOrders order){
		//查询出库单明细中剩余数量不等于0的明细
		List<WmsOutOrdersItems> wmsOutOrdersItems = wmsOutOrdersItemsService.selectByMainId(order.getId());
		if(wmsOutOrdersItems.size()>0){
			for (WmsOutOrdersItems item : wmsOutOrdersItems) {
				//剩余打包数量=拣货数量-打包数量
				int surplusQuantity = item.getPickedQuantity()- ObjectUtil.defaultIfNull(item.getPackedQuantity(),0);
				if(surplusQuantity>0){
					throw new JeecgBootException("出库单"+order.getOrderNo()+"存在未打包的商品");
				}
			}
		}
		//更新出库单状态为打包完成
		LambdaUpdateWrapper<WmsOutOrders> set = new LambdaUpdateWrapper<WmsOutOrders>()
				.eq(WmsOutOrders::getId, order.getId())
				.set(WmsOutOrders::getStatus, WarehouseDictEnum.OUTBOUND_PACKED.getCode());
		wmsOutOrdersService.update(null,set);
		//更新出库单明细状态为打包完成
		LambdaUpdateWrapper<WmsOutOrdersItems> update = new LambdaUpdateWrapper<WmsOutOrdersItems>()
				.eq(WmsOutOrdersItems::getOrderId, order.getId())
				.set(WmsOutOrdersItems::getStatus, WarehouseDictEnum.OUTBOUND_DETAIL_PACKED.getCode());
		wmsOutOrdersItemsService.update(null,update);
		//更新包裹状态为打包完成
		 LambdaUpdateWrapper<WmsShipment> set2 = new LambdaUpdateWrapper<WmsShipment>()
				 .eq(WmsShipment::getOrderId, order.getId())
				 .set(WmsShipment::getStatus, WarehouseDictEnum.PACKAGE_STATUS_PACKED.getCode());
		 update(null,set2);
	}

	/**
	 * 批量完成某波次的打包
	 */
	public void completePackByWaveBatch(String waveIds){
		//校验waveids
		if(StringUtils.isEmpty(waveIds)){
			throw new JeecgBootException("请选择波次");
		}
		String[] waveIdsArray = waveIds.split(",");
		for (String waveId : waveIdsArray) {
			owner.completePackByWave(waveId);
		}

	}

	/**
	 * 完成某波次的打包
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public void completePackByWave(String waveId){
		//根据波次id查询波次下的所有出库单
		 List<WmsOutOrders> wmsOutOrders = wmsOutOrdersService.selectByWaveId(waveId);
		 for (WmsOutOrders order : wmsOutOrders) {
			 completePackByOrder(order);
		 }
		 //更新波次状态为打包完成
		 LambdaUpdateWrapper<WmsWaveMaster> update = new LambdaUpdateWrapper<WmsWaveMaster>()
				 .eq(WmsWaveMaster::getId, waveId)
				 .set(WmsWaveMaster::getStatus, WarehouseDictEnum.WAVE_PACKED.getCode());
		 wmsWaveMasterService.update(null,update);
	}

	@Override
	public String generateShipmentNumber() {
		//当前8位时间戳(年月日)"-"
		String time = DateUtils.now().substring(0, 10).replace("-", "");
		//key
		String key = "SH"+time;
		long incr = redisUtil.incr(key, 1);
		if(incr == 1){
			//设置过期时间，设置24小时+10秒的目的是避免并发产生订单号重复
			redisUtil.expire(key, 24*60*60+10);
		}
		//将incr组成4位字符串
		String incrStr = String.format("%04d", incr);
		String shipmentNumber = "SH"+time+incrStr;

		return shipmentNumber;
	}



	/**
	 * 保存运单号
	 * @param waybillNos 运单号
	 * @param shipments 包裹
	 */
	@Transactional(rollbackFor = Exception.class)
	public void saveWaybill(List<String> waybillNos,List<WmsShipment> shipments){
		//如果运单号的数量和包裹数量不一致不能继续，抛出异常
		if(waybillNos.size() != shipments.size()){
			throw new RuntimeException("运单数量和包裹数量不一致");
		}
		//将运单号依次保存到包裹中
		for (int i = 0; i < waybillNos.size(); i++) {
			LambdaUpdateWrapper<WmsShipment> set = new LambdaUpdateWrapper<WmsShipment>()
					.eq(WmsShipment::getId, shipments.get(i).getId())
					.set(WmsShipment::getTrackingNo, waybillNos.get(i));
			wmsShipmentMapper.update(null,set);
		}
		WmsShipment wmsShipment = shipments.get(0);
		//订单id
		String orderId = wmsShipment.getOrderId();
		//更新订单"createdWaybill"为1
		LambdaUpdateWrapper<WmsOutOrders> update = new LambdaUpdateWrapper<WmsOutOrders>()
				.eq(WmsOutOrders::getId, orderId)
				.set(WmsOutOrders::getCreatedWaybill, "1");
		wmsOutOrdersService.update(null, update);
	}


	/**
	 * 根据订单id查询包裹
	 * @param orderId
	 * @return
	 */
	public List<WmsShipment> selectByOrderId(String orderId) {
		LambdaQueryWrapper<WmsShipment> eq = new LambdaQueryWrapper<WmsShipment>()
				.eq(WmsShipment::getOrderId, orderId)
				.eq(WmsShipment::getStatus, WarehouseDictEnum.PACKAGE_STATUS_PACKED.getCode());
		return list(eq);
	}

}
