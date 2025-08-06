package com.soluvis.ds.apigw.v1.lib.winker.engine;

import java.net.URI;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.soluvis.ds.apigw.v1.biz.cti.winker.repository.CtiWinkerRepository;
import com.soluvis.ds.apigw.v1.lib.winker.engine.listener.DefaultEventListener;

/**
 * @Class 		: WinkerConnector
 * @date   		: 2025. 2. 17.
 * @author   	: sahnjeok, PA2412013
 * ----------------------------------------
 * @notify
 *  Winker Websocket 연결 API
 */
public class WinkerConnector extends WebSocketClient
{
	static final Logger logger = LoggerFactory.getLogger(WinkerConnector.class);
	
	public static final int MAX_SEND_SIZE = 1024*16 ;
	int refid = 100;
	Charset mWebSocketEncoding = StandardCharsets.UTF_8 ;
    ByteBuffer mBBufferWriter ;
    DefaultEventListener mEventListener = null ;
	StringBuilder sbRequest = new StringBuilder(MAX_SEND_SIZE) ;

	/**
	 * @Constructor	: WinkerConnector
	 * @param uri
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  1. EventListener 추가
	 *  2. URI 정보를 입력 받아 Websocket 연결 시도
	 */
	public WinkerConnector(URI uri)
	{
		super(uri);
	    logger.info("WinkerClient allocate direct buffer....") ;
	    
		mBBufferWriter 	= ByteBuffer.allocateDirect(MAX_SEND_SIZE);
	}
	CtiWinkerRepository ctiWinkerRepository;
	public void setRepository(CtiWinkerRepository ctiWinkerRepository) {
		this.ctiWinkerRepository = ctiWinkerRepository;
	}
	public void setEventListener() {
		DefaultEventListener listener = new DefaultEventListener();
		listener.setRepository(ctiWinkerRepository);
		addCTIEventListner(listener);
	}
	
	/**
	 * @Method		: connectWinker
	 * @date   		: 2025. 4. 8.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  WebSocket 연결
	 */
	public void connectWinker() {
		try {
			connectBlocking();
		} catch (InterruptedException e) {
			logger.error("", e);
		}
	}
	
	/**
	 * @Method		: reconnectWinker
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  WebSocket 연결 끊겼을 경우 재시도
	 */
	public void reconnectWinker() {
		try {
			reconnectBlocking();
		} catch (InterruptedException e) {
			logger.error("", e);
		}
	}
	
	public WinkerConnector(URI uri, Draft draft)
	{
		super(uri, draft);
		
	    refid = 100 ;
	    
	    logger.info("Session is connected, so allocate direct buffer....") ;
		
		mBBufferWriter 	= ByteBuffer.allocateDirect(MAX_SEND_SIZE);
	}
	
	@Override
	public void onMessage(String message)
	{
		logger.info("onMessage String [{}]", message);
		
		wEventHandler(message) ;
	}
	
	/**
	 * @Method		: onMessage
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  ByteBuffer로 받은 메세지 처리
	 */
	@Override
	public void onMessage(ByteBuffer message) {
//		logger.info("onMessage ByteBuffer [{}]", message);
		try{
			String strMessage = new String(message.array(), 0, message.capacity(), mWebSocketEncoding);
			logger.info("ByteBuffer onMessage>>{}", strMessage) ;
			wEventHandler(strMessage) ;
		} catch ( Exception ex ) {
			logger.error( "ByteBuffer onMessage Exception.... ", ex );
		}
	}
	
	
	/**
	 * @Method		: onOpen
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  Websocket Open 이벤트 발생 시에 "*SkipInit!#" 메세지를 Winker에 전송.
	 */
	@Override
	public void onOpen(ServerHandshake handshake) {
		logger.info("WinkerClient onOpen You are connected to WinkerServer: {}", getURI());

		send(ByteBuffer.wrap("*SkipInit!#".getBytes(mWebSocketEncoding))) ;
		
		String eventstr = "EventID=<"+ WinkerConst.EVENT_SERVER_CONNECTED +"/>;EventName=<EventServerConnected/>" ;
		
		if ( mEventListener != null ) {
			mEventListener.onWinkEvent(WinkerConst.EVENT_SERVER_CONNECTED, eventstr, null, null, null, null);
		}
		
	}
	
