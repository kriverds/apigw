package com.soluvis.ds.apigw.v1.biz.vars.eai.vo;

import com.soluvis.ds.apigw.v1.biz.common.eai.vo.BaseReq;
import com.soluvis.ds.apigw.v1.util.LangUtil;

import lombok.Getter;
import lombok.ToString;

/**
 * @Class 		: IVZZMOARSH001Req
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  IF-IS-0004(보이는ARS 콜센터현황조회) Request VO Class
 */
@ToString
public class IVZZMOARSH001Req implements BaseReq {
							//No Name
	String mrp;				//1  MRP
	String waitCustCnt;		//2  대기고객수
	String workTimeYn;		//3  근무시간여부
	String rsltCd;			//4  결과코드
	String msg;				//5  메세지
	
	static final int LEN_MRP 			= 5;
	static final int LEN_WAIT_CUST_CNT 	= 5;
	static final int LEN_WORK_TIME_YN 	= 1;
	static final int LEN_RSLT_CD 		= 4;
	static final int LEN_MSG 			= 10;
	@Getter
	static final int HEADER_DATA_LEN = 25;
	
	@Getter
	static final String HEADER_KEY = "201803191000IVR103376709";
	@Getter
	static final String HEADER_INTF_ID = "IVZZMOARSH001";
	@Getter
	static final String HEADER_SVC_ID = "CSST0010";

	private IVZZMOARSH001Req(Builder builder) {
		mrp 			= LangUtil.rSpacePad(builder.mrp, LEN_MRP);
		waitCustCnt 	= LangUtil.lPad(builder.waitCustCnt, "0", LEN_WAIT_CUST_CNT);
		workTimeYn 		= LangUtil.rSpacePad(builder.workTimeYn, LEN_WORK_TIME_YN);
		rsltCd 			= LangUtil.rSpacePad("", LEN_RSLT_CD);
		msg 			= LangUtil.rSpacePad("", LEN_MSG);
	}
	
	@Override
	public String toEaiString() {
		StringBuilder sb = new StringBuilder();
		sb.append(mrp).append(waitCustCnt).append(workTimeYn).append(rsltCd).append(msg);
		return sb.toString();
	}
	
	public static class Builder {
		private String mrp;
		private String waitCustCnt;
		private String workTimeYn;
		
		public Builder mrp(String mrp) {
			this.mrp = mrp;
			return this;
		}
		public Builder waitCustCnt(String waitCustCnt) {
			this.waitCustCnt = waitCustCnt;
			return this;
		}
		public Builder workTimeYn(String workTimeYn) {
			this.workTimeYn = workTimeYn;
			return this;
		}
		
		public IVZZMOARSH001Req build() {
			return new IVZZMOARSH001Req(this);
		}
	}

}
