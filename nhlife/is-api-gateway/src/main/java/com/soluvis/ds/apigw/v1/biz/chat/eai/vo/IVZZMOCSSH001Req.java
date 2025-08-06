package com.soluvis.ds.apigw.v1.biz.chat.eai.vo;

import com.soluvis.ds.apigw.v1.biz.common.eai.vo.BaseReq;
import com.soluvis.ds.apigw.v1.util.LangUtil;

import lombok.Getter;
import lombok.ToString;

/**
 * @Class 		: IVZZMOCSSH001Req
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  IF-IS-0005(통합통계 채팅상담 연동) Request VO Class
 */
@ToString
public class IVZZMOCSSH001Req implements BaseReq {
						//No Name
	String domain;		//1  도메인
	String nodeAlias;	//2  카테고리별칭
	String fromDate;	//3  조회시작일자
	String fromTime;	//4  조회시작시간, HHMI(분:00/15/30/45)
	String toDate;		//5  조회종료일자
	String toTime;		//6  조회종료시간, HHMI(분:00/15/30/45)
	
	static final int LEN_DOMAIN 		= 14;
	static final int LEN_NODE_ALIAS 	= 20;
	static final int LEN_FROM_DATE 		= 8;
	static final int LEN_FROM_TIME 		= 4;
	static final int LEN_TO_DATE 		= 8;
	static final int LEN_TO_TIME		= 4;
	@Getter
	static final int HEADER_DATA_LEN = 58;
	
	@Getter
	static final String HEADER_KEY = "201611091618IVR103376709";
	@Getter
	static final String HEADER_INTF_ID = "IVZZMOCSSH001";
	@Getter
	static final String HEADER_SVC_ID = "CSST0030";
	
	private IVZZMOCSSH001Req(Builder builder) {
		domain 		= LangUtil.rSpacePad(builder.domain, LEN_DOMAIN);
		nodeAlias 	= LangUtil.rSpacePad(builder.nodeAlias, LEN_NODE_ALIAS);
		fromDate 	= LangUtil.rSpacePad(builder.fromDate, LEN_FROM_DATE);
		fromTime 	= LangUtil.rSpacePad(builder.fromTime, LEN_FROM_TIME);
		toDate 		= LangUtil.rSpacePad(builder.toDate, LEN_TO_DATE);
		toTime 		= LangUtil.rSpacePad(builder.toTime, LEN_TO_TIME);
	}
	
	@Override
	public String toEaiString() {
		StringBuilder sb = new StringBuilder();
		sb.append(domain).append(nodeAlias).append(fromDate).append(fromTime).append(toDate)
		.append(toTime);
		return sb.toString();
	}
	
	public static class Builder {
		String domain;
		String nodeAlias;
		String fromDate;
		String fromTime;
		String toDate;
		String toTime;
		
		public Builder domain(String domain) {
			this.domain = domain;
			return this;
		}
		public Builder nodeAlias(String nodeAlias) {
			this.nodeAlias = nodeAlias;
			return this;
		}
		public Builder fromDate(String fromDate) {
			this.fromDate = fromDate;
			return this;
		}
		public Builder fromTime(String fromTime) {
			this.fromTime = fromTime;
			return this;
		}
		public Builder toDate(String toDate) {
			this.toDate = toDate;
			return this;
		}
		public Builder toTime(String toTime) {
			this.toTime = toTime;
			return this;
		}
		
		public IVZZMOCSSH001Req build() {
			return new IVZZMOCSSH001Req(this);
		}
	}

}
