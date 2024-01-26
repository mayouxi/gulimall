package com.sjy.gulimall.cart.Service.Impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.sjy.common.utils.R;
import com.sjy.gulimall.cart.Service.CartService;
import com.sjy.gulimall.cart.feign.ProductFeignService;
import com.sjy.gulimall.cart.intercept.CartInterceptor;
import com.sjy.gulimall.cart.vo.CartItemVo;
import com.sjy.gulimall.cart.vo.CartVo;
import com.sjy.gulimall.cart.vo.SkuInfoVo;
import com.sjy.gulimall.cart.vo.UserInfoTo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    StringRedisTemplate redisTemplate;
    // 用户标识前缀
    private final String CART_PREFIX = "gulimall:cart:";
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    ExecutorService executorService;

    @Override
    public CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOperations();
        String productRedisValue = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(productRedisValue)) {
            CartItemVo cartItemVo = new CartItemVo();
            CompletableFuture<Void> getSkuInfoFuture = CompletableFuture.runAsync(() -> {
                R productSkuInfo = productFeignService.getSkuInfo(skuId);
                SkuInfoVo skuInfo = productSkuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });
                cartItemVo.setSkuId(skuInfo.getSkuId());
                cartItemVo.setTitle(skuInfo.getSkuTitle());
                cartItemVo.setImage(skuInfo.getSkuDefaultImg());
                cartItemVo.setPrice(skuInfo.getPrice());
                cartItemVo.setCount(num);
            }, executorService);

            CompletableFuture<Void> getSkuAttrFuture = CompletableFuture.runAsync(() -> {
                List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItemVo.setSkuAttr(skuSaleAttrValues);
            }, executorService);
            CompletableFuture.allOf(getSkuAttrFuture, getSkuInfoFuture).get();
            String jsonString = JSON.toJSONString(cartItemVo);
            cartOps.put(skuId.toString(), jsonString);
            return cartItemVo;
        } else {
            CartItemVo cartItemVo = JSON.parseObject(productRedisValue, CartItemVo.class);
            cartItemVo.setCount(cartItemVo.getCount() + num);
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItemVo));
            return cartItemVo;
        }
    }

    @Override
    public CartItemVo getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOperations();
        String str = (String) cartOps.get(skuId.toString());
        CartItemVo cartItem = JSON.parseObject(str, CartItemVo.class);
        return cartItem;
    }

    /**
     * 获取购物车所有数据
     *
     * @return
     */
    @Override
    public CartVo getCart() throws ExecutionException, InterruptedException {
        CartVo cartVo = new CartVo();
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();
        if (userInfoTo.getUserId() != null) {
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItemVo> tempCartItems = getCartItems(tempCartKey);
            // 临时购物车有数据，合并
            if (tempCartItems != null && !tempCartItems.isEmpty()) {
                for (CartItemVo cartItemVo : tempCartItems) {
                    addToCart(cartItemVo.getSkuId(), cartItemVo.getCount());
                }
                clearCart(tempCartKey);
            }
            cartVo.setItems(getCartItems(cartKey));
        } else {
            cartVo.setItems(getCartItems(CART_PREFIX + userInfoTo.getUserKey()));
        }
        return cartVo;
    }

    private List<CartItemVo> getCartItems(String cartKey) {
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        List<Object> values = operations.values();
        if (values != null && values.size() > 0) {
            List<CartItemVo> collection = values.stream().map(item -> {
                String obj = (String) item;
                CartItemVo cartItemVo = JSON.parseObject(obj, CartItemVo.class);
                return cartItemVo;
            }).collect(Collectors.toList());
            return collection;
        }
        return null;
    }


    @Override
    public void clearCart(String cartKey) {
        // 直接删除该键
        redisTemplate.delete(cartKey);
    }

    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> operations = getCartOperations();
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCheck(check == 1);
        operations.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    @Override
    public void countItem(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> cartOperations = getCartOperations();
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        cartOperations.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    /**
     * 删除购物项
     *
     * @param skuId
     */
    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOperations();
        cartOps.delete(skuId.toString());
    }

    @Override
    public List<CartItemVo> getUserCartItems() {
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();
        if (userInfoTo == null) {
            return null;
        } else {
            List<CartItemVo> collect = getCartItems(CART_PREFIX + userInfoTo.getUserId()).stream()
                    .filter(CartItemVo::getCheck)
                    .map(item -> {
                        // TODO 1、更新为最新价格
                        R price = productFeignService.getPrice(item.getSkuId());
                        String data = (String) price.get("data");
                        item.setPrice(new BigDecimal(data));
                        return item;
                    })
                    .collect(Collectors.toList());
            return collect;
        }
    }


    public BoundHashOperations<String, Object, Object> getCartOperations() {
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        return operations;
    }
}
