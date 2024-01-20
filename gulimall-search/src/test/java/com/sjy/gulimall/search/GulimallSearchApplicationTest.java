package com.sjy.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.sjy.gulimall.search.config.GulimallElasticSearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GulimallSearchApplicationTest {

    @Autowired
    RestHighLevelClient client;

    @Test
    public void indexData() throws IOException {

        // 设置索引
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");

        User user = new User();
        user.setUserName("张三");
        user.setAge(20);
        user.setGender("男");
        String jsonString = JSON.toJSONString(user);

        //设置要保存的内容，指定数据和类型
        indexRequest.source(jsonString, XContentType.JSON);

        //执行创建索引和保存数据
        IndexResponse index = client.index(indexRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);

        System.out.println(index);

    }

    @Data
    class User {
        int age;
        String gender;
        String userName;
    }

}
