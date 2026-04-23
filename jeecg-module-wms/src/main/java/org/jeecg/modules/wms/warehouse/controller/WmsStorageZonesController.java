package org.jeecg.modules.wms.warehouse.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.query.QueryRuleEnum;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.wms.warehouse.entity.WmsStorageZones;
import org.jeecg.modules.wms.warehouse.entity.WmsWarehouses;
import org.jeecg.modules.wms.warehouse.service.IWmsStorageZonesService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import org.jeecg.modules.wms.warehouse.service.IWmsWarehousesService;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.jeecg.common.system.base.controller.JeecgController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.apache.shiro.authz.annotation.RequiresPermissions;

 /**
 * @Description: 储区表
 * @Author: jeecg-boot
 * @Date:   2026-04-23
 * @Version: V1.0
 */
@Tag(name="储区表")
@RestController
@RequestMapping("/warehouse/wmsStorageZones")
@Slf4j
public class WmsStorageZonesController extends JeecgController<WmsStorageZones, IWmsStorageZonesService> {
	@Autowired
	private IWmsStorageZonesService wmsStorageZonesService;

	@Autowired
	private IWmsWarehousesService iWmsWarehousesService;
	
	/**
	 * 分页列表查询
	 *
	 * @param wmsStorageZones
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "储区表-分页列表查询")
	@Operation(summary="储区表-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<WmsStorageZones>> queryPageList(WmsStorageZones wmsStorageZones,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
        QueryWrapper<WmsStorageZones> queryWrapper = QueryGenerator.initQueryWrapper(wmsStorageZones, req.getParameterMap());
		Page<WmsStorageZones> page = new Page<WmsStorageZones>(pageNo, pageSize);
		IPage<WmsStorageZones> pageList = wmsStorageZonesService.page(page, queryWrapper);

		//如果pageList为空，直接返回
		if(pageList.getRecords().isEmpty()){
			return Result.OK(pageList);
		}
		//先提取出pageList的仓库ID，使用stream流
		List<String> collect = pageList.getRecords().stream().map(WmsStorageZones::getWarehouseId).collect(Collectors.toList());
		//根据仓库ID查询仓库表，拿到仓库名称
		BaseMapper<WmsWarehouses> baseMapper = iWmsWarehousesService.getBaseMapper();
		List<WmsWarehouses> warehouses = baseMapper.selectByIds(collect);
		//将仓库名称设置到pageList
		pageList.getRecords().stream().forEach(item->{
			//仓库ID
			String warehouseId = item.getWarehouseId();
			//从warehouses中查询ID所对应的名称
			WmsWarehouses wmsWarehouses = warehouses.stream().filter(warehouse -> warehouse.getId().equals(warehouseId)).findFirst().orElse(new WmsWarehouses());
			item.setWarehouseName(wmsWarehouses.getWarehouseName());
		});

		return Result.OK(pageList);
	}
	
	/**
	 *   添加
	 *
	 * @param wmsStorageZones
	 * @return
	 */
	@AutoLog(value = "储区表-添加")
	@Operation(summary="储区表-添加")
	@RequiresPermissions("warehouse:wms_storage_zones:add")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody WmsStorageZones wmsStorageZones) {
		wmsStorageZonesService.save(wmsStorageZones);
		return Result.OK("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param wmsStorageZones
	 * @return
	 */
	@AutoLog(value = "储区表-编辑")
	@Operation(summary="储区表-编辑")
	@RequiresPermissions("warehouse:wms_storage_zones:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody WmsStorageZones wmsStorageZones) {
		wmsStorageZonesService.updateById(wmsStorageZones);
		return Result.OK("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "储区表-通过id删除")
	@Operation(summary="储区表-通过id删除")
	@RequiresPermissions("warehouse:wms_storage_zones:delete")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		wmsStorageZonesService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "储区表-批量删除")
	@Operation(summary="储区表-批量删除")
	@RequiresPermissions("warehouse:wms_storage_zones:deleteBatch")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.wmsStorageZonesService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功!");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "储区表-通过id查询")
	@Operation(summary="储区表-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<WmsStorageZones> queryById(@RequestParam(name="id",required=true) String id) {
		WmsStorageZones wmsStorageZones = wmsStorageZonesService.getById(id);
		if(wmsStorageZones==null) {
			return Result.error("未找到对应数据");
		}
		return Result.OK(wmsStorageZones);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param wmsStorageZones
    */
    @RequiresPermissions("warehouse:wms_storage_zones:exportXls")
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, WmsStorageZones wmsStorageZones) {
        return super.exportXls(request, wmsStorageZones, WmsStorageZones.class, "储区表");
    }

    /**
      * 通过excel导入数据
    *
    * @param request
    * @param response
    * @return
    */
    @RequiresPermissions("warehouse:wms_storage_zones:importExcel")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
        return super.importExcel(request, response, WmsStorageZones.class);
    }

}
