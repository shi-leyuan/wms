package org.jeecg.modules.wms.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.modules.wms.goods.entity.*;
import org.jeecg.modules.wms.goods.excel.WmsProductsImport;
import org.jeecg.modules.wms.goods.mapper.WmsProductsMapper;
import org.jeecg.modules.wms.goods.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @Description: 商品信息表
 * @Author: jeecg-boot
 * @Date:   2025-04-14
 * @Version: V1.0
 */
@Service
public class WmsProductsServiceImpl extends ServiceImpl<WmsProductsMapper, WmsProducts> implements IWmsProductsService {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private IWmsCargoOwnersService wmsCargoOwnersService;

    @Autowired
    private IWmsProductCategoriesService wmsProductCategoriesService;

    @Autowired
    private IWmsProductImagesService wmsProductImagesService;

    @Autowired
    private IWmsProductBrandService wmsProductBrandService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void add(WmsProducts wmsProducts) {
        //校验货主加商品编码在商品表的唯一性
        if(checkProductCode(wmsProducts)){
            throw new RuntimeException("货主加商品编码在商品表中已存在");
        }
        //生成商品条码
        wmsProducts.setProductBarcode(generateProductBarcode(wmsProducts));
        //保存商品
        save(wmsProducts);
//        String productImgs= wmsProducts.getProductImgs();
//        if(productImgs!=null){
//            //图片
//            String[] productImgArr = productImgs.split(",");
//            for (String productImg : productImgArr) {
//                WmsProductImages wmsProductImages = new WmsProductImages();
//                wmsProductImages.setProductId(wmsProducts.getId());
//                wmsProductImages.setOriginal(productImg);
//                wmsProductImagesService.save(wmsProductImages);
//            }
//        }
    }

    /**
     * 校验货主加商品编码在商品表的唯一性
     * @param wmsProducts
     * @return 不存在返回false，存在返回true
     */
    public boolean checkProductCode(WmsProducts wmsProducts) {
        String ownerId = wmsProducts.getOwnerId();
        String productCode = wmsProducts.getProductCode();
        //根据货主和商品编码查询
        List<WmsProducts> wmsProductsList = baseMapper.selectList(lambdaQuery().getWrapper()
                .eq(WmsProducts::getOwnerId,ownerId)
                .eq(WmsProducts::getProductCode,productCode));
        return wmsProductsList.size()>0;

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void edit(WmsProducts wmsProducts) {
        String id = wmsProducts.getId();
        WmsProducts wmsProductsOld = getById(id);
        //如果商品编码有变化
        if(!wmsProductsOld.getProductCode().equals(wmsProducts.getProductCode())){
            //校验商品编码在商品表中的 uniqueness
            if(checkProductCode(wmsProducts)){
                throw new RuntimeException("商品编码在商品表中已存在");
            }
        }
        //如果商品条码为空则生成商品条码
        if(wmsProducts.getProductBarcode().isEmpty()){
            wmsProducts.setProductBarcode(generateProductBarcode(wmsProducts));
        }
        updateById(wmsProducts);
        //如果商品图片有变化
//        String productImgsOld = wmsProductsOld.getProductImgs()==null?"":wmsProductsOld.getProductImgs();
//        String productImgsNew = wmsProducts.getProductImgs()==null?"":wmsProducts.getProductImgs();
//        if(!productImgsOld.equals(productImgsNew)){
//            LambdaQueryWrapper<WmsProductImages> queryWrapper = new LambdaQueryWrapper<WmsProductImages>().eq(WmsProductImages::getProductId, id);
//            wmsProductImagesService.remove(queryWrapper);
//            if(productImgsNew!=null){
//                //图片
//                String[] productImgArr = productImgsNew.split(",");
//                for (String productImg : productImgArr) {
//                    WmsProductImages wmsProductImages = new WmsProductImages();
//                    wmsProductImages.setProductId(wmsProducts.getId());
//                    wmsProductImages.setOriginal(productImg);
//                    wmsProductImagesService.save(wmsProductImages);
//                }
//            }
//        }
    }

    @Override
    public void importProduct(List<WmsProductsImport> cachedDataList) {
        //遍历
        for (WmsProductsImport wmsProductsImport : cachedDataList) {
            //校验数据的合法性...

            //将wmsProductsImport转成 WmsProducts
            WmsProducts products = convertWmsProducts(wmsProductsImport);


            //保存到数据库
            boolean save = save(products);
            if(!save){
               //记录失败原因，失败数量
            }
        }


    }

    private WmsProducts convertWmsProducts(WmsProductsImport wmsProductsImport){
        WmsProducts wmsProducts = new WmsProducts();
        BeanUtils.copyProperties(wmsProductsImport,wmsProducts);

        //处理名称
        //货主名称
        String ownerName = wmsProductsImport.getOwnerName();
        //根据货主名称查询货主
        LambdaQueryWrapper<WmsCargoOwners> eq = new LambdaQueryWrapper<WmsCargoOwners>()
                .eq(WmsCargoOwners::getOwnerName, ownerName);
        WmsCargoOwners wmsCargoOwners = wmsCargoOwnersService.getOne(eq);
        wmsProducts.setOwnerId(wmsCargoOwners.getId());

        //根据商品分类名称查询分类
        String categoryName = wmsProductsImport.getCategoryName();
        LambdaQueryWrapper<WmsProductCategories> queryWrapper = new LambdaQueryWrapper<WmsProductCategories>()
                .eq(WmsProductCategories::getCategoryName, categoryName);
        WmsProductCategories wmsProductCategories = wmsProductCategoriesService.getOne(queryWrapper);
        wmsProducts.setCategoryId(wmsProductCategories.getId());
        //根据品牌名称查询 品牌
        String brandName = wmsProductsImport.getProductBrandName();
        LambdaQueryWrapper<WmsProductBrand> queryWrapper2 = new LambdaQueryWrapper<WmsProductBrand>()
                .eq(WmsProductBrand::getName, brandName);
        WmsProductBrand wmsProductBrand = wmsProductBrandService.getOne(queryWrapper2);
        wmsProducts.setProductBrand(wmsProductBrand.getId());

        return wmsProducts;

    }


    /**
     * 生成商品条码，全局唯一
     *
     * 编码规则：6位货主编码 + 商品类别编码 + 6位序号
     */
    public String generateProductBarcode(WmsProducts wmsProducts) {
        if (wmsProducts == null) {
            throw new JeecgBootException("商品信息不能为空");
        }

        String ownerId = wmsProducts.getOwnerId();
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new JeecgBootException("货主不能为空");
        }

        WmsCargoOwners owner = wmsCargoOwnersService.getById(ownerId);
        if (owner == null) {
            throw new JeecgBootException("货主不存在，请重新选择货主");
        }

        String ownerCode = owner.getOwnerCode();
        if (ownerCode == null || ownerCode.trim().isEmpty()) {
            throw new JeecgBootException("货主编码为空，无法生成商品条码");
        }

        String categoryId = wmsProducts.getCategoryId();
        if (categoryId == null || categoryId.trim().isEmpty()) {
            throw new JeecgBootException("商品分类不能为空");
        }

        WmsProductCategories category = wmsProductCategoriesService.getById(categoryId);
        if (category == null) {
            throw new JeecgBootException("商品分类不存在，请重新选择商品分类");
        }

        String categoryCode = category.getCategoryCode();
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            throw new JeecgBootException("商品类别编码为空，无法生成商品条码");
        }

        long incr = redisUtil.incr("WMS_PRO_BARCODE", 1);
        String serialCode = String.format("%06d", incr);

        return ownerCode + categoryCode + serialCode;
    }


}
