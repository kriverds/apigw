package com.soluvis.ds.apigw.v1.lib.winker.engine.listener;

/**
 * @Class 		: WinkerEventListener
 * @date   		: 2025. 4. 8.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  WINKER EVENT LISTENER 껍데기 인터페이스
 */
public interface WinkerEventListener {
	public void onWinkEvent(int eventID, String eventData, String param1, String param2, String param3, String param4);
}
