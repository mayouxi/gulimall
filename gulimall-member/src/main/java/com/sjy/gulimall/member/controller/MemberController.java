package com.sjy.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.sjy.gulimall.member.feign.CouponFeignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sjy.gulimall.member.entity.MemberEntity;
import com.sjy.gulimall.member.service.MemberService;
import com.sjy.common.utils.PageUtils;
import com.sjy.common.utils.R;



/**
 * ??Ա
 *
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 15:07:52
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Autowired
    private CouponFeignService couponFeignService; //注入刚才的CouponFeignService接口

    @RequestMapping("/coupons")
    public R coupons(){
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("会员昵称张三");
        R membercoupons = couponFeignService.memberList();

        return R.ok().put("member", memberEntity).put("coupons", membercoupons.get("coupons"));
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