	/**
	 * @Method		: onClose
	 * @date   		: 2025. 2. 17.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  Websocket Close 이벤트 발생 시 호출
	 * 
	 *  Websocket 종료하는 로직이 없기 때문에 Close 발생시 에러로 취급.
	 *  해당 이벤트 발생 후 Websocket 요청 하면 Reconnect 시도
	 */
	@Override
	public void onClose(int code, String reason, boolean remote) {
		logger.error("WinkerClient onClose You have been disconnected from: {}; Code: {} {}", getURI(), code, reason);

		String eventstr = "EventID=<" + WinkerConst.EVENT_SERVER_DISCONNECTED + "/>;EventName=<EventServerDisconnected/>";

		if (mEventListener != null) {
			mEventListener.onWinkEvent(WinkerConst.EVENT_SERVER_DISCONNECTED, eventstr, null, null, null, null);
		}
	}
	
	@Override
	public void onError(Exception ex) {
		logger.error("onError {}", ex);
	}
	
	public void addCTIEventListner(DefaultEventListener listener)
	{
		mEventListener = listener ;
	}
	
	private synchronized int getRefid()
	{
		return ++ this.refid ;
	}

/*
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * @date   		: 2025. 2. 17.
 * @author   	: PA2412013
 * -----------------------------------------------------------------------------------------------------------------------------------------
 * @notify
 *  라이브러리 구역 수정하지 마세요
 */
	
    /**
     *   Server에 socket으로 상담원 상태 데이터를 보냅니다. 동시에 호출될 일이 없으므로 굳이 sync를 넣지 않는다.
     *
     *   @param  pData      socket으로 보낼 데이터. 문자열 String.
     *   @exception Exception
     */
     private boolean  writeData(StringBuilder pData)
     {
         boolean 	retVal = true ;
         int 		intI = 0 ;
 		 String 	tmp = pData.toString() ;
 		 byte[]		tmpByte = null ;
 		
 		 try {

 	 		tmpByte = tmp.getBytes(mWebSocketEncoding) ;
 	 		 
 			//2016-04-06 8859_1에서 UTF-8로 encoding 변경시 string에서 length를 구하면 한글이나 특수문자가 1로 되어서 문제가 생기므로 byte의 길이로 처리한다.
 	        if ( tmpByte.length > MAX_SEND_SIZE )   // pData.length() > MAX_SEND_SIZE )
 	        {
 	        	logger.info("Size to write [{}]", tmpByte.length) ; // pData.length()
 	        	
 	        	for ( intI = 0 ; intI*MAX_SEND_SIZE < tmpByte.length ; intI++ ) // pData.length()
 	        	{
 					mBBufferWriter.clear() ;
 					if ( (intI+1)*MAX_SEND_SIZE > tmpByte.length  ) // pData.length()
 					{
 						mBBufferWriter.put(tmpByte, intI*MAX_SEND_SIZE, tmpByte.length-intI*MAX_SEND_SIZE) ; // pData.length()
 					} else
 					{
 						mBBufferWriter.put(tmpByte, intI*MAX_SEND_SIZE, MAX_SEND_SIZE) ;
 					}

 					mBBufferWriter.flip() ;
 					
					if ( ReadyState.OPEN == getReadyState() )
					{
						this.send(mBBufferWriter);
						
						logger.info("Written & Limit size [-,{}]", mBBufferWriter.limit()) ;
					} else
					{
						logger.info("Connection already closed, so can not write....[{}]", tmp) ;
						
						retVal = false ;
						
						break ;
					}
 	        	}
 	        } else
 	        {
 				mBBufferWriter.clear() ;
 				mBBufferWriter.put(tmpByte) ;
 				mBBufferWriter.flip() ;
                
 				if ( ReadyState.OPEN == getReadyState() ) {
					send(mBBufferWriter);
				} else {
					logger.info("Connection already closed, so can not write....[{}]", tmp) ;
				}
 	        }
 		} catch (BufferOverflowException ex)
 		{
 			logger.error("Exception....[{}/{}/{}/{}/{}]", pData.length(), tmpByte!=null?tmpByte.length:0, intI, intI*MAX_SEND_SIZE, (intI+1)*MAX_SEND_SIZE) ;
 			logger.error("", ex) ;
 			close();
 			retVal = false ;
         } catch (Exception ex) {
        	 logger.error("Exception....[{}]", pData.length()) ;
        	 logger.error("", ex);
        	 close();
             retVal = false ;
         }

         return retVal ;
     }
     
