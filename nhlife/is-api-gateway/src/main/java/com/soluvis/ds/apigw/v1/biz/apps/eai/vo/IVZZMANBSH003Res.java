package com.soluvis.ds.apigw.v1.biz.apps.eai.vo;

import java.util.ArrayList;
import java.util.List;

import com.soluvis.ds.apigw.v1.util.LangUtil;

import lombok.Getter;
import lombok.ToString;

/**
 * @Class 		: IVZZMANBSH003Res
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  IF-IS-0001(청약진행현황조회-실적) Response VO Class
 */
@Getter
@ToString
public class IVZZMANBSH003Res {
	
	IVZZMANBSH003Res(){}

	static final int LEN_OFFSET				= 1873;
	
						//No Name
	String nextKey;		//1  다음키
	String nextYn;		//2  다음여부
	String listCnt;		//3  리스트개수
	
	static final int LEN_NEXT_KEY			= 500;
	static final int LEN_NEXT_YN			= 1;
	static final int LEN_LIST_CNT			= 3;
											//504
											//2377
						//No Name
	String poliNo;		//1  증권번호
	String lschgDtm;	//2  최종변경일시
	String prodLccd;	//3  상품대분류코드
	String cntrDt;		//4  계약일자
	String psDt;		//5  청약일자
	String susTrmsDtm;	//6  청약전송일시
	String exDt;		//7  소멸일자
	String cntrScd;		//8  계약상태코드
	String cntrDtlScd;	//9  계약상세상태코드
	String nbptMbp;		//10 신계약시점월납기준보험료
	String nbptMmCvav;	//11 신계약시점월환산성적
	String clctBrofBrCd;//12 모집지점사무소코드
	String clpPsnNo;	//13 모집자개인번호
	String prodNm;		//14 상품명
	String rvcyCd;		//15 납입주기코드
	String cntrmNm;		//16 공제계약자성명
	String isrdNm;		//17 피보험자성명
	String tprm;		//18 합계보험료
	String uwRsltCd;	//19 인수심사결과코드명
	
	static final int LEN_POLI_NO 			= 15;
	static final int LEN_LSCHG_DTM 			= 17;
	static final int LEN_PROD_LCCD 			= 1;
	static final int LEN_CNTR_DT 			= 8;
	static final int LEN_PS_DT 				= 8;
	static final int LEN_SUS_TRMS_DTM 		= 17;
	static final int LEN_EX_DT 				= 8;
	static final int LEN_CNTR_SCD 			= 2;
	static final int LEN_CNTR_DTL_SCD 		= 4;
	static final int LEN_NBPT_MBP 			= 20;
	static final int LEN_NBPT_MM_CVAV 		= 20;
	static final int LEN_CLCT_BROF_BR_CD	= 6;
	static final int LEN_CLP_PSN_NO 		= 9;
	static final int LEN_PROD_NM 			= 100;
	static final int LEN_RVCY_CD 			= 2;
	static final int LEN_CNTRM_NM 			= 100;
	static final int LEN_ISRD_NM 			= 100;
	static final int LEN_TPRM 				= 20;
	static final int LEN_UW_RSLT_CD 		= 100;
											//557
	@Getter
	List<IVZZMANBSH003Res> dataList;

	public IVZZMANBSH003Res(byte[] arrByte) {
		int offset = LEN_OFFSET;
		this.nextKey 	= LangUtil.splitByteArray(arrByte, offset, LEN_NEXT_KEY).trim();		offset += LEN_NEXT_KEY;
		this.nextYn 		= LangUtil.splitByteArray(arrByte, offset, LEN_NEXT_YN).trim();		offset += LEN_NEXT_YN;
		this.listCnt 	= LangUtil.splitByteArray(arrByte, offset, LEN_LIST_CNT).trim();		offset += LEN_LIST_CNT;
		
		dataList = new ArrayList<>();
		
		int iListCnt = LangUtil.toInt(listCnt);
		for (int i = 0; i < iListCnt; i++) {
			IVZZMANBSH003Res res = new IVZZMANBSH003Res();
			res.poliNo 		= LangUtil.splitByteArray(arrByte, offset, LEN_POLI_NO).trim();		offset += LEN_POLI_NO;
			res.lschgDtm 	= LangUtil.splitByteArray(arrByte, offset, LEN_LSCHG_DTM).trim();		offset += LEN_LSCHG_DTM;
			res.prodLccd 	= LangUtil.splitByteArray(arrByte, offset, LEN_PROD_LCCD).trim();		offset += LEN_PROD_LCCD;
			res.cntrDt 		= LangUtil.splitByteArray(arrByte, offset, LEN_CNTR_DT).trim();		offset += LEN_CNTR_DT;
			res.psDt 		= LangUtil.splitByteArray(arrByte, offset, LEN_PS_DT).trim();			offset += LEN_PS_DT;
			res.susTrmsDtm 	= LangUtil.splitByteArray(arrByte, offset, LEN_SUS_TRMS_DTM).trim();	offset += LEN_SUS_TRMS_DTM;
			res.exDt 		= LangUtil.splitByteArray(arrByte, offset, LEN_EX_DT).trim();			offset += LEN_EX_DT;
			res.cntrScd 	= LangUtil.splitByteArray(arrByte, offset, LEN_CNTR_SCD).trim();		offset += LEN_CNTR_SCD;
			res.cntrDtlScd 	= LangUtil.splitByteArray(arrByte, offset, LEN_CNTR_DTL_SCD).trim();	offset += LEN_CNTR_DTL_SCD;
			res.nbptMbp 	= LangUtil.splitByteArray(arrByte, offset, LEN_NBPT_MBP).trim();		offset += LEN_NBPT_MBP;
			res.nbptMmCvav 	= LangUtil.splitByteArray(arrByte, offset, LEN_NBPT_MM_CVAV).trim();	offset += LEN_NBPT_MM_CVAV;
			res.clctBrofBrCd= LangUtil.splitByteArray(arrByte, offset, LEN_CLCT_BROF_BR_CD).trim();offset += LEN_CLCT_BROF_BR_CD;
			res.clpPsnNo 	= LangUtil.splitByteArray(arrByte, offset, LEN_CLP_PSN_NO).trim();	offset += LEN_CLP_PSN_NO;
			
			res.prodNm 		= LangUtil.splitByteArray(arrByte, offset, LEN_PROD_NM).trim();		offset += LEN_PROD_NM;
			res.rvcyCd 		= LangUtil.splitByteArray(arrByte, offset, LEN_RVCY_CD).trim();		offset += LEN_RVCY_CD;
			res.cntrmNm 	= LangUtil.splitByteArray(arrByte, offset, LEN_CNTRM_NM).trim();		offset += LEN_CNTRM_NM;
			res.isrdNm 		= LangUtil.splitByteArray(arrByte, offset, LEN_ISRD_NM).trim();		offset += LEN_ISRD_NM;
			res.tprm 		= LangUtil.splitByteArray(arrByte, offset, LEN_TPRM).trim();			offset += LEN_TPRM;
			res.uwRsltCd 	= LangUtil.splitByteArray(arrByte, offset, LEN_UW_RSLT_CD).trim();	offset += LEN_UW_RSLT_CD;
			dataList.add(res);
		}
	}
}
