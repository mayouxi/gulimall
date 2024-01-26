package com.sjy.gulimall.authServer.controller;

import com.alibaba.fastjson.TypeReference;
import com.sjy.common.constant.AuthServerConstant;
import com.sjy.common.exception.BizCodeEnum;
import com.sjy.common.to.MemberResponseVo;
import com.sjy.common.utils.R;
import com.sjy.gulimall.authServer.feign.MemberFeignService;
import com.sjy.gulimall.authServer.vo.UserLoginVo;
import com.sjy.gulimall.authServer.vo.UserRegisterVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.sjy.common.constant.AuthServerConstant.LOGIN_USER;

@Controller
public class LoginController {
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    MemberFeignService memberFeignService;

    @GetMapping(value = "/sms/sendCode")
    @ResponseBody
    public R sendCode(@RequestParam("phone") String phone) {
        String code_time = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (!StringUtils.isEmpty(code_time)) {
            long time = Long.parseLong(code_time.split("_")[1]);
            if (System.currentTimeMillis() - time < 60 * 1000) {
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }
        int code = (int) ((Math.random() * 9 + 1) * 100000);
        System.out.println("手机号:" + phone);
        System.out.println("验证码：" + code);
        String codeNum = String.valueOf(code);
        String redisStorage = codeNum + "_" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, redisStorage, 10, TimeUnit.MINUTES);
        return R.ok();
    }


    //BindingResult参数获取校验结果
//RedirectAttributes可以通过session保存信息并在重定向的时候携带过去。这里用于存错误消息
    @PostMapping(value = "/register")
    public String register(@Valid UserRegisterVo vos, BindingResult result,
                           RedirectAttributes attributes) {

        //如果有错误回到注册页面
        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream().collect(
                    Collectors.toMap(k -> k.getField(), k -> k.getDefaultMessage()));
//            flash是一闪而过，此数据只取一次
            attributes.addFlashAttribute("errors", errors);

            //效验出错，重定向到注册页面。不用转发是为了防止刷新时重复提交表单
            // 不用return reg是因为本来就在注册页面点击发送了这个注册请求，要重定向清空表单
            return "redirect:http://auth.gulimall.com/reg.html";
        }
        //1、效验验证码
        String code = vos.getCode();

        //获取存入Redis里的验证码
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vos.getPhone());
        if (!StringUtils.isEmpty(redisCode)) {
            // 判断验证码是否正确【有BUG，如果字符串存储有问题，没有解析出code，数据为空，导致验证码永远错误】
            if (code.equals(redisCode.split("_")[0])) {
                //删除验证码（不可重复使用）;令牌机制
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vos.getPhone());
                //验证码通过，真正注册，调用远程服务进行注册【会员服务】
                R register = memberFeignService.register(vos);
                if (register.getCode() == 0) {
                    //成功，重定向到登录页面
                    return "redirect:http://auth.gulimall.com/login.html";
                } else {
                    //失败
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg", register.getData("msg", new TypeReference<String>() {
                    }));
                    attributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
            } else {
                //验证码错误
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                attributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        } else {
            // redis中验证码过期
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码过期");
            attributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }

    /**
     * @param attributes 放错误消息
     * @param session            将登录用户信息写入session并重定向到主页，右上角显示你好，Xxx。
     *                           整合了SpringSession，扩大了session作用域到"gulimall.com"，解决了session共享问题，
     * @return 路由到的页面，不加@ResponseBody，防止返回值是字符串
     */
    @GetMapping(value = "/login/{username}/{password}")
    public String login(@PathVariable("username") String username,@PathVariable("password") String password, RedirectAttributes attributes, HttpSession session) {
        UserLoginVo vo = new UserLoginVo(username, password);
        //远程登录
        R login = memberFeignService.login(vo);

        if (login.getCode() == 0) {
//登陆成功，将登录者信息放到session中，重定向到首页。
            MemberResponseVo data = login.getData("data", new TypeReference<MemberResponseVo>() {
            });
            session.setAttribute(LOGIN_USER, data);
            return "redirect:http://gulimall.com";
        } else {
//登陆失败，将错误消息添加到attributes，重定向到登录页。
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", login.getData("msg", new TypeReference<String>() {
            }));
            attributes.addFlashAttribute("errors", errors);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }

    @GetMapping(value = "/login.html")
    public String loginPage(HttpSession session) {

        //从session先取出来用户的信息，判断用户是否已经登录过了
        Object attribute = session.getAttribute(LOGIN_USER);
        //如果用户没登录那就跳转到登录页面
        if (attribute == null) {
            return "login";
        } else {
            System.out.println(attribute);
            return "redirect:http://gulimall.com";
        }

    }

}
 