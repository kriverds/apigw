package com.soluvis.ds.apigw.v1.biz.apps.eai.vo;

import com.soluvis.ds.apigw.v1.biz.common.eai.vo.BaseReq;
import com.soluvis.ds.apigw.v1.util.LangUtil;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class 		: IVZZMANBSH003Req
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  IF-IS-0001(청약진행현황조회-실적) Request VO Class
 */
@ToString
public class IVZZMANBSH003Req implements BaseReq {
						//No Name
	String msgCd;		//1  메시지코드
	String msgCont;		//2  메시지내용
	String inqStdt;		//3  조회시작일자
	String inqEddt;		//4  조회종료일자
	String inqDcd;		//5  조회구분
	String brCd;		//6  사무소코드
	String cntPerPage;	//7  페이지당건수
	@Setter
	String nextKey;		//8  다음조회키
	String nextYn;		//9  다음여부
	String pageCnt;		//10 반복건수
	
	String poliNo;		//11 증권번호
	String lschgDtm;	//12 최종변경일시
	String prodLccd;	//13 상품대분류코드
	String cntrDt;		//14 계약일자
	String psDt;		//15 청약일자
	String susTrmsDtm;	//16 청약전송일시
	String exDt;		//17 소멸일자
	String cntrScd;		//18 계약상태코드
	String cntrDtlScd;	//19 계약상세상태코드
	String nbptMbp;		//20 신계약시점월납기준보험료
	String nbptMmCvav;	//21 신계약시점월환산성적
	String clctBrofBrCd;//22 모집지점사무소코드
	String clpPsnNo;	//23 모집자개인번호
	String prodNm;		//24 상품명
	String rvcyCd;		//25 납입주기코드
	String cntrmNm;		//26 공제계약자성명
	String isrdNm;		//27 피보험자성명
	String tprm;		//28 합계보험료
	String uwRsltCd;	//29 인수심사결과코드명
	
	static final int LEN_MSG_CD 		= 7;
	static final int LEN_MSG_CONT 		= 1500;
	static final int LEN_INQ_STDT 		= 8;
	static final int LEN_INQ_EDDT 		= 8;
	static final int LEN_INQ_DCD 		= 1;
	static final int LEN_BR_CD 			= 6;
	static final int LEN_CNT_PER_PAGE 	= 9;
	static final int LEN_NEXT_KEY 		= 500;
	static final int LEN_NEXT_YN 		= 1;
	static final int LEN_PAGE_CNT 		= 3;
	static final int LEN_POLI_NO 		= 15;
	static final int LEN_LSCHG_DTM 		= 17;
	static final int LEN_PROD_LCCD 		= 1;
	static final int LEN_CNTR_DT 		= 8;
	static final int LEN_PS_DT 			= 8;
	static final int LEN_SUS_TRMS_DTM 	= 17;
	static final int LEN_EX_DT 			= 8;
	static final int LEN_CNTR_SCD 		= 2;
	static final int LEN_CNTR_DTL_SCD 	= 4;
	static final int LEN_NBPT_MBP 		= 20;
	static final int LEN_NBPT_MM_CVAV 	= 20;
	static final int LEN_CLCT_BROF_BR_CD= 6;
	static final int LEN_CLP_PSN_NO 	= 9;
	static final int LEN_PROD_NM 		= 100;
	static final int LEN_RVCY_CD 		= 2;
	static final int LEN_CNTRM_NM 		= 100;
	static final int LEN_ISRD_NM 		= 100;
	static final int LEN_TPRM 			= 20;
	static final int LEN_UW_RSLT_CD 	= 100;
	@Getter
	static final int HEADER_DATA_LEN = 2600;
	
	@Getter
	static final String HEADER_KEY = "201701091800IVR103376709";
	@Getter
	static final String HEADER_INTF_ID = "IVZZMANBSH003";
	@Getter
	static final String HEADER_SVC_ID = "NBLAS526";
	
