package com.sjy.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.mysql.cj.util.StringUtils;
import com.sjy.common.to.mq.OrderTo;
import com.sjy.common.to.mq.StockDetailTo;
import com.sjy.common.to.mq.StockLockedTo;
import com.sjy.common.utils.R;
import com.sjy.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.sjy.gulimall.ware.entity.WareOrderTaskEntity;
import com.sjy.gulimall.ware.exception.NoStockException;
import com.sjy.gulimall.ware.feign.OrderFeignService;
import com.sjy.gulimall.ware.service.WareOrderTaskDetailService;
import com.sjy.gulimall.ware.service.WareOrderTaskService;
import com.sjy.gulimall.ware.vo.OrderItemVo;
import com.sjy.gulimall.ware.vo.OrderVo;
import com.sjy.gulimall.ware.vo.SkuHasStockVo;
import com.sjy.gulimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjy.common.utils.PageUtils;
import com.sjy.common.utils.Query;

import com.sjy.gulimall.ware.dao.WareSkuDao;
import com.sjy.gulimall.ware.entity.WareSkuEntity;
import com.sjy.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();

        String wareId = (String) params.get("wareId");
        if (!StringUtils.isNullOrEmpty(wareId)) {
            wrapper.eq("ware_id", wareId);
        }

        String skuId = (String) params.get("skuId");
        if (!StringUtils.isNullOrEmpty(skuId)) {
            wrapper.eq("sku_id", skuId);
        }


        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, String skuName, Integer skuNum) {
        WareSkuEntity wareSkuEntity = this.baseMapper.selectOne(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (wareSkuEntity == null) {
            //新增
            wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setStock(skuNum);
        } else {
            wareSkuEntity.setStock(wareSkuEntity.getStock() + skuNum);
        }
        wareSkuEntity.setSkuName(skuName);
        wareSkuEntity.setWareId(wareId);
        wareSkuEntity.setSkuId(skuId);

        this.saveOrUpdate(wareSkuEntity);
    }

    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        //id转换成volist
        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            SkuHasStockVo skuHasStockVo = new SkuHasStockVo();
            Long count = this.baseMapper.getStockBySkuId(skuId);
            skuHasStockVo.setSkuId(skuId);
            skuHasStockVo.setHasStock(count != null && count > 0);
            return skuHasStockVo;
        }).collect(Collectors.toList());
        return collect;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean orderLockStock(WareSkuLockVo vo) {
        /**
         * 保存库存工作单详情信息追溯
         */
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskEntity.setCreateTime(new Date());
        wareOrderTaskService.save(wareOrderTaskEntity);


        List<OrderItemVo> locks = vo.getLocks();
        List<SkuWareHashStock> collect = locks.stream().map(item -> {
            SkuWareHashStock stock = new SkuWareHashStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            // 查询这个商品在哪里有库存
            List<Long> wareIds = wareSkuDao.listWareIdHashSkuStock(skuId);
            stock.setWareIds(wareIds);
            return stock;
        }).collect(Collectors.toList());
        for (SkuWareHashStock skuWareHashStock : collect) {
            Boolean skuStocked = false;
            Long skuId = skuWareHashStock.getSkuId();
            List<Long> wareIds = skuWareHashStock.getWareIds();
            if (wareIds == null || wareIds.size() == 0) {
                // 没有任何仓库有这个商品的库存
                throw new NoStockException(skuId);
            }
            for (Long wareId : skuWareHashStock.getWareIds()) {
                // 有库存
                if (this.baseMapper.lockStock(wareId, skuWareHashStock.getSkuId(), skuWareHashStock.getNum())) {
                    skuStocked = true;
                    WareOrderTaskDetailEntity wareOrderTaskDetailEntity = WareOrderTaskDetailEntity.builder()
                            .taskId(wareOrderTaskEntity.getId())
                            .skuId(skuWareHashStock.getSkuId())
                            .skuName("")
                            .skuNum(skuWareHashStock.getNum())
                            .wareId(wareId)
                            .lockStatus(1)
                            .build();
                    wareOrderTaskDetailService.save(wareOrderTaskDetailEntity);
                    // 告诉MQ库存锁定成功
                    StockLockedTo lockedTo = new StockLockedTo();
                    lockedTo.setId(wareOrderTaskEntity.getId());
                    StockDetailTo detailTo = new StockDetailTo();
                    BeanUtils.copyProperties(wareOrderTaskDetailEntity, detailTo);
                    lockedTo.setDetailTo(detailTo);
                    //告诉MQ库存锁定成功，生产者-交换机-死信队列-交换机-普通队列-消费者
                    //消息内容为库存锁定单传输对象，里面包括库存单id和库存详情单对象
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", lockedTo);
                    break;
                }
            }
            if (!skuStocked) {
                throw new NoStockException(skuId);
            }
        }
        return true;
    }

    /**
     * 1、库存自动解锁
     *      下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚。之前锁定的库存就要自动解锁
     * 2、订单失败
     *      锁库存失败，则库存回滚了，这种情况无需解锁
     *      如何判断库存是否锁定失败呢？查询数据库关于这个订单的锁库存消息即可
     *  自动ACK机制：只要解决库存的消息失败，一定要告诉服务器解锁是失败的。启动手动ACK机制
     * @param to
     *
     */
    @Override
    public void unlockStock(StockLockedTo to) {
        StockDetailTo detail = to.getDetailTo();
        Long detailId = detail.getId();

        /**
         * 1、查询数据库关于这个订单的锁库存消息
         *    有，证明库存锁定成功了。
         *      1、没有这个订单。必须解锁
         *      2、有这个订单。不是解锁库存。
         *          订单状态：已取消：解锁库存
         *          订单状态：没取消：不能解锁
         *    没有，库存锁定失败了，库存回滚了。这种情况无需解锁
         */

        WareOrderTaskDetailEntity byId = wareOrderTaskDetailService.getById(detailId);
        if (byId != null) {
            Long id = to.getId();   // 库存工作单的Id，拿到订单号
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();   // 根据订单号查询订单的状态
            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0) {
                // 订单数据返回成功
                OrderVo data = r.getData(new TypeReference<OrderVo>() {
                });
                if (data == null || data.getStatus() == 4) {
                    // 订单不存在、订单已经被取消了，才能解锁库存
                    if (byId.getLockStatus() == 1){
                        // 当前库存工作单详情，状态1 已锁定但是未解锁才可以解锁
                        unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                    }
                } else {
                    // 消息拒绝以后重新放到队列里面，让别人继续消费解锁
                    throw new RuntimeException("远程服务失败");
                }
            }
        } else {
            // 无需解锁
        }
    }

    @Override
    public void unlockStock(OrderTo orderTo) {
        //为防止重复解锁，需要重新查询工作单
        String orderSn = orderTo.getOrderSn();
        WareOrderTaskEntity taskEntity = wareOrderTaskService.getBaseMapper().selectOne((new QueryWrapper<WareOrderTaskEntity>().eq("order_sn", orderSn)));
        //查询出当前订单相关的且处于锁定状态的工作单详情
        List<WareOrderTaskDetailEntity> lockDetails = wareOrderTaskDetailService.list(
                new QueryWrapper<WareOrderTaskDetailEntity>()
                        .eq("task_id", taskEntity.getId())
                        .eq("lock_status", 1));
        for (WareOrderTaskDetailEntity lockDetail : lockDetails) {
            unLockStock(lockDetail.getSkuId(),lockDetail.getWareId(),lockDetail.getSkuNum(),lockDetail.getId());
        }
    }

    /**
     * 解库存锁
     *
     * @param skuId        商品id
     * @param wareId       仓库id
     * @param num          解锁数量
     * @param taskDetailId 库存工作单ID
     */
    private void unLockStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        // 库存解锁
        wareSkuDao.unlockStock(skuId, wareId, num);
        // 更新库存工作单的状态
        WareOrderTaskDetailEntity wareOrderTaskDetailEntity = WareOrderTaskDetailEntity.builder()
                .id(taskDetailId)
                .lockStatus(2)
                .build();
        wareOrderTaskDetailService.updateById(wareOrderTaskDetailEntity);
    }

    @Data
    class SkuWareHashStock {
        private Long skuId;     // skuid
        private Integer num;    // 锁定件数
        private List<Long> wareIds;  // 锁定仓库id
    }

    @Autowired
    WareOrderTaskService wareOrderTaskService;

    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    OrderFeignService orderFeignService;


}