 	/**
 	 * 서버에서 받은 이벤트를 처리하는 함수이다. 이 함수에서 기본 처리를 하고 다시 CallBack 함수를 호출한다.
 	 * 이 함수까지는 API 영역의 함수이며 tCallback 함수는 고객사별로 달라질 수 있다.
 	 *
 	 * @param msg   	{required} 서버에서 받은 메시지 내용
 	 *
 	 * @return
 	 */
 	private void wEventHandler(String msg)
 	{
 		// 1) 데이터를 ; 를 구분자로 split 한다.
 		// 2) *로 시작하는 *1444979038725#; 와 같은 heartbeat message는 그대로 재전송...
 		// 3) char(1) 로 시작하는 것은 char(2) 까지 다시 자른 다음에 먼저 처리한다.
 		//    반복해서 char(1) 이 있는지 다시 확인하고 없으면 최종 ;에 대해서 처리한다.
 		String[] msgArr = msg.split(WinkerConst.LST_DIM) ;
 		
 		for(final String msgText:msgArr)
 		{
 			if ( "".equals(msgText) ) {
				continue ;
			}
 			
 			if ( msgText.charAt(0) == '*' )
 			{
 				logger.debug("wEventHandler Resend heartBeat.... [{}]", msgText);
 				
 				// *로 시작하는 *1444979038725#; 와 같은 heartbeat message는 그대로 재전송...
 				StringBuilder heartbeat = new StringBuilder() ;
 				heartbeat.append(msgText) ;
 				
 				writeData(heartbeat) ;
 			} else
 			if ( msgText.charAt(0) == WinkerConst.DATA_STX.charAt(0) )
 			{
 				// 전문 종료 구분자로 다시 split 한다.
 				String[] orgArr = msgText.split(WinkerConst.DATA_ETX) ;
 				
 				for(final String orgText:orgArr)
 				{
 					String tOrgText = orgText;
 					if ( "".equals(tOrgText) ) {
						continue ;
					}
 					
 					if ( tOrgText.charAt(0) == WinkerConst.DATA_STX.charAt(0))
 					{
 						tOrgText = tOrgText.substring(1,tOrgText.length()) ;
 						
 						String[] dataArr = tOrgText.split(WinkerConst.MSG_DIM) ;
 						
 						// 화면과 연동하기 위한 CallBack 함수를 호출한다.
 						if ( dataArr.length == 4 ) {
							this.wCallback(Integer.parseInt(dataArr[0]), tOrgText, dataArr[1], dataArr[2], dataArr[3], null) ;
						} else {
							this.wCallback(Integer.parseInt(dataArr[0]), tOrgText, dataArr[1], null, dataArr[2], null) ;
						}
 					} else
 					{
 						// 서울:100~1
 						String[] statArr = tOrgText.split(WinkerConst.OBJ_DIM) ;	// 서울:100~1 에서 : 로 분리
 						
 						if ( statArr.length == 2 )
 						{
 							String[] valArr  = statArr[1].split(WinkerConst.VAL_DIM) ;	// ~ 로 분리
 						
 							if ( statArr.length == 2 )
 							{
 								String[] idArr = statArr[0].split(WinkerConst.TYP_DIM) ;		// 101^2:100~1 일 경우에 대해
 								
 								// 화면과 연동하기 위한 CallBack 함수를 호출한다.
 								// msgText : 원본 메시지
 								// idArr[0] : object dbid 또는 name. 201^2:100~1 일 경우에 대해 201. 서울:100~1 일 경우에 '서울'
 								// idArr[1] : object type. 201^2:100~1 일 경우에 대해 2. 서울:200~1 일 경우에는 undefined
 								// valArr[0] : 통계ID. 201^2:100~1 에서 100
 								// valArr[1] : 통계값. 201^2:100~1 에서 1
 								this.wCallback(WinkerConst.EVENT_STAT_INFO, msgText, idArr[0], idArr[1], valArr[0], valArr[1]) ;
 							} else{
 								logger.info("wEventHandler Invalid StatData value....[{}]", statArr[1]);
 							}
 						} else{
 							logger.info("wEventHandler Invalid StatData name....[{}]", msgText);
 						}
 					}
 				}
 			} else
 			{
 				// 서울:100~1
 				String[] statArr = msgText.split(WinkerConst.OBJ_DIM) ;	// 서울:100~1 에서 : 로 분리
 				
 				if ( statArr.length == 2 )
 				{
 					String[] valArr  = statArr[1].split(WinkerConst.VAL_DIM) ;	// ~ 로 분리
 				
 					if ( statArr.length == 2 )
 					{
 						String[] idArr = statArr[0].split(WinkerConst.TYP_DIM) ;		// 101^2:100~1 일 경우에 대해
 						
 						// 화면과 연동하기 위한 CallBack 함수를 호출한다.
 						// msgText : 원본 메시지
 						// idArr[0] : object dbid 또는 name. 201^2:100~1 일 경우에 대해 201. 서울:100~1 일 경우에 '서울'
 						// idArr[1] : object type. 201^2:100~1 일 경우에 대해 2. 서울:200~1 일 경우에는 undefined
 						// valArr[0] : 통계ID. 201^2:100~1 에서 100
 						// valArr[1] : 통계값. 201^2:100~1 에서 1
 						this.wCallback(WinkerConst.EVENT_STAT_INFO, msgText, idArr[0], idArr[1], valArr[0], valArr[1]) ;
 					} else{
 						logger.info("wEventHandler Invalid StatData value....[{}]", statArr[1]);
 					}
 				} else{
 					logger.info("wEventHandler Invalid StatData name....[{}]", msgText);
 				}
 			}
 		}
 	}
 	
