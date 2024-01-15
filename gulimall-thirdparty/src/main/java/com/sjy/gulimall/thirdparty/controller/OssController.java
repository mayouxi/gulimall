package com.sjy.gulimall.thirdparty.controller;

import com.sjy.common.utils.R;
import com.sjy.gulimall.thirdparty.utils.MinioUtils;
import io.minio.errors.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("oss")
public class OssController {
    /*
       上传新闻封面图
    */
    @PostMapping(value = "/uploadNewsImg")  //前端提交文件，默认是键值对提交，它的键默认为file
    public R uploadNewsImg(@RequestParam("file") MultipartFile file) throws ServerException, InvalidBucketNameException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException, InvalidExpiresRangeException {
        InputStream inputStream = file.getInputStream();
        MinioUtils.putObject("gulimall", file.getOriginalFilename(), inputStream);
        String url = MinioUtils.getUrl("gulimall",
                file.getOriginalFilename());
        return R.ok(url);
    }

}
