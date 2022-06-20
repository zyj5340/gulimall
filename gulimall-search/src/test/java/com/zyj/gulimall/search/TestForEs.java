package com.zyj.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.zyj.gulimall.search.config.ElasticConfig;
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

/**
 * @author Stone
 * @date 2022/5/13
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class TestForEs {
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 存储数据
     */
    @Test
    public void indexData() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");
        User user = new User();
        user.setUsername("张三");
        user.setGender("男");
        user.setAge(18);
        String s = JSON.toJSONString(user);
        indexRequest.source(s, XContentType.JSON);
        //执行
        IndexResponse index = restHighLevelClient.index(indexRequest, ElasticConfig.COMMON_OPTIONS);
        System.out.println(index);
    }

    @Data
    public class User{
        private String username;
        private String gender;
        private Integer age;
    }
}