 	/**
 	 * CTI에서 발생하는 이벤트를 고객사 화면으로 던져주기 위한 CallBack 함수를 호출한다.
 	 * CallBack 함수는 wInitialize, wConnect에서 지정할 수도 있고, 여기서 직접 지정할 수도 있다.
 	 * 고객사별로 이 함수내의 CallBack 함수 처리 내용이 바뀔 수 있다.
 	 *
 	 * @param eventId  	 {required} CallBack 함수에 전달할 이벤트 ID
 	 * @param eventMsg   {required} CallBack 함수에 전달할 이벤트 내용
 	 * @param objectDBId {required} CallBack 함수에 전달할 통계값에 대한 objectDBId
 	 * @param objectType {required} CallBack 함수에 전달할 통계값에 대한 objectType
 	 * @param statId   	 {required} CallBack 함수에 전달할 통계값에 대한 StatID
 	 * @param statVal    {required} CallBack 함수에 전달할 통계값에 대한 StatVal
 	 *
 	 * @return
 	 */
 	private void wCallback(int eventId, String eventMsg, String objectDBId, String objectType, String statId, String statVal)
 	{
 		if ( mEventListener != null ) {
			mEventListener.onWinkEvent(eventId, eventMsg, objectDBId, objectType, statId, statVal) ;
		}
 	}
	
