package com.sjy.gulimall.product.feign;

import com.sjy.common.to.SkuHasStockVo;
import com.sjy.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


@FeignClient("gulimall-ware")
public interface WareFeignService {

    @PostMapping(value = "/ware/waresku/hasStock")
    R getSkuHasStocks(@RequestBody List<Long> skuIds);

}
