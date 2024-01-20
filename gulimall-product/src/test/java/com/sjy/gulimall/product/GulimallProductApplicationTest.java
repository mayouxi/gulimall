package com.sjy.gulimall.product;

import com.sjy.gulimall.product.dao.BrandDao;
import com.sjy.gulimall.product.entity.BrandEntity;
import com.sjy.gulimall.product.service.BrandService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallProductApplicationTest {

    @Autowired
    BrandService brandService;


    @Autowired
    StringRedisTemplate redisTemplate;
    @Test
    public void testRedis() {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        // 保存
        ops.set("hello", "world_" + UUID.randomUUID().toString());
        // 查询
        String hello = ops.get("hello");
        System.out.println(hello);

    }
    @Test
    public void contextLoads(){
        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setName("华为");
        brandService.save(brandEntity);
        System.out.println("保存成功");
    }

}