	/**
	 * 서버에 한명의 상담사 생성을 요청한다.
	 *
	 * @param intOwnerDBID		{required} 생성할 상담사가 위치할 Person 하위의 folder dbid. 0이면 Person 폴더 최상위에 생성.
	 * @param strEmpID			{required} 생성할 상담사의 Agent EmpID=UserName
	 * @param strFirstName		{required} 생성할 상담사의 Agent First Name
	 * @param strLastName		{required} 생성할 상담사의 Agent Last Name
	 * @param strAgentLoginID	{required} 상담사에게 추가하고자 하는 AgentLoginID@PBX1. 여러개 하고자 할 경우 ',' 로 구분한다. 이건 없으면 생성된다. 있으면 있는거 사용된다.
	 *									   '*@PBX1' 와 같이 지정하면 해당 교환기의 아무거나 사용된다. '12345@PBX1,*@PBX2' 와 같이 지정하면 PBX1에서는 12345 사용, PBX2에서는 사용하지 않는 AgentLogin 찾아서 할당.
	 *										'12345' 와 같이 @ 없이 지정하면 agentlogin dbid로 처리한다. dbid를 지정하면 pbx 지정이 필요 없기 때문이다.
	 * @param strSkillDBID		{required} 상담사에게 추가하고자 하는 스킬의 DBID. 여러개 하고자 할 경우 ',' 로 구분한다. strSkillLevel의 구분 갯수와 동일해야 한다.
	 * @param strSkillLevel		{required} 상담사에게 추가하고자 하는 스킬의 level. 여러개 하고자 할 경우 ',' 로 구분한다. strSkillDBID의 구분 갯수와 동일해야 한다.
	 *
	 * @return		요청 접수 ReferenceID. -1 이면 요청 실패.
	 * First 상담원명
	 * Last 소속(CS/TM)
	 * Employee 사번
	 * User Name 사번
	 * Agent Login CTI_LOGIN_ID
	 */
	public synchronized int createAgent(int ownerDBID, String empID, String firstName,String lastName, String agentLoginID, String skillDBIDs, String skillLevels)
	{
		logger.info("createAgent starts....[{},{},{},{},{},{},{}]", ownerDBID, empID, firstName, lastName, agentLoginID, skillDBIDs, skillLevels);
		
		int oDBID = 0;
		if ( ownerDBID > 0 ) {
			oDBID = ownerDBID ;
		}
		if ( empID == null ) {
			return -1;
		}
		if ( empID.isEmpty() ) {
			return -1;
		}
		if ( firstName.isEmpty() ) {
			return -1;
		}
		if ( lastName.isEmpty() ) {
			return -1;
		}
	
		int lRefid = getRefid() ;

		try {
			synchronized(sbRequest)
			{
				sbRequest.setLength(0) ;
				
				sbRequest.append(WinkerConst.DATA_STX)
						 .append(WinkerConst.REQUEST_CREATE_AGENT)
						 .append("~").append(lRefid)
						 .append("~" ).append(oDBID)
						 .append(":").append(empID)
						 .append(":").append(firstName)
						 .append(":").append(lastName)
						 .append(":").append(agentLoginID)
						 .append(":").append(skillDBIDs)
						 .append(":").append(skillLevels)
						 .append(WinkerConst.DATA_ETX) ;

				writeData(sbRequest) ;
			}
		} catch (Exception e) {
			logger.error("createAgent Exception....", e) ;
		}
		logger.info("createAgent ends....");
	
		return lRefid ;
	}
	
