package com.sjy.gulimall.ware.vo;

import com.sjy.common.to.MemberAddressVo;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareVo {
    private MemberAddressVo addressVo;
    private BigDecimal fare;
}