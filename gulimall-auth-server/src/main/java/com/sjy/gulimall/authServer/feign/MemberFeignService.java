package com.sjy.gulimall.authServer.feign;

import com.sjy.common.utils.R;
import com.sjy.gulimall.authServer.vo.UserLoginVo;
import com.sjy.gulimall.authServer.vo.UserRegisterVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-member")
public interface MemberFeignService {
    @PostMapping(value = "/member/member/register")
    R register(@RequestBody UserRegisterVo vo);

    @PostMapping(value = "/member/member/login")
    R login(@RequestBody UserLoginVo vo);

}
 