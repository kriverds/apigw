package com.soluvis.ds.apigw.v1.biz.common.eai.vo;

import com.soluvis.ds.apigw.v1.util.LangUtil;

import lombok.ToString;

/**
 * @Class 		: CommonEaiHeader
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  EAI 공통 헤더 VO 클래스
 */
@ToString
public class CommonEaiHeader {
							//No Name
	String icLen;			//1
	String icFrom;			//2
	String icTo;			//3
	String icKey2;			//4
	String icMsgFlag;		//5
	String icDestinationId;	//6
	String icKey;			//7
	String icCommand;		//8
	String icType;			//9
	String ecDataLen;		//10
	String ecDnis;			//11
	String ecType;			//12
	String ecSendDate;		//13
	String ecCh;			//14
	String ecEquipNumber;	//15
	String ecRcveSvcId;		//16
	String ecEaiIntfId;		//17
	String ecOtsdOrgCd;		//18
	String ecOtsdBsnCd;		//19
	String ecOtsdKdtpCd;	//20
	String ecOtsdTrCd;		//21
	String ecOprBrCd;		//22
	String ecOptRpsnNo;		//23
	String ecTgrmRspDtm;	//24
	String ecTgrmPrcrsltDcd;//25
	String ecObsSysId;		//26
	String ecTgrmErrMsgCd;	//27
	String ecCustName;		//28
	String ecCustNo;		//29
	String ecAccountCard;	//30
	String ecAniNumber;		//31
	
	static final int LEN_STX 				= 1;
	static final int LEN_IC_LEN 			= 5;
	static final int LEN_IC_FROM 			= 8;
	static final int LEN_IC_TO 				= 8;
	static final int LEN_IC_KEY2 			= 9;
	static final int LEN_IC_MSG_FLAG 		= 1;
	static final int LEN_IC_DESTINATION_ID 	= 16;
	static final int LEN_IC_KEY 			= 24;
	static final int LEN_IC_COMMAND 		= 5;
	static final int LEN_IC_TYPE 			= 4;
	static final int LEN_EC_DATA_LEN 		= 8;
	static final int LEN_EC_DNIS 			= 20;
	static final int LEN_EC_TYPE 			= 4;
	static final int LEN_EC_SEND_DATE 		= 14;
	static final int LEN_EC_CH 				= 4;
	static final int LEN_EC_EQUIP_NUMBER 	= 4;
	static final int LEN_EC_RCVE_SVC_ID 	= 8;
	static final int LEN_EC_EAI_INTF_ID 	= 13;
	static final int LEN_EC_OTSD_ORG_CD 	= 4;
	static final int LEN_EC_OTSD_BSN_CD 	= 4;
	static final int LEN_EC_OTSD_KDTP_CD 	= 8;
	static final int LEN_EC_OPR_TR_CD 		= 10;
	static final int LEN_EC_OPR_BR_CD 		= 6;
	static final int LEN_EC_OPT_RPSN_NO 	= 9;
	static final int LEN_EC_TGRM_RSP_DTM 	= 14;
	static final int LEN_EC_TGRM_PRCRSLT_DCD = 1;
	static final int LEN_EC_OBS_SYS_ID 		= 2;
	static final int LEN_EC_TGRM_ERR_MSG_CD = 7;
	static final int LEN_EC_CUST_NAME 		= 50;
	static final int LEN_EC_CUST_NO 		= 13;
	static final int LEN_EC_ACCOUNT_CARD 	= 30;
	static final int LEN_EC_ANI_NUMBER 		= 20;
	static final int LEN_ETX 				= 1;
	
	static final int TOTAL_LEN = 335;
	
	CommonEaiHeader(Builder builder) {
		int iLen = TOTAL_LEN + LangUtil.toInt(builder.ecDataLen);
		
		icLen 			= LangUtil.lPad(Integer.toString(iLen), "0", LEN_IC_LEN);
		icFrom 			= LangUtil.rSpacePad("", LEN_IC_FROM);
		icTo 			= LangUtil.rSpacePad("", LEN_IC_TO);
		icKey2 			= LangUtil.rSpacePad("", LEN_IC_KEY2);
		icMsgFlag 		= LangUtil.rSpacePad("1", LEN_IC_MSG_FLAG);
		icDestinationId = LangUtil.rSpacePad("", LEN_IC_DESTINATION_ID);
		icKey 			= LangUtil.rSpacePad(builder.icKey, LEN_IC_KEY);
		icCommand 		= LangUtil.rSpacePad("13000", LEN_IC_COMMAND);
		icType 			= LangUtil.rSpacePad("PKT1", LEN_IC_TYPE);
		ecDataLen 		= LangUtil.lPad(builder.ecDataLen, "0", LEN_EC_DATA_LEN);
		ecDnis 			= LangUtil.rSpacePad("60001", LEN_EC_DNIS);
		ecType 			= LangUtil.rSpacePad("SEND", LEN_EC_TYPE);
		ecSendDate 		= LangUtil.rSpacePad("", LEN_EC_SEND_DATE);
		ecCh 			= LangUtil.rSpacePad("", LEN_EC_CH);
		ecEquipNumber 	= LangUtil.rSpacePad("IVR1", LEN_EC_EQUIP_NUMBER);
		ecRcveSvcId 	= LangUtil.rSpacePad(builder.ecRcveSvcId, LEN_EC_RCVE_SVC_ID);
		ecEaiIntfId 	= LangUtil.rSpacePad(builder.ecEaiIntfId, LEN_EC_EAI_INTF_ID);
		ecOtsdOrgCd 	= LangUtil.rSpacePad("", LEN_EC_OTSD_ORG_CD);
		ecOtsdBsnCd 	= LangUtil.rSpacePad("", LEN_EC_OTSD_BSN_CD);
		ecOtsdKdtpCd 	= LangUtil.rSpacePad("", LEN_EC_OTSD_KDTP_CD);
		ecOtsdTrCd 		= LangUtil.rSpacePad("", LEN_EC_OPR_TR_CD);
		ecOprBrCd 		= LangUtil.rSpacePad("008016", LEN_EC_OPR_BR_CD);
		ecOptRpsnNo 	= LangUtil.rSpacePad("H13099999", LEN_EC_OPT_RPSN_NO);
		ecTgrmRspDtm 	= LangUtil.rSpacePad("", LEN_EC_TGRM_RSP_DTM);
		ecTgrmPrcrsltDcd= LangUtil.rSpacePad("0", LEN_EC_TGRM_PRCRSLT_DCD);
		ecObsSysId 		= LangUtil.rSpacePad("", LEN_EC_OBS_SYS_ID);
		ecTgrmErrMsgCd 	= LangUtil.rSpacePad("", LEN_EC_TGRM_ERR_MSG_CD);
		ecCustName 		= LangUtil.rSpacePad("", LEN_EC_CUST_NAME);
		ecCustNo 		= LangUtil.rSpacePad("", LEN_EC_CUST_NO);
		ecAccountCard 	= LangUtil.rSpacePad("", LEN_EC_ACCOUNT_CARD);
		ecAniNumber 	= LangUtil.rSpacePad("", LEN_EC_ANI_NUMBER);
	}
	
	public String toEaiString() {
		StringBuilder sb = new StringBuilder();
		sb.append(icLen).append(icFrom).append(icTo).append(icKey2).append(icMsgFlag)
		.append(icDestinationId).append(icKey).append(icCommand).append(icType).append(ecDataLen)
		.append(ecDnis).append(ecType).append(ecSendDate).append(ecCh).append(ecEquipNumber)
		.append(ecRcveSvcId).append(ecEaiIntfId).append(ecOtsdOrgCd).append(ecOtsdBsnCd).append(ecOtsdKdtpCd)
		.append(ecOtsdTrCd).append(ecOprBrCd).append(ecOptRpsnNo).append(ecTgrmRspDtm).append(ecTgrmPrcrsltDcd)
		.append(ecObsSysId).append(ecTgrmErrMsgCd).append(ecCustName).append(ecCustNo).append(ecAccountCard)
		.append(ecAniNumber);
		return sb.toString();
	}
	
	public static class Builder {
		String icKey;
		String ecDataLen;
		String ecRcveSvcId;
		String ecEaiIntfId;
		
		public Builder icKey(String icKey) {
			this.icKey = icKey;
			return this;
		}
		public Builder ecDataLen(String ecDataLen) {
			this.ecDataLen = ecDataLen;
			return this;
		}
		public Builder ecRcveSvcId(String ecRcveSvcId) {
			this.ecRcveSvcId = ecRcveSvcId;
			return this;
		}
		public Builder ecEaiIntfId(String ecEaiIntfId) {
			this.ecEaiIntfId = ecEaiIntfId;
			return this;
		}
		
		public CommonEaiHeader build() {
			return new CommonEaiHeader(this);
		}
	}
}
