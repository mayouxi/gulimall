package com.sjy.gulimall.thirdparty;

import com.sjy.gulimall.thirdparty.utils.MinioUtils;
import io.minio.errors.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.validation.constraints.Min;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallThirdPartyApplicationTest {
    @Test
    public void test() throws ServerException, InvalidBucketNameException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, RegionConflictException {
        boolean b = MinioUtils.bucketExists();
        System.out.println("ok123" + b);

//        MinioUtils.createBucket("sjy");
//        MinioUtils.putObject("sjy", "tet.java","/Users/mayouxi/IdeaProjects/gulimall/gulimall-third-party/src/test/java/com/sjy/gulimall/thirdparty/GulimallThirdPartyApplicationTest.java");
    }

}
