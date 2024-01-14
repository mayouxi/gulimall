package com.sjy.gulimall.member.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * ??Ա?ղص?ר???
 * 
 * @author sunjiayang
 * @email 2785631446@qq.com
 * @date 2024-01-14 15:07:59
 */
@Data
@TableName("ums_member_collect_subject")
public class MemberCollectSubjectEntity implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	@TableId
	private Long id;
	/**
	 * subject_id
	 */
	private Long subjectId;
	/**
	 * subject_name
	 */
	private String subjectName;
	/**
	 * subject_img
	 */
	private String subjectImg;
	/**
	 * ?url
	 */
	private String subjectUrll;

}
