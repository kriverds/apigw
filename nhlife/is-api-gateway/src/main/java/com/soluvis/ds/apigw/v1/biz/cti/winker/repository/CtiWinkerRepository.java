package com.soluvis.ds.apigw.v1.biz.cti.winker.repository;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.soluvis.ds.apigw.v1.biz.cti.winker.mapper.CtiWinkerMapper;

/**
 * @Class 		: CtiWinkerRepository
 * @date   		: 2025. 4. 7.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  CTI WINKER Repository
 */
@Repository
public class CtiWinkerRepository {
	
	/**
	 * 스프링 DI
	 */
	CtiWinkerMapper ctiWinkerMapper;
	public CtiWinkerRepository(CtiWinkerMapper ctiWinkerMapper) {
		this.ctiWinkerMapper = ctiWinkerMapper;
	}
	
	/**
	 * @Method		: checkCtiUser
	 * @date   		: 2025. 2. 27.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  CTI 생성/삭제 해야 할 유저 조회
	 */
	public List<Map<String,Object>> checkCtiUser(){
		return ctiWinkerMapper.selectCheckCtiUser();
	}
	
	/**
	 * @Method		: setCtiCreated
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  CTI 생성여부 Y로 설정
	 */
	public int setCtiCreated(Map<String,Object> params){
		return ctiWinkerMapper.updateCtiCreated(params);
	}
	/**
	 * @Method		: setCtiDeleted
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  CTI 생성여부 N으로 설정
	 */
	public int setCtiDeleted(Map<String,Object> params){
		return ctiWinkerMapper.updateCtiDeleted(params);
	}
	
	/**
	 * @Method		: getUsersHaveSkill
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  특정 스킬을 가지고 있는 상담사 리스트를 조회
	 */
	public List<Map<String,Object>> getUsersHaveSkill(Map<String,Object> params){
		return ctiWinkerMapper.selectUsersHaveSkill(params);
	}
	
	/**
	 * @Method		: getUserSkills
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  특정 상담사의 보유 스킬 리스트를 조회
	 */
	public List<Map<String,Object>> getUserSkills(Map<String,Object> params){
		return ctiWinkerMapper.selectUserSkills(params);
	}
	
	/**
	 * @Method		: getPersonDbid
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  상담사의 CTI DBID 조회
	 */
	public List<Map<String,Object>> getPersonDbid(Map<String,Object> params){
		return ctiWinkerMapper.selectPersonDbid(params);
	}
	
	/**
	 * @Method		: getDummySkillId
	 * @date   		: 2025. 4. 7.
	 * @author   	: PA2412013
	 * ----------------------------------------
	 * @notify
	 *  더미스킬 ID 조회
	 */
	public String getDummySkillId() {
		return ctiWinkerMapper.selectDummySkillId();
	}
}