	private IVZZMANBSH003Req(Builder builder) {
		msgCd 		= LangUtil.rSpacePad("", LEN_MSG_CD);
		msgCont 	= LangUtil.rSpacePad("", LEN_MSG_CONT);
		inqStdt 	= LangUtil.rSpacePad(builder.inqStdt, LEN_INQ_STDT);
		inqEddt 	= LangUtil.rSpacePad(builder.inqEddt, LEN_INQ_EDDT);
		inqDcd 		= LangUtil.rSpacePad("1", LEN_INQ_DCD);
		brCd 		= LangUtil.rSpacePad(builder.brCd, LEN_BR_CD);
		cntPerPage 	= LangUtil.rSpacePad("000000100", LEN_CNT_PER_PAGE);
		nextKey 	= LangUtil.rSpacePad(builder.nextKey, LEN_NEXT_KEY);
		nextYn 		= LangUtil.rSpacePad("N", LEN_NEXT_YN);
		pageCnt 	= LangUtil.rSpacePad("000", LEN_PAGE_CNT);
		
		poliNo 		= LangUtil.rSpacePad("", LEN_POLI_NO);
		lschgDtm 	= LangUtil.rSpacePad("", LEN_LSCHG_DTM);
		prodLccd 	= LangUtil.rSpacePad("", LEN_PROD_LCCD);
		cntrDt 		= LangUtil.rSpacePad("", LEN_CNTR_DT);
		psDt 		= LangUtil.rSpacePad("", LEN_PS_DT);
		susTrmsDtm 	= LangUtil.rSpacePad("", LEN_SUS_TRMS_DTM);
		exDt 		= LangUtil.rSpacePad("", LEN_EX_DT);
		cntrScd 	= LangUtil.rSpacePad("", LEN_CNTR_SCD);
		cntrDtlScd 	= LangUtil.rSpacePad("", LEN_CNTR_DTL_SCD);
		nbptMbp 	= LangUtil.rSpacePad("", LEN_NBPT_MBP);
		nbptMmCvav 	= LangUtil.rSpacePad("", LEN_NBPT_MM_CVAV);
		clctBrofBrCd= LangUtil.rSpacePad("", LEN_CLCT_BROF_BR_CD);
		clpPsnNo 	= LangUtil.rSpacePad("", LEN_CLP_PSN_NO);
		prodNm 		= LangUtil.rSpacePad("", LEN_PROD_NM);
		rvcyCd 		= LangUtil.rSpacePad("", LEN_RVCY_CD);
		cntrmNm 	= LangUtil.rSpacePad("", LEN_CNTRM_NM);
		isrdNm 		= LangUtil.rSpacePad("", LEN_ISRD_NM);
		tprm 		= LangUtil.rSpacePad("", LEN_TPRM);
		uwRsltCd 	= LangUtil.rSpacePad("", LEN_UW_RSLT_CD);
	}
	
	@Override
	public String toEaiString() {
		StringBuilder sb = new StringBuilder();
		sb.append(msgCd).append(msgCont).append(inqStdt).append(inqEddt).append(inqDcd)
		.append(brCd).append(cntPerPage).append(nextKey).append(nextYn).append(pageCnt)
		.append(poliNo).append(lschgDtm).append(prodLccd).append(cntrDt).append(psDt)
		.append(susTrmsDtm).append(exDt).append(cntrScd).append(cntrDtlScd).append(nbptMbp)
		.append(nbptMmCvav).append(clctBrofBrCd).append(clpPsnNo).append(prodNm).append(rvcyCd)
		.append(cntrmNm).append(isrdNm).append(tprm).append(uwRsltCd);
		return sb.toString();
	}
	
	public static class Builder {
		String inqStdt;
		String inqEddt;
		String brCd;
		String nextKey;
		
		public Builder inqStdt(String inqStdt) {
			this.inqStdt = inqStdt;
			return this;
		}
		public Builder inqEddt(String inqEddt) {
			this.inqEddt = inqEddt;
			return this;
		}
		public Builder brCd(String brCd) {
			this.brCd = brCd;
			return this;
		}
		public Builder nextKey(String nextKey) {
			this.nextKey = nextKey;
			return this;
		}
		
		public IVZZMANBSH003Req build() {
			return new IVZZMANBSH003Req(this);
		}
	}

}
