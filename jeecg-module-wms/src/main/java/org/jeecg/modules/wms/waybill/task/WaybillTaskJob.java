package org.jeecg.modules.wms.waybill.task;


import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.wms.config.WarehouseDictEnum;
import org.jeecg.modules.wms.outorder.entity.WmsOutOrders;
import org.jeecg.modules.wms.outorder.service.IWmsOutOrdersService;
import org.jeecg.modules.wms.waybill.service.IWmsWaybillService;
import org.jeecg.modules.wms.waybill.task.handler.SimpleThreadCallable;
import org.jeecg.modules.wms.waybill.task.handler.WaybillThreadCallable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Mr.M
 * @version 1.0
 * @description 生成运单定时任务
 * @date 2025/7/15 11:51
 */
@Component
@Slf4j
public class WaybillTaskJob {


    @Resource(name = "waybillThreadPool")
    private ThreadPoolExecutor waybillThreadPool;
    @Autowired
    private IWmsOutOrdersService wmsOutOrdersService;
    @Autowired
    private IWmsWaybillService wmsWaybillService;


    /**
     * 测试
     *
     */
    //@Scheduled(cron = "0/2 * * * * ?")
    public void test1() {
        log.info("==============开始执行定时任务=================");
        for (int i = 0; i < 10; i++) {
            //定义任务对象
            SimpleThreadCallable simpleThreadCallable = new SimpleThreadCallable();
            // 启动多个线程完成请求顺丰下单并获取运单
            waybillThreadPool.submit(simpleThreadCallable);
        }

    }

    // 用于防重入
    private volatile boolean isRunning = false;

    //@Scheduled(cron = "0/2 * * * * ?")
    public void test2() {


        // 防重入检查
        if (isRunning) {
            log.warn("上次任务仍在执行，跳过本次调度");
            return;
        }
        log.info("==============开始执行定时任务=================");
        long startTime = System.currentTimeMillis();
        isRunning = true;

        try {
            // 检查线程池基本状态
            if (waybillThreadPool.getQueue().size() > 1000) {
                log.error("线程池队列堆积严重");
                return;
            }
            //任务列表
            List<Future<Boolean>> futures = new ArrayList<>(10);

            // 提交任务
            for (int i = 0; i < 10; i++) {
                SimpleThreadCallable simpleThreadCallable = new SimpleThreadCallable();
                Future submit = waybillThreadPool.submit(simpleThreadCallable);
                futures.add(submit);
            }

            // 等待所有任务完成（最多30秒）
            int successCount = 0;
            for (Future<Boolean> future : futures) {
                try {
                    Boolean result = future.get(30, TimeUnit.SECONDS);
                    if (Boolean.TRUE.equals(result)) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("等待任务结果异常", e);
                    future.cancel(true);
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("任务执行完成，成功数量: 10, 总耗时: {}ms", successCount, totalTime);

        } finally {
            isRunning = false;
        }
    }

    /**
     * 定时获取面单
     * 首先获取打包完成的波次，调用顺丰接口进行下单，获取面单 pdf并存储至minio
     */
    @Scheduled(cron = "0/20 * * * * ?")
    public void createWaybills() {

        // 防重入检查
        if (isRunning) {
            log.warn("上次任务仍在执行，跳过本次调度");
            return;
        }
        log.info("==============开始执行定时任务=================");
        long startTime = System.currentTimeMillis();
        isRunning = true;

        try {
            // 检查线程池基本状态
            if (waybillThreadPool.getQueue().size() > 1000) {
                log.error("线程池队列堆积严重");
                return;
            }
            //查询出库单状态为打包完成且未创建运单的出库单
            WmsOutOrders wmsOutOrders = new WmsOutOrders();
            wmsOutOrders.setStatus(WarehouseDictEnum.OUTBOUND_PACKED.getCode());
            wmsOutOrders.setCreatedWaybill("0");
            //todo:出库单表添加status、createdWaybill联合索引
            IPage<WmsOutOrders> wmsOutOrdersIPage = wmsOutOrdersService.queryList(wmsOutOrders, 1, 1000);
            List<WmsOutOrders> records = wmsOutOrdersIPage.getRecords();
            if (records.size() <= 0) {
                return;
            }
            //任务列表
            List<Future<Boolean>> futures = new ArrayList<>(10);

            // 提交任务
            for (WmsOutOrders outOrders : records) {
                //定义任务对象
                WaybillThreadCallable waveThreadCallable = new WaybillThreadCallable(wmsWaybillService, outOrders.getId());
                //定义FutureTask
                FutureTask<Boolean> futureTask = new FutureTask<>(waveThreadCallable);
                //添加到任务列表
                futures.add(futureTask);
                // 启动多个线程完成请求顺丰下单并获取运单
                waybillThreadPool.submit(futureTask);
            }

            // 等待所有任务完成（最多30秒）
            int successCount = 0;
            for (Future<Boolean> future : futures) {
                try {
                    Boolean result = future.get(30, TimeUnit.SECONDS);
                    if (Boolean.TRUE.equals(result)) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("等待任务结果异常", e);
                    future.cancel(true);
                }
            }

            long totalTime = System.currentTimeMillis() - startTime;
            log.info("任务执行完成，成功数量: 10, 总耗时: {}ms", successCount, totalTime);

        } finally {
            isRunning = false;
        }
    }
}