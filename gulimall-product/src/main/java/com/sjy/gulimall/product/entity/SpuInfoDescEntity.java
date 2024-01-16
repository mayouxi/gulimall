package com.sjy.gulimall.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * spu??Ϣ???
 * 
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 13:43:11
 */
@Data
@TableName("pms_spu_info_desc")
public class SpuInfoDescEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * ??Ʒid
	 */
	@TableId(type = IdType.INPUT)
	private Long spuId;
	/**
	 * ??Ʒ???
	 */
	private String decript;

}