	/**
	 * 서버에 한명의 상담원 정보 변경을 요청한다.
	 *
	 * @param intDBID				{required} 변경할 상담원의 Agent DBID. intDBID/strEmpID 둘중에 하나는 필수 값
	 * @param strEmpID				{required} 변경할 상담원의 Agent 현재 EmpID. intDBID/strEmpID 둘중에 하나는 필수 값
	 * @param strEmpIDNew			{optional} 변경할 상담원의 Agent New EmpID. 공백이면 바뀌지 않는다.
	 * @param strFirstName			{optional} 변경할 상담원의 Agent First Name. 공백이면 바뀌지 않는다.
	 * @param strLastName			{optional} 변경할 상담원의 Agent Last Name. 공백이면 바뀌지 않는다.
	 *
	 * @return		요청 접수 ReferenceID. -1 이면 요청 실패.
	 */
	public synchronized int updateAgent(int ownerDBID, String empID, String empIDNew, String firstName, String lastName)
	{
		logger.info("updateAgent starts....[{},{},{},{},{}]", ownerDBID, empID, empIDNew, firstName, lastName);
		
		int oDBID = 0;
		if ( ownerDBID > 0 ) {
			oDBID = ownerDBID ;
		}
		if ( empID == null || empID.isEmpty() ) {
			return -1;
		}
	
		int lRefid = getRefid();
		
		try {
			synchronized(sbRequest)
			{
				sbRequest.setLength(0) ;
				
				sbRequest.append(WinkerConst.DATA_STX)
						 .append(WinkerConst.REQUEST_UPDATE_AGENT)
						 .append("~").append(lRefid)
						 .append("~" ).append(oDBID)
						 .append(":").append(empID)
						 .append(":").append(empIDNew)
						 .append(":").append(firstName)
						 .append(":").append(lastName)
						 .append(WinkerConst.DATA_ETX) ;

				writeData(sbRequest) ;
			}
		} catch (Exception e) {
			logger.error("updateAgent Exception....", e) ;
		}
		logger.info("updateAgent ends....");
		return lRefid ;
	}
	
	/**
	 * 서버에 한명의 상담원 삭제를 요청한다.
	 *
	 * @param intDBID				{required} 삭제할 상담원의 Agent DBID. intDBID/strEmpID 둘중에 하나는 필수 값
	 * @param strEmpID				{required} 삭제할 상담원의 Agent 현재 EmpID. intDBID/strEmpID 둘중에 하나는 필수 값
	 *
	 * @return		요청 접수 ReferenceID. -1 이면 요청 실패.
	 */
	public synchronized int deleteAgent(int ownerDBID, String empID)
	{
		logger.info("deleteAgent starts....[{},{}]", ownerDBID, empID);
		
		int oDBID = 0;
		if ( ownerDBID > 0 ) {
			oDBID = ownerDBID ;
		}
		if ( empID == null || empID.isEmpty() ) {
			return -1;
		}
	
		int lRefid = getRefid() ;
		
		try {
			synchronized(sbRequest)
			{
				sbRequest.setLength(0) ;
				
				sbRequest.append(WinkerConst.DATA_STX)
						 .append(WinkerConst.REQUEST_DELETE_AGENT)
						 .append("~").append(lRefid)
						 .append("~" ).append(oDBID)
						 .append(":").append(empID)
						 .append(WinkerConst.DATA_ETX) ;

				writeData(sbRequest) ;
			}
		} catch (Exception e) {
			logger.error("deleteAgent Exception....", e) ;
		}
		logger.info("deleteAgent ends....");
		return lRefid ;
	}
	
	/**
	 * 서버에 한명의 상담원에 AgentID를 추가 요청한다.
	 *
	 * @param intDBID				{required} 변경할 상담원의 Person DBID. intDBID/strEmpID 둘중에 하나는 필수 값
	 * @param strEmpID				{required} 변경할 상담원의 Person EmpID. intDBID/strEmpID 둘중에 하나는 필수 값
	 * @param strAddAgentLoginID	{required} 상담원에게 추가하고자 하는 AgentID DBID 또는 AgentLoginID@PBX1.
	 *
	 * @return		요청 접수 ReferenceID. -1 이면 요청 실패.
	 */
	public synchronized int addAgentIDToAgent(int ownerDBID, String empID, String addAgentLoginID)
	{
		logger.info("addAgentIDToAgent starts....[{},{},{}]", ownerDBID, empID, addAgentLoginID);
		
		int oDBID = 0;
		if ( ownerDBID > 0 ) {
			oDBID = ownerDBID ;
		}
		if ( empID == null || empID.isEmpty() ) {
			return -1;
		}
	
		int lRefid = getRefid() ;
		
		try {
			synchronized(sbRequest)
			{
				sbRequest.setLength(0) ;
				
				sbRequest.append(WinkerConst.DATA_STX)
						 .append(WinkerConst.REQUEST_ADD_AGENTID_TO_AGENT)
						 .append("~").append(lRefid)
						 .append("~" ).append(oDBID)
						 .append(":").append(empID)
						 .append(":").append(addAgentLoginID)
						 .append(WinkerConst.DATA_ETX) ;

				writeData(sbRequest) ;
			}
		} catch (Exception e) {
			logger.error("addAgentIDToAgent Exception....", e) ;
		}
		logger.info("addAgentIDToAgent ends....");
		return lRefid ;
	}
	
	/**
	 * AgentID 생성을 요청한다.
	 *
	 * @param strCreateAgentLoginID	{required} 생성하고자 하는 AgentLoginID@PBX1.
	 *
	 * @return		요청 접수 ReferenceID. -1 이면 요청 실패.
	 */
	public synchronized int createAgentID(String createAgentLoginID)
	{
		logger.info("createAgentID starts....[{}]", createAgentLoginID);
		
		if ( createAgentLoginID == null || createAgentLoginID.isEmpty() ) {
			return -1;
		}
	
		int lRefid = getRefid() ;
		
		try {
			synchronized(sbRequest)
			{
				sbRequest.setLength(0) ;
				
				sbRequest.append(WinkerConst.DATA_STX)
						 .append(WinkerConst.REQUEST_CREATE_AGENTID)
						 .append("~").append(lRefid)
						 .append("~" ).append(createAgentLoginID)
						 .append(WinkerConst.DATA_ETX) ;

				writeData(sbRequest) ;
			}
		} catch (Exception e) {
			logger.error("createAgentID Exception....", e) ;
		}
		logger.info("createAgentID ends....");
		return lRefid ;
	}

	/**
	 * 서버에 한명의 상담원의 스킬 추가를 요청한다.
	 *
	 * @param strAgentDBID	{required} 스킬을 추가할 상담원의 Agent DBID
	 * @param strSkillDBID	{required} 상담원에게 추가하고자 하는 스킬의 DBID. 여러개 하고자 할 경우 ',' 로 구분한다. strSkillLevel의 구분 갯수와 동일해야 한다.
	 * @param strSkillLevel	{required} 상담원에게 추가하고자 하는 스킬의 level. 여러개 하고자 할 경우 ',' 로 구분한다. strSkillDBID의 구분 갯수와 동일해야 한다.
	 *
	 * @return		요청 접수 ReferenceID. -1 이면 요청 실패.
	 */
	public synchronized int addSkillMulti(String agentDBID, String[] skillDBID, String[] skillLevel)
	{
		logger.info("addSkillMulti starts....[{},{},{}]", agentDBID, skillDBID, skillLevel);
		
		if ( agentDBID == null || agentDBID.isEmpty() ) {
			return -1;
		}
		if ( skillDBID == null || skillDBID.length == 0 ) {
			return -1;
		}
		if ( skillLevel == null || skillLevel.length == 0 ) {
			return -1;
		}
	
		int lRefid = getRefid() ;
		try {
			synchronized(sbRequest)
			{
				sbRequest.setLength(0) ;
				
				sbRequest.append(WinkerConst.DATA_STX)
						 .append(WinkerConst.REQUEST_ADD_SKILL)
						 .append("~").append(lRefid)
						 .append("~" ).append(agentDBID)
						 .append(":").append(String.join(",", skillDBID))
						 .append(":").append(String.join(",", skillLevel))
						 .append(WinkerConst.DATA_ETX);

				writeData(sbRequest) ;
			}
		} catch (Exception e) {
			logger.error("addSkillMulti Exception....", e) ;
		}
		logger.info("addSkillMulti ends....");
		return lRefid ;
	}

	/**
	 * 서버에 한명의 상담원의 스킬 변경을 요청한다.
	 *
	 * @param strAgentDBID	{required} 스킬을 변경할 상담원의 Agent DBID
	 * @param strSkillDBID	{required} 상담원에게 변경하고자 하는 스킬의 DBID. 여러개 하고자 할 경우 ',' 로 구분한다. strSkillLevel의 구분 갯수와 동일해야 한다.
	 * @param strSkillLevel	{required} 상담원에게 변경하고자 하는 스킬의 level. 여러개 하고자 할 경우 ',' 로 구분한다. strSkillDBID의 구분 갯수와 동일해야 한다.
	 *
	 * @return		요청 접수 ReferenceID. -1 이면 요청 실패.
	 */
	public synchronized int updateSkillMulti(String agentDBID, String[] skillDBID, String[] skillLevel)
	{
		logger.info("updateSkillMulti starts....[{},{},{}]", agentDBID, skillDBID, skillLevel);
		
		if ( agentDBID == null || agentDBID.isEmpty() ) {
			return -1;
		}
		if ( skillDBID == null || skillDBID.length == 0 ) {
			return -1;
		}
		if ( skillLevel == null || skillLevel.length == 0 ) {
			return -1;
		}
	
		int lRefid = getRefid() ;
		try {
			synchronized(sbRequest)
			{
				sbRequest.setLength(0) ;
				
				sbRequest.append(WinkerConst.DATA_STX)
						 .append(WinkerConst.REQUEST_UPDATE_SKILL)
						 .append("~").append(lRefid)
						 .append("~" ).append(agentDBID)
						 .append(":").append(String.join(",", skillDBID))
						 .append(":").append(String.join(",", skillLevel))
						 .append(WinkerConst.DATA_ETX);

				writeData(sbRequest) ;
			}
		} catch (Exception e) {
			logger.error("updateSkillMulti Exception....", e) ;
		}
		logger.info("updateSkillMulti ends....");
		return lRefid ;
	}

	/**
	 * 서버에 한명의 상담원의 스킬 삭제를 요청한다.
	 *
	 * @param strAgentDBID	{required} 스킬을 삭제할 상담원의 Agent DBID
	 * @param strSkillDBID	{required} 상담원에게 삭제하고자 하는 스킬의 DBID. 여러개 하고자 할 경우 ',' 로 구분한다.
	 *
	 * @return		요청 접수 ReferenceID. -1 이면 요청 실패.
	 */
	public synchronized int deleteSkillMulti(String agentDBID, String[] skillDBID)
	{
		logger.info("deleteSkillMulti starts....[{},{}]", agentDBID, skillDBID);
		
		if ( agentDBID == null || agentDBID.isEmpty() ) {
			return -1;
		}
		if ( skillDBID == null || skillDBID.length == 0 ) {
			return -1;
		}
	
		int lRefid = getRefid() ;
		try {
			synchronized(sbRequest)
			{
				sbRequest.setLength(0) ;
				
				sbRequest.append(WinkerConst.DATA_STX)
						 .append(WinkerConst.REQUEST_DELETE_SKILL)
						 .append("~").append(lRefid)
						 .append("~" ).append(agentDBID)
						 .append(":").append(String.join(",", skillDBID))
						 .append(WinkerConst.DATA_ETX);

				writeData(sbRequest) ;
			}
		} catch (Exception e) {
			logger.error("deleteSkillMulti Exception....", e) ;
		}
		logger.info("deleteSkillMulti ends....");
		return lRefid ;
	}
}
