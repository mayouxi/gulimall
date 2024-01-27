package com.sjy.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.sjy.common.exception.NoStockException;
import com.sjy.common.to.MemberResponseVo;
import com.sjy.common.to.mq.OrderTo;
import com.sjy.common.utils.R;
import com.sjy.gulimall.order.entity.OrderItemEntity;
import com.sjy.gulimall.order.enume.OrderStatusEnum;
import com.sjy.gulimall.order.feign.CartFeignService;
import com.sjy.gulimall.order.feign.MemberFeignService;
import com.sjy.gulimall.order.feign.ProductFeignService;
import com.sjy.gulimall.order.feign.WareFeignService;
import com.sjy.gulimall.order.intercept.LoginUserInterceptor;
import com.sjy.gulimall.order.service.OrderItemService;
import com.sjy.gulimall.order.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sjy.common.utils.PageUtils;
import com.sjy.common.utils.Query;

import com.sjy.gulimall.order.dao.OrderDao;
import com.sjy.gulimall.order.entity.OrderEntity;
import com.sjy.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import static com.sjy.gulimall.order.constant.OrderConstant.USER_ORDER_TOKEN_PREFIX;


@Service("orderService")
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        // 1.获取登录用户信息
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        // 2.封装订单
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        // 我们要从request里获取用户数据，但是其他线程是没有这个信息的，
        // 所以可以手动设置新线程里也能共享当前的request数据
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        // 1.远程查询所有的收获地址列表
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            // 因为异步线程需要新的线程，而新的线程里没有request数据，所以我们自己设置进去
            RequestContextHolder.setRequestAttributes(attributes);

            List<MemberAddressVo> address;
            try {
                address = memberFeignService.getAddress(memberResponseVo.getId());
                confirmVo.setAddressVos(address);
            } catch (Exception e) {
                log.warn("\n远程调用会员服务失败 [会员服务可能未启动]");
            }
        }, executor);
        // 2. 远程查询购物车服务，并得到每个购物项是否有库存
        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            // 异步线程共享 RequestContextHolder.getRequestAttributes()
            RequestContextHolder.setRequestAttributes(attributes);

            // feign在远程调用之前要构造请求 调用很多拦截器
            // 远程获取用户的购物项
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);
        }, executor).thenRunAsync(() -> {
            RequestContextHolder.setRequestAttributes(attributes);
            List<OrderItemVo> items = confirmVo.getItems();
            // 获取所有商品的id
            List<Long> skus = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            R hasStock = wmsFeignService.getSkusHasStock(skus);
            List<SkuStockVo> data = hasStock.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if (data != null) {
                // 各个商品id 与 他们库存状态的映射map // 学习下收集成map的用法
                Map<Long, Boolean> stocks = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(stocks);
            }
        }, executor);
        // 3.查询用户积分
        Integer integration = memberResponseVo.getIntegration();
        confirmVo.setIntegration(integration);

        // 4.其他数据在类内部自动计算

        // 5.防重令牌 设置用户的令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        confirmVo.setOrderToken(token);
        // redis中添加用户id，这个设置可以防止订单重复提交。生成完一次订单后删除redis
        stringRedisTemplate.opsForValue().set(USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId(), token, 10, TimeUnit.MINUTES);
        // 等待所有异步任务完成
        CompletableFuture.allOf(getAddressFuture, cartFuture).get();
        return confirmVo;
    }

    ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();

    /**
     * 提交订单
     *
     * @param vo
     * @return
     */
    // @Transactional(isolation = Isolation.READ_COMMITTED) 设置事务的隔离级别
    // @Transactional(propagation = Propagation.REQUIRED)   设置事务的传播级别
    @Transactional(rollbackFor = Exception.class)
    // @GlobalTransactional(rollbackFor = Exception.class)
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {

        confirmVoThreadLocal.set(vo);

        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        //去创建、下订单、验令牌、验价格、锁定库存...

        // 1.从拦截器中获取当前用户登录的信息
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        responseVo.setCode(0);

        // 2.redis验签
        Long result = checkRedisToken(vo, memberResponseVo);

        if (result == 0L) {
            //令牌验证失败
            responseVo.setCode(1);
            return responseVo;
        } else {
            //令牌验证成功
            //1、创建订单、订单项等信息
            OrderCreateTo order = createOrder();

            //2、验证价格
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();

            if (true || Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                //金额对比
                //TODO 3、保存订单
                saveOrder(order);

                //4、库存锁定,只要有异常，回滚订单数据
                //订单号、所有订单项信息(skuId,skuNum,skuName)
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());

                //获取出要锁定的商品数据信息
                List<OrderItemVo> orderItemVos = order.getOrderItems().stream().map((item) -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(orderItemVos);

                //TODO 调用远程锁定库存的方法
                //出现的问题：扣减库存成功了，但是由于网络原因超时，出现异常，导致订单事务回滚，库存事务不回滚(解决方案：seata)
                //为了保证高并发，不推荐使用seata，因为是加锁，并行化，提升不了效率,可以发消息给库存服务
                R r = wmsFeignService.orderLockStock(lockVo);
                if (r.getCode() == 0) {
                    //锁定成功
                    responseVo.setOrder(order.getOrder());
                    // int i = 10/0;

                    //TODO 订单创建成功，发送消息给MQ
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());

                    //删除购物车里的数据
                    stringRedisTemplate.delete(CART_PREFIX + memberResponseVo.getId());
                    return responseVo;
                } else {
                    //锁定失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                    // responseVo.setCode(3);
                    // return responseVo;
                }

            } else {
                responseVo.setCode(2);
                return responseVo;
            }
        }
    }

    private Long checkRedisToken(OrderSubmitVo vo, MemberResponseVo memberResponseVo) {
        //2、验证令牌是否合法【令牌的对比和删除必须保证原子性】
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();

        //通过lure脚本原子验证令牌和删除令牌
        Long result = stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                Arrays.asList(USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId()),
                orderToken);
        return result;
    }

    /**
     * 保存订单所有数据
     *
     * @param orderCreateTo
     */
    private void saveOrder(OrderCreateTo orderCreateTo) {

        //获取订单信息
        OrderEntity order = orderCreateTo.getOrder();
        order.setModifyTime(new Date());
        order.setCreateTime(new Date());
        //保存订单
        this.baseMapper.insert(order);

        //获取订单项信息
        List<OrderItemEntity> orderItems = orderCreateTo.getOrderItems();
        //批量保存订单项数据
        orderItemService.saveBatch(orderItems);
    }


    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity order_sn = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return order_sn;
    }

    /**
     * 订单的关闭
     * @param entity
     */
    @Override
    public void closeOrder(OrderEntity entity) {
        // 1、查询当前这个订单的最新状态
        OrderEntity orderEntity = this.getById(entity.getId());
        if (Objects.equals(orderEntity.getStatus(), OrderStatusEnum.CREATE_NEW.getCode())) {
            // 2、关单
            OrderEntity update = new OrderEntity();
            update.setId(entity.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity, orderTo);
            try {
                //TODO 确保每个消息发送成功，给每个消息做好日志记录，(给数据库保存每一个详细信息)保存每个消息的详细信息
                // 给库存队列发消息
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
            } catch (Exception e) {
                //TODO 定期扫描数据库，重新发送失败的消息
            }
        }
    }

    /**
     * 创建订单、订单项等信息
     *
     * @return
     */
    private OrderCreateTo createOrder() {
        OrderCreateTo createTo = new OrderCreateTo();
        // 1、生成一个订单号。IdWorker.getTimeId()是Mybatis提供的生成订单号方法，ID=Time+Id
        String orderSn = IdWorker.getTimeId();
        // 2、构建一个订单
        OrderEntity orderEntity = buildOrder(orderSn);
        // 3、获取到所有的订单项
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);
        // 4、计算价格、积分等相关信息
        computePrice(orderEntity, itemEntities);

        createTo.setOrder(orderEntity);
        createTo.setOrderItems(itemEntities);
        return createTo;
    }

    /**
     * 构建订单
     *
     * @param orderSn
     * @return
     */
    private OrderEntity buildOrder(String orderSn) {
        MemberResponseVo respVp = LoginUserInterceptor.loginUser.get();
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        entity.setMemberId(respVp.getId());

        OrderSubmitVo orderSubmitVo = confirmVoThreadLocal.get();
        // 1、获取运费 和 收货信息
        R fare = wmsFeignService.getFare(orderSubmitVo.getAddrId());
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
        });
        // 2、设置运费
        entity.setFreightAmount(fareResp.getFare());
        // 3、设置收货人信息
        entity.setReceiverPostCode(fareResp.getAddressVo().getPostCode());
        entity.setReceiverProvince(fareResp.getAddressVo().getProvince());
        entity.setReceiverRegion(fareResp.getAddressVo().getRegion());
        entity.setReceiverCity(fareResp.getAddressVo().getCity());
        entity.setReceiverDetailAddress(fareResp.getAddressVo().getDetailAddress());
        entity.setReceiverName(fareResp.getAddressVo().getName());
        entity.setReceiverPhone(fareResp.getAddressVo().getPhone());
        // 4、设置订单的相关状态信息
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        // 5、默认取消信息
        entity.setAutoConfirmDay(7);
        return entity;
    }

    /**
     * 构建所有订单项数据
     *
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        // 最后确定每个购物项的价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (currentUserCartItems != null && currentUserCartItems.size() > 0) {
            List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity itemEntity = buildOrderItem(cartItem);
                itemEntity.setOrderSn(orderSn);
                return itemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }
        return null;
    }

    /**
     * 构建某一个订单项
     *
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        // 1、订单信息：订单号 v
        // 2、商品的spu信息
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(data.getId());
        itemEntity.setSpuBrand(data.getBrandId().toString());
        itemEntity.setSpuName(data.getSpuName());
        itemEntity.setCategoryId(data.getCatalogId());
        // 3、商品的sku信息  v
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        itemEntity.setSkuQuantity(cartItem.getCount());
        itemEntity.setSkuAttrsVals(StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";"));
        // 4、优惠信息【不做】
        // 5、积分信息
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        // 6、订单项的价格信息
        itemEntity.setPromotionAmount(new BigDecimal("0"));
        itemEntity.setCouponAmount(new BigDecimal("0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0"));
        // 当前订单项的实际金额 总额-各种优惠
        BigDecimal orign = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        BigDecimal subtract = orign.subtract(itemEntity.getCouponAmount()).
                subtract(itemEntity.getCouponAmount()).
                subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(subtract);
        return itemEntity;
    }

    /**
     * 计算价格
     *
     * @param orderEntity
     * @param itemEntities
     */
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        BigDecimal gift = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");
        // 1、订单的总额，叠加每一个订单项的总额信息
        for (OrderItemEntity entity : itemEntities) {
            total = total.add(entity.getRealAmount());
            coupon = coupon.add(entity.getCouponAmount());
            integration = integration.add(entity.getIntegrationAmount());
            promotion = promotion.add(entity.getPromotionAmount());
            gift = gift.add(new BigDecimal(entity.getGiftIntegration()));
            growth = growth.add(new BigDecimal(entity.getGiftGrowth()));
        }
        // 订单总额
        orderEntity.setTotalAmount(total);
        // 应付总额
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setPromotionAmount(promotion);
        // 设置积分等信息
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());
        orderEntity.setDeleteStatus(0);//0 未删除
    }

    @Autowired
    ExecutorService executor;

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    WareFeignService wmsFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    OrderItemService orderItemService;

    private final String CART_PREFIX = "gulimall:cart:";
}