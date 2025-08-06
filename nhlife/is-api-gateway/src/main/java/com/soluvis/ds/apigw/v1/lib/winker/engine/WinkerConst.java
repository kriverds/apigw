package com.soluvis.ds.apigw.v1.lib.winker.engine;

import lombok.Getter;

/**
 * @Class 		: WinkerConst
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  Winker Const 클래스
 */
public class WinkerConst {

	WinkerConst() {}
	
	public static final String DATA_STX							= "\u0001" ;
	public static final String DATA_ETX							= "\u0002" ;
	public static final String MSG_DIM							= "~" ;		// send : stx type~refid~requestdetail etx
	public static final String VAL_DIM							= "~" ;		// receive : object:statid~val;
	public static final String OBJ_DIM							= ":" ;		// receive : object:statid~val;
	public static final String LST_DIM							= ";" ;		// receive : object:statid~val;
	public static final String STA_DIM							= "↑" ;		// send : object↑statid1↑statid2↑statid3;
	public static final String TYP_DIM							= "^" ;		// send/receive : objectdbid^type
	
	public static final int REQUEST_EVENT_LOGIN					= 0; // Request이기도 하고 Event이기도 하다.
	public static final int REQUEST_ORG_INFO					= 1;
	public static final int EVENT_ORG_INFO						= 2;
	public static final int REQUEST_TEAM_INFO					= 3;
	public static final int EVENT_TEAM_INFO						= 4;
	public static final int REQUEST_VDN_LIST					= 5;
	public static final int EVENT_VDN_LIST						= 6;
	public static final int REQUEST_QUEUE_LIST					= 7;
	public static final int EVENT_QUEUE_LIST					= 8;
	public static final int REQUEST_SWITCH_INFO					= 9;
	public static final int EVENT_SWITCH_INFO					= 10;
	public static final int REQUEST_OPEN_STAT					= 11;
	public static final int REQUEST_CLOSE_STAT					= 12;
	public static final int EVENT_STAT_INFO						= 13;
	public static final int REQUEST_AGENT_ORG_INFO				= 14;
	public static final int REQUEST_SKILL_ORG_INFO				= 15;
	public static final int EVENT_AGENT_ORG_INFO				= 16;
	public static final int EVENT_SKILL_ORG_INFO				= 17;
	
	public static final int REQUEST_ADD_SKILL					= 18;
	public static final int REQUEST_UPDATE_SKILL				= 19;
	public static final int REQUEST_DELETE_SKILL				= 20;
	public static final int EVENT_SKILL_ADDED					= 21;
	public static final int EVENT_SKILL_UPDATED					= 22;
	public static final int EVENT_SKILL_DELETED					= 23;
	
	public static final int REQUEST_AGENT_LOGOUT				= 24;
	public static final int EVENT_AGENT_LOGOUT					= 25;
	
	public static final int REQUEST_LISTEN_DN					= 26;
	public static final int EVENT_LISTEN_DN						= 27;
	
	public static final int REQUEST_UPDATE_ANNEX				= 28;
	public static final int EVENT_ANNEX_UPDATED					= 29;
	
	public static final int REQUEST_STOP_LISTEN_DN				= 30;
	public static final int EVENT_LISTEN_DN_STOPPED				= 31;
	
	public static final int REQUEST_ADD_AGENT_TO_GROUP			= 32;
	public static final int EVENT_AGENT_ADDED_TO_GROUP			= 33;
	
	public static final int REQUEST_DELETE_AGENT_FROM_GROUP		= 34;
	public static final int EVENT_AGENT_DELETED_FROM_GROUP		= 35;
	
	public static final int REQUEST_CREATE_AGENT				= 36;
	public static final int EVENT_AGENT_CREATED					= 37;
	
	public static final int REQUEST_CREATE_AGENTGROUP			= 38;
	public static final int EVENT_AGENTGROUP_CREATED			= 39;
	
	public static final int REQUEST_DELETE_AGENTGROUP			= 40;
	public static final int EVENT_AGENTGROUP_DELETED			= 41;
	
	public static final int REQUEST_UPDATE_AGENTGROUP			= 42;
	public static final int EVENT_AGENTGROUP_UPDATED			= 43;
	
	public static final int REQUEST_DELETE_AGENT				= 44;
	public static final int EVENT_AGENT_DELETED					= 45;
	
	public static final int REQUEST_UPDATE_AGENT				= 46;
	public static final int EVENT_AGENT_UPDATED					= 47;
	
	public static final int REQUEST_ADD_AGENTID_TO_AGENT		= 48;
	public static final int REQUEST_DELETE_AGENTID_FROM_AGENT	= 49;
	public static final int EVENT_AGENTID_ADDED_TO_AGENT		= 50;
	public static final int EVENT_AGENTID_DELETED_FROM_AGENT	= 51;
	
	public static final int REQUEST_DELETE_AGENT_ONLY			= 52;
	public static final int EVENT_AGENT_ONLY_DELETED			= 53;
	
	public static final int REQUEST_CREATE_AGENTID				= 54;
	public static final int EVENT_AGENTID_CREATED				= 55;
	public static final int REQUEST_DELETE_AGENTID				= 56;
	public static final int EVENT_AGENTID_DELETED				= 57;
	
	public static final int EVENT_ERROR							= 98;		// 에러로만 처리됩니다.
	public static final int EVENT_LOGIN_INVALID					= 99;		// client 연결이 끊어집니다.
	public static final int EVENT_SERVER_CONNECTED				= 100;
	public static final int EVENT_SERVER_DISCONNECTED			= 101;

	@Getter
	static final String[] EVENT_NAME = {
		"REQUEST_EVENT_LOGIN","REQUEST_ORG_INFO","EVENT_ORG_INFO","REQUEST_TEAM_INFO","EVENT_TEAM_INFO","REQUEST_VDN_LIST","EVENT_VDN_LIST","REQUEST_QUEUE_LIST","EVENT_QUEUE_LIST","REQUEST_SWITCH_INFO",
		"EVENT_SWITCH_INFO","REQUEST_OPEN_STAT","REQUEST_CLOSE_STAT","EVENT_STAT_INFO","REQUEST_AGENT_ORG_INFO","REQUEST_SKILL_ORG_INFO","EVENT_AGENT_ORG_INFO","EVENT_SKILL_ORG_INFO","REQUEST_ADD_SKILL","REQUEST_UPDATE_SKILL",
		"REQUEST_DELETE_SKILL","EVENT_SKILL_ADDED","EVENT_SKILL_UPDATED","EVENT_SKILL_DELETED","REQUEST_AGENT_LOGOUT","EVENT_AGENT_LOGOUT","REQUEST_LISTEN_DN","EVENT_LISTEN_DN","REQUEST_UPDATE_ANNEX","EVENT_ANNEX_UPDATED",
		"REQUEST_STOP_LISTEN_DN","EVENT_LISTEN_DN_STOPPED","REQUEST_ADD_AGENT_TO_GROUP","EVENT_AGENT_ADDED_TO_GROUP","REQUEST_DELETE_AGENT_FROM_GROUP","EVENT_AGENT_DELETED_FROM_GROUP","REQUEST_CREATE_AGENT","EVENT_AGENT_CREATED","REQUEST_CREATE_AGENTGROUP","EVENT_AGENTGROUP_CREATED",
		"REQUEST_DELETE_AGENTGROUP","EVENT_AGENTGROUP_DELETED","REQUEST_UPDATE_AGENTGROUP","EVENT_AGENTGROUP_UPDATED","REQUEST_DELETE_AGENT","EVENT_AGENT_DELETED","REQUEST_UPDATE_AGENT","EVENT_AGENT_UPDATED","REQUEST_ADD_AGENTID_TO_AGENT","REQUEST_DELETE_AGENTID_FROM_AGENT",
		"EVENT_AGENTID_ADDED_TO_AGENT","EVENT_AGENTID_DELETED_FROM_AGENT","REQUEST_DELETE_AGENT_ONLY","EVENT_AGENT_ONLY_DELETED","REQUEST_CREATE_AGENTID","EVENT_AGENTID_CREATED","REQUEST_DELETE_AGENTID","EVENT_AGENTID_DELETED","","",
		"","","","","","","","","","",
		"","","","","","","","","","",
		"","","","","","","","","","",
		"","","","","","","","","EVENT_ERROR","EVENT_LOGIN_INVALID",
		"EVENT_SERVER_CONNECTED","EVENT_SERVER_DISCONNECTED",
	};
}
