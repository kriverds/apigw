CREATE OR REPLACE PROCEDURE SWM.b_del_appUserVacation(
	P_SysKind		IN VARCHAR2,  		--시스템구분(CS/TM)
	P_YYYYMM		IN VARCHAR2 		--대상년월
)
IS
BEGIN
	DELETE FROM SWM.T_APPUSERVACATION
	WHERE syskind = P_SysKind
	AND HLDS_DT >= TO_TIMESTAMP(P_YYYYMM||'-01', 'YYYY-MM-DD')
	AND HLDS_DT < ADD_MONTHS(TO_TIMESTAMP(P_YYYYMM||'-01', 'YYYY-MM-DD'), 1);
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.B_GET_APPMINIPDP (
	p_syskind IN VARCHAR2,
	p_userid  IN VARCHAR2,
	p_cursor OUT SYS_REFCURSOR
)
IS
BEGIN
	OPEN p_cursor FOR
	
	WITH usr AS (
		SELECT   u.syskind                                                              AS syskind,
				 u.level1cd                                                             AS level1cd,
				 u.level2cd                                                             AS level2cd,
				 u.userid                                                               AS userid,
				 u.peripheralnumber                                                     AS peripheralnumber,
				 sa.logid                                                               AS logid,
				 NVL(SUM(sa.acdcalls), 0)                                               AS ibcnt,
				 NVL(SUM(sa.acdtime), 0)                                                AS ibsec,
				 NVL(SUM(sa.auxoutcalls), 0)+ NVL(SUM(sa.acwoutcalls), 0)               AS obcnt,
				 NVL(SUM(sa.auxouttime), 0)+ NVL(SUM(sa.acwouttime), 0)                 AS obsec,
				 NVL(SUM(sa.acdcalls), 0)+ NVL(SUM(sa.auxoutcalls + sa.acwoutcalls), 0) AS totalcall,
				 RANK() OVER (PARTITION BY u.level1cd, u.level2cd ORDER BY NVL(SUM(sa.acdcalls), 0) DESC) AS "rank"
		FROM	 swm.t_user u
				 LEFT OUTER JOIN swm.cagent sa
				 			  ON sa.logid = u.userid
		WHERE    u.deleted = 'N'
		AND      ('_ALL_' = p_syskind OR u.syskind = p_syskind)
		AND      ('_ALL_' = p_userid OR u.userid  = p_userid)
		AND      u.level1cd IS NOT NULL
		AND      u.level2cd IS NOT NULL
		AND      LENGTH(u.userid) <= '10'
		GROUP BY u.syskind, u.level1cd, u.level2cd, u.userid, u.peripheralnumber, sa.logid
	),
	usr_skill AS (
		SELECT  cs.dbid        AS sk_dbid,
				cp.employee_id AS employee_id,
				t.dg_name      AS dg_name,
				t.sk_ename     AS sk_ename
		FROM    cc.cfg_person cp 
				INNER JOIN cc.cfg_skill_level csl
						ON cp.dbid     = csl.person_dbid
					   AND csl.level_ != 0
				INNER JOIN cc.cfg_skill cs
						ON csl.skill_dbid = cs.dbid
				INNER JOIN (SELECT dm.dg_name, dm.sk_dbid, dm.sk_kname, dm.sk_ename
							FROM   swm.t_ivr_dg_map dm
								   INNER JOIN swm.v_menu_visual_map vm
										   ON dm.menu_code = vm.menucode
							WHERE  sk_dbid IS NOT NULL AND sk_dbid != 999999 AND sk_kname IS NOT NULL AND sk_ename IS NOT NULL
							UNION ALL
							SELECT dm.dg_name, TO_NUMBER(c.code) AS sk_dbid, c.data1 AS sk_kname, c.name AS sk_ename
							FROM   swm.t_ivr_dg_map dm
								  ,swm.ax_common_code_m c
							WHERE  dm.vipyn   = 'Y'
							AND    c.use_yn   = 'Y'
							AND    c.group_cd = 'CFG_SKILL_CD'
							AND    c.name     = 'CS_A_VIP') t
						ON cs.dbid = t.sk_dbid
	)
	SELECT  u.syskind,
			u.level1cd,
			u.level2cd,
			u.userid,
			u.peripheralnumber,
			u.ibcnt,
			u.ibsec,
			u.obcnt,
			u.obsec,
			u.totalcall AS totcnt,
			CASE WHEN u.logid IS NULL OR u.ibcnt < 1 THEN 0 ELSE u."rank" END AS rank,
			NVL((SELECT SUM(inqueue)
				 FROM   swm.cskill cs
						INNER JOIN usr_skill us
								ON cs.split = us.dg_name
							   AND us.employee_id = u.userid), 0) AS qcnt,
			NVL((SELECT COUNT(DISTINCT logid)
				 FROM   swm.cagent ca
						INNER JOIN usr_skill us
								ON ca.logid = us.employee_id
				 WHERE  ca.workmode = '4'
				 AND    EXISTS (SELECT 1 FROM usr_skill x WHERE x.employee_id = u.userid AND x.sk_dbid = us.sk_dbid)), 0) AS readyagents
	FROM	usr u;
END;

CREATE OR REPLACE PROCEDURE SWM.b_get_TMAgent_Call(
	P_GB				IN VARCHAR2, 		--시작날짜
	P_VAL				IN VARCHAR2,		--종료날짜
	P_outCursor 		OUT SYS_REFCURSOR
)
IS
	v_SDT	VARCHAR2(14);
BEGIN
	IF P_GB = 'DT' THEN 
		IF P_VAL = '' THEN v_SDT := '2500-01-01';
		ELSE v_SDT := P_VAL;
		END IF;
	ELSIF P_GB = 'DD' THEN v_SDT := TO_CHAR(SYSTIMESTAMP-1*(CAST(P_VAL AS NUMBER)), 'YYYYMMDDHH24MISS');
	ELSIF P_GB = 'HH' THEN v_SDT := TO_CHAR(SYSTIMESTAMP-(1/24*(CAST(P_VAL AS NUMBER))), 'YYYYMMDDHH24MISS');
	ELSIF P_GB = 'MI' THEN v_SDT := TO_CHAR(SYSTIMESTAMP-(1/24/60*(CAST(P_VAL AS NUMBER))), 'YYYYMMDDHH24MISS');
	ELSE v_SDT := '2500-01-01';
	END IF;
	
	OPEN P_outCursor FOR
	SELECT 
		  substr(swm.U2K(TTIMETS),1,10)  AS DT
		, swm.U2K(TTIMETS - (TDIAL + TRING + TTALK  +  TTALKHOLD  + TDIALHOLD) ) AS DateTimeStart -- DateAdd(ss,-1*Duration,DateTime) as DateTimeStart
		, swm.U2K(TTIMETS) AS DateTimeEnd -- a.DateTime as DateTimeEnd 
		, U.Name AS Name
		, U.PeriPheralNumber as CtiId
		, THISDN as ExtNo
		, Level1Cd,Level2Cd,Level3Cd
		, OtherDN AS DigitsDialed  -- 발신번호 OtherDN
		, TDIAL + TRING +  TTALK +  TTALKHOLD  + TDIALHOLD AS Duration  
		, TDIAL + TRING AS DelayTime -- (RingTime+DelayTime) as DelayTime  -- 발신/링 시간   TDIAL + TRING
		, TTALK AS TalkTime  -- 통화시간  TTALK
		, TTALKHOLD  + TDIALHOLD AS HoldTime  -- HOLD 시간  HOLD  +  TTALKHOLD  + TDIALHOLD
	FROM SW.CALLSTAT C,
	     CC.CFG_PERSON P,
	    swm.T_USER U
	WHERE ETIMETS >= swm.k2u(v_SDT)
	  AND C.PERSON  = P.DBID
	  AND P.Employee_ID = U.Userid 
	  AND U.SysKind='TM'
	order by 1,2;
	
END;

CREATE OR REPLACE PROCEDURE SWM.b_set_appcallback(
	P_SysKind			IN VARCHAR2, 		--시스템구분(CS/TM)
	P_CallbackId		IN VARCHAR2,		--콜백예약이력번호
	P_AcptDate			IN VARCHAR2,		--콜백접수일시("YYYY-MM-DD hh:mi:ss")
	P_PhoneNo			IN VARCHAR2,		--통화예약전화번호
	P_CustNo			IN VARCHAR2,		--고객번호
	P_CustNm			IN VARCHAR2,		--고객명 (테이블에 쌓지 않음)
	P_MenuCd			IN VARCHAR2,		--IVR메뉴코드
	P_UserId	 	    IN VARCHAR2,		--상담사ID
	P_ProcDate			IN VARCHAR2,   	--콜백처리일시("YYYY-MM-DD hh:mi:ss",전화걸기일시)
	P_ProcYN			IN VARCHAR2     	--콜백처리여부(Y:처리, N:미처리, D:삭제)
)
IS
	v_AcptDate	TIMESTAMP;
	v_ProcDate	TIMESTAMP;
BEGIN
	v_AcptDate := TO_TIMESTAMP(P_AcptDate, 'YYYY-MM-DD HH24:MI:SS');
	v_ProcDate := TO_TIMESTAMP(P_ProcDate, 'YYYY-MM-DD HH24:MI:SS');
	MERGE INTO swm.t_appcallback trg
	USING 
		(SELECT
			P_SysKind			AS syskind,
			P_CallbackId		AS callbackid,
			v_AcptDate			AS acptdate,
			P_PhoneNo			AS phoneno,
			P_CustNo			AS custno,
			P_MenuCd			AS menucd,
			P_UserId			AS userid,
			v_ProcDate			AS procdate,
			P_ProcYN			AS procyn
		FROM dual) src
	ON (src.syskind = trg.syskind AND src.callbackid = trg.callbackid)
	WHEN MATCHED THEN
		UPDATE SET
			trg.acptdate    	= src.acptdate,
			trg.phoneno    		= src.phoneno,
			trg.custno    		= src.custno,
			trg.menucd    		= src.menucd,
			trg.userid    		= src.userid,
			trg.procdate    	= src.procdate,
			trg.procyn    		= src.procyn,
			trg.lastmodifydate 	= SYSTIMESTAMP
	WHEN NOT MATCHED THEN
		INSERT (
			syskind,
			callbackid,
			acptdate,
			phoneno,
			custno,
			menucd,
			userid,
			procdate,
			procyn,
			createdate,
			lastmodifydate
		)
		VALUES (
			src.syskind,
			src.callbackid,
			src.acptdate,
			src.phoneno,
			src.custno,
			src.menucd,
			src.userid,
			src.procdate,
			src.procyn,
			SYSTIMESTAMP,
			SYSTIMESTAMP
		);
		
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.b_set_appcode(
	P_SysKind		IN VARCHAR2,  		--시스템구분(CS/TM)
	P_LrgCd			IN VARCHAR2, 		--대분류코드
	P_LrgNm			IN VARCHAR2,		--대분류코드명
	P_SmlCd			IN VARCHAR2, 		--소분류코드
	P_SmlNm			IN VARCHAR2,		--소분류코드명
	P_Seq			IN NUMBER,		 	--정렬순서
	P_UseYN			IN VARCHAR2		 	--사용여부(Y/N)
)
IS
BEGIN
	BEGIN
		MERGE INTO swm.t_appcode trg
		USING 
			(SELECT
				P_SysKind		AS syskind,
				P_LrgCd			AS lrgcd,
				P_LrgNm			AS lrgnm,
				P_SmlCd			AS smlcd,
				P_SmlNm			AS smlnm,
				P_Seq			AS seq,
				P_UseYN			AS useyn
			FROM dual) src
		ON (src.syskind = trg.syskind AND src.lrgcd = trg.lrgcd AND src.smlcd = trg.smlcd)
		WHEN MATCHED THEN
			UPDATE SET
				trg.lrgnm    		= src.lrgnm,
				trg.smlnm  			= src.smlnm,
				trg.seq     		= src.seq,
				trg.useyn			= src.useyn,
				trg.lastmodifydate 	= SYSTIMESTAMP
		WHEN NOT MATCHED THEN
			INSERT (
				syskind,
				lrgcd,
				lrgnm,
				smlcd,
				smlnm,
				seq,
				useyn,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.syskind,
				src.lrgcd,
				src.lrgnm,
				src.smlcd,
				src.smlnm,
				src.seq,
				src.useyn,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			);
	END;
	BEGIN
		MERGE INTO swm.t_cdinf trg
		USING 
			(SELECT
				P_SysKind || '_' || P_LrgCd	AS lrgcd,
				P_LrgNm			AS lrgnm,
				P_SmlCd			AS smlcd,
				P_SmlNm			AS smlnm,
				(CASE WHEN P_UseYN = 'Y' THEN 'N' ELSE 'Y' END) AS deleted,
				P_Seq			AS seq,
				'Y'				AS edityn
			FROM dual) src
		ON (src.lrgcd = trg.lrgcd AND src.smlcd = trg.smlcd)
		WHEN MATCHED THEN
			UPDATE SET
				trg.lrgnm    		= src.lrgnm,
				trg.smlnm  			= src.smlnm,
				trg.deleted  		= src.deleted,
				trg.seq     		= src.seq,
				trg.edityn			= src.edityn,
				trg.lastmodifydate 	= SYSTIMESTAMP
		WHEN NOT MATCHED THEN
			INSERT (
				lrgcd,
				lrgnm,
				smlcd,
				smlnm,
				deleted,
				seq,
				edityn,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.lrgcd,
				src.lrgnm,
				src.smlcd,
				src.smlnm,
				src.deleted,
				src.seq,
				src.edityn,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			);
	END ;
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.b_set_appcounsel(
	P_SysKind		IN VARCHAR2,  	--시스템구분(CS/TM)
	P_DT			IN VARCHAR2,	--상담일자("YYYY-MM-DD")
	P_LEVEL1CD		IN VARCHAR2,  	--센터코드
	P_LEVEL2CD		IN VARCHAR2,  	--그룹코드
	P_LEVEL3CD		IN VARCHAR2,  	--팀코드
	P_UserId		IN VARCHAR2, 	--사용자ID
	P_LrgCd			IN VARCHAR2, 	--상담대분류코드
	P_SmlCd			IN VARCHAR2, 	--상담소분류코드
	P_Cnt			IN NUMBER,		--상담건수(일)
	P_ClsSec		IN NUMBER		--통화시간(초)	
)
IS
BEGIN
	MERGE INTO swm.t_appcounsel target
	USING 
		(SELECT
			P_SysKind			AS syskind,
			P_DT				AS dt,
			P_LEVEL1CD			AS level1cd,
			P_LEVEL2CD			AS level2cd,
			P_LEVEL3CD			AS level3cd,
			P_UserId			AS userid,
			NVL(P_LrgCd, ' ')	AS lrgcd,
			NVL(P_SmlCd, ' ')	AS smlcd,
			P_Cnt				AS cnt,
			P_ClsSec			AS clssec
		FROM dual) source
	ON (source.syskind = target.syskind AND source.dt = target.dt AND source.level1cd = target.level1cd AND source.level2cd = target.level2cd 
		AND source.level3cd = target.level3cd AND source.userid = target.userid AND source.lrgcd = target.lrgcd AND source.smlcd = target.smlcd)
	WHEN MATCHED THEN
		UPDATE SET
			target.cnt    			= source.cnt,
			target.clssec    		= source.clssec,
			target.lastmodifydate 	= SYSTIMESTAMP
	WHEN NOT MATCHED THEN
		INSERT (
			syskind,
			dt,
			level1cd,
			level2cd,
			level3cd,
			userid,
			lrgcd,
			smlcd,
			cnt,
			clssec,
			createdate,
			lastmodifydate
		)
		VALUES (
			source.syskind,
			source.dt,
			source.level1cd,
			source.level2cd,
			source.level3cd,
			source.userid,
			source.lrgcd,
			source.smlcd,
			source.cnt,
			source.clssec,
			SYSTIMESTAMP,
			SYSTIMESTAMP
		);
		
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.b_set_appholiday(
	P_SysKind		IN VARCHAR2, 				--시스템구분(CS/TM)
	P_CNTR_CD		IN VARCHAR2,				--지점 코드
	P_SOLAR_DT		IN VARCHAR2,   			--양력일자("YYYY-MM-DD")
	P_HLD_YN		IN VARCHAR2, 				--휴일 여부(Y/N) 토+일+공휴일
	P_PHLD_YN		IN VARCHAR2 DEFAULT 'N' 	--공휴일 여부(Y/N) 공휴일
)
IS
	v_SOLAR_DT TIMESTAMP;
BEGIN
	v_SOLAR_DT := TO_TIMESTAMP(P_SOLAR_DT, 'YYYY-MM-DD');
	BEGIN
		MERGE INTO swm.t_appholiday trg
		USING 
			(SELECT
				P_SysKind			AS syskind,
				P_CNTR_CD			AS cntr_cd,
				v_SOLAR_DT 			AS solar_dt,
				P_HLD_YN			AS hld_yn,
				P_PHLD_YN			AS phld_yn
			FROM dual) src
		ON (src.syskind = trg.syskind AND src.cntr_cd = trg.cntr_cd AND src.solar_dt = trg.solar_dt)
		
		WHEN MATCHED THEN
			UPDATE SET
				trg.hld_yn    		= src.hld_yn,
				trg.phld_yn    		= src.phld_yn,
				trg.lastmodifydate 	= SYSTIMESTAMP
				
		WHEN NOT MATCHED THEN
			INSERT (
				syskind,
				cntr_cd,
				solar_dt,
				hld_yn,
				phld_yn,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.syskind,
				src.cntr_cd,
				src.solar_dt,
				src.hld_yn,
				src.phld_yn,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			);
	END;
	BEGIN
		MERGE INTO swm.t_holiday trg
		USING 
			(SELECT DISTINCT
				omm.mrp AS mrp,
				syskind AS syskind,
				cntr_cd AS cntr_cd,
				solar_dt AS solar_dt,
				TO_CHAR(solar_dt, 'YYYY') AS "year",
				TO_CHAR(solar_dt, 'MMDD') AS mmdd,
				hld_yn AS hld_yn,
				phld_yn AS phld_yn
			FROM swm.t_appholiday ahd
			LEFT OUTER JOIN swm.t_orgmrpmapping omm ON ahd.cntr_cd = omm.level1cd
			WHERE 	ahd.cntr_cd = P_CNTR_CD
			AND 	ahd.solar_dt = v_SOLAR_DT
			AND 	ahd.syskind = P_SysKind
			AND 	omm.mrp IS NOT NULL
			) src
		ON (src.mrp = trg.mrp AND src."year" = trg.holidayyear AND src.mmdd = trg.holidaymonthday)
		
		WHEN MATCHED THEN
			UPDATE SET
				trg.kind			= 1,
				trg.dt				= TO_CHAR(src.solar_dt, 'YYYY-MM-DD'),
				trg.descript		= (CASE WHEN src.phld_yn='Y' THEN '공휴일' ELSE null END),
				trg.phld_yn    		= src.phld_yn,
				trg.lastmodifydate 	= SYSTIMESTAMP
			WHERE (src.hld_yn='Y' OR src.phld_yn='Y')
			
		WHEN NOT MATCHED THEN
			INSERT (
				mrp,
				holidayyear,
				holidaymonthday,
				dt,
				kind,
				descript,
				mentfilenm,
				phld_yn,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.mrp,
				src."year",
				src.mmdd,
				TO_CHAR(src.solar_dt, 'YYYY-MM-DD'),
				'1',
				(CASE WHEN src.phld_yn='Y' THEN '공휴일' ELSE null END),
				null,
				src.phld_yn,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			)
			WHERE (src.hld_yn = 'Y' OR src.phld_yn = 'Y');
	END; 
	BEGIN
		DELETE FROM swm.t_holiday trg 
		WHERE EXISTS (
			SELECT 1
			FROM (
				SELECT DISTINCT
					omm.mrp AS mrp,
					syskind AS syskind,
					cntr_cd AS cntr_cd,
					solar_dt AS solar_dt,
					TO_CHAR(solar_dt, 'YYYY') AS "year",
					TO_CHAR(solar_dt, 'MMDD') AS mmdd,
					hld_yn AS hld_yn,
					phld_yn AS phld_yn
				FROM swm.t_appholiday ahd
				LEFT OUTER JOIN swm.t_orgmrpmapping omm ON ahd.cntr_cd = omm.level1cd
				WHERE 	ahd.cntr_cd = P_CNTR_CD
				AND 	ahd.solar_dt = v_SOLAR_DT
				AND 	ahd.syskind = P_SysKind
				AND 	omm.mrp IS NOT NULL
			) src
			WHERE src.mrp = trg.mrp 
			AND src."year" = trg.holidayyear 
			AND src.mmdd = trg.holidaymonthday
			AND src.hld_yn ='N' 
			AND src.phld_yn = 'N'
		);
	END;
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.b_set_appholiday_tm(
	P_SysKind		IN VARCHAR2, 				--시스템구분(CS/TM)
	P_CNTR_CD		IN VARCHAR2,				--지점 코드
	P_SOLAR_DT		IN VARCHAR2,   			--양력일자("YYYY-MM-DD")
	P_HLD_YN		IN VARCHAR2, 				--휴일 여부(Y/N) 토+일+공휴일
	P_PHLD_YN		IN VARCHAR2 DEFAULT 'N' 	--공휴일 여부(Y/N) 공휴일
)
IS
	v_SOLAR_DT TIMESTAMP;
BEGIN
	v_SOLAR_DT := TO_TIMESTAMP(P_SOLAR_DT, 'YYYY-MM-DD');
	BEGIN
		MERGE INTO swm.t_appholiday_tm trg
		USING 
			(SELECT
				P_SysKind			AS syskind,
				P_CNTR_CD			AS cntr_cd,
				v_SOLAR_DT			AS solar_dt,
				P_HLD_YN			AS hld_yn,
				P_PHLD_YN			AS phld_yn
			FROM dual) src
		ON (src.syskind = trg.syskind AND src.cntr_cd = trg.cntr_cd AND src.solar_dt = trg.solar_dt)
		
		WHEN MATCHED THEN
			UPDATE SET
				trg.hld_yn    		= src.hld_yn,
				trg.phld_yn    		= src.phld_yn,
				trg.lastmodifydate 	= SYSTIMESTAMP
				
		WHEN NOT MATCHED THEN
			INSERT (
				syskind,
				cntr_cd,
				solar_dt,
				hld_yn,
				phld_yn,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.syskind,
				src.cntr_cd,
				src.solar_dt,
				src.hld_yn,
				src.phld_yn,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			);
	END;
	BEGIN
		MERGE INTO swm.t_holiday_tm trg
		USING 
			(SELECT DISTINCT
				omm.smlcd AS mrp,
				syskind AS syskind,
				cntr_cd AS cntr_cd,
				solar_dt AS solar_dt,
				TO_CHAR(solar_dt, 'YYYY') AS "year",
				TO_CHAR(solar_dt, 'MMDD') AS mmdd,
				hld_yn AS hld_yn,
				phld_yn AS phld_yn
			FROM swm.t_appholiday_tm ahd
			LEFT OUTER JOIN swm.t_cdinf omm ON ahd.cntr_cd = omm.linkcd
			WHERE 	ahd.cntr_cd = P_CNTR_CD
			AND 	ahd.solar_dt = v_SOLAR_DT
			AND 	ahd.syskind = P_SysKind
			AND 	omm.smlcd IS NOT NULL
			) src
		ON (src.mrp = trg.mrp AND src."year" = trg.holidayyear AND src.mmdd = trg.holidaymonthday)
		
		WHEN MATCHED THEN
			UPDATE SET
				trg.kind			= 1,
				trg.dt				= TO_CHAR(src.solar_dt, 'YYYY-MM-DD'),
				trg.descript		= (CASE WHEN src.phld_yn='Y' THEN '공휴일' ELSE null END),
				trg.phld_yn    		= src.phld_yn,
				trg.lastmodifydate 	= SYSTIMESTAMP
			WHERE (src.hld_yn='Y' OR src.phld_yn='Y')
			
		WHEN NOT MATCHED THEN
			INSERT (
				mrp,
				holidayyear,
				holidaymonthday,
				dt,
				kind,
				descript,
				mentfilenm,
				phld_yn,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.mrp,
				src."year",
				src.mmdd,
				TO_CHAR(src.solar_dt, 'YYYY-MM-DD'),
				'1',
				(CASE WHEN src.phld_yn='Y' THEN '공휴일' ELSE null END),
				null,
				src.phld_yn,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			)
			WHERE (src.hld_yn = 'Y' OR src.phld_yn = 'Y');
	END; 
	BEGIN
		DELETE FROM swm.t_holiday_tm trg 
		WHERE EXISTS (
			SELECT 1
			FROM (
				SELECT DISTINCT
					omm.smlcd AS mrp,
					syskind AS syskind,
					cntr_cd AS cntr_cd,
					solar_dt AS solar_dt,
					TO_CHAR(solar_dt, 'YYYY') AS "year",
					TO_CHAR(solar_dt, 'MMDD') AS mmdd,
					hld_yn AS hld_yn,
					phld_yn AS phld_yn
				FROM swm.t_appholiday_tm ahd
				LEFT OUTER JOIN swm.t_cdinf omm ON ahd.cntr_cd = omm.linkcd
				WHERE 	ahd.cntr_cd = P_CNTR_CD
				AND 	ahd.solar_dt = v_SOLAR_DT
				AND 	ahd.syskind = P_SysKind
				AND 	omm.smlcd IS NOT NULL
			) src
			WHERE src.mrp = trg.mrp 
			AND src."year" = trg.holidayyear 
			AND src.mmdd = trg.holidaymonthday
			AND src.hld_yn ='N' 
			AND src.phld_yn = 'N'
		);
	END;
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.b_set_apporg(
	P_SysKind		IN VARCHAR2, 		--시스템구분(CS/TM)
	P_ORG_LVL		IN VARCHAR2,		--조직레벨(CS: 센터:1,그룹:2,팀:3TM : 지점,그룹,팀)
	P_ORG_CD		IN VARCHAR2, 		--조직코드	
	P_ORG_UP_CD		IN VARCHAR2, 		--조직상위코드(그룹일 경우 지점코드, 팀일 경우 그룹코드)
	P_ORG_NM		IN VARCHAR2,		--조직명
	P_CNTR_TYP_CD	IN VARCHAR2,		--지점유형(TM 시스템에서 지점유형 -OUB:OutBound지점 -POM:POM지점 -AGT:대리점)
	P_BIZ_CLAS_CD	IN VARCHAR2,		--업무분류코드(CS의 경우 inbound, Outbound, Help, Claim, IT-Help)
	P_USE_YN	 	IN VARCHAR2,		--사용여부(Y/N)
	P_SRT_SEQ		IN NUMBER,			--정렬순서(센터,그룹,팀 의 정렬순서)
	P_SRT_KEY		IN VARCHAR2 		--센터^그룹^팀의 조합
)
IS
BEGIN
	MERGE INTO swm.t_apporg trg
	USING 
		(SELECT
			P_SysKind		AS syskind,
			P_ORG_LVL		AS org_lvl,
			P_ORG_CD		AS org_cd,
			NVL(P_ORG_UP_CD, ' ')	AS org_up_cd,
			P_ORG_NM		AS org_nm,
			P_CNTR_TYP_CD	AS cntr_typ_cd,
			P_BIZ_CLAS_CD	AS biz_clas_cd,
			P_USE_YN	 	AS use_yn,
			P_SRT_SEQ		AS srt_seq,
			P_SRT_KEY		AS srt_key
		FROM dual) src
	ON (	src.syskind 	= trg.syskind 
		AND src.org_cd 		= trg.org_cd
		AND src.org_lvl 	= trg.org_lvl
		AND src.org_up_cd 	= trg.org_up_cd)
	WHEN MATCHED THEN
		UPDATE SET
			trg.org_nm     		= src.org_nm,
			trg.cntr_typ_cd		= src.cntr_typ_cd,
			trg.biz_clas_cd		= src.biz_clas_cd,
			trg.use_yn     		= src.use_yn,
			trg.srt_seq    		= src.srt_seq,
			trg.srt_key    		= src.srt_key,
			trg.lastmodifydate 	= SYSTIMESTAMP
	WHEN NOT MATCHED THEN
		INSERT (
			syskind,
			org_cd,
			org_lvl,
			org_up_cd,
			org_nm,
			cntr_typ_cd,
			biz_clas_cd,
			use_yn,
			srt_seq,
			srt_key,
			createdate,
			lastmodifydate
		)
		VALUES (
			src.syskind,
			src.org_cd,
			src.org_lvl,
			src.org_up_cd,
			src.org_nm,
			src.cntr_typ_cd,
			src.biz_clas_cd,
			src.use_yn,
			src.srt_seq,
			src.srt_key,
			SYSTIMESTAMP,
			SYSTIMESTAMP
		);
		
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.b_set_apptmagent_day(
	P_DT				IN VARCHAR2,		-- 일자
	P_CNTR_CD			IN VARCHAR2,		-- 센터코드
	P_GRP_CD			IN VARCHAR2,		-- 그룹코드
	P_TEAM_CD			IN VARCHAR2,		-- 팀코드
	P_USR_ID			IN VARCHAR2,		-- 상담사ID
	P_CONT_ST_NM		IN VARCHAR2,		-- 구분 신계약(1)/해약(2)/정산(3)
	P_TOT_CNT			IN NUMBER,			-- 전체건수
	P_TOT_AMT			IN NUMBER,			-- 전체금액
	P_GUARANT_CNT		IN NUMBER,			-- 보장성건수
	P_GUARANT_AMT		IN NUMBER,			-- 보장성건수
	P_EXCHNG_GRD		IN NUMBER			-- 보장성건수
)
IS
	v_dt		VARCHAR2(10);
BEGIN
	
	v_dt := swm.f_conv_dt(P_DT);
	
	BEGIN
		MERGE INTO swm.t_apptmagent_day trg
		USING 
			(SELECT
				v_dt					AS dt,
				P_CNTR_CD				AS cntr_cd,
				P_GRP_CD				AS grp_cd,
				P_TEAM_CD				AS team_cd,
				P_USR_ID				AS usr_id,
				P_CONT_ST_NM			AS cont_st_nm,
				P_TOT_CNT				AS tot_cnt,
				P_TOT_AMT				AS tot_amt,
				P_GUARANT_CNT			AS guarant_cnt,
				P_GUARANT_AMT			AS guarant_amt,
				P_EXCHNG_GRD			AS exchng_grd
			FROM dual) src
		ON (src.dt = trg.dt AND src.cntr_cd = trg.cntr_cd AND src.grp_cd = trg.grp_cd 
		AND src.team_cd = trg.team_cd AND src.usr_id = trg.usr_id AND src.cont_st_nm = trg.cont_st_nm)
		WHEN MATCHED THEN
			UPDATE SET
				trg.tot_cnt    		= src.tot_cnt,
				trg.tot_amt    		= src.tot_amt,
				trg.guarant_cnt    	= src.guarant_cnt,
				trg.guarant_amt    	= src.guarant_amt,
				trg.exchng_grd    	= src.exchng_grd,
				trg.lastmodifydate 	= SYSTIMESTAMP
		WHEN NOT MATCHED THEN
			INSERT (
				dt,
				cntr_cd,
				grp_cd,
				team_cd,
				usr_id,
				cont_st_nm,
				tot_cnt,
				tot_amt,
				guarant_cnt,
				guarant_amt,
				exchng_grd,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.dt,
				src.cntr_cd,
				src.grp_cd,
				src.team_cd,
				src.usr_id,
				src.cont_st_nm,
				src.tot_cnt,
				src.tot_amt,
				src.guarant_cnt,
				src.guarant_amt,
				src.exchng_grd,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			);
	END;
	/*MIX 프로시저 호출*/
	swm.b_set_apptmagent_day_mix(
		v_dt,
	   	P_CNTR_CD,
	   	P_GRP_CD,
	   	P_TEAM_CD,
	   	P_USR_ID,
	   	P_CONT_ST_NM,
  	 	P_TOT_CNT,
	   	P_TOT_AMT,
	   	P_GUARANT_CNT,
	   	P_GUARANT_AMT,
	   	P_EXCHNG_GRD
	);
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.b_set_apptmagent_day_mix(
	P_DT				IN VARCHAR2,		-- 일자
	P_CNTR_CD			IN VARCHAR2,		-- 센터코드
	P_GRP_CD			IN VARCHAR2,		-- 그룹코드
	P_TEAM_CD			IN VARCHAR2,		-- 팀코드
	P_USR_ID			IN VARCHAR2,		-- 상담사ID
	P_CONT_ST_NM		IN VARCHAR2,		-- 구분 신계약(1)/해약(2)/정산(3)
	P_TOT_CNT			IN NUMBER,			-- 전체건수
	P_TOT_AMT			IN NUMBER,			-- 전체금액
	P_GUARANT_CNT		IN NUMBER,			-- 보장성건수
	P_GUARANT_AMT		IN NUMBER,			-- 보장성건수
	P_EXCHNG_GRD		IN NUMBER,			-- 보장성건수
	P_DATA_SRC			IN VARCHAR2 DEFAULT 'APP' -- 데이터원본여부(APP:1차,2차 / EAI:3차복합)
)
IS
	v_dt			VARCHAR2(10);
	v_cntr_exist	NUMBER(10);
BEGIN
	v_dt := swm.f_conv_dt(P_DT);
	BEGIN
		SELECT count(*) INTO v_cntr_exist FROM dual
		WHERE P_CNTR_CD IN(SELECT level1cd FROM swm.v_orginf WHERE syskind='TM' AND divcd='5' AND org_lvl=1);
	END;
	
	IF NOT (P_DATA_SRC='APP' AND v_cntr_exist > 0) THEN
		BEGIN
			MERGE INTO swm.t_apptmagent_day_mix trg
			USING 
				(SELECT
					v_dt				AS dt,
					P_CNTR_CD			AS cntr_cd,
					P_GRP_CD			AS grp_cd,
					P_TEAM_CD			AS team_cd,
					P_USR_ID			AS usr_id,
					P_CONT_ST_NM		AS cont_st_nm,
					P_TOT_CNT			AS tot_cnt,
					P_TOT_AMT			AS tot_amt,
					P_GUARANT_CNT		AS guarant_cnt,
					P_GUARANT_AMT		AS guarant_amt,
					P_EXCHNG_GRD		AS exchng_grd
				FROM dual) src
			ON (src.dt = trg.dt AND src.cntr_cd = trg.cntr_cd AND src.grp_cd = trg.grp_cd 
			AND src.team_cd = trg.team_cd AND src.usr_id = trg.usr_id AND src.cont_st_nm = trg.cont_st_nm)
			WHEN MATCHED THEN
				UPDATE SET
					trg.tot_cnt    		= src.tot_cnt,
					trg.tot_amt    		= src.tot_amt,
					trg.guarant_cnt    	= src.guarant_cnt,
					trg.guarant_amt    	= src.guarant_amt,
					trg.exchng_grd    	= src.exchng_grd,
					trg.lastmodifydate 	= SYSTIMESTAMP
			WHEN NOT MATCHED THEN
				INSERT (
					dt,
					cntr_cd,
					grp_cd,
					team_cd,
					usr_id,
					cont_st_nm,
					tot_cnt,
					tot_amt,
					guarant_cnt,
					guarant_amt,
					exchng_grd,
					createdate,
					lastmodifydate
				)
				VALUES (
					src.dt,
					src.cntr_cd,
					src.grp_cd,
					src.team_cd,
					src.usr_id,
					src.cont_st_nm,
					src.tot_cnt,
					src.tot_amt,
					src.guarant_cnt,
					src.guarant_amt,
					src.exchng_grd,
					SYSTIMESTAMP,
					SYSTIMESTAMP
				);
		END;
	END IF;
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.b_set_apptmagent_month(
	P_DT				IN VARCHAR2,		-- 일자
	P_CNTR_CD			IN VARCHAR2,		-- 센터코드
	P_GRP_CD			IN VARCHAR2,		-- 그룹코드
	P_TEAM_CD			IN VARCHAR2,		-- 팀코드
	P_USR_ID			IN VARCHAR2,		-- 상담사id 
	P_STD_CNT			IN NUMBER,			-- 전체건수
	P_STD_AMT			IN NUMBER,			-- 전체금액
	P_CALL_TIME			IN NUMBER			-- 통화시간
)
IS
	v_dt		VARCHAR2(10);
BEGIN
	v_dt := swm.f_conv_dt(P_DT);
	BEGIN
		MERGE INTO swm.t_apptmagent_month trg
		USING 
			(SELECT
				v_dt					AS dt,
				P_CNTR_CD				AS cntr_cd,
				P_GRP_CD				AS grp_cd,
				P_TEAM_CD				AS team_cd,
				P_USR_ID				AS usr_id,
				P_STD_CNT				AS std_cnt,
				P_STD_AMT				AS std_amt,
				P_CALL_TIME				AS call_time
			FROM dual) src
		ON (src.dt = trg.dt AND src.cntr_cd = trg.cntr_cd AND src.grp_cd = trg.grp_cd 
		AND src.team_cd = trg.team_cd AND src.usr_id = trg.usr_id)
		WHEN MATCHED THEN
			UPDATE SET
				trg.std_cnt    		= src.std_cnt,
				trg.std_amt    		= src.std_amt,
				trg.call_time    	= src.call_time,
				trg.lastmodifydate 	= SYSTIMESTAMP
		WHEN NOT MATCHED THEN
			INSERT (
				dt,
				cntr_cd,
				grp_cd,
				team_cd,
				usr_id,
				std_cnt,
				std_amt,
				call_time,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.dt,
				src.cntr_cd,
				src.grp_cd,
				src.team_cd,
				src.usr_id,
				src.std_cnt,
				src.std_amt,
				src.call_time,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			);
	END; 
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.b_set_apptmagent_time(
	P_DT			IN VARCHAR2,		-- 일자
	P_TM			IN VARCHAR2,		-- 시간
	P_CNTR_CD		IN VARCHAR2,		-- 센터코드
	P_GRP_CD		IN VARCHAR2,		-- 그룹코드
	P_TEAM_CD		IN VARCHAR2,		-- 팀코드
	P_USR_ID		IN VARCHAR2,		-- 상담사id 
	P_CONT_DV_NM	IN VARCHAR2,		-- 구분 (청약/계약/콜)
	P_CNT			IN NUMBER,			-- 건수
	P_INSUFEE		IN NUMBER			-- 금액 (월납기준보험료)
)
IS
	v_dt		VARCHAR2(10);
BEGIN
	v_dt := swm.f_conv_dt(P_DT);
	BEGIN
		MERGE INTO swm.t_apptmagent_time trg
		USING 
			(SELECT
				v_dt				AS dt,
				P_TM				AS tm,
				P_CNTR_CD			AS cntr_cd,
				P_GRP_CD			AS grp_cd,
				P_TEAM_CD			AS team_cd,
				P_USR_ID			AS usr_id,
				P_CONT_DV_NM		AS cont_dv_nm,
				P_CNT				AS cnt,
				P_INSUFEE			AS insufee
			FROM dual) src
		ON (src.dt = trg.dt AND src.tm = trg.tm AND src.cntr_cd = trg.cntr_cd AND src.grp_cd = trg.grp_cd 
		AND src.team_cd = trg.team_cd AND src.usr_id = trg.usr_id AND src.cont_dv_nm = trg.cont_dv_nm)
		WHEN MATCHED THEN
			UPDATE SET
				trg.cnt    			= src.cnt,
				trg.insufee    		= src.insufee,
				trg.lastmodifydate 	= SYSTIMESTAMP
		WHEN NOT MATCHED THEN
			INSERT (
				dt,
				tm,
				cntr_cd,
				grp_cd,
				team_cd,
				usr_id,
				cont_dv_nm,
				cnt,
				insufee,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.dt,
				src.tm,
				src.cntr_cd,
				src.grp_cd,
				src.team_cd,
				src.usr_id,
				src.cont_dv_nm,
				src.cnt,
				src.insufee,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			);
	END;
	
	b_set_apptmagent_time_mix(
		v_dt,
		P_TM,
		P_CNTR_CD,
		P_GRP_CD,
		P_TEAM_CD,
		P_USR_ID,
		P_CONT_DV_NM,
		P_CNT,
		P_INSUFEE);
			
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.b_set_apptmagent_time_mix(
	P_DT				IN VARCHAR2,		-- 일자
	P_TM				IN VARCHAR2,		-- 시간
	P_CNTR_CD			IN VARCHAR2,		-- 센터코드
	P_GRP_CD			IN VARCHAR2,		-- 그룹코드
	P_TEAM_CD			IN VARCHAR2,		-- 팀코드
	P_USR_ID			IN VARCHAR2,		-- 상담사id 
	P_CONT_DV_NM		IN VARCHAR2,		-- 구분 (청약/계약/콜)
	P_CNT				IN NUMBER,			-- 건수
	P_INSUFEE			IN NUMBER,			-- 금액 (월납기준보험료)
	P_DATA_SRC      	IN VARCHAR2 DEFAULT 'APP' -- 데이터원본여부(APP:1차,2차 / EAI:3차복합)
)
IS
	v_dt			VARCHAR2(10);
	v_cntr_exist	NUMBER(10);
BEGIN
	v_dt := swm.f_conv_dt(P_DT);
	BEGIN
		SELECT count(*) INTO v_cntr_exist FROM dual
		WHERE P_CNTR_CD IN(SELECT level1cd FROM swm.v_orginf WHERE syskind='TM' AND divcd='5' AND org_lvl=1);
	END;
	
	IF NOT (P_DATA_SRC='APP' AND P_CONT_DV_NM <> '3' AND v_cntr_exist > 0) THEN
		BEGIN
			MERGE INTO swm.t_apptmagent_time_mix trg
			USING 
				(SELECT
					P_DT					AS dt,
					P_TM					AS tm,
					P_CNTR_CD				AS cntr_cd,
					P_GRP_CD				AS grp_cd,
					P_TEAM_CD				AS team_cd,
					P_USR_ID				AS usr_id,
					P_CONT_DV_NM			AS cont_dv_nm,
					P_CNT					AS cnt,
					P_INSUFEE				AS insufee
				FROM dual) src
			ON (src.dt = trg.dt AND src.tm = trg.tm AND src.cntr_cd = trg.cntr_cd AND src.grp_cd = trg.grp_cd 
			AND src.team_cd = trg.team_cd AND src.usr_id = trg.usr_id AND src.cont_dv_nm = trg.cont_dv_nm)
			WHEN MATCHED THEN
				UPDATE SET
					trg.cnt    			= src.cnt,
					trg.insufee    		= src.insufee,
					trg.lastmodifydate 	= SYSTIMESTAMP
			WHEN NOT MATCHED THEN
				INSERT (
					dt,
					tm,
					cntr_cd,
					grp_cd,
					team_cd,
					usr_id,
					cont_dv_nm,
					cnt,
					insufee,
					createdate,
					lastmodifydate
				)
				VALUES (
					src.dt,
					src.tm,
					src.cntr_cd,
					src.grp_cd,
					src.team_cd,
					src.usr_id,
					src.cont_dv_nm,
					src.cnt,
					src.insufee,
					SYSTIMESTAMP,
					SYSTIMESTAMP
				);
		END; 
	END IF;
	
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.b_set_apptmteam_day(
	P_DT			IN VARCHAR2,		-- 일자
	P_CNTR_CD		IN VARCHAR2,		-- 센터코드
	P_GRP_CD		IN VARCHAR2,		-- 그룹코드
	P_TEAM_CD		IN VARCHAR2,		-- 팀코드
	P_SUBS_CNT		IN NUMBER,			-- 청약건수(Sales)
	P_SUBS_AMT		IN NUMBER,			-- 청약금액(월남기준보험료(MTPM_BASC_INSUFEE))
	P_CONT_CNT		IN NUMBER,			-- 계약건수
	P_CONT_AMT		IN NUMBER,			-- 계약금액(월남기준보험료(MTPM_BASC_INSUFEE))
	P_ETCO_CNT		IN NUMBER,			-- 재적(조회일현자 코드가 있는TMR)
	P_CALL_PRS_CNT	IN NUMBER,			-- 활동인원(a.TMR(교육제외),전산상 콜이력이 존재하는 TMR)
	P_TMR_CNT		IN NUMBER,			-- 가동인원(청약건수1이상인 TRM)
	P_CALL_CNT		IN NUMBER,			-- 통화시도건수(Dial(고객에게 통화시도건))
	P_CUR_CNT		IN NUMBER,			-- 통화성공건수(Contact 1)
	P_CUST_RSPS01	IN NUMBER,			-- 도입거절건수(통화결과값이 "도입거절"인 건수,BL등)
	P_EXEC_CNT		IN NUMBER,			-- DB컨택건수(Connected LIST(사용DB)
	P_LEAD_TIME		IN NUMBER			-- 전체콜타임(Total NTT)
)
IS
	v_dt		VARCHAR2(10);
BEGIN
	v_dt := swm.f_conv_dt(P_DT);
	BEGIN
		MERGE INTO swm.t_apptmteam_day trg
		USING 
			(SELECT
				v_dt				AS dt,
				P_CNTR_CD			AS cntr_cd,
				P_GRP_CD			AS grp_cd,
				P_TEAM_CD			AS team_cd,
				P_SUBS_CNT			AS subs_cnt,
				P_SUBS_AMT			AS subs_amt,
				P_CONT_CNT			AS cont_cnt,
				P_CONT_AMT			AS cont_amt,
				P_ETCO_CNT			AS etco_cnt,
				P_CALL_PRS_CNT		AS call_prs_cnt,
				P_TMR_CNT			AS tmr_cnt,
				P_CALL_CNT			AS call_cnt,
				P_CUR_CNT			AS cur_cnt,
				P_CUST_RSPS01		AS cust_rsps01,
				P_EXEC_CNT			AS exec_cnt,
				P_LEAD_TIME			AS lead_time
			FROM dual) src
		ON (src.dt = trg.dt AND src.cntr_cd = trg.cntr_cd AND src.grp_cd = trg.grp_cd 
		AND src.team_cd = trg.team_cd)
		WHEN MATCHED THEN
			UPDATE SET
				trg.subs_cnt    		= src.subs_cnt,
				trg.subs_amt    		= src.subs_amt,
				trg.cont_cnt    		= src.cont_cnt,
				trg.cont_amt    		= src.cont_amt,
				trg.etco_cnt    		= src.etco_cnt,
				trg.call_prs_cnt    	= src.call_prs_cnt,
				trg.tmr_cnt    			= src.tmr_cnt,
				trg.call_cnt    		= src.call_cnt,
				trg.cur_cnt    			= src.cur_cnt,
				trg.cust_rsps01    		= src.cust_rsps01,
				trg.exec_cnt    		= src.exec_cnt,
				trg.lead_time    		= src.lead_time,
				trg.lastmodifydate 	= SYSTIMESTAMP
		WHEN NOT MATCHED THEN
			INSERT (
				dt,
				cntr_cd,
				grp_cd,
				team_cd,
				subs_cnt,
				subs_amt,
				cont_cnt,
				cont_amt,
				etco_cnt,
				call_prs_cnt,
				tmr_cnt,
				call_cnt,
				cur_cnt,
				cust_rsps01,
				exec_cnt,
				lead_time,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.dt,
				src.cntr_cd,
				src.grp_cd,
				src.team_cd,
				src.subs_cnt,
				src.subs_amt,
				src.cont_cnt,
				src.cont_amt,
				src.etco_cnt,
				src.call_prs_cnt,
				src.tmr_cnt,
				src.call_cnt,
				src.cur_cnt,
				src.cust_rsps01,
				src.exec_cnt,
				src.lead_time,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			);
	END; 
	
	b_set_appTMTeam_Day_MIX(
		v_dt,
		P_CNTR_CD,
		P_GRP_CD,
		P_TEAM_CD,
		P_SUBS_CNT,
		P_SUBS_AMT,
		P_CONT_CNT,
		P_CONT_AMT,
		P_ETCO_CNT,
		P_CALL_PRS_CNT,
		P_TMR_CNT,
		P_CALL_CNT,
		P_CUR_CNT,
		P_CUST_RSPS01,
		P_EXEC_CNT,
		P_LEAD_TIME);
		
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.b_set_apptmteam_day_mix(
	P_DT			IN VARCHAR2,		-- 일자
	P_CNTR_CD		IN VARCHAR2,		-- 센터코드
	P_GRP_CD		IN VARCHAR2,		-- 그룹코드
	P_TEAM_CD		IN VARCHAR2,		-- 팀코드
	P_SUBS_CNT		IN NUMBER,			-- 청약건수(Sales)
	P_SUBS_AMT		IN NUMBER,			-- 청약금액(월남기준보험료(MTPM_BASC_INSUFEE))
	P_CONT_CNT		IN NUMBER,			-- 계약건수
	P_CONT_AMT		IN NUMBER,			-- 계약금액(월남기준보험료(MTPM_BASC_INSUFEE))
	P_ETCO_CNT		IN NUMBER,			-- 재적(조회일현자 코드가 있는TMR)
	P_CALL_PRS_CNT	IN NUMBER,			-- 활동인원(a.TMR(교육제외),전산상 콜이력이 존재하는 TMR)
	P_TMR_CNT		IN NUMBER,			-- 가동인원(청약건수1이상인 TRM)
	P_CALL_CNT		IN NUMBER,			-- 통화시도건수(Dial(고객에게 통화시도건))
	P_CUR_CNT		IN NUMBER,			-- 통화성공건수(Contact 1)
	P_CUST_RSPS01	IN NUMBER,			-- 도입거절건수(통화결과값이 "도입거절"인 건수,BL등)
	P_EXEC_CNT		IN NUMBER,			-- DB컨택건수(Connected LIST(사용DB)
	P_LEAD_TIME		IN NUMBER,			-- 전체콜타임(Total NTT)
	P_DATA_SRC		IN VARCHAR2 DEFAULT 'APP' -- 데이터원본여부(APP:1차,2차 / EAI:3차복합)
)
IS
	v_dt			VARCHAR2(10);
	v_cntr_exist	NUMBER(10);
BEGIN
	v_dt := swm.f_conv_dt(P_DT);
	BEGIN
		SELECT count(*) INTO v_cntr_exist FROM dual
		WHERE P_CNTR_CD IN(SELECT level1cd FROM swm.v_orginf WHERE syskind='TM' AND divcd='5' AND org_lvl=1);
	END;
	
	IF NOT (P_DATA_SRC='APP' AND v_cntr_exist > 0) THEN
		MERGE INTO swm.t_apptmteam_day_mix trg
		USING 
			(SELECT
				v_dt				AS dt,
				P_CNTR_CD			AS cntr_cd,
				P_GRP_CD			AS grp_cd,
				P_TEAM_CD			AS team_cd,
				P_SUBS_CNT			AS subs_cnt,
				P_SUBS_AMT			AS subs_amt,
				P_CONT_CNT			AS cont_cnt,
				P_CONT_AMT			AS cont_amt,
				P_ETCO_CNT			AS etco_cnt,
				P_CALL_PRS_CNT		AS call_prs_cnt,
				P_TMR_CNT			AS tmr_cnt,
				P_CALL_CNT			AS call_cnt,
				P_CUR_CNT			AS cur_cnt,
				P_CUST_RSPS01		AS cust_rsps01,
				P_EXEC_CNT			AS exec_cnt,
				P_LEAD_TIME			AS lead_time
			FROM dual) src
		ON (src.dt = trg.dt AND src.cntr_cd = trg.cntr_cd AND src.grp_cd = trg.grp_cd 
		AND src.team_cd = trg.team_cd)
		WHEN MATCHED THEN
			UPDATE SET
				trg.subs_cnt    	= src.subs_cnt,
				trg.subs_amt    	= src.subs_amt,
				trg.cont_cnt    	= src.cont_cnt,
				trg.cont_amt    	= src.cont_amt,
				trg.etco_cnt    	= src.etco_cnt,
				trg.call_prs_cnt    = src.call_prs_cnt,
				trg.tmr_cnt    		= src.tmr_cnt,
				trg.call_cnt    	= src.call_cnt,
				trg.cur_cnt    		= src.cur_cnt,
				trg.cust_rsps01    	= src.cust_rsps01,
				trg.exec_cnt    	= src.exec_cnt,
				trg.lead_time    	= src.lead_time,
				trg.lastmodifydate 	= SYSTIMESTAMP
		WHEN NOT MATCHED THEN
			INSERT (
				dt,
				cntr_cd,
				grp_cd,
				team_cd,
				subs_cnt,
				subs_amt,
				cont_cnt,
				cont_amt,
				etco_cnt,
				call_prs_cnt,
				tmr_cnt,
				call_cnt,
				cur_cnt,
				cust_rsps01,
				exec_cnt,
				lead_time,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.dt,
				src.cntr_cd,
				src.grp_cd,
				src.team_cd,
				src.subs_cnt,
				src.subs_amt,
				src.cont_cnt,
				src.cont_amt,
				src.etco_cnt,
				src.call_prs_cnt,
				src.tmr_cnt,
				src.call_cnt,
				src.cur_cnt,
				src.cust_rsps01,
				src.exec_cnt,
				src.lead_time,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			);
	END IF;
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.b_set_apptmteam_month(
	P_YM					IN VARCHAR2,	-- 일자
	P_CNTR_CD				IN VARCHAR2,	-- 센터코드
	P_GRP_CD				IN VARCHAR2,	-- 그룹코드
	P_TEAM_CD				IN VARCHAR2,	-- 팀코드
	P_ETCO_CNT				IN NUMBER,		-- 재적(조회일 현재 코드가 있는 TMR)
	P_CALL_PRS_CNT			IN NUMBER,		-- 활동인원
	P_SUBS_CNT				IN NUMBER,		-- 가동인원(청약건수1이상인 TMR(가동률= 가동인원/재적*100)
	P_TOT_MTPM_INSUFEE		IN NUMBER,		-- 당월 전체 보험료 (월납환산P누계)
	P_GUARANT_MTPM_INSUFEE	IN NUMBER,		-- 당월 보장성 보험료 (보장성월납환산P)
	P_SAVE_MTPM_INSUFEE		IN NUMBER		-- 당월 저축성 보혐료
)
IS
	v_ym		VARCHAR2(10);
BEGIN
	v_ym := swm.f_conv_dt(P_YM);
	BEGIN
		MERGE INTO swm.t_apptmteam_month trg
		USING 
			(SELECT
				v_ym					AS ym,
				P_CNTR_CD				AS cntr_cd,
				P_GRP_CD				AS grp_cd,
				P_TEAM_CD				AS team_cd,
				P_ETCO_CNT				AS etco_cnt,
				P_CALL_PRS_CNT			AS call_prs_cnt,
				P_SUBS_CNT				AS subs_cnt,
				P_TOT_MTPM_INSUFEE		AS tot_mtpm_insufee,
				P_GUARANT_MTPM_INSUFEE	AS guarant_mtpm_insufee,
				P_SAVE_MTPM_INSUFEE		AS save_mtpm_insufee
			FROM dual) src
		ON (src.ym = trg.ym AND src.cntr_cd = trg.cntr_cd AND src.grp_cd = trg.grp_cd 
		AND src.team_cd = trg.team_cd)
		WHEN MATCHED THEN
			UPDATE SET
				trg.etco_cnt    			= src.etco_cnt,
				trg.call_prs_cnt    		= src.call_prs_cnt,
				trg.subs_cnt    			= src.subs_cnt,
				trg.tot_mtpm_insufee    	= src.tot_mtpm_insufee,
				trg.guarant_mtpm_insufee 	= src.guarant_mtpm_insufee,
				trg.save_mtpm_insufee 		= src.save_mtpm_insufee,
				trg.lastmodifydate 			= SYSTIMESTAMP
		WHEN NOT MATCHED THEN
			INSERT (
				ym,
				cntr_cd,
				grp_cd,
				team_cd,
				etco_cnt,
				call_prs_cnt,
				subs_cnt,
				tot_mtpm_insufee,
				guarant_mtpm_insufee,
				save_mtpm_insufee,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.ym,
				src.cntr_cd,
				src.grp_cd,
				src.team_cd,
				src.etco_cnt,
				src.call_prs_cnt,
				src.subs_cnt,
				src.tot_mtpm_insufee,
				src.guarant_mtpm_insufee,
				src.save_mtpm_insufee,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			);
	END; 
	
	b_set_appTMTeam_Month_MIX(
		P_YM,						
		P_CNTR_CD,				
		P_GRP_CD,				
		P_TEAM_CD,
		P_ETCO_CNT,				
		P_CALL_PRS_CNT,			
		P_SUBS_CNT,				
		P_TOT_MTPM_INSUFEE,		
		P_GUARANT_MTPM_INSUFEE);
		
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.b_set_apptmteam_month_mix(
	P_YM					IN VARCHAR2,	-- 년도
	P_CNTR_CD				IN VARCHAR2,	-- 센터코드
	P_GRP_CD				IN VARCHAR2,	-- 그룹코드
	P_TEAM_CD				IN VARCHAR2,	-- 팀코드
	P_ETCO_CNT				IN NUMBER,		-- 재적(조회일 현재 코드가 있는 TMR)
	P_CALL_PRS_CNT			IN NUMBER,		-- 활동인원
	P_SUBS_CNT				IN NUMBER,		-- 가동인원(청약건수1이상인 TMR(가동률= 가동인원/재적*100)
	P_TOT_MTPM_INSUFEE		IN NUMBER,		-- 월납환산P누계(당월 전체 보험료)
	P_GUARANT_MTPM_INSUFEE	IN NUMBER,		-- 보장성월납환산P(당월 보장성 보험료)
	P_DATA_SRC				IN VARCHAR2 DEFAULT 'APP' -- 데이터원본여부(APP:1차,2차 / EAI:3차복합)
)
IS
	v_ym			VARCHAR2(10);
	v_cntr_exist	NUMBER(10);
BEGIN
	v_ym := swm.f_conv_dt(P_YM);
	BEGIN
		SELECT count(*) INTO v_cntr_exist FROM dual
		WHERE P_CNTR_CD IN(SELECT level1cd FROM swm.v_orginf WHERE syskind='TM' AND divcd='5' AND org_lvl=1);
	END;
	
	IF NOT (P_DATA_SRC='APP' AND v_cntr_exist > 0) THEN
		MERGE INTO swm.t_apptmteam_month_mix trg
		USING 
			(SELECT
				P_YM					AS ym,
				P_CNTR_CD				AS cntr_cd,
				P_GRP_CD				AS grp_cd,
				P_TEAM_CD				AS team_cd,
				P_ETCO_CNT				AS etco_cnt,
				P_CALL_PRS_CNT			AS call_prs_cnt,
				P_SUBS_CNT				AS subs_cnt,
				P_TOT_MTPM_INSUFEE		AS tot_mtpm_insufee,
				P_GUARANT_MTPM_INSUFEE	AS guarant_mtpm_insufee
			FROM dual) src
		ON (src.ym = trg.ym AND src.cntr_cd = trg.cntr_cd AND src.grp_cd = trg.grp_cd 
		AND src.team_cd = trg.team_cd)
		WHEN MATCHED THEN
			UPDATE SET
				trg.etco_cnt    			= src.etco_cnt,
				trg.call_prs_cnt    		= src.call_prs_cnt,
				trg.subs_cnt    			= src.subs_cnt,
				trg.tot_mtpm_insufee    	= src.tot_mtpm_insufee,
				trg.guarant_mtpm_insufee 	= src.guarant_mtpm_insufee,
				trg.lastmodifydate 			= SYSTIMESTAMP
		WHEN NOT MATCHED THEN
			INSERT (
				ym,
				cntr_cd,
				grp_cd,
				team_cd,
				etco_cnt,
				call_prs_cnt,
				subs_cnt,
				tot_mtpm_insufee,
				guarant_mtpm_insufee,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.ym,
				src.cntr_cd,
				src.grp_cd,
				src.team_cd,
				src.etco_cnt,
				src.call_prs_cnt,
				src.subs_cnt,
				src.tot_mtpm_insufee,
				src.guarant_mtpm_insufee,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			);
	END IF;
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.b_set_appuser(
	P_SysKind		IN VARCHAR2, 	--시스템구분(CS/TM)
	P_UserId		IN VARCHAR2,	--사용자ID
	P_KORN_NM		IN VARCHAR2,	--한글성명
	P_ENG_NM		IN VARCHAR2,	--영문성명
	P_Level1Cd		IN VARCHAR2,	--센터코드
	P_Level2Cd		IN VARCHAR2,	--그룹코드
	P_Level3Cd		IN VARCHAR2,	--팀코드
	P_GRP_ATHT		IN VARCHAR2,	--그룹권한
	P_ORG_GRD		IN VARCHAR2,	--조직등급
	P_BIZ_CLAS_CD	IN VARCHAR2,	--업무분류코드, CS의 경우(공통코드:W034)
	P_ETCO_DT		IN VARCHAR2, 	--입사일자(YYYY-MM-DD)
	P_LVCO_DT		IN VARCHAR2,	--퇴사일자(YYYY-MM-DD) (퇴사일자로 사용여부판단)
	P_EXT_NO		IN VARCHAR2,	--내선번호
	P_CTI_LGIN_ID	IN VARCHAR2,	--CTI로그인ID
	P_CTI_USE_YN	IN VARCHAR2,	--CTI사용여부(N:미사용,Y:사용)
	P_MSGR_USE_YN	IN VARCHAR2,	--메신저사용여부(N:미사용,Y:사용)
	P_ETCO_TIMM		IN NUMBER		--입사차월
)
IS
BEGIN
	/*t_appuser 데이터 삽입*/
	BEGIN
		MERGE INTO swm.t_appuser trg
		USING 
			(SELECT 
				P_SysKind 		AS syskind, 
				P_UserId 		AS userid, 
				P_KORN_NM 		AS korn_nm, 
				P_ENG_NM 		AS eng_nm, 
				P_Level1Cd 		AS level1cd,
				P_Level2Cd 		AS level2cd,
				P_Level3Cd 		AS level3cd,
				P_GRP_ATHT 		AS grp_atht,
				P_ORG_GRD 		AS org_grd,
				P_BIZ_CLAS_CD 	AS biz_clas_cd,
				P_ETCO_DT 		AS etco_dt,
				P_LVCO_DT 		AS lvco_dt,
				P_EXT_NO 		AS ext_no,
				P_CTI_LGIN_ID 	AS cti_lgin_id,
				P_CTI_USE_YN 	AS cti_use_yn,
				P_MSGR_USE_YN 	AS msgr_use_yn,
				P_ETCO_TIMM 	AS etco_timm
			FROM dual) src
		ON (src.syskind = trg.syskind AND src.userid = trg.userid)
		WHEN MATCHED THEN
			UPDATE SET 	
				trg.korn_nm 		= src.korn_nm,
				trg.eng_nm 			= src.eng_nm,
				trg.level1cd 		= src.level1cd,
				trg.level2cd 		= src.level2cd,
				trg.level3cd 		= src.level3cd,
				trg.grp_atht 		= src.grp_atht,
				trg.org_grd 		= src.org_grd,
				trg.biz_clas_cd 	= src.biz_clas_cd,
				trg.etco_dt 		= src.etco_dt,
				trg.lvco_dt 		= src.lvco_dt,
				trg.ext_no 			= src.ext_no,
				trg.cti_lgin_id 	= src.cti_lgin_id,
				trg.cti_use_yn 		= src.cti_use_yn,
				trg.msgr_use_yn 	= src.msgr_use_yn,
				trg.etco_timm 		= src.etco_timm,
				trg.lastmodifydate 	= SYSTIMESTAMP
				
		WHEN NOT MATCHED THEN
			INSERT (
				syskind, 
				userid,
				korn_nm,
				eng_nm,
				level1cd,
				level2cd,
				level3cd,
				grp_atht,
				org_grd,
				biz_clas_cd,
				etco_dt,
				lvco_dt,
				ext_no,
				cti_lgin_id,
				cti_use_yn,
				msgr_use_yn,
				etco_timm,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.syskind, 
				src.userid,
				src.korn_nm,
				src.eng_nm,
				src.level1cd,
				src.level2cd,
				src.level3cd,
				src.grp_atht,
				src.org_grd,
				src.biz_clas_cd,
				src.etco_dt,
				src.lvco_dt,
				src.ext_no,
				src.cti_lgin_id,
				src.cti_use_yn,
				src.msgr_use_yn,
				src.etco_timm,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			);
	END;
	/*t_user 데이터 삽입*/
	BEGIN
		MERGE INTO swm.t_user trg
		USING 
			(SELECT 
				P_SysKind 		AS syskind, 
				P_UserId 		AS userid, 
				P_KORN_NM 		AS korn_nm, 
				P_ENG_NM 		AS eng_nm, 
				P_Level1Cd 		AS level1cd,
				P_Level2Cd 		AS level2cd,
				P_Level3Cd 		AS level3cd,
				P_GRP_ATHT 		AS grp_atht,
				P_ORG_GRD 		AS org_grd,
				P_BIZ_CLAS_CD 	AS biz_clas_cd,
				P_ETCO_DT 		AS etco_dt,
				P_LVCO_DT 		AS lvco_dt,
				P_EXT_NO 		AS ext_no,
				P_CTI_LGIN_ID 	AS cti_lgin_id,
				P_CTI_USE_YN 	AS cti_use_yn,
				P_MSGR_USE_YN 	AS msgr_use_yn,
				P_ETCO_TIMM 	AS etco_timm
			FROM dual) src
		ON (src.syskind = trg.syskind AND src.userid = trg.userid)
		WHEN MATCHED THEN
			UPDATE SET
				trg.name 			= src.korn_nm,
				trg.engname 		= src.eng_nm,
				trg.level1cd 		= src.level1cd,
				trg.level2cd 		= src.level2cd,
				trg.level3cd 		= src.level3cd,
				trg.userclascd 		= src.grp_atht,
				trg.orgclascd 		= src.org_grd,
				trg.bizclascd 		= src.biz_clas_cd,
				trg.joindt 			= TO_TIMESTAMP(src.etco_dt, 'YYYY-MM-DD'),
				trg.retireddt 		= (CASE WHEN (src.lvco_dt IS NULL OR src.lvco_dt = '') THEN NULL else TO_TIMESTAMP(src.lvco_dt, 'YYYY-MM-DD') END),
				trg.extno 			= src.ext_no,
				trg.peripheralnumber= (CASE WHEN (src.cti_lgin_id = '' AND src.lvco_dt != '') THEN peripheralnumber
											WHEN (REGEXP_LIKE(src.cti_lgin_id, '^[0-9]+$')) THEN src.cti_lgin_id
											ELSE NULL END),
				trg.ctiuseyn 		= src.cti_use_yn,
				trg.msguseyn 		= src.msgr_use_yn,
				trg.orgcd 			= src.level3cd,
				trg.workperiod 		= src.etco_timm,
				trg.deleted			= (CASE WHEN (src.lvco_dt IS NULL OR src.lvco_dt = '') THEN 'N'
											WHEN TO_TIMESTAMP(src.lvco_dt, 'YYYY-MM-DD') > SYSTIMESTAMP THEN 'N'
											ELSE 'Y' END),
				trg.loginable		= (CASE WHEN (src.lvco_dt IS NULL OR src.lvco_dt = '') THEN 'Y'
											WHEN TO_TIMESTAMP(src.lvco_dt, 'YYYY-MM-DD') > SYSTIMESTAMP THEN 'Y'
											ELSE 'N' END),
				trg.lastmodifydate 	= SYSTIMESTAMP
		WHEN NOT MATCHED THEN
			INSERT (
				syskind, 
				userid,
				name,
				engname,
				level1cd,
				level2cd,
				level3cd,
				userclascd,
				orgclascd,
				bizclascd,
				joindt,
				retireddt,
				extno,
				peripheralnumber,
				ctiuseyn,
				msguseyn,
				orgcd,
				workperiod,
				deleted,
				loginable,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.syskind, 
				src.userid,
				src.korn_nm,
				src.eng_nm,
				src.level1cd,
				src.level2cd,
				src.level3cd,
				src.grp_atht,
				src.org_grd,
				src.biz_clas_cd,
				TO_TIMESTAMP(src.etco_dt, 'YYYY-MM-DD'),
				(CASE WHEN (src.lvco_dt IS NULL OR src.lvco_dt = '') THEN NULL ELSE TO_TIMESTAMP(src.lvco_dt, 'YYYY-MM-DD') END),
				src.ext_no,
				(CASE WHEN (REGEXP_LIKE(src.cti_lgin_id, '^[0-9]+$')) THEN src.cti_lgin_id ELSE null END),
				src.cti_use_yn,
				src.msgr_use_yn,
				src.level3cd,
				src.etco_timm,
				(CASE WHEN (src.lvco_dt IS NULL OR src.lvco_dt = '') THEN 'N'
					  WHEN TO_TIMESTAMP(src.lvco_dt, 'YYYY-MM-DD') > SYSTIMESTAMP THEN 'N'
					  ELSE 'Y' END),
				(CASE WHEN (src.lvco_dt IS NULL OR src.lvco_dt = '') THEN 'Y'
					  WHEN TO_TIMESTAMP(src.lvco_dt, 'YYYY-MM-DD') > SYSTIMESTAMP THEN 'Y'
					  ELSE 'N' END),
				SYSTIMESTAMP,
				SYSTIMESTAMP
			);
	END;
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.b_set_appuservacation(
	P_SysKind		IN VARCHAR2, 		--시스템구분(CS/TM)
	P_HLDS_DT		IN VARCHAR2,		--휴가일자(YYYY-MM-DD)
	P_UserId		IN VARCHAR2,		--사용자 ID
	P_HLDS_DV_CD	IN VARCHAR2  		--휴가 구분 코드
)
IS
	v_HLDS_DT	TIMESTAMP;
BEGIN
	v_HLDS_DT := TO_TIMESTAMP(P_HLDS_DT, 'YYYY-MM-DD');
	MERGE INTO swm.t_appuservacation trg
	USING 
		(SELECT
			P_SysKind			AS syskind,
			v_HLDS_DT			AS hlds_dt,
			P_UserId			AS userid,
			P_HLDS_DV_CD		AS hlds_dv_cd
		FROM dual) src
	ON (src.syskind = trg.syskind AND src.hlds_dt = trg.hlds_dt AND src.userid = trg.userid)
	WHEN MATCHED THEN
		UPDATE SET
			trg.hlds_dv_cd    	= src.hlds_dv_cd,
			trg.lastmodifydate 	= SYSTIMESTAMP
	WHEN NOT MATCHED THEN
		INSERT (
			syskind,
			hlds_dt,
			userid,
			hlds_dv_cd,
			createdate,
			lastmodifydate
		)
		VALUES (
			src.syskind,
			src.hlds_dt,
			src.userid,
			src.hlds_dv_cd,
			SYSTIMESTAMP,
			SYSTIMESTAMP
		);
		
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.P_CS_ACDBYCOMPANY_EXCL_CRITICS (
	p_cursor OUT SYS_REFCURSOR
)
IS
BEGIN
	OPEN p_cursor FOR
	
	WITH usr AS (
		SELECT  u.level1cd, u.level2cd, u.userid, cp.dbid
		FROM	swm.t_user u
				INNER JOIN swm.cagent ca
						ON u.userid  = ca.logid
					   AND u.deleted = 'N'
					   AND u.syskind = 'CS'
				INNER JOIN cc.cfg_person cp
						ON u.userid = cp.employee_id
		WHERE   NOT EXISTS (SELECT 1
							FROM   swm.t_appuservacation uv
							WHERE  uv.syskind = u.syskind
							AND    uv.userid  = u.userid
							AND    TO_CHAR(uv.hlds_dt, 'YYYYMMDD') = TO_CHAR(SYSDATE, 'YYYYMMDD')
							AND    hlds_dv_cd IN ('003', '012', '008', '009', '011', '010', '005', '006', '007', '013'))
	)
	SELECT t.level1cd, t.level2cd, t.level1nm, t.level2nm, t.worker, t.incall, t.acdcall, t.acdrate, t.average,
		   RANK() OVER (ORDER BY t.acdrate desc) AS "rank"
	FROM
	(
		SELECT us.level1cd, us.level2cd, us.level1nm, us.level2nm, us.worker, us.incall, us.acdcall,
			   ROUND(CASE WHEN (us.acdcall > 0 AND us.incall > 0) THEN (us.acdcall / us.incall) * 100 ELSE 0 END, 1) AS acdrate,
			   ROUND(CASE WHEN (us.acdcall > 0 AND us.worker > 0) THEN us.acdcall / us.worker ELSE 0 END, 1) AS average
		FROM   (
			SELECT  vo.level1cd,
					vo.level2cd,
					vo.level1nm,
					vo.level2nm,
					NVL(t.acdcalls, 0) + NVL(t.abncalls, 0) AS incall ,
					NVL(t.acdcalls, 0) AS acdcall,
					NVL((SELECT COUNT(DISTINCT userid) FROM usr WHERE level1cd = vo.level1cd AND level2cd = vo.level2cd), 0) AS worker
			FROM	swm.v_orginf vo
					LEFT JOIN (
						SELECT   u.level1cd, u.level2cd, SUM(c.acdcalls) AS acdcalls, SUM(c.abncalls) AS abncalls
						FROM	 usr u
								 INNER JOIN cc.cfg_skill_level csl
								 		 ON u.dbid      = csl.person_dbid
								 		AND csl.level_ != 0
								 INNER JOIN cc.cfg_skill cs
								 		 ON csl.skill_dbid = cs.dbid
								 INNER JOIN swm.t_ivr_dg_map dm
								 		 ON cs.dbid = dm.sk_dbid
								 		AND dm.dnis = '60001'
								 		AND dm.menu_code NOT IN ('A04', 'H04')
								 		AND dm.deleted_at IS NULL
								 INNER JOIN swm.v_menu_visual_map vm
								 		 ON dm.menu_code = vm.menucode
								 INNER JOIN swm.cskill c
								 		 ON dm.dg_name = c.split
						GROUP BY u.level1cd, u.level2cd
					) t ON vo.level1cd = t.level1cd AND vo.level2cd = t.level2cd
			WHERE   vo.syskind   = 'CS'
			AND     vo.level1cd  = 'CSL'
			AND     vo.level3cd != 'CSL813'
		--	AND     vo.org_lvl   = 3
			AND     EXISTS (SELECT 1 FROM swm.t_user WHERE deleted = 'N' AND level1cd = vo.level1cd AND level2cd = vo.level2cd)
			GROUP BY vo.level1cd, vo.level2cd, vo.level1nm, vo.level2nm,t.acdcalls, t.abncalls
		) us
	) t;
END;

CREATE OR REPLACE PROCEDURE SWM.P_GET_IVR_DATA
(
	I_DN IN VARCHAR,
	O_HOLIDAY_YN OUT VARCHAR,		-- 2.휴일여부(공휴일 + 토 + 일)
	O_COUNSELTIME_YN OUT VARCHAR,	-- 3.상담업무시간 여부(상담업무시간(WT) OR 상담+업무처리시간(WT3))
	O_WORKTIME_YN OUT VARCHAR,		-- 4.업무처리시간 여부(업무처리시간(WT2) OR 상담+업무처리시간(WT3))
	O_CALLBACK_YN OUT VARCHAR,		-- 5.콜백사용여부
	O_FOCUS_YN OUT VARCHAR,			-- 6.업무집중시간 여부
	O_SYSERR_CD OUT VARCHAR,		-- 7.
	O_MANYCALL_YN OUT VARCHAR,		-- 8.	
	O_CALLBACK_KIND OUT VARCHAR,	-- 9.	
	O_MAINNOTI_YN OUT VARCHAR,		-- 10.
	O_QUEUENOTI_YN OUT VARCHAR,		-- 11.
	O_PHOLIDAY_YN OUT VARCHAR		-- 12.
)
AS
	V_DT DATE;
	V_TIME_GB VARCHAR(10);
	V_DN VARCHAR(10);
	V_HHMM VARCHAR(4);
BEGIN
	V_DN := I_DN;
	V_DT := SYSDATE;
	BEGIN
		SELECT 	TO_CHAR(V_DT, 'HH24MI') INTO V_HHMM 
		FROM 	DUAL;		
	END;
	BEGIN
		SELECT	TIME_GB INTO V_TIME_GB
		FROM	SWM.T_TIMECHECK
		WHERE 	MRP = V_DN
				AND SUBSTR(WEEK_GB, 0, 1) = (
					CASE WHEN NVL((
						SELECT	PHLD_YN
						FROM	SWM.T_HOLIDAY
						WHERE 	MRP = V_DN
								AND HOLIDAYYEAR = TO_CHAR(V_DT, 'YYYY')
								AND HOLIDAYMONTHDAY = TO_CHAR(V_DT, 'MMDD')					
					), 'N') = 'Y' THEN '8'
					ELSE TO_CHAR(V_DT, 'D') END
				)
				AND STDT <= V_HHMM AND EDDT > V_HHMM;
		EXCEPTION
		WHEN NO_DATA_FOUND THEN
			V_TIME_GB := 'NWT';
	END;
	DBMS_OUTPUT.PUT_LINE(V_TIME_GB);
	BEGIN
		SELECT	(CASE WHEN V_TIME_GB IN ('WT', 'WT3') THEN 'Y' ELSE 'N' END),					-- 상담업무시간 여부
				(CASE WHEN V_TIME_GB IN ('WT2', 'WT3') THEN 'Y' ELSE 'N' END),					-- 업무처리시간 여부
				(CASE WHEN COUNT(*) > 0 THEN 'Y' WHEN V_TIME_GB = 'NWH' THEN 'Y' ELSE 'N' END)	-- 휴일여부(공휴일 + 토 + 일)
				INTO O_COUNSELTIME_YN, O_WORKTIME_YN, O_HOLIDAY_YN
		FROM	SWM.T_HOLIDAY
		WHERE	MRP = V_DN
				AND HOLIDAYYEAR = TO_CHAR(V_DT, 'YYYY')
				AND HOLIDAYMONTHDAY = TO_CHAR(V_DT, 'MMDD');
	END;
	BEGIN
		SELECT	(
					CASE WHEN M.CALLBACK_YN = 'Y' THEN (
						CASE WHEN T.STDT <= V_HHMM AND T.EDDT > TO_CHAR((SYSDATE + NVL(CALLBACK_ENDTIME, '00') / (24*60)), 'HHMI') AND T.TIME_GB  IN ('WT', 'WT3') THEN 'Y'
						ELSE 'N' END
					) 
					ELSE 'N' END
				)
				INTO O_CALLBACK_YN
		FROM	SWM.T_MRP M
				LEFT OUTER JOIN SWM.T_TIMECHECK T
				ON (M.MRP = T.MRP AND SUBSTR(T.WEEK_GB, 0, 1) = TO_CHAR(V_DT, 'D') AND T.STDT <= V_HHMM AND T.EDDT > V_HHMM)
		WHERE 	M.MRP = V_DN;
		EXCEPTION
		WHEN NO_DATA_FOUND THEN
			O_CALLBACK_YN := 'N';
	END;
	BEGIN
		SELECT	NVL(WORKJUB_YN, 'N'), 
				NVL(EMERGENCY_SN, '0'), 
				NVL(CALL_RUN_MENU, 'N'), 
				NVL(CALLBACK_SN, 'A')
				INTO O_FOCUS_YN, O_SYSERR_CD, O_MANYCALL_YN, O_CALLBACK_KIND
		FROM 	SWM.T_MRP
		WHERE 	MRP = V_DN;
		EXCEPTION
		WHEN NO_DATA_FOUND THEN
			O_FOCUS_YN := 'N';
			O_SYSERR_CD := '0';
			O_MANYCALL_YN := 'N';
			O_CALLBACK_KIND := 'A';
	END;
	BEGIN
		SELECT	CASE WHEN COUNT(MENT) > 0 THEN 'Y' ELSE 'N' END INTO O_MAINNOTI_YN
		FROM 	SWM.T_EMERLIST
		WHERE	MRP = V_DN
				AND FILENAME LIKE 'MainNoti%'
				AND (DATEFROM || ' ' || TIMEFROM || ':00.0000') <= TO_CHAR(V_DT, 'YYYY-MM-DD HH24:MI:SS.SSSS') 
				AND (DATETO || ' ' || TIMETO || ':59.9999') >= TO_CHAR(V_DT, 'YYYY-MM-DD HH24:MI:SS.SSSS');
		EXCEPTION
		WHEN NO_DATA_FOUND THEN
			O_MAINNOTI_YN := 'N';
	END;
	BEGIN
		SELECT	CASE WHEN COUNT(MENT) > 0 THEN 'Y' ELSE 'N' END INTO O_QUEUENOTI_YN
		FROM 	SWM.T_EMERLIST
		WHERE	MRP = V_DN
				AND FILENAME LIKE 'QueueNoti%'
				AND (DATEFROM || ' ' || TIMEFROM || ':00.0000') <= TO_CHAR(V_DT, 'YYYY-MM-DD HH24:MI:SS.SSSS') 
				AND (DATETO || ' ' || TIMETO || ':59.9999') >= TO_CHAR(V_DT, 'YYYY-MM-DD HH24:MI:SS.SSSS');
		EXCEPTION
		WHEN NO_DATA_FOUND THEN
			O_QUEUENOTI_YN := 'N';
	END;
	BEGIN
		SELECT 	CASE WHEN TO_CHAR(V_DT, 'D') = 1 THEN 'Y' 
				ELSE NVL((
					SELECT	PHLD_YN
					FROM 	SWM.T_HOLIDAY
					WHERE 	MRP = V_DN
							AND HOLIDAYYEAR = TO_CHAR(V_DT, 'YYYY')
							AND HOLIDAYMONTHDAY = TO_CHAR(V_DT, 'MMDD')		
				), 'N') END
				INTO O_PHOLIDAY_YN
		FROM DUAL;		
		EXCEPTION
		WHEN NO_DATA_FOUND THEN
			O_PHOLIDAY_YN := 'N';
	END;
END;

CREATE OR REPLACE PROCEDURE SWM.P_GET_IVR_EMERMENT
(
	I_DN IN VARCHAR,
	I_KIND IN VARCHAR,
	O_MENT OUT VARCHAR
)
AS
	V_DT DATE;
	V_DN VARCHAR(10);
	V_KIND VARCHAR(5);
BEGIN
	V_DN := I_DN;
	V_KIND := I_KIND;
	V_DT := SYSDATE;
	
	BEGIN
		SELECT	MENT INTO O_MENT
		FROM 	SWM.T_EMERLIST
		WHERE	MRP = V_DN
				AND (DATEFROM || ' ' || TIMEFROM || ':00.0000') <= TO_CHAR(V_DT, 'YYYY-MM-DD HH24:MI:SS.SSSS') 
				AND (DATETO || ' ' || TIMETO || ':59.9999') >= TO_CHAR(V_DT, 'YYYY-MM-DD HH24:MI:SS.SSSS')
				AND ((V_KIND = '01' AND FILENAME LIKE 'MainNoti%') OR (V_KIND = '02' AND FILENAME LIKE 'QueueNoti%'));
		EXCEPTION
		WHEN NO_DATA_FOUND THEN
			O_MENT := '';
	END;
	DBMS_OUTPUT.PUT_LINE(O_MENT);
END;

CREATE OR REPLACE PROCEDURE SWM.P_GET_MONITOR_MINIMAINTEL_PDP (
	p_cursor OUT SYS_REFCURSOR
)
IS
BEGIN
	OPEN p_cursor FOR
	
	SELECT  cs.mrp, cs.name, cs.ToAgent, cs.Answer, cs.RouterCallsQNow,
			ROUND(CASE WHEN (cs.Answer > 0 AND cs.ToAgent > 0) THEN (cs.Answer / cs.ToAgent) * 100 ELSE 0 END, 1) AS AnswerPct,
			NVL((SELECT  count(DISTINCT ca.logid)
				 FROM	 (
							 SELECT code AS dbid,
							 		CASE
							 			WHEN name = 'CS_A_INCIDENT' THEN '60008'
							 			ELSE m.mrp
							 		END AS mrp
							 FROM   swm.ax_common_code_m c
							 	   ,swm.t_mrp m
							 WHERE  c.group_cd = 'CFG_SKILL_CD'
							 AND    c.use_yn = 'Y'
							 AND    m.use_yn = 'Y'
							 AND    SUBSTR(c.name, 4, 1) = m.menu_code
							 AND    m.mrp IN ('60001', '60002', '60008')
						 ) d
				 		 INNER JOIN cc.cfg_skill_level csl
				 		 		 ON d.dbid      = csl.skill_dbid
				 		 		AND csl.level_ != 0
				 		 INNER JOIN cc.cfg_person cp
				 		 		 ON csl.person_dbid = cp.dbid
				 		 INNER JOIN swm.cagent ca
				 		 		 ON cp.employee_id = ca.logid
				 		 		AND ca.workmode    = '4'
				 		 INNER JOIN swm.t_user u
				 		 		 ON ca.logid  = u.userid
				 		 		AND u.syskind = 'CS'
				 		 		AND u.deleted = 'N'
				 WHERE   d.mrp  = cs.mrp), 0) AS Ready
	FROM 
	(
		SELECT   d.mrp, d.name,
				 SUM(NVL(cs.acdcalls, 0) + NVL(cs.abncalls, 0)) AS ToAgent,
				 SUM(NVL(cs.acdcalls, 0)) AS Answer,
				 SUM(NVL(cs.inqueue, 0)) AS RouterCallsQNow
		FROM	 (
					SELECT  dm.dg_name,
							CASE
								WHEN vm.mrp = '60001' AND dm.menu_code IN ('A04', 'H04') THEN '60008'
								ELSE vm.mrp
							END AS mrp,
							CASE
								WHEN vm.mrp = '60001' AND dm.menu_code NOT IN ('A04', 'H04') THEN '일반상담'
								WHEN vm.mrp = '60008' OR (vm.mrp = '60001' AND dm.menu_code IN ('A04', 'H04')) THEN '사고상담'
								WHEN vm.mrp = '60002' THEN vm.mrpname
							END AS name
					FROM	swm.t_ivr_dg_map dm
							INNER JOIN swm.v_menu_visual_map vm
									ON dm.dnis = vm.mrp
								   AND dm.menu_code = vm.menucode
								   AND dm.deleted_at IS NULL
					WHERE   vm.mrp IN ('60001', '60002', '60008')
				) d
				 LEFT JOIN swm.cskill cs ON d.dg_name = cs.split
		GROUP BY d.mrp, d.name
	) cs;
END;

CREATE OR REPLACE PROCEDURE SWM.P_GET_MONITOR_SKILL_REAL_TIME (
	p_cursor OUT SYS_REFCURSOR
)
IS
BEGIN
	OPEN p_cursor FOR
	
	SELECT  t.name AS "구분",
			SUM(CASE WHEN t.status != 'logout' THEN 1 ELSE 0 END) AS "계",
			SUM(CASE WHEN t.status IN ('ib', 'ob') THEN 1 ELSE 0 END) AS "통화중",
			SUM(CASE WHEN t.status = 'acw' THEN 1 ELSE 0 END)         AS "후처리",
			SUM(CASE WHEN t.status = 'ready' THEN 1 ELSE 0 END)       AS "대기",
			SUM(CASE WHEN t.status = 'aux' THEN 1 ELSE 0 END)         AS "이석"
	FROM
	(
		SELECT m.name, us.userid, us.status
		FROM
		(
			SELECT code AS dbid,
					CASE
						WHEN c.name = 'CS_A_INCIDENT' THEN '60008'
						ELSE m.mrp
					END AS mrp,
					CASE
						WHEN c.name = 'CS_A_INCIDENT' THEN '사고상담'
						WHEN m.mrp = '60001' THEN '일반상담'
						ELSE m.mrp_name
					END AS name
			FROM   swm.ax_common_code_m c
				  ,swm.t_mrp m
			WHERE  c.group_cd = 'CFG_SKILL_CD'
			AND    c.use_yn = 'Y'
			AND    m.use_yn = 'Y'
			AND    SUBSTR(c.name, 4, 1) = m.menu_code
			AND    m.mrp IN ('60001', '60002', '60008')
		) m
		LEFT JOIN
		(
			SELECT  u.userid, cs.dbid, cd.linkcd AS status
			FROM	swm.t_user u
					INNER JOIN swm.cagent c
							ON u.userid = c.logid
					INNER JOIN cc.cfg_person cp
							ON cp.employee_id = u.userid
					INNER JOIN cc.cfg_skill_level csl
							ON csl.person_dbid = cp.dbid
						   AND csl.level_     != 0
					INNER JOIN cc.cfg_skill cs
							ON cs.dbid = csl.skill_dbid
					INNER JOIN swm.t_cdinf cd
							ON cd.lrgcd   = 'AGS'
						   AND c.workmode = cd.smlcd
			WHERE   u.deleted = 'N'
			AND     u.syskind = 'CS'
		) us ON m.dbid = us.dbid
		GROUP BY m.name, us.userid, us.status
	) t
	GROUP BY t.name;
END;

CREATE OR REPLACE PROCEDURE SWM.p_get_nasWfm_SumAgent_30min(
	P_SDT				IN VARCHAR2, 		--시작날짜
	P_EDT				IN VARCHAR2,		--종료날짜
	P_outCursor 		OUT SYS_REFCURSOR
)
IS
	v_FROM_HH	VARCHAR2(2);
BEGIN
	v_FROM_HH := TO_CHAR(SYSTIMESTAMP-(2/24), 'HH24');
	IF v_FROM_HH = '21' THEN
		v_FROM_HH := '00';
	END IF;

	OPEN P_outCursor FOR
	SELECT HD
		  ,WK
		  ,REPLACE(DT, '-', '') AS DT
		  ,HH
		  ,MM
		  ,Level1Cd
		  ,Level2Cd
		  ,Level3Cd
		  ,PeripheralNumber
		  ,MRP
		  ,MenuCode
		  ,SUM(IB_RNA       ) AS IB_RNA
		  ,SUM(IB           ) AS IB
		  ,SUM(Answer10s    ) AS Answer10s
		  ,SUM(Answer20s    ) AS Answer20s
		  ,SUM(IB_TalkTime  ) AS IB_TalkTime
		  ,SUM(OB_Try       ) AS OB_Try
		  ,SUM(OB_DelayTime ) AS OB_DelayTime
		  ,SUM(OB_Answer    ) AS OB_Answer
		  ,SUM(OB_TalkTime  ) AS OB_TalkTime
		  ,SUM(HoldTime     ) AS HoldTime
		  ,SUM(PDS          ) AS PDS
		  ,SUM(PDS_TalkTime ) AS PDS_TalkTime
		  ,SUM(PT_Answer    ) AS PT_Answer
		  ,SUM(PT_Request   ) AS PT_Request
		  ,SUM(PT_TalkTime  ) AS PT_TalkTime
		  ,SUM(Hold         ) AS Hold
		  ,SUM(IB_MOD       ) AS IB_MOD
	FROM (
			SELECT 
			       CASE WHEN H.DT IS NOT NULL THEN 'Y' ELSE 'N' END AS HD
			     , CASE WHEN a.ROW_WEEK = '일요일' THEN '1' 
						WHEN a.ROW_WEEK = '월요일' THEN '2' 
						WHEN a.ROW_WEEK = '화요일' THEN '3' 
						WHEN a.ROW_WEEK = '수요일' THEN '4' 
						WHEN a.ROW_WEEK = '목요일' THEN '5' 
						WHEN a.ROW_WEEK = '금요일' THEN '6' 
				       ELSE '7' END as WK
                  , a.ROW_DATE AS DT
                  , SUBSTR(a.STARTTIME,1,2) AS HH
				  ,CASE WHEN SUBSTR(a.STARTTIME,3,2) >= '00' and SUBSTR(a.STARTTIME,3,2) < '30' THEN '00'
						WHEN SUBSTR(a.STARTTIME,3,2) >= '30' and SUBSTR(a.STARTTIME,3,2) < '59' THEN '30' END AS MM
				  , a.LEVEL1CD AS Level1Cd  -- ,a.[Level1Cd]	누적 통계의 사용자 조직(대)
				  , a.LEVEL2CD AS Level2Cd 	-- ,a.[Level2Cd]    누적 통계의 사용자 조직(중)
				  , a.LEVEL3CD AS Level3Cd 	-- ,a.[Level3Cd]	누적 통계의 사용자 조직(소)
				,a.AGENT_LOGID  AS PeripheralNumber --   ,a.[PeripheralNumber]
				,I.DNIS AS  MRP -- ,a.[MRP]
				,I.MENU_CODE AS MenuCode --  ,a.[MenuCode]
				,a.N_RONA AS IB_RNA  -- ,[IB_RNA]
				,a.N_TALK_ACD AS IB  -- ,[IB]
				,a.N_RING_ACD_1 AS Answer10s -- ,[Answer10s]
				,a.N_RING_ACD_2 AS Answer20s -- ,[Answer20s]
				,a.T_TALK_ACD AS IB_TalkTime --  ,[IB_TalkTime]
				,a.N_DIAL_OB AS OB_Try --  ,[OB_Try]
				,a.T_DIAL_OB AS OB_DelayTime --  ,[OB_DelayTime]
				,a.N_TALK_OW_OB AS OB_Answer --  ,[OB_Answer]
				,a.T_TALK_OW_OB AS OB_TalkTime --  ,[OB_TalkTime]
				,a.T_HOLD AS HoldTime --  ,[HoldTime]
				,0 AS PDS --  ,[PDS]  PDS 사용 안함
				,0 AS PDS_TalkTime --  ,[PDS_TalkTime] PDS 사용 안함
				,a.N_TRANS_TRST AS PT_Answer --  ,[PT_Answer]  호전환 수신 (Cisco에는 호전환 수신 없음)
				,a.N_TRANS_TRSM AS PT_Request --  ,[PT_Request] 호전환 발신 
				,0 AS PT_TalkTime --  ,[PT_TalkTime] T_TALK_IW_TRST_CO + T_TALK_IW_TRSM_CO 이지만 다른 시간에 중보 포함되는 항목으로 제외함
				,a.N_HOLD AS HOLD --  ,[Hold]
				  --2021.10.08 그룹별 응대현황(21.10~) 기준 IB건수 컬럼 추가
				  ,case u.BizClasCd 
				 	when '01' then (case when I.DNIS='60006' or (I.DNIS ='60001' and I.MENU_CODE<>'A04') then N_TALK_ACD+N_RONA else 0 end)
				 	when '03' then (case when I.DNIS='60002' then N_TALK_ACD+N_RONA else 0 end)
				 	when '04' then (case when I.DNIS='60008' or (I.DNIS='60001' and I.MENU_CODE='A04') then N_TALK_ACD+N_RONA else 0 end)
				   else 0 end as IB_MOD
			  FROM SWM.AGENT_FT A , SWM.t_User U, SWM.T_IVR_DG_MAP I,  SWM.T_HOLIDAY H
			  WHERE a.AGENT_LOGID = u.PeripheralNumber 
			  AND a.ROW_DATE >= P_SDT and a.ROW_DATE <= P_EDT
			  AND ((a.ROW_DATE <= TO_CHAR(u.RetiredDt, 'YYYY-MM-DD') OR u.RetiredDt IS NULL) and a.ROW_DATE >= TO_CHAR(u.JoinDt, 'YYYY-MM-DD')) 
			  AND a.ROW_DATE = H.DT(+)
			  AND a.STARTTIME  >= v_FROM_HH || '00' -- AND HH >= @FROM_HH
			  AND a.DG_CODE  = I.MENU_CODE 
	) T1
	GROUP BY HD, WK, DT, HH, MM, Level1Cd, Level2Cd, Level3Cd, PeripheralNumber, MRP, MenuCode
	ORDER BY HD, WK, DT, HH, MM, Level1Cd, Level2Cd, Level3Cd, PeripheralNumber, MRP, MenuCode;
END;

CREATE OR REPLACE PROCEDURE SWM.p_get_nasWfm_SumAgent_Day(
	P_DateFrom				IN VARCHAR2,		--조회 날짜 from
	P_DateTo				IN VARCHAR2,		--조회 날짜 to
	P_arrCenterCd			IN VARCHAR2,		--센터
	P_arrGroupCd			IN VARCHAR2,		--그룹
	P_arrTeamCd				IN VARCHAR2,		--팀 
	P_arrAgentId			IN VARCHAR2,		--상담사ID
	P_checkGB				IN VARCHAR2,		--퇴사자 여부
	P_SysKind				IN VARCHAR2,			--시스템구분
	P_outCursor 			OUT SYS_REFCURSOR
)
IS
	v_FROM_HH	VARCHAR2(2);
BEGIN
	v_FROM_HH := TO_CHAR(SYSTIMESTAMP-(2/24), 'HH24');
	IF v_FROM_HH = '21' THEN
		v_FROM_HH := '00';
	END IF;

	
	BEGIN
		EXECUTE IMMEDIATE 'TRUNCATE TABLE SWM.HOL_INFO_TEMP';
		--휴가와 공휴일 조회
		INSERT INTO SWM.HOL_INFO_TEMP 
		SELECT HD, CTI_ID
		FROM (
				SELECT  NVL(t1.HD, t2.HD) AS HD
						,NVL(t1.PeripheralNumber, t2.PeripheralNumber) AS CTI_ID
				FROM (
						SELECT  TO_CHAR(HLDS_DT, 'YYYY-MM-DD') AS HD
								,a.SysKind
								,Level1Cd
								,PeripheralNumber 
						FROM swm.t_appUserVacation a 
						INNER JOIN swm.t_User b ON (a.UserId = b.UserID AND a.SysKind = b.SysKind)
						WHERE HLDS_DT >= to_date(P_DateFrom, 'YYYY-MM-DD')  AND HLDS_DT <= to_date(P_DateTo, 'YYYY-MM-DD') 
						AND Level1Cd IS NOT NULL 
						AND Level2Cd IS NOT NULL 
						AND Level3Cd IS NOT NULL 
						AND b.PeripheralNumber IS NOT NULL 
						AND a.HLDS_DV_CD NOT IN ('015','016','018','019','022','023') --오전반차,오후반차는 실적인정
				) t1
	
				FULL OUTER JOIN (
					SELECT  TO_CHAR(SOLAR_DT, 'YYYY-MM-DD') AS HD
							,a.SysKind
							,Level1Cd
							,PeripheralNumber
					FROM swm.t_User a
					INNER JOIN swm.t_appHoliday b ON (a.SysKind = b.SysKind AND Level1Cd = b.CNTR_CD AND HLD_YN = 'Y')
					WHERE SOLAR_DT >=to_date(P_DateFrom, 'YYYY-MM-DD') AND SOLAR_DT <= to_date(P_DateTo, 'YYYY-MM-DD') 
					AND Level1Cd IS NOT NULL 
					AND Level2Cd IS NOT NULL 
					AND Level3Cd IS NOT NULL 
					AND a.PeripheralNumber IS NOT NULL 
				) t2 ON (t1.SysKind = t2.SysKind AND t1.Level1Cd = t2.Level1Cd AND t1.PeripheralNumber = t2.PeripheralNumber AND t1.HD = t2.HD)
		) HOL_INFO;
	
		COMMIT;
	END;
	
	
	
	--일별 상담사 실적 조회
	BEGIN
		OPEN P_outCursor FOR
		SELECT	NVL(DT,'합계') AS DT
				,Level1Cd
				,Level2Cd
				,Level3Cd
				,Level1Nm
				,Level2Nm
				,Level3Nm
				,SortOrd
				,UserID
				,UserNm
				,WorkPeriod
				,IB_RNA
				,IB
				,Answer10s
				,Answer20s
				,(IB_TalkTime + OB_TalkTime + (Reason_Time-Reason0_Time) + ReadyTime) AS LoginTime
				,IB_TalkTime AS IB_TalkTime
				,OB_Try
				,OB_DelayTime AS OB_DelayTime
				,OB_Answer
				,OB_TalkTime AS OB_TalkTime
				,IB_TalkTime+OB_TalkTime AS CallTime
				,swm.f_avg_value0_int(IB_TalkTime+OB_TalkTime,TOTALCALL) AS CallTime_AVG
				,CASE WHEN DT IS NULL THEN '' ELSE TO_CHAR(RANK() OVER (PARTITION BY Level2Cd ORDER BY TOTALCALL DESC)) END AS GROUPRANK
				,swm.f_avg_value0_int(IB_TalkTime,IB) AS IB_TalkTime_AVG
				,swm.f_avg_value0_int(OB_DelayTime,OB_Try) AS OB_Try_AVG
				,swm.f_avg_value0_int(OB_TalkTime,OB_Answer) AS OB_Answer_AVG
				,swm.f_avg_value0_int(Reason1_Time,TOTALCALL) AS Reason1_Time_AVG
				,TOTALCALL
				,HoldTime AS HoldTime
				,PDS
				,PT_Answer
				,PT_Request
				,PT_TalkTime AS PT_TalkTime
				,Reason
				,Reason0
				,Reason1
				,Reason2
				,Reason3
				,Reason4
				,Reason5
				,Reason6
				,Reason7
				,Reason_Time  AS Reason_Time
				--,Reason0_Time  AS Reason0_Time
				,Reason1_Time AS Reason1_Time
				,Reason2_Time AS Reason2_Time
				,Reason3_Time AS Reason3_Time
				,Reason4_Time AS Reason4_Time
				,Reason5_Time AS Reason5_Time
				,Reason6_Time AS Reason6_Time
				,Reason7_Time AS Reason7_Time
				,ReadyTime AS ReadyTime
				,Reason_Time-Reason0_Time-Reason1_Time AS Reason_Time_Total
				,CASE WHEN DT IS NULL THEN '1' ELSE '3' END AS Color
				,OB_TalkTime+OB_DelayTime AS OB_TotalTime
				,swm.f_avg_value0_int(OB_TalkTime+OB_DelayTime,OB_Try) AS OB_Total_AVG
		FROM 
		(
			SELECT   REPLACE(T1.DT, '-', '') AS DT
					,T1.Level1Cd
					,T1.Level2Cd
					,T1.Level3Cd
					,VO.SortOrd
					,VO.Level1Nm
					,VO.Level2Nm
					,VO.Level3Nm
					,T1.PeripheralNumber
					,T1.UserID
					,T1.Name AS UserNm
					,T1.WorkPeriod
					,SUM(T1.IB_RNA) AS IB_RNA
					,SUM(T1.IB) AS IB
					,SUM(T1.Answer10s) AS Answer10s
					,SUM(T1.Answer20s) AS Answer20s
					,SUM(T1.IB_TalkTime) AS IB_TalkTime
					,SUM(T1.OB_Try) AS OB_Try
					,SUM(T1.OB_DelayTime) AS OB_DelayTime
					,SUM(T1.OB_Answer) AS OB_Answer
					,SUM(T1.OB_TalkTime) AS OB_TalkTime
					,SUM(T1.HoldTime) AS HoldTime
					,SUM(T1.PDS) AS PDS
					,SUM(T1.PT_Answer) AS PT_Answer
					,SUM(T1.PT_Request) AS PT_Request
					,SUM(T1.PT_TalkTime) AS PT_TalkTime
					,SUM(T1.Reason) AS Reason
					,SUM(T1.Reason0) AS Reason0
					,SUM(T1.Reason1) AS Reason1
					,SUM(T1.Reason2) AS Reason2
					,SUM(T1.Reason3) AS Reason3
					,SUM(T1.Reason4) AS Reason4
					,SUM(T1.Reason5) AS Reason5
					,SUM(T1.Reason6) AS Reason6
					,SUM(T1.Reason7) AS Reason7
					,SUM(T1.Reason_Time) AS Reason_Time
					,SUM(T1.Reason0_Time) AS Reason0_Time
					,SUM(T1.Reason1_Time) AS Reason1_Time
					,SUM(T1.Reason2_Time) AS Reason2_Time
					,SUM(T1.Reason3_Time) AS Reason3_Time
					,SUM(T1.Reason4_Time) AS Reason4_Time
					,SUM(T1.Reason5_Time) AS Reason5_Time
					,SUM(T1.Reason6_Time) AS Reason6_Time
					,SUM(T1.Reason7_Time) AS Reason7_Time
					,SUM(T1.ReadyTime) AS ReadyTime
					,SUM(IB) + SUM(OB_Answer) AS TOTALCALL					
			FROM
			(
				SELECT	 ROW_DATE AS DT
						,a.LEVEL1CD AS Level1Cd  -- ,a.[Level1Cd]	누적 통계의 사용자 조직(대)
					    ,a.LEVEL2CD AS Level2Cd 	-- ,a.[Level2Cd]    누적 통계의 사용자 조직(중)
					    ,a.LEVEL3CD AS Level3Cd 	-- ,a.[Level3Cd]	누적 통계의 사용자 조직(소)
						,a.AGENT_LOGID  AS PeripheralNumber --   ,a.[PeripheralNumber]
						,a.AGENT_EMPID AS UserID
						,u.name AS name
						,u.WORKPERIOD AS WorkPeriod
						,SUM(a.N_RONA) AS IB_RNA  -- ,[IB_RNA]
						,SUM(a.N_TALK_ACD) AS IB  -- ,[IB]
						,SUM(a.N_TALK_ACD_0) AS Answer10s -- ,[Answer10s]
						,SUM(a.N_TALK_ACD_1) AS Answer20s -- ,[Answer20s]
						,SUM(a.T_TALK_ACD) AS IB_TalkTime --  ,[IB_TalkTime]
						,SUM(a.N_DIAL_OB) AS OB_Try --  ,[OB_Try]
						,SUM(a.T_DIAL_OB) AS OB_DelayTime --  ,[OB_DelayTime]
						,SUM(a.N_TALK_OW_OB) AS OB_Answer --  ,[OB_Answer]
						,SUM(a.T_TALK_OW_OB) AS OB_TalkTime --  ,[OB_TalkTime]
						,SUM(a.T_HOLD) AS HoldTime --  ,[HoldTime]
						,0 AS PDS --  ,[PDS]  PDS 사용 안함
						,SUM(a.N_TRANS_TRST) AS PT_Answer --  ,[PT_Answer]  호전환 수신 (Cisco에는 호전환 수신 없음)
						,SUM(a.N_TRANS_TRSM) AS PT_Request --  ,[PT_Request] 호전환 발신 
						,0 AS PT_TalkTime --  ,[PT_TalkTime] T_TALK_IW_TRST_CO + T_TALK_IW_TRSM_CO 이지만 다른 시간에 중보 포함되는 항목으로 제외함
						,SUM(T_TI_NREADY) 	as    Reason_Time
						,SUM(T_TI_NREADY_0) as    Reason0_Time
						,SUM(T_TI_NREADY_1) as    Reason1_Time
						,SUM(T_TI_NREADY_2) as    Reason2_Time
						,SUM(T_TI_NREADY_3) as    Reason3_Time
						,SUM(T_TI_NREADY_4) as    Reason4_Time
						,SUM(T_TI_NREADY_5) as    Reason5_Time
						,SUM(T_TI_NREADY_6) as    Reason6_Time
						,SUM(T_TI_NREADY_7) as    Reason7_Time
						,SUM(N_NREADY)      as    Reason   
					  	,SUM(N_NREADY_0)    as    Reason0
					  	,SUM(N_NREADY_1)    as    Reason1
					  	,SUM(N_NREADY_2)    as    Reason2
					  	,SUM(N_NREADY_3)    as    Reason3
					  	,SUM(N_NREADY_4)    as    Reason4
					  	,SUM(N_NREADY_5)    as    Reason5
					  	,SUM(N_NREADY_6)    as    Reason6
					  	,SUM(N_NREADY_7)    as    Reason7
					  	,SUM(T_TI_READY) 	as    ReadyTime
				FROM swm.agent_ft a LEFT OUTER JOIN swm.HOL_INFO_TEMP HI ON (a.ROW_DATE = hi.HD AND a.AGENT_LOGID = hi.CTI_ID)
					, SWM.t_User u
					, SWM.T_IVR_DG_MAP i
				WHERE a.AGENT_LOGID = u.PERIPHERALNUMBER 
				AND a.ROW_DATE >= P_DateFrom and a.ROW_DATE <= P_DateTo
				AND ((a.ROW_DATE < TO_CHAR(u.RetiredDt, 'YYYY-MM-DD') OR u.RetiredDt IS NULL) and a.ROW_DATE >= TO_CHAR(u.JoinDt, 'YYYY-MM-DD')) 
				AND a.DG_CODE = I.MENU_CODE 
				AND	(u.Level1Cd = P_arrCenterCd OR P_arrCenterCd = '%')
				AND	(u.Level2Cd = P_arrGroupCd OR P_arrGroupCd = '%')
				AND (P_arrTeamCd = '%' OR u.Level3Cd IN (SELECT column_value FROM swm.f_get_TableFromString(P_arrTeamCd,',')))
				AND (U.Deleted = P_checkGB OR P_checkGB = 'Y')
				AND U.SysKind = P_SysKind
				AND (P_arrAgentId = '%' OR U.UserID IN (SELECT column_value FROM swm.f_get_TableFromString(P_arrAgentId,','))) 
				AND hi.hd IS NULL
				GROUP BY ROW_DATE, a.LEVEL1CD, a.LEVEL2CD, a.LEVEL3CD, a.AGENT_LOGID, a.AGENT_EMPID, u.name, u.WORKPERIOD
			) T1
			INNER JOIN swm.v_OrgInf vo ON(T1.Level1Cd = VO.Level1Cd and T1.Level2Cd = VO.Level2Cd and T1.Level3Cd = VO.Level3Cd)
			GROUP BY ROLLUP((T1.DT, T1.Level1Cd,T1.Level2Cd,T1.Level3Cd,T1.PeripheralNumber,T1.UserID,T1.Name,T1.WorkPeriod, VO.Level1Nm,VO.Level2Nm,VO.Level3Nm,VO.SortOrd))
	
		) T2
	
		UNION ALL
	
		SELECT  '평균' AS DT
				,'' Level1Cd
				,'' Level2Cd
				,'' Level3Cd
				,'' Level1Nm
				,'' Level2Nm
				,'' Level3Nm
				,'' SortOrd
				,'' UserID
				,'' UserNm
				,NULL AS WorkPeriod
				,ROUND(AVG(IB_RNA),2) AS IB_RNA
				,ROUND(AVG(IB),2) AS IB
				,ROUND(AVG(Answer10s),2) AS Answer10s
				,ROUND(AVG(Answer20s),2) AS Answer20s
				,ROUND(AVG(IB_TalkTime + OB_TalkTime + (Reason_Time-Reason0_Time) + ReadyTime),2) AS LoginTime
				,ROUND(AVG(IB_TalkTime),2) AS IB_TalkTime
				,ROUND(AVG(OB_Try),2) AS OB_Try
				,ROUND(AVG(OB_DelayTime),2) AS OB_DelayTime
				,ROUND(AVG(OB_Answer),2) AS OB_Answer
				,ROUND(AVG(OB_TalkTime),2) AS OB_TalkTime
				,ROUND(AVG(IB_TalkTime+OB_TalkTime),2) AS CallTime
				,ROUND(AVG(swm.f_avg_value0_int(IB_TalkTime+OB_TalkTime,IB+OB_Answer)),2) AS CallTime_AVG
				,'' GROUPRANK
				,ROUND(AVG(swm.f_avg_value0_int(IB_TalkTime,IB)),2) AS IB_TalkTime_AVG
				,ROUND(AVG(swm.f_avg_value0_int(OB_DelayTime,OB_Try)),2) AS OB_Try_AVG
				,ROUND(AVG(swm.f_avg_value0_int(OB_TalkTime,OB_Answer)),2) AS OB_Answer_AVG
				,ROUND(AVG(swm.f_avg_value0_int(Reason1_Time,IB+OB_Answer)),2) AS Reason1_Time_AVG
				,ROUND(AVG(IB+OB_Answer),2) AS TOTALCALL
				,ROUND(AVG(HoldTime),2) AS HoldTime
				,ROUND(AVG(PDS),2) AS PDS
				,ROUND(AVG(PT_Answer),2) AS PT_Answer
				,ROUND(AVG(PT_Request),2) AS PT_Request
				,ROUND(AVG(PT_TalkTime),2) AS PT_TalkTime				
				,ROUND(AVG(Reason),2) AS Reason
				,ROUND(AVG(Reason0),2) AS Reason0
				,ROUND(AVG(Reason1),2) AS Reason1
				,ROUND(AVG(Reason2),2) AS Reason2
				,ROUND(AVG(Reason3),2) AS Reason3
				,ROUND(AVG(Reason4),2) AS Reason4
				,ROUND(AVG(Reason5),2) AS Reason5
				,ROUND(AVG(Reason6),2) AS Reason6
				,ROUND(AVG(Reason7),2) AS Reason7
				,ROUND(AVG(Reason_Time),2)  AS Reason_Time
				,ROUND(AVG(Reason1_Time),2) AS Reason1_Time
				,ROUND(AVG(Reason2_Time),2) AS Reason2_Time
				,ROUND(AVG(Reason3_Time),2) AS Reason3_Time
				,ROUND(AVG(Reason4_Time),2) AS Reason4_Time
				,ROUND(AVG(Reason5_Time),2) AS Reason5_Time
				,ROUND(AVG(Reason6_Time),2) AS Reason6_Time
				,ROUND(AVG(Reason7_Time),2) AS Reason7_Time
				,ROUND(AVG(ReadyTime),2) AS ReadyTime
				,ROUND(AVG(Reason_Time-Reason0_Time-Reason1_Time),2) AS Reason_Time_Total
				,'2' AS Color
				,ROUND(AVG(OB_TalkTime+OB_DelayTime),2) AS OB_TotalTime
				,ROUND(AVG(swm.f_avg_value0_int(OB_TalkTime+OB_DelayTime,OB_Try)),2) AS OB_Total_AVG
						
		FROM
		(
			SELECT	 ROW_DATE AS DT
					,a.LEVEL1CD AS Level1Cd  -- ,a.[Level1Cd]	누적 통계의 사용자 조직(대)
				    ,a.LEVEL2CD AS Level2Cd 	-- ,a.[Level2Cd]    누적 통계의 사용자 조직(중)
				    ,a.LEVEL3CD AS Level3Cd 	-- ,a.[Level3Cd]	누적 통계의 사용자 조직(소)
					,a.AGENT_LOGID  AS PeripheralNumber --   ,a.[PeripheralNumber]
					,SUM(a.N_RONA) AS IB_RNA  -- ,[IB_RNA]
					,SUM(a.N_TALK_ACD) AS IB  -- ,[IB]
					,SUM(a.N_TALK_ACD_0) AS Answer10s -- ,[Answer10s]
					,SUM(a.N_TALK_ACD_1) AS Answer20s -- ,[Answer20s]
					,SUM(a.T_TALK_ACD) AS IB_TalkTime --  ,[IB_TalkTime]
					,SUM(a.N_DIAL_OB) AS OB_Try --  ,[OB_Try]
					,SUM(a.T_DIAL_OB) AS OB_DelayTime --  ,[OB_DelayTime]
					,SUM(a.N_TALK_OW_OB) AS OB_Answer --  ,[OB_Answer]
					,SUM(a.T_TALK_OW_OB) AS OB_TalkTime --  ,[OB_TalkTime]
					,SUM(a.T_HOLD) AS HoldTime --  ,[HoldTime]
					,0 AS PDS --  ,[PDS]  PDS 사용 안함
					,SUM(a.N_TRANS_TRST) AS PT_Answer --  ,[PT_Answer]  호전환 수신 (Cisco에는 호전환 수신 없음)
					,SUM(a.N_TRANS_TRSM) AS PT_Request --  ,[PT_Request] 호전환 발신 
					,0 AS PT_TalkTime --  ,[PT_TalkTime] T_TALK_IW_TRST_CO + T_TALK_IW_TRSM_CO 이지만 다른 시간에 중보 포함되는 항목으로 제외함
					,SUM(T_TI_NREADY) 	as    Reason_Time
					,SUM(T_TI_NREADY_0) as    Reason0_Time
					,SUM(T_TI_NREADY_1) as    Reason1_Time
					,SUM(T_TI_NREADY_2) as    Reason2_Time
					,SUM(T_TI_NREADY_3) as    Reason3_Time
					,SUM(T_TI_NREADY_4) as    Reason4_Time
					,SUM(T_TI_NREADY_5) as    Reason5_Time
					,SUM(T_TI_NREADY_6) as    Reason6_Time
					,SUM(T_TI_NREADY_7) as    Reason7_Time
					,SUM(N_NREADY)      as    Reason   
				  	,SUM(N_NREADY_0)    as    Reason0
				  	,SUM(N_NREADY_1)    as    Reason1
				  	,SUM(N_NREADY_2)    as    Reason2
				  	,SUM(N_NREADY_3)    as    Reason3
				  	,SUM(N_NREADY_4)    as    Reason4
				  	,SUM(N_NREADY_5)    as    Reason5
				  	,SUM(N_NREADY_6)    as    Reason6
				  	,SUM(N_NREADY_7)    as    Reason7
				  	,SUM(T_TI_READY) 	as    ReadyTime
			FROM swm.agent_ft a LEFT OUTER JOIN swm.HOL_INFO_TEMP HI ON (a.ROW_DATE = hi.HD AND a.AGENT_LOGID = hi.CTI_ID)
				, SWM.t_User u
				, SWM.T_IVR_DG_MAP i
			WHERE a.AGENT_LOGID = u.PERIPHERALNUMBER 
			AND a.ROW_DATE >= P_DateFrom and a.ROW_DATE <= P_DateTo
			AND ((a.ROW_DATE < TO_CHAR(u.RetiredDt, 'YYYY-MM-DD') OR u.RetiredDt IS NULL) and a.ROW_DATE >= TO_CHAR(u.JoinDt, 'YYYY-MM-DD')) 
			AND a.DG_CODE = I.MENU_CODE 
			AND	(u.Level1Cd = P_arrCenterCd OR P_arrCenterCd = '%')
			AND	(u.Level2Cd = P_arrGroupCd OR P_arrGroupCd = '%')
			AND (P_arrTeamCd = '%' OR u.Level3Cd IN (SELECT column_value FROM swm.f_get_TableFromString(P_arrTeamCd,',')))
			AND (U.Deleted = P_checkGB OR P_checkGB = 'Y')
			AND U.SysKind = P_SysKind
			AND (P_arrAgentId = '%' OR U.UserID IN (SELECT column_value FROM swm.f_get_TableFromString(P_arrAgentId,','))) 
			AND hi.hd IS NULL
			GROUP BY ROW_DATE, a.LEVEL1CD, a.LEVEL2CD, a.LEVEL3CD, a.AGENT_LOGID
		) T1
		INNER JOIN swm.v_OrgInf vo ON(T1.Level1Cd = VO.Level1Cd and T1.Level2Cd = VO.Level2Cd and T1.Level3Cd = VO.Level3Cd)
		ORDER BY Color , DT, SortOrd, UserNm;
	END;
	
END;

CREATE OR REPLACE PROCEDURE SWM.p_get_nasWfm_SumHo_30min(
	P_SDT				IN VARCHAR2, 		--시작날짜
	P_EDT				IN VARCHAR2,		--종료날짜
	P_outCursor 		OUT SYS_REFCURSOR
)
IS
	v_FROM_HH	VARCHAR2(2);
BEGIN
	v_FROM_HH := TO_CHAR(SYSTIMESTAMP-(2/24), 'HH24');
	IF v_FROM_HH = '21' THEN
		v_FROM_HH := '00';
	END IF;

	OPEN P_outCursor FOR
	SELECT HD
		  ,WK
		  ,REPLACE(DT, '-', '') AS DT
		  ,HH
		  ,MM
		  ,MRP
		  ,CTIQ	
		  ,SUM(AllCalls	      ) AS AllCalls
		  ,SUM(ToAgent        ) AS ToAgent
		  ,SUM(Abandon        ) AS Abandon
		  ,SUM(Abandon10s     ) AS Abandon10s
		  ,SUM(Abandon20s     ) AS Abandon20s
		  ,SUM(Abandon30s     ) AS Abandon30s
		  ,SUM(Abandon40s     ) AS Abandon40s
		  ,SUM(Abandon50s     ) AS Abandon50s
		  ,SUM(Abandon60s     ) AS Abandon60s
		  ,SUM(Abandon120s    ) AS Abandon120s
		  ,SUM(Abandon180s    ) AS Abandon180s
		  ,SUM(Abandon300s    ) AS Abandon300s
		  ,SUM(Callback       ) AS Callback
		  ,SUM(Answer         ) AS Answer
		  ,SUM(Answer10s      ) AS Answer10s
		  ,SUM(Answer20s      ) AS Answer20s
		  ,SUM(Answer30s      ) AS Answer30s
		  ,SUM(Answer40s      ) AS Answer40s
		  ,SUM(Answer50s      ) AS Answer50s
		  ,SUM(Answer60s      ) AS Answer60s
		  ,SUM(Answer120s     ) AS Answer120s
		  ,SUM(Answer180s     ) AS Answer180s
		  ,SUM(Answer300s     ) AS Answer300s
		  ,SUM(TalkTime       ) AS TalkTime
		  ,SUM(RingTime       ) AS RingTime
		  ,SUM(ToVocalARS     ) AS ToVocalARS
		  ,SUM(ToVisualARS    ) AS ToVisualARS
		  ,SUM(FromVocalARS   ) AS FromVocalARS
		  ,SUM(FromVisualARS  ) AS FromVisualARS
		  ,SUM(CallEndAtQueue ) AS CallEndAtQueue
		  ,SUM(NetQTime       ) AS NetQTime
		  ,SUM(LocalQTime     ) AS LocalQTime
	FROM (
			SELECT
			       CASE WHEN H.DT IS NOT NULL THEN 'Y' ELSE 'N' END AS HD
			     , CASE WHEN S.ROW_WEEK = '일요일' THEN '1' 
						WHEN S.ROW_WEEK = '월요일' THEN '2' 
						WHEN S.ROW_WEEK = '화요일' THEN '3' 
						WHEN S.ROW_WEEK = '수요일' THEN '4' 
						WHEN S.ROW_WEEK = '목요일' THEN '5' 
						WHEN S.ROW_WEEK = '금요일' THEN '6' 
				       ELSE '7' END as WK
                  , S.ROW_DATE AS DT
                  , SUBSTR(S.STARTTIME,1,2) AS HH
				  ,CASE WHEN SUBSTR(S.STARTTIME,3,2) >= '00' and SUBSTR(S.STARTTIME,3,2) < '30' THEN '00'
						WHEN SUBSTR(S.STARTTIME,3,2) >= '30' and SUBSTR(S.STARTTIME,3,2) < '59' THEN '30' END AS MM
				  ,I.DNIS AS MRP
				  ,C.CTIQ AS CTIQ
                  -- , AllCalls --				  
                  , I.N_IN - I.N_TO_AGENT + (N_TALK_ACD + N_AB_ACD) AS AllCalls-- IVR 총콜수 : IVR 총콜수 - IVR연결요청호 + (응답+포기)
                  -- , ACDCalls --
                  , N_TALK_ACD + N_AB_ACD AS ToAgent -- 상담사 연결 요청호 (N_ENTER : N_TALK_ACD + N_AB_ACD 응답+포기를 사용해야 함)
                  , N_AB_ACD   AS Abandon -- 포기호
                  , N_AB_ACD_0 AS Abandon10s 
                  , N_AB_ACD_1 AS Abandon20s 
                  , N_AB_ACD_2 AS Abandon30s 
                  , N_AB_ACD_3 AS Abandon40s 
                  , N_AB_ACD_4 AS Abandon50s 
                  , N_AB_ACD_5 AS Abandon60s 
                  , N_AB_ACD_6 AS Abandon120s
                  , N_AB_ACD_7 AS Abandon180s
                  , N_AB_ACD_8 AS Abandon300s
				  , 0  AS Callback 
                  , N_TALK_ACD   AS Answer  -- AcdCalls
                  , N_WAIT_ACD_0 AS Answer10s  
                  , N_WAIT_ACD_1 AS Answer20s  
                  , N_WAIT_ACD_2 AS Answer30s  
                  , N_WAIT_ACD_3 AS Answer40s  
                  , N_WAIT_ACD_4 AS Answer50s  
                  , N_WAIT_ACD_5 AS Answer60s  
                  , N_WAIT_ACD_6 AS Answer120s 
                  , N_WAIT_ACD_7 AS Answer180s 
                  , N_WAIT_ACD_8 AS Answer300s 
                  , T_TALK_ACD   AS TalkTime   
                  , T_RING_ACD   AS RingTime
                  , N_TO_VOCAL AS ToVocalARS -- ,[ToVocalARS]
                  , N_TO_VISUAL AS ToVisualARS -- ,[ToVisualARS]
                  , N_FROM_VOCAL AS FromVocalARS -- ,[FromVocalARS]
                  , N_FROM_VISUAL AS FromVisualARS -- ,[FromVisualARS]
                  , N_NON_SERVICE AS CallEndAtQueue -- [CallEndAtQueue]  강제종료호
                  , T_WAIT_ACD AS NetQTime -- [NetQTime] 큐대기시간(응답호)
                  , T_AB_ACD AS LocalQTime -- [LocalQTime]큐대기시간(포기호)
			FROM SWM.SKILL_FT S, 
			     IV.T_SUM_ARS_FT I, 
				 SWM.T_HOLIDAY H,
                 (SELECT SmlCd, CtiqCd AS CTIQ
				   FROM SWM.T_CDINF
				  WHERE LrgCd IN ('MN', 'MNARS')
				    AND CtiqCd IS NOT NULL -- AND CtiqCd <> '' 
				  ) C
            WHERE S.ROW_DATE >= P_SDT AND S.ROW_DATE <= P_EDT
		      AND S.ROW_DATE = I.DT
		      AND I.HH = SUBSTR(S.STARTTIME, 1,2)
 			  AND I.MM = SUBSTR(S.STARTTIME, 3,2)
		      AND S.DG_CODE = I.MENU_CODE 
		      AND S.ROW_DATE = H.DT(+)
		      AND S.STARTTIME  >= v_FROM_HH || '00' -- AND HH >= @FROM_HH
--		      AND MenuCode <> ''
		      AND S.DG_CODE  = C.SMLCD
		    UNION ALL  
			SELECT
			       CASE WHEN H.DT IS NOT NULL THEN 'Y' ELSE 'N' END AS HD
			     , CASE WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '일요일' THEN '1' 
						WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '월요일' THEN '2' 
						WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '화요일' THEN '3' 
						WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '수요일' THEN '4' 
						WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '목요일' THEN '5' 
						WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '금요일' THEN '6' 
				       ELSE '7' END as WK
                  , CB.ROW_DATE AS DT
                  , SUBSTR(CB.STARTTIME,1,2) AS HH
				  ,CASE WHEN SUBSTR(CB.STARTTIME,3,2) >= '00' and SUBSTR(CB.STARTTIME,3,2) < '30' THEN '00'
						WHEN SUBSTR(CB.STARTTIME,3,2) >= '30' and SUBSTR(CB.STARTTIME,3,2) < '59' THEN '30' END AS MM
				  ,I.DNIS AS MRP
				 -- ,C.CTIQ AS CTIQ
				   , CB.DG_CODE AS CTIQ
                  -- , AllCalls --				  
                  , 0 AS AllCalls 
                  -- , ACDCalls --
                  ,0 AS ToAgent -- 상담사 연결 요청호 (N_ENTER : N_TALK_ACD + N_AB_ACD 응답+포기를 사용해야 함)
                  , 0   AS Abandon -- 포기호
                  , 0 AS Abandon10s 
                  , 0 AS Abandon20s 
                  , 0 AS Abandon30s 
                  , 0 AS Abandon40s 
                  , 0 AS Abandon50s 
                  , 0 AS Abandon60s 
                  , 0 AS Abandon120s
                  , 0 AS Abandon180s
                  , 0 AS Abandon300s
				  , CB.CALLBACK_CNT  AS Callback 
                  , 0   AS Answer  -- AcdCalls
                  , 0 AS Answer10s  
                  , 0 AS Answer20s  
                  , 0 AS Answer30s  
                  , 0 AS Answer40s  
                  , 0 AS Answer50s  
                  , 0 AS Answer60s  
                  , 0 AS Answer120s 
                  , 0 AS Answer180s 
                  , 0 AS Answer300s 
                  , 0   AS TalkTime   
                  , 0   AS RingTime
                  , 0 AS ToVocalARS -- ,[ToVocalARS]
                  , 0 AS ToVisualARS -- ,[ToVisualARS]
                  , 0 AS FromVocalARS -- ,[FromVocalARS]
                  , 0 AS FromVisualARS -- ,[FromVisualARS]
                  , 0 AS CallEndAtQueue -- [CallEndAtQueue]  강제종료호
                  , 0 AS NetQTime -- [NetQTime] 큐대기시간(응답호)
                  , 0 AS LocalQTime -- [LocalQTime]큐대기시간(포기호)
			FROM SWM.V_CALLBACK_FT CB,
				 SWM.T_IVR_DG_MAP I, 
				 SWM.T_HOLIDAY H ,
                 (SELECT SmlCd, CtiqCd AS CTIQ
				   FROM SWM.T_CDINF
				  WHERE LrgCd IN ('MN', 'MNARS')
				    AND CtiqCd IS NOT NULL -- AND CtiqCd <> '' 
				  ) C
            WHERE CB.ROW_DATE >= P_SDT AND CB.ROW_DATE <= P_EDT
		      AND CB.STARTTIME  >= v_FROM_HH || '00' -- AND HH >= @FROM_HH
		      AND CB.ROW_DATE = H.DT(+)
--		      AND MenuCode <> ''
		      AND CB.DG_CODE  = C.SMLCD
		      AND CB.DG_CODE = I.MENU_CODE 
		      
	) T1
	GROUP BY HD, WK, DT, HH, MM, MRP, CTIQ
	ORDER BY HD, WK, DT, HH, MM, MRP, CTIQ;
END;

CREATE OR REPLACE PROCEDURE SWM.p_get_nasWfm_SumMrp_Month(
	P_DateFrom			IN VARCHAR2, 		--시작날짜
	P_DateTo			IN VARCHAR2,		--종료날짜
	P_outCursor 		OUT SYS_REFCURSOR
)
IS
BEGIN

	OPEN P_outCursor FOR
	select REPLACE(DT, '-', '') AS DT
			,Name
			,SUM(NVL(ToAgent,0)) AS ToAgent		--요청호
			,SUM(NVL(Answer,0)) AS Answer		--응대호
			,SUM(NVL(Answer10s,0)) AS Answer10s
			,SUM(NVL(Answer20s,0)) AS Answer20s
			,REPLACE(swm.f_percentage_value_not100(SUM(NVL(Answer20s,0))+SUM(NVL(Answer10s,0)),SUM(NVL(ToAgent,0))), '%', '') AS SL -- 서비스레벨 (채팅상담: 목표내수락건수 / 인입)
			,SortOrd
	from (
			--1레벨
			select	SUBSTR(ROW_DATE,1, 7) AS DT
					,(case SH.DNIS when '60008' then '60001' else SH.DNIS end) as MRP
					,M.MRP_Name       AS NAME 
					,SH.N_TALK_ACD + SH.N_AB_ACD AS TOAGENT
					,SH.N_TALK_ACD   AS ANSWER
					,SH.N_TALK_ACD_0 AS ANSWER10S
					,SH.N_TALK_ACD_1 AS ANSWER20S
					,M.Sort       AS SortOrd
             FROM ( SELECT  ROW_DATE, DNIS, N_TALK_ACD, N_AB_ACD, N_TALK_ACD_0, N_TALK_ACD_1
			          FROM SWM.SKILL_DY S , 
			               SWM.T_IVR_DG_MAP I
                    WHERE S.ROW_DATE >= P_DateFrom AND ROW_DATE <= P_DateTo
			          AND S.ROW_DATE NOT IN (SELECT DT FROM SWM.T_HOLIDAY WHERE MRP = '60001')
			          AND S.DG_CODE = I.MENU_CODE
                  ) SH					  
			LEFT OUTER JOIN swm.t_mrp M ON ((case SH.DNIS when '60008' then '60001' else SH.DNIS end) = M.MRP)    
			UNION ALL 

			select	SUBSTR(DT, 1, 7) DT
					,SC.MRP
					,'채팅상담' as NAME
					,ToAgent
					,Answer
					,Answer10s
					,0 as Answer20s
					,9999 as SortOrd
			FROM swm.t_Sum_Chat_Day SC
			WHERE	DT >= P_DateFrom AND DT <= P_DateTo
			AND		HD = 'N'

			/*2레벨: CS센터상세*/
			UNION ALL
            SELECT	SUBSTR(ROW_DATE,1, 7) AS DT
					,M.MRP as MRP
					,M.MRP_Name       AS NAME 
					,SH.N_TALK_ACD + SH.N_AB_ACD AS TOAGENT
					,SH.N_TALK_ACD   AS ANSWER
					,SH.N_TALK_ACD_0 AS ANSWER10S
					,SH.N_TALK_ACD_1 AS ANSWER20S
					,M.Sort       AS SortOrd
             FROM ( SELECT ROW_DATE, DNIS, N_TALK_ACD, N_AB_ACD, N_TALK_ACD_0, N_TALK_ACD_1, MENU_CODE
			          FROM SWM.SKILL_DY S , 
			               SWM.T_IVR_DG_MAP I
                    WHERE S.ROW_DATE >= P_DateFrom AND ROW_DATE <= P_DateTo
			          AND S.ROW_DATE NOT IN (SELECT DT FROM SWM.T_HOLIDAY WHERE MRP = '60001')
			          AND S.DG_CODE = I.MENU_CODE
					  AND I.DNIS in ('60001','60008')
                  ) SH					
			LEFT OUTER JOIN ( 
				select '60001' as MRP, MRP as orgMRP, '일반상담' as MRP_Name, 'A' as MenuCd, 11 as Sort from swm.t_mrp t1 where MRP='60001'
				union all
				select '60001' as MRP,MRP as orgMRP, '사고상담' as MRP_Name, 'A04' as MenuCd, 12 as Sort from swm.t_mrp t1  where MRP='60001'
				union all		
				select '60001' as MRP,MRP as orgMRP, '사고상담' as MRP_Name, 'A04' as MenuCd, 12 as Sort from swm.t_mrp t1  where MRP='60008'
				)  M ON (SH.DNIS = M.orgMRP and  (case when SH.DNIS='60001' and SH.MENU_CODE='A04' then 'A04' 			
												  when SH.DNIS='60008' then 'A04' 			
											 else 'A' end) = M.MenuCd) 	
			/*3레벨-사고상담 상세*/
			UNION ALL

            SELECT	SUBSTR(ROW_DATE,1, 7) AS DT
					,M.MRP as MRP
					,M.MRP_Name       AS NAME 
					,SH.N_TALK_ACD + SH.N_AB_ACD AS ToAgent
					,SH.N_TALK_ACD   AS Answer
					,SH.N_TALK_ACD_0 AS Answer10s
					,SH.N_TALK_ACD_1 AS Answer20s
					,M.Sort       AS SortOrd
             FROM ( SELECT ROW_DATE, DNIS, N_TALK_ACD, N_AB_ACD, N_TALK_ACD_0, N_TALK_ACD_1, MENU_CODE
			          FROM SWM.SKILL_DY S , 
			               SWM.T_IVR_DG_MAP I
                    WHERE S.ROW_DATE >= P_DateFrom AND ROW_DATE <= P_DateTo
			          AND S.ROW_DATE NOT IN (SELECT DT FROM SWM.T_HOLIDAY WHERE MRP = '60001')
			          AND S.DG_CODE = I.MENU_CODE
					  AND I.DNIS in ('60001','60008')
					  AND ((I.DNIS='60001' and I.Menu_Code='A04') or (I.DNIS='60008'))
                  ) SH
			LEFT OUTER JOIN ( 		
				select '60001' as MRP, MRP as orgMRP, '1544-4000' as MRP_Name, 'A04' as MenuCd, 13 as Sort from swm.t_mrp t1 where MRP='60001'
				union all		
				select '60001' as MRP, MRP as orgMRP, '1833-4100' as MRP_Name, 'A' as MenuCd, 14 as Sort from swm.t_mrp t1 where MRP='60008'
			)  M ON (SH.DNIS = M.orgMRP and  (case when SH.DNIS='60001' and SH.Menu_Code='A04' then 'A04' 			
												  when SH.DNIS='60008' then 'A' 			
											      else 'A' end) = M.MenuCd)  	
	) T
	GROUP BY DT, Name, SortOrd
	UNION ALL
	--그룹별응대현황의 일반상담 합계 Row 추가
	SELECT 
		REPLACE(SUBSTR(P_DateFrom, 1, 7), '-', '') AS DT,
		'일반상담(그룹별)' AS Name,
		NVL(ToAgent,0) AS ToAgent,	--요청호
		NVL(Answer,0) AS Answer,	--응대호
		0 	AS Answer10s,
		0 	AS Answer20s,
		'0.00' 	AS SL,					
		20 		AS SortOrd	
	FROM 
	(
		select 
			(case grouping(t2.Level2Cd) when 1 then sum(t1.ToAgent)/count(distinct(t2.Level2Cd)) else sum(t1.ToAgent) end) as ToAgent,-- o요청호
			(case grouping(t2.Level2Cd) when 1 then sum(t1.Answer )/count(distinct(t2.Level2Cd)) else sum(t1.Answer ) end) as Answer,-- o응대호(호현황기준)	
			sum(t2.IB)					as IB,			-- 응대호(업무구분별 상담원의 IB)
			cast(round(sum(t1.ToAgent) * (1.0 * sum(t2.LP_RealWorkSecTime) / sum(t2.LP_RealWorkSecTime_Sum)), 2) AS NUMBER)  as DivHo ,-- 배정호(=전체요청호 * 정원의 실근무시간 / 전체정원의 실근무시간)
			cast(round(sum(t1.ToAgent) * (1.0 * sum(t2.LP_RealWorkSecTime) / sum(t2.LP_RealWorkSecTime_Sum)), 2) AS NUMBER) - sum(t2.IB) as Abandon 	-- 포기호(=배정호 - 응대호)	
		from (
			select 
				DT,
				max(MRP) as MRP,			-- 대표번호
				max(ToAgent) as ToAgent,	-- 요청호
				max(Answer) as Answer		-- 응답호
			from swm.t_Sum_Answer_Ho_Bizkind_New3
			where DT >= P_DateFrom and DT<=P_DateTo
			and   BizClasCd = '01'
			and	  Level1Cd = 'CSL'
			group by DT
		) t1
		inner join (
			select
				a.DT, a.Level1Cd, a.Level2Cd, a.Level2Nm,
				a.BizClasCd,		-- 업무구분 (=01: 일반상담, 03: 헬프데스크, 04: 사고상담)
				a.IB,				-- 응대호
				a.IB_TalkTime,		-- IB통화시간
				a.UserCnt,			-- 상담사수(퇴사자제외)
				a.VCUserCnt,		-- 휴가자수(일반휴가+반차)
				a.NormalVCUserCnt,	-- 일반휴가자
				a.HalfVCUserCnt,	-- 반차자		
				a.Worker,			-- 근무인원	
				a.ReasonTime,		-- 인정시간 (=인정시간(교육(2)+식사(3)+코칭(5)))
				a.Reason1_Time,		-- 후처리시간
				nvl(e.AddReasonTime, 0) as AddReasonTime,				-- 보정시간		
	
				(c.LimitPerson *9*60*60) as LP_TotWorkSecTime,--LP총근무시간(분)
				(c.LimitPerson *9*60*60) - a.ReasonTime - nvl(e.AddReasonTime, 0) as LP_RealWorkSecTime, -- LP실근무시간(총근무시간-인정시간-보정시간)
				(d.TotalLimitPerson *9*60*60) - a.TotReasonTime - nvl(f.TotAddReasonTime, 0) as LP_RealWorkSecTime_Sum,-- 전체정원의 실근무시간 (=(총 정원 * 9시간) - 총 (신)인정시간)
	
				a.RealWorkTime as RW_TotWorkSecTime,		-- RW총근무시간(초)(=((근무인원 - 휴가자) * 9시간) + (반차자 * 4시간))
				a.RealWorkTime - a.ReasonTime - nvl(e.AddReasonTime, 0)  as RW_RealWorkSecTime,--RW실근무시간(초)= 총근무시간-인정시간-보정시간		
				b.RW_RealWorkSecTime_Sum as RW_RealWorkSecTime_Sum,-- 합계-RW실근무시간 
	
	
				a.Worker * (a.TotWorkTime - (a.ReasonTime + nvl(e.AddReasonTime, 0))) / a.TotWorkTime as RealWorker,	-- 실투입인원(=근무인원 * (실근무시간 / 총근무시간) )
				c.LimitPerson,		-- 정원
				d.TotalLimitPerson	-- 전체정원
					
			
			from swm.t_Sum_Answer_Ho_Bizkind_New3 a
			-- RW_RealWorkSecTime_Sum: 합계-RW실근무시간 
			left outer join (
				select DT, Level1Cd, BizClasCd, sum(RW_RealWorkSecTime) as RW_RealWorkSecTime_Sum --합계-RW실근무시간 
				from(
					select 
						a.DT,
						a.Level1Cd,
						a.BizClasCd,
						a.RealWorkTime - a.ReasonTime - nvl(b.AddReasonTime, 0)  as RW_RealWorkSecTime-- 실투입인원
					from swm.t_Sum_Answer_Ho_Bizkind_New3 a
					left outer join (
						select DT, Level1Cd, Level2Cd, BizClasCd, AddReasonTime from swm.t_Group_AddReasonTime_New
					) b on (a.DT = b.DT and a.Level1Cd = b.Level1Cd and  a.Level2Cd = b.Level2Cd and a.BizClasCd = b.BizClasCd)
					where a.DT >= P_DateFrom and a.DT<=P_DateTo
					and   a.BizClasCd = '01'
					and	  a.Level1Cd = 'CSL'
				) t
				group by t.DT, t.Level1Cd, t.BizClasCd
			)  b on (a.DT = b.DT and a.Level1Cd = b.Level1Cd and a.BizClasCd = b.BizClasCd)
			left outer join (
				select DT, a.Level1Cd, a.Level2Cd, a.BizClasCd, nvl(sum(LimitPerson), 0) as LimitPerson
				from swm.t_Group_Limit_BizKind a
				where a.DT = substr(P_DateFrom, 1, 7)
				and   a.BizClasCd = '01'
				group by DT, a.Level1Cd, a.Level2Cd, a.BizClasCd
			)  c on (substr(a.DT, 1, 7) = c.DT and a.Level1Cd = c.Level1Cd and a.Level2Cd = c.Level2Cd and a.BizClasCd = c.BizClasCd)
			left outer join (
				select DT, a.Level1Cd, a.BizClasCd, SUM(LimitPerson) as TotalLimitPerson
				from swm.t_Group_Limit_BizKind a
				where a.DT = substr(P_DateFrom, 1, 7)
				and   a.BizClasCd = '01'
				group by DT, a.Level1Cd, a.BizClasCd
			) d on (substr(a.DT, 1, 7) = d.DT and a.Level1Cd = d.Level1Cd and a.BizClasCd = d.BizClasCd)
			-- AddReasonTime: 보정시간
			left outer join (
				select DT, Level1Cd, Level2Cd, BizClasCd, AddReasonTime from swm.t_Group_AddReasonTime_New
				where DT >= P_DateFrom and DT<=P_DateTo
				and   BizClasCd = '01'
			) e on (a.DT = e.DT and a.Level1Cd = e.Level1Cd and  a.Level2Cd = e.Level2Cd and a.BizClasCd = e.BizClasCd)
			-- TotAddReasonTime: 총보정시간
			left outer join (
				select DT, BizClasCd, sum(AddReasonTime) TotAddReasonTime from swm.t_Group_AddReasonTime_New
				where DT >= P_DateFrom and DT<=P_DateTo
				group by DT, BizClasCd
			) f on (a.DT = f.DT and a.BizClasCd = f.BizClasCd)
		
	
			where a.DT >= P_DateFrom and a.DT<=P_DateTo
			and   a.BizClasCd = '01'
			and	  a.Level1Cd = 'CSL'
	
		) t2 on (t1.DT=t2.DT)
		inner join swm.v_OrgInf o on (t2.Level1Cd=o.Level1Cd and t2.Level2Cd=o.Level2Cd and ORG_LVL=2)
			
		group by rollup((t2.Level1Cd,t2.Level2Cd,o.Level2Nm,o.SortOrd))
		having grouping(t2.Level2Cd) in (1) --합계Row만 조회;
		
	)
	ORDER BY DT, SortOrd;
END;

CREATE OR REPLACE PROCEDURE SWM.p_get_nasWfm_SumReason_30min(
	P_SDT				IN VARCHAR2, 		--시작날짜
	P_EDT				IN VARCHAR2,		--종료날짜
	P_outCursor 		OUT SYS_REFCURSOR
)
IS
	v_FROM_HH	VARCHAR2(2);
BEGIN
	v_FROM_HH := TO_CHAR(SYSTIMESTAMP-(2/24), 'HH24');
	IF v_FROM_HH = '21' THEN
		v_FROM_HH := '00';
	END IF;

	OPEN P_outCursor FOR
	SELECT HD
		  ,WK
		  ,REPLACE(DT, '-', '') AS DT
		  ,HH
		  ,MM
		  ,Level1Cd
		  ,Level2Cd
		  ,Level3Cd
		  ,PeripheralNumber
		 -- ,FORMAT(MIN(LoginTime), 'yyyyMMddHHmmss')  AS LoginTime 
		 -- ,FORMAT(MAX(LogoutTime), 'yyyyMMddHHmmss') AS LogoutTime
		  ,TO_CHAR(MIN(LoginTime), 'YYYYMMDDHH24MISS') AS LoginTime 
		  ,TO_CHAR(MAX(LogoutTime), 'YYYYMMDDHH24MISS') AS LogoutTime 
		  ,SUM(Reason       ) AS Reason
		  ,SUM(Reason0      ) AS Reason0
		  ,SUM(Reason1      ) AS Reason1
		  ,SUM(Reason2      ) AS Reason2
		  ,SUM(Reason3      ) AS Reason3
		  ,SUM(Reason4      ) AS Reason4
		  ,SUM(Reason5      ) AS Reason5
		  ,SUM(Reason6      ) AS Reason6
		  ,SUM(Reason7      ) AS Reason7
		  ,SUM(Reason8      ) AS Reason8
		  ,SUM(Reason9      ) AS Reason9
		  ,SUM(Reason_Time  ) AS Reason_Time
		  ,SUM(Reason0_Time ) AS Reason0_Time
		  ,SUM(Reason1_Time ) AS Reason1_Time
		  ,SUM(Reason2_Time ) AS Reason2_Time
		  ,SUM(Reason3_Time ) AS Reason3_Time
		  ,SUM(Reason4_Time ) AS Reason4_Time
		  ,SUM(Reason5_Time ) AS Reason5_Time
		  ,SUM(Reason6_Time ) AS Reason6_Time
		  ,SUM(Reason7_Time ) AS Reason7_Time
		  ,SUM(Reason8_Time ) AS Reason8_Time
		  ,SUM(Reason9_Time ) AS Reason9_Time
		  ,SUM(ReadyTime    ) AS ReadyTime
	FROM (
			SELECT 
			       CASE WHEN H.DT IS NOT NULL THEN 'Y' ELSE 'N' END AS HD
                  , a.ROW_DATE AS DT
			     , CASE WHEN a.ROW_WEEK = '일요일' THEN '1' 
						WHEN a.ROW_WEEK = '월요일' THEN '2' 
						WHEN a.ROW_WEEK = '화요일' THEN '3' 
						WHEN a.ROW_WEEK = '수요일' THEN '4' 
						WHEN a.ROW_WEEK = '목요일' THEN '5' 
						WHEN a.ROW_WEEK = '금요일' THEN '6' 
				       ELSE '7' END as WK
                  , SUBSTR(a.STARTTIME,1,2) AS HH
				  ,CASE WHEN SUBSTR(a.STARTTIME,3,2) >= '00' and SUBSTR(a.STARTTIME,3,2) < '30' THEN '00'
						WHEN SUBSTR(a.STARTTIME,3,2) >= '30' and SUBSTR(a.STARTTIME,3,2) < '59' THEN '30' END AS MM
				  , U.LEVEL1CD AS Level1Cd  -- ,a.[Level1Cd]	현재 사용자의 조직(대)
				  , U.LEVEL2CD AS Level2Cd 	-- ,a.[Level2Cd]    현재 사용자의 조직(중)
				  , U.LEVEL3CD AS Level3Cd 	-- ,a.[Level3Cd]	현재 사용자의 조직(소)
--				  , a.LEVEL1CD AS Level1Cd  -- ,a.[Level1Cd]	누적 통계의 사용자 조직(대)
--				  , a.LEVEL2CD AS Level2Cd 	-- ,a.[Level2Cd]    누적 통계의 사용자 조직(중)
--				  , a.LEVEL3CD AS Level3Cd 	-- ,a.[Level3Cd]	누적 통계의 사용자 조직(소)
                  ,a.AGENT_LOGID  AS PeripheralNumber --   ,a.[PeripheralNumber]
				  , D_LOGGD_IN AS LoginTime 	-- 로그인시각 최초 로그인 시각 (YYYYMMDDHH24MISS)
				  , D_LOGGD_OUT AS LogoutTime				  
				  ,	N_NREADY      as    Reason   
				  ,	N_NREADY_0    as    Reason0  
				  ,	N_NREADY_1    as    Reason1  
				  ,	N_NREADY_2    as    Reason2  
				  ,	N_NREADY_3    as    Reason3  
				  ,	N_NREADY_4    as    Reason4  
				  ,	N_NREADY_5    as    Reason5  
				  ,	N_NREADY_6    as    Reason6  
				  ,	N_NREADY_7    as    Reason7  
				  ,	N_NREADY_8    as    Reason8  
				  ,	N_NREADY_9    as    Reason9  
				  ,	T_TI_NREADY   as    Reason_Time 
				  ,	T_TI_NREADY_0 as    Reason0_Time
				  ,	T_TI_NREADY_1 as    Reason1_Time
				  ,	T_TI_NREADY_2 as    Reason2_Time
				  ,	T_TI_NREADY_3 as    Reason3_Time
				  ,	T_TI_NREADY_4 as    Reason4_Time
				  ,	T_TI_NREADY_5 as    Reason5_Time
				  ,	T_TI_NREADY_6 as    Reason6_Time
				  ,	T_TI_NREADY_7 as    Reason7_Time
				  ,	T_TI_NREADY_8 as    Reason8_Time
				  ,	T_TI_NREADY_9 as    Reason9_Time
				  ,	T_TI_READY	  as    ReadyTime			  
--			  FROM AGENT_FT
			  FROM SWM.AGENT_FT A , SWM.t_User U, SWM.T_HOLIDAY H
--			  WHERE DT >= '2025-01-06' and DT <= @EDT
--			  AND HH >= @FROM_HH
			  WHERE a.AGENT_LOGID = u.PeripheralNumber -- AS-IS는 User의 RetiredDt 체크를 하지만 로그인을 안하면 건수가 없으므로 제외 (a.DT < u.RetiredDt OR u.RetiredDt IS NULL) and a.DT >= u.JoinDt) 
			  AND a.ROW_DATE >= P_SDT and a.ROW_DATE <= P_EDT
			  AND a.ROW_DATE = H.DT(+)
			  AND a.STARTTIME  >= v_FROM_HH || '00' -- AND HH >= @FROM_HH
	) T1
	GROUP BY HD, DT, WK, HH, MM, Level1Cd, Level2Cd, Level3Cd, PeripheralNumber
	ORDER BY HD, DT, WK, HH, MM, Level1Cd, Level2Cd, Level3Cd, PeripheralNumber;
END;

CREATE OR REPLACE PROCEDURE SWM.P_GET_SUM_AGENT_BY_SKILL (
	p_cursor OUT SYS_REFCURSOR
)
IS
BEGIN
	OPEN p_cursor FOR
	
	SELECT   t."rank",
			 t.level2nm,
			 t.level3nm,
			 t.name,
			 t.calls AS totalcall
	FROM
	(
		SELECT   vo.level2nm,
				 vo.level3nm,
				 vo.divcd,
				 u.name,
				 SUM(NVL(c.acdcalls, 0) + NVL(c.acwoutcalls, 0) + NVL(c.auxoutcalls, 0)) AS calls,
				 ROW_NUMBER() OVER(PARTITION BY vo.divcd ORDER BY SUM(NVL(c.acdcalls, 0) + NVL(c.acwoutcalls, 0) + NVL(c.auxoutcalls, 0)) DESC) AS "rank"
		FROM	 swm.t_user u
				 INNER JOIN swm.v_orginf vo 
						 ON u.level1cd = vo.level1cd 
						AND u.level2cd = vo.level2cd 
						AND u.level3cd = vo.level3cd 
				 INNER JOIN swm.cagent c
						 ON u.userid = c.logid 
		WHERE    u.deleted    = 'N'
		AND      u.syskind    = 'CS'
		--AND      u.userclascd = 'A5'
		AND      u.level1cd   = 'CSL'
		AND      u.level3cd  != 'CSL813'
		AND      vo.divcd IN ('01', '03', '04') -- 01: Inbound, 03: Help, 04: Claim
		GROUP BY u.level1cd, u.level2cd, u.level3cd, vo.level1nm, vo.level2nm, vo.level3nm, u.userid, u.name, vo.divcd, vo.divnm
	) t
	WHERE    t."rank" < 6
	ORDER BY t.divcd, t."rank";
END;

CREATE OR REPLACE PROCEDURE SWM.p_set_chat(
	P_SDT					IN VARCHAR2,
	P_EDT					IN VARCHAR2
)
IS
	v_SDT			VARCHAR2(10);
	v_EDT			VARCHAR2(10);
	v_MM_SDT		VARCHAR2(10);
	v_MM_EDT		VARCHAR2(10);
	v_SMM			VARCHAR2(7);
	v_EMM			VARCHAR2(7);
BEGIN
	IF (P_SDT IS NULL OR P_SDT = '') THEN
		v_SDT := TO_CHAR(SYSTIMESTAMP, 'YYYY-MM-DD');
	ELSE
		v_SDT := P_SDT;
	END IF;
	IF (P_EDT IS NULL OR P_EDT = '') THEN
		v_EDT := TO_CHAR(SYSTIMESTAMP+1, 'YYYY-MM-DD');
	ELSE
		v_EDT := P_EDT;
	END IF;
	
	v_MM_SDT := TO_CHAR(TRUNC(TO_TIMESTAMP(v_SDT, 'YYYY-MM-DD'), 'MM'), 'YYYY-MM-DD');
	v_MM_EDT := TO_CHAR(LAST_DAY(TO_TIMESTAMP(v_EDT, 'YYYY-MM-DD')), 'YYYY-MM-DD');
	v_SMM := SUBSTR(v_SDT,1,7);
	v_EMM := SUBSTR(v_EDT,1,7);

	/*TEMP 테이블*/
	BEGIN
		
		BEGIN
			EXECUTE IMMEDIATE 'TRUNCATE TABLE swm.t_temp_chat';
		END;
		
		BEGIN
			INSERT INTO swm.t_Temp_Chat 
			(
				domain, 
				nodealias, 
				"DATE", 
				"TIME", 
				incnt, 
				callbackcnt, 
				failincnt, 
				totalanswercnt, 
				totalnotanswercnt, 
				servicelevelcnt, 
				lastupdatedate
			) 
			SELECT 	
				domain, 
				nodealias, 
				"DATE", 
				"TIME", 
				SUM(incnt), 
				SUM(callbackcnt), 
				SUM(failincnt), 
				SUM(totalanswercnt), 
				SUM(totalnotanswercnt), 
				SUM(servicelevelcnt), 
				MAX(lastupdatedate)
			FROM swm.t_eaichatresult_15min
			WHERE "DATE" >= TO_CHAR(TO_TIMESTAMP(v_SDT, 'YYYY-MM-DD'), 'YYYYMMDD') 
			AND "DATE" < TO_CHAR(TO_TIMESTAMP(v_EDT, 'YYYY-MM-DD'), 'YYYYMMDD') 
			GROUP BY domain, nodealias, "DATE", "TIME";
		END;
	END;
	BEGIN
		
		DELETE FROM swm.t_sum_chat_minute 
		WHERE dt >=v_SDT AND dt <v_EDT;
	END;
	BEGIN		
		/*15분별*/
		INSERT INTO swm.t_sum_chat_minute
	    (	 
			hd,
			wk,
			dt,
			hh,
			mm,
			mrp,
			allcalls,
			toagent,
			abandon,
			callback,
			answer,
			answer10s
		)
		SELECT
			CASE WHEN t2.hd IS NOT NULL THEN 'Y' ELSE 'N' END AS hd,
			wk, dt, hh, mm, t1.mrp, allcalls, toagent,	abandon, callback, answer, answer10s
		FROM 
		(
			SELECT
				wk, dt, hh, mm, mrp, allcalls, toagent,	abandon, callback, answer, answer10s
			FROM (
				SELECT
					TO_CHAR(TO_TIMESTAMP("DATE", 'YYYYMMDD'),'D') AS wk,
					TO_CHAR(TO_TIMESTAMP("DATE", 'YYYYMMDD'), 'YYYY-MM-DD') AS dt,
					SUBSTR("TIME",1,2)	AS hh,
					SUBSTR("TIME", LENGTH("TIME")-1,2) AS mm,
					'60001C' 				AS mrp,
					incnt+failincnt 		AS allcalls,
					incnt 					AS toagent,
					totalanswercnt 			AS answer,
					totalnotanswercnt 		AS abandon,
					callbackcnt 			AS callback,
					servicelevelcnt 		AS answer10s
				FROM
				(
					SELECT
						domain, nodealias, "DATE", "TIME", incnt, callbackcnt, failincnt, totalanswercnt, totalnotanswercnt, servicelevelcnt, lastupdatedate
					FROM swm.t_temp_chat
				) t
				WHERE incnt+failincnt <> 0
			) a
		) t1
		LEFT OUTER JOIN 
		(
			SELECT mrp||'C' AS mrp, 
				CASE WHEN kind='2' THEN TO_CHAR(SYSTIMESTAMP, 'YYYY')||'-'||SUBSTR(holidaymonthday, 1, 2)||'-'||SUBSTR(holidaymonthday, LENGTH(holidaymonthday)-1, 2)
					 ELSE holidayyear||'-'||SUBSTR(holidaymonthday, 1, 2)||'-'||SUBSTR(holidaymonthday, LENGTH(holidaymonthday)-1, 2) END AS hd
			FROM swm.t_holiday
		) t2 ON (t1.mrp=t2.mrp AND t1.dt=t2.hd AND hd >= v_SDT AND hd < v_EDT);
	END;
	
	/*시간별*/
	BEGIN
		
		DELETE FROM swm.t_sum_chat_hour WHERE dt >=v_SDT AND dt <v_EDT;
	  
	    INSERT INTO swm.t_sum_chat_hour
	    (	 
			hd, 
			wk, 
			dt, 
			hh, 
			mrp, 
			allcalls, 
			toagent,	
			abandon, 
			callback, 
			answer, 
			answer10s   
	    )
		SELECT
			hd, wk, dt, hh, mrp, 
			SUM(allcalls) 	AS allcalls,
			SUM(toagent) 	AS toagent,
			SUM(abandon) 	AS abandon, 
			SUM(callback) 	AS callback,	
			SUM(answer) 	AS answer, 
			SUM(answer10s) 	AS answer10s
		from swm.t_sum_chat_minute
		WHERE dt >=v_SDT 
		AND dt <v_EDT
		GROUP BY hd, wk, dt, hh, mrp;
	END;
	
	/*일별*/
	BEGIN
	
		DELETE FROM swm.t_sum_chat_day WHERE dt >=v_SDT AND dt <v_EDT;
	  
	    INSERT INTO swm.t_sum_chat_day
	    (	 
			hd, 
			wk, 
			dt, 
			mrp, 
			allcalls, 
			toagent, 
			abandon, 
			callback, 
			answer, 
			answer10s
	    )
		SELECT
			hd, wk, dt, mrp, 
			SUM(allcalls) 	AS allcalls,
			SUM(toagent) 	AS toagent,
			SUM(abandon) 	AS abandon, 
			SUM(callback) 	AS callback,	
			SUM(answer) 	AS answer, 
			SUM(answer10s) 	AS answer10s
		FROM swm.t_sum_chat_hour 
		WHERE dt >=v_SDT 
		AND dt <v_EDT
		GROUP BY hd, wk, dt, mrp;
	END;
	/*월별*/
	BEGIN
		DELETE FROM swm.t_sum_chat_month WHERE dt >=v_SMM AND dt <=v_EMM;
		
	    INSERT INTO swm.t_sum_chat_month
	    (	 
			dt, 
			mrp, 
			allcalls, 
			toagent,	
			abandon, 
			callback, 
			answer, 
			answer10s
	    )
		SELECT
			SUBSTR(dt,1,7) 	AS dt, 
			mrp, 
			SUM(allcalls) 	AS allcalls,
			SUM(toagent) 	AS toagent,
			SUM(abandon) 	AS abandon, 
			SUM(callback) 	AS callback,	
			SUM(answer) 	AS answer, 
			SUM(answer10s) 	AS answer10s
		FROM swm.t_sum_chat_day 
		WHERE dt >=v_MM_SDT 
		AND dt <=v_MM_EDT 
		AND hd='N'
		GROUP BY SUBSTR(dt,1,7), mrp;
	END;
	/*일자+그룹별 집계*/
	BEGIN
		DELETE FROM swm.t_sum_chat_day_group WHERE dt >=v_SDT AND dt <v_EDT;
	
		INSERT INTO swm.t_sum_chat_day_group
	    (	 
	    	hd,
	    	wk,
			dt, 
			mrp, 
			accountgroupname,
			accountgroupid,
			allcalls, 
			toagent,	
			abandon, 
			callback, 
			answer, 
			answer10s
	    )

		SELECT
			CASE WHEN T2.HD IS NOT NULL THEN 'Y' ELSE 'N' END AS HD,
			wk, dt, t1.mrp, accountgroupname, accountgroupid, allcalls, toagent, abandon, callback, answer, answer10s
		FROM 
		(
			SELECT
				wk, dt, mrp, accountgroupname, accountgroupid, allcalls, toagent, abandon, callback, answer, answer10s
			FROM (
				SELECT
					TO_CHAR(TO_TIMESTAMP("DATE", 'YYYYMMDD'),'D') AS wk, 
					TO_CHAR(TO_TIMESTAMP("DATE", 'YYYYMMDD'), 'YYYY-MM-DD') AS dt,
					'60001C' AS mrp, 
					accountgroupname, 
					accountgroupid, 
					incnt+failincnt AS allcalls,
					incnt AS toagent,
					totalanswercnt AS answer,
					totalnotanswercnt AS abandon,
					callbackcnt AS callback,
					servicelevelcnt AS answer10s
				FROM
				(
					SELECT 	
						domain, nodealias, "DATE", accountgroupname, accountgroupid, 
						SUM(incnt) AS incnt, SUM(callbackcnt) AS callbackcnt, SUM(failincnt) AS failincnt, SUM(totalanswercnt) AS totalanswercnt,
						SUM(totalnotanswercnt) AS totalnotanswercnt, SUM(servicelevelcnt) AS servicelevelcnt
					FROM swm.t_eaichatresult_15min
					WHERE "DATE" >= REPLACE(v_SDT, '-', '')  AND "DATE" < REPLACE(v_EDT, '-', '')
					GROUP BY domain, nodealias, "DATE", accountgroupname, accountgroupid
				) t
				where incnt+failincnt <> 0
			) a
		) t1
		LEFT OUTER JOIN 
		(
			SELECT 
				mrp||'C' AS mrp, 
				CASE WHEN kind='2' THEN TO_CHAR(SYSTIMESTAMP, 'YYYY')||'-'||SUBSTR(holidaymonthday,1,2)||'-'||SUBSTR(holidaymonthday,3,2)
					 ELSE holidayyear||'-'||SUBSTR(holidaymonthday,1,2)||'-'||SUBSTR(holidaymonthday,3,2) END AS hd
			FROM swm.t_holiday
		) t2 ON (t1.mrp=t2.mrp AND t1.dt=t2.hd AND hd >= v_SDT AND hd < v_EDT)
		WHERE accountgroupname IS NOT NULL;
	END;
--	EXCEPTION
--		WHEN OTHERS THEN
--			ROLLBACK;
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.P_SET_SUM_ANSWER_HO_BIZKIND_NEW2_RESUM(
	--P_SDT					 IN VARCHAR2,	-- 시작일자
	--P_EDT				     IN VARCHAR2,	-- 종료일자
	I_ROW_DATE		IN VARCHAR2 --시작일자
--	O_SQL_ECODE		OUT	INT,
--    O_SQL_EMSG		OUT	VARCHAR2
)
IS


	
    O_SQL_ECODE number := 0; --초기값은 0으로..
    O_SQL_EMSG  VARCHAR2(200) := 'PROC P_SET_SUM_ANSWER_HO_BIZKIND_NEW2_RESUM ONGOING...'; --초기값은 정상 처리된걸로..

BEGIN



	BEGIN
		--데이터를 집계해야 하는경우 STAT_HIST 데이터를 M추가 한다.        
		--배치 시작전 기본 데이터를 세팅한다.(집계시작날짜, 집계구분, 집계타겟날짜, 타겟테이블/간략명, 타겟프로시저명, 결과, 결과MSG, 종료날짜)
        INSERT INTO  SWM.AX_STAT_HIST
        (STARTTIME, STATGUBUN, TARGETTIME, TARGETTABLE, EXPROC, EXP, EXPMSG, ENDTIME)
        VALUES	(
					SYSDATE,					--STARTTIME
					'31(DAY)',					--STATGUBUN
					SUBSTR(I_ROW_DATE,1,8),		--TARGETTIME
					'T_SUM_ANSWER_HO_BIZKIND_NEW2_RESUM',					--TARGETTABLE
					'P_SET_SUM_ANSWER_HO_BIZKIND_NEW2_RESUM',				--EXPROC
					'ONGOING',					--EXP
					NULL,						--EXPMSG
					NULL						--ENDTIME
				);

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] STAT_HIST INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리

    END;
	
    BEGIN
		--집계를 수행해야 하는 경우 순서대로 프로세스를 수행한다.
		--집계대상 시간대에 데이터가 집계되어 있다면 삭제
		DELETE
		FROM  	SWM.T_SUM_ANSWER_HO_BIZKIND_NEW2_BACKUP 
		WHERE	DT = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2);

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] T_SUM_ANSWER_HO_BIZKIND_NEW2_BACKUP DELETE.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


	BEGIN
		INSERT INTO T_SUM_ANSWER_HO_BIZKIND_NEW2_BACKUP 
		SELECT TO_CHAR(SYSDATE,'YYYY-MM-DD') AS RunDate
			  ,DT
			  ,Level1Cd
			  ,Level2Cd
			  ,Level2Nm
			  ,DIVCD
			  ,MRP
			  ,ToAgent
			  ,Answer
			  ,IB
			  ,IB_TalkTime
			  ,OB_Answer
			  ,OB_TalkTime
			  ,UserCnt
			  ,VCUserCnt
			  ,NormalVCUserCnt
			  ,HalfVCUserCnt
			  ,TotWorkTime
			  ,ReasonTime
			  ,TotReasonTime
			  ,RealWorkTime
			  ,Worker
			  ,RealWorker
			  ,Note
			  ,MfyId
			  ,MfyDate
			  ,Reason1_Time
		  FROM t_Sum_Answer_Ho_Bizkind_NEW2 
		  WHERE DT = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
	 	;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] T_SUM_ANSWER_HO_BIZKIND_NEW2_BACKUP INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;
   
   
	BEGIN
		--집계를 수행해야 하는 경우 순서대로 프로세스를 수행한다.
		--집계대상 시간대에 데이터가 집계되어 있다면 삭제
		DELETE
		FROM  	SWM.T_SUM_ANSWER_HO_BIZKIND_NEW2
		WHERE	DT = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2);

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] T_SUM_ANSWER_HO_BIZKIND_NEW2 DELETE.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;   


	BEGIN
		insert into SWM.T_SUM_ANSWER_HO_BIZKIND_NEW2
		(
			DT, Level1Cd, Level2Cd, Level2Nm, DIVCD, MRP, 
			ToAgent, Answer,
			IB,IB_TalkTime,
			OB_Answer, OB_TalkTime, 
			UserCnt, VCUserCnt, NormalVCUserCnt, HalfVCUserCnt, 
			TotWorkTime, ReasonTime, TotReasonTime, RealWorkTime, Worker, RealWorker,Reason1_Time
		)
		select 
			t1.DT,t2.Level1Cd,t2.Level2Cd,t2.Level2Nm,t2.DivCd,'' as MRP,
			t1.ToAgent,t1.Answer,
			t2.IB,
			t2.IB_TalkTime,
			t2.OB_Answer,t2.OB_TalkTime,
			t3.UserCnt,--상담사수(퇴사자제외)
			t3.VCUserCnt,--휴가자수(일반휴가+반차)
			t3.NormalVCUserCnt, --일반휴가자
			t3.HalfVCUserCnt,--반차자
			t3.TotWorkTime, --총근무시간=(총원-휴가자)*9시간+(반차)*4시간
			NVL(t3.ReasonTime,0) as ReasonTime, --인정시간(교육+식사+코칭)
			NVL(t3.TotReasonTime,0) as TotReasonTime, --총인정시간(교육+식사+코칭)
			NVL(t3.RealWorkTime,0) as RealWorkTime,--RW_총근무시간
			NVL(t3.Worker,0) as Worker,--근무인원	
			NVL(t3.RealWorker,0) as RealWorker, --실투입인원(근무인원*(실근무시간/총근무시간))
			NVL(t3.Reason1_Time,0) as Reason1_Time --후처리시간					
		from (
			--인바운드 호현황
			
			SELECT
				DivCd
				,DT
				,WK
				,HD
				,sum(ToAgent) as ToAgent
				,sum(Answer)-sum(Callback) as Answer
			from (
				SELECT 
					/*업무구분 추출*/
					   case when I.DNIS='60006' or (I.DNIS ='60001' and I.Menu_Code<>'A04') then '01' --CS콜센터+080무료전화
							when I.DNIS='60008' or (I.DNIS='60001' and I.Menu_Code='A04') then '04' --사고보험금전담센터
							when I.DNIS='60002' then '03' else 'N/A' end as DivCd --임직원헬프데스크
	                  ,S.ROW_DATE AS DT
	 			      ,CASE WHEN S.ROW_WEEK = '일요일' THEN '1' 
					 		WHEN S.ROW_WEEK = '월요일' THEN '2' 
							WHEN S.ROW_WEEK = '화요일' THEN '3' 
							WHEN S.ROW_WEEK = '수요일' THEN '4' 
							WHEN S.ROW_WEEK = '목요일' THEN '5' 
							WHEN S.ROW_WEEK = '금요일' THEN '6' 
					       ELSE '7' END as WK				  
				      , CASE WHEN H.DESCRIPT = '공휴일' THEN 'Y' ELSE 'N' END AS HD
					  , I.DNIS AS MRP
	                  , N_TALK_ACD + N_AB_ACD + N_RING_AB_ACD AS ToAgent -- 상담사 연결 요청호 (N_ENTER : N_TALK_ACD + N_AB_ACD 응답+포기를 사용해야 함)
	                  , N_TALK_ACD   AS Answer -- 응답호
	                  , 0 AS  Callback
				FROM SWM.SKILL_DY S, 
					 SWM.T_HOLIDAY H,
					 SWM.T_IVR_DG_MAP I
	            WHERE --S.ROW_DATE >= P_SDT AND S.ROW_DATE < P_EDT
	            	S.ROW_DATE = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
			      AND S.DG_CODE = I.Menu_Code
			      AND S.ROW_DATE = H.DT(+)
			      AND I.DNIS IN ('60001','60002','60006','60008')
	            UNION ALL
				SELECT 
					/*업무구분 추출*/
					   case when I.DNIS='60006' or (I.DNIS ='60001' and I.Menu_Code<>'A04') then '01' --CS콜센터+080무료전화
							when I.DNIS='60008' or (I.DNIS='60001' and I.Menu_Code='A04') then '04' --사고보험금전담센터
							when I.DNIS='60002' then '03' else 'N/A' end as DivCd --임직원헬프데스크
	                  , CB.ROW_DATE AS DT
				     , CASE WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '일요일' THEN '1' 
							WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '월요일' THEN '2' 
							WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '화요일' THEN '3' 
							WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '수요일' THEN '4' 
							WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '목요일' THEN '5' 
							WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '금요일' THEN '6' 
					       ELSE '7' END as WK
				      , CASE WHEN H.DESCRIPT = '공휴일' THEN 'Y' ELSE 'N' END AS HD
					  , I.DNIS AS MRP
	                  , 0 AS ToAgent -- 상담사 연결 요청호 (N_ENTER : N_TALK_ACD + N_AB_ACD 응답+포기를 사용해야 함)
	                  , 0 AS Answer -- 응답호
	                  , 0 AS  Callback
				FROM SWM.V_CALLBACK_FT CB, 
					 SWM.T_HOLIDAY H,
					 SWM.T_IVR_DG_MAP I
	            WHERE --CB.ROW_DATE >= P_SDT AND CB.ROW_DATE < P_EDT
	            	CB.ROW_DATE = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
			      AND CB.DG_CODE = I.Menu_Code
			      AND CB.ROW_DATE = H.DT(+)
			      AND I.DNIS IN ('60001','60002','60006','60008')
				) a	
			group by DivCd,DT,WK,HD
		) t1 
		inner join (
			--IB,OB실적
			select 
			--     AGENT_LOGID,
			 --    I.DNIS,
			 --    I.Menu_Code,
				 u.BizClasCd AS divCd
				,A.ROW_DATE AS DT
				,A.Level1Cd AS Level1Cd
				,A.Level2Cd AS Level2Cd
				,o.Level2Nm
				,sum(case u.BizClasCd
					when '01' then (case when I.DNIS='60006' or (I.DNIS ='60001' and I.Menu_Code<>'A04') then a.N_TALK_ACD + a.N_RONA else 0 end)
					when '03' then (case when I.DNIS='60002' then a.N_TALK_ACD + a.N_RONA else 0 end)
					when '04' then (case when I.DNIS='60008' or (I.DNIS='60001' and I.Menu_Code='A04') then a.N_TALK_ACD + a.N_RONA else 0 end)
					else 0 end) as IB
				,sum(case u.BizClasCd
					when '01' then (case when I.DNIS='60006' or (I.DNIS ='60001' and I.Menu_Code<>'A04') then a.T_TALK_ACD else 0 end)
					when '03' then (case when I.DNIS='60002' then a.T_TALK_ACD else 0 end)
					when '04' then (case when I.DNIS='60008' or (I.DNIS='60001' and I.Menu_Code='A04') then a.T_TALK_ACD else 0 end)
					else 0 end) as IB_TalkTime
			    ,sum(N_TALK_OW_OB) as OB_Answer
				,sum(T_TALK_OW_OB) as OB_TalkTime			
			FROM SWM.AGENT_DY A, 
				 SWM.T_IVR_DG_MAP I,
				 SWM.v_OrgInf O,
				 SWM.t_User_DAY U
	--		inner join v_OrgInf o (nolock) on (o.Level1Cd=a.Level1Cd and o.Level2Cd=a.Level2Cd and o.Level3Cd=a.Level3Cd)
	--		inner join t_User_DAY u (nolock) on (a.PeripheralNumber = u.PeripheralNumber) 
	--		where  DT >= @SDT and DT < @EDT	
	        --WHERE A.ROW_DATE >= '2025-01-01' AND A.ROW_DATE < '2025-01-31'
			WHERE --A.ROW_DATE >= P_SDT AND A.ROW_DATE < P_EDT
			A.ROW_DATE = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
			and u.SysKind='CS'
			and a.AGENT_LOGID  = u.PeripheralNumber
			and	u.BizClasCd in ('01','03','04') --인바운드,사고,헬프
			AND a.row_date = u.dt
			and	(to_char(u.RetiredDt,'YYYY-MM-DD') > a.ROW_DATE or u.RetiredDt is null) and to_char(u.JoinDt,'YYYY-MM-DD') <= a.ROW_DATE 		
			AND (o.Level1Cd=a.Level1Cd and o.Level2Cd=a.Level2Cd and o.Level3Cd=a.Level3Cd)
			AND ( A.DG_CODE = I.MENU_CODE) -- 상담사의 OB 또는 상태 통계는 기본적으로 Z00 코드를 가지고 있음
	--        AND AGENT_LOGID = '51280093'
			group by a.ROW_DATE, a.Level1Cd, a.Level2Cd, o.Level2Nm, u.BizClasCd     
		--	 , AGENT_LOGID, I.DNIS, I.Menu_Code
		) t2 on (t1.DT=t2.DT and t1.divCd=t2.divCd)
		inner join (
		     select
				A.DT,
				A.Level1Cd,
				A.Level2Cd,
				A.divCd,
				A.UserCnt,--상담사수(퇴사자제외)
				A.VCUserCnt,--휴가자수(일반휴가+반차)
				A.NormalVCUserCnt, --일반휴가자
				A.HalfVCUserCnt,--반차자
				A.TotWorkTime, --총근무시간=(총원-휴가자)*9시간+(반차)*4시간
				A.ReasonTime, --인정시간(교육+식사+코칭)
				A.Reason1_Time, --후처리시간
				A.TotReasonTime,  --총인정시간(교육+식사+코칭)
				A.RealWorkTime,--RW_총근무시간
				A.Worker,--근무인원	
				-- convert(numeric(12,2), round(A.Worker*(1.0*A.RealWorkTime/A.TotWorkTime),2)) as RealWorker --실투입인원(근무인원*(실근무시간/총근무시간))
	           round(A.Worker*(1.0*A.RealWorkTime/A.TotWorkTime),2) as RealWorker --실투입인원(근무인원*(실근무시간/총근무시간))
				from
			(
				select 
					a.DT,
					a.Level1Cd,
					a.Level2Cd,
					a.divCd,
					UserCnt,--상담사수(퇴사자제외)
					NVL(b.VCUserCnt,0) as VCUserCnt,--휴가자수(일반휴가+반차)
					NVL(b.NormalVCUserCnt,0) as NormalVCUserCnt, --일반휴가자
					NVL(b.HalfVCUserCnt,0) as HalfVCUserCnt,--반차자
					(UserCnt-NVL(b.NormalVCUserCnt,0))*9*60*60 + NVL(b.HalfVCUserCnt,0)*4*60*60 as TotWorkTime, --총근무시간=(총원-휴가자)*9시간+(반차)*4시간
					c.Reason_Time as ReasonTime, --인정시간(교육+식사+코칭)
					c.Reason1_Time as Reason1_Time, --후처리시간
					d.Reason_Time as TotReasonTime, --총인정시간(교육+식사+코칭)
					((UserCnt-NVL(b.NormalVCUserCnt,0))*9*60*60 + NVL(b.HalfVCUserCnt,0)*4*60*60) as RealWorkTime,--RW_총근무시간[RW_TotWorkSecTime]상담사 실근무현황((근무인원(당일 팀내 사용자수-휴가자)*9시간)+(반차*4시간)
					UserCnt-NVL(b.VCUserCnt,0) as Worker--근무인원		
				from 
				(
					--그룹별 근무자(퇴사자제외)
					select 
						s.DT,
						u.Level1Cd,
						u.Level2Cd,
						u.BizClasCd AS divCd,
						count(u.UserID) as UserCnt 					
					FROM (SELECT DISTINCT ROW_DATE AS DT 
					        FROM SWM.SKILL_DY
				           WHERE --ROW_DATE >= P_SDT and ROW_DATE < P_EDT
				           	ROW_DATE = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
				             AND ROW_DATE NOT IN (SELECT DT FROM SWM.T_HOLIDAY WHERE MRP = '60001')) S
					      , swm.t_User_DAY u
					where	u.SysKind='CS'
					and		u.BizClasCd in ('01','03','04') --인바운드,사고,헬프 						
					and		(to_char(u.RetiredDt,'YYYY-MM-DD') > S.DT or u.RetiredDt is null) and to_char(u.JoinDt,'YYYY-MM-DD') <= S.DT
					AND s.dt = u.dt
					--and		u.EvltYN = 'Y'						
					group by s.DT,u.Level1Cd,u.Level2Cd,u.BizClasCd
					ORDER BY s.DT
				) a
				left outer join (
					--그룹별 휴가인원정보 (NEW3과 다른부분)
					select s.DT,u.Level1Cd,u.Level2Cd,u.BizClasCd AS divCd, 
							count(*) as VCUserCnt, --휴가인원
							sum(case when HLDS_DV_CD not in ('015','016') then 1 else 0 end ) as NormalVCUserCnt, --일반휴가자 인원
							sum(case when HLDS_DV_CD in ('015','016') then 1 else 0 end ) as HalfVCUserCnt --오전반차/오후반차 인원	   
	                FROM (SELECT distinct ROW_DATE AS DT  
					        FROM SWM.SKILL_DY
				           WHERE --ROW_DATE >= P_SDT and ROW_DATE < P_EDT
					           ROW_DATE = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
				             AND ROW_DATE NOT IN (SELECT DT FROM SWM.T_HOLIDAY WHERE MRP = '60001')) S
				          , (SELECT * 
				             FROM SWM.t_appUserVacation
				            WHERE  --HLDS_DT >= '20250101'|| '000000'  and HLDS_DT < '20250103'|| '235959' 
				            HLDS_DT >= SUBSTR(I_ROW_DATE,1,8) || '000000' AND HLDS_DT < SUBSTR(I_ROW_DATE,1,8) || '235959'
				            ) V
				          , swm.t_User_DAY U
					where	u.SysKind='CS'
					and		u.BizClasCd in ('01','03','04') --인바운드,사고,헬프 	
					AND  S.dt = u.dt
					and		(to_char(u.RetiredDt,'YYYY-MM-DD') > s.DT or u.RetiredDt is null) and to_char(u.JoinDt,'YYYY-MM-DD') <= s.DT
					--and		u.EvltYN = 'Y'				
					AND    v.UserId=u.UserID
					group by s.DT,u.Level1Cd,u.Level2Cd,u.BizClasCd
					
				) b on (a.DT=b.DT and a.Level1Cd=b.Level1Cd and a.Level2Cd=b.Level2Cd and a.divCd=b.divCd)
			
				left outer join (
					--인정시간(교육+식사+코칭+사무)
					select 
						s.DT,
						s.Level1Cd,
						s.Level2Cd,
						u.BizClasCd AS divCd,					
						sum(Reason_Time) as Reason_Time, -- 인정시간 	
						sum(Reason1_Time) as Reason1_Time				
					from (select 
							   ROW_DATE AS DT,
							   Level1Cd AS Level1Cd,
							   Level2Cd AS Level2Cd,
							   Level3Cd AS Level3Cd,
							   AGENT_LOGID AS PeripheralNumber,
							   SUM(a.T_TI_NREADY_2+a.T_TI_NREADY_3+a.T_TI_NREADY_5) as Reason_Time, --인정시간(교육(2)+식사(3)+코칭(5)), 20170412.양현정C요청
							   SUM(a.T_TI_NREADY_1) as Reason1_Time
							from SWM.AGENT_DY A 
	--						where DT>=@SDT and DT< @EDT
							where --ROW_DATE>=P_SDT and ROW_DATE< P_EDT
							 ROW_DATE = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
							group by ROW_DATE,Level1Cd,Level2Cd,Level3Cd,AGENT_LOGID					
					      ) s 
					inner join swm.t_User_day u on (s.dt = u.dt AND s.PeripheralNumber = u.PeripheralNumber)
					where	u.SysKind='CS'
					and		u.BizClasCd in ('01','03','04') --인바운드,사고,헬프 	
					and		(to_char(u.RetiredDt,'YYYY-MM-DD')  > s.DT or u.RetiredDt is null) and to_char(u.JoinDt,'YYYY-MM-DD') <= s.DT		
					--and		u.EvltYN = 'Y'	
					group by S.DT,s.Level1Cd,s.Level2Cd,u.BizClasCd
				) c on (a.DT=c.DT and a.Level1Cd=c.Level1Cd and a.Level2Cd=c.Level2Cd and a.divCd=c.divCd)
	
				left outer join (
					--총인정시간(교육+식사+코칭+사무)
					select 
						s.DT,s.Level1Cd,u.BizClasCd AS divCd,					
						sum(Reason_Time) as Reason_Time -- 인정시간 					
					from (
							select ROW_DATE AS DT,
							       Level1Cd,
							       Level2Cd,
							       Level3Cd,
							       AGENT_LOGID AS PeripheralNumber,
								SUM(a.T_TI_NREADY_2+a.T_TI_NREADY_3+a.T_TI_NREADY_5) as Reason_Time --인정시간(교육(2)+식사(3)+코칭(5)), 20170412.양현정C요청
	                         FROM SWM.AGENT_DY A
						-- 	where DT>=@SDT and DT< @EDT
	                        where --ROW_DATE>=P_SDT and ROW_DATE< P_EDT
	                         ROW_DATE = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
							group by ROW_DATE,Level1Cd,Level2Cd,Level3Cd,AGENT_LOGID					
					) s 			
					 inner join swm.t_User_day u  on (s.dt = u.dt AND s.PeripheralNumber = u.PeripheralNumber)
					where	u.SysKind='CS'
					and		u.BizClasCd in ('01','03','04') --인바운드,사고,헬프 	
					and		(to_char(u.RetiredDt,'YYYY-MM-DD') > s.DT or u.RetiredDt is null) and to_char(u.JoinDt,'YYYY-MM-DD') <= s.DT	
					--and		u.EvltYN = 'Y'	
					group by s.DT,s.Level1Cd,u.BizClasCd
				) d on (a.DT=d.DT and a.Level1Cd=d.Level1Cd and a.divCd=d.divCd)
			) A
		) 
		t3 on (t2.DT=t3.DT and t2.Level1Cd=t3.Level1Cd and t2.Level2Cd=t3.Level2Cd and t2.divCd=t3.divCd);
	
		COMMIT;
	
		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] T_SUM_ANSWER_HO_BIZKIND_NEW2 INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
		ROLLBACK; 	--오류가 발생하여 롤백처리
	END;

	IF	O_SQL_ECODE  <>  0  THEN
		--오류가 발생한 경우 FAIL과 오류코드 및 시간을 업데이트 한다.
		BEGIN
			UPDATE	SWM.AX_STAT_HIST
			SET		EXP 		= 	'FAIL',
					EXPMSG 		= 	O_SQL_EMSG,
					ENDTIME 	= 	SYSDATE
			WHERE	STATGUBUN 	= 	'31(DAY)'
			AND		TARGETTIME 	= 	SUBSTR(I_ROW_DATE,1,8)
			AND 	TARGETTABLE = 	'T_SUM_ANSWER_HO_BIZKIND_NEW2_RESUM'
			AND		EXPROC		= 	'P_SET_SUM_ANSWER_HO_BIZKIND_NEW2_RESUM'
			AND 	EXP			= 	'ONGOING';
		END;
    ELSE
		--오류가 없는 경우 SUCCESS와 결과 시간을 업데이트 한다.
		BEGIN
			UPDATE	SWM.AX_STAT_HIST
			SET		EXP 		= 	'SUCCESS',
					EXPMSG 		= 	'SUCCESS',
					ENDTIME 	= 	SYSDATE
			WHERE	STATGUBUN 	= 	'31(DAY)'
			AND		TARGETTIME 	= 	SUBSTR(I_ROW_DATE,1,8)
			AND 	TARGETTABLE = 	'T_SUM_ANSWER_HO_BIZKIND_NEW2_RESUM'
			AND		EXPROC		= 	'P_SET_SUM_ANSWER_HO_BIZKIND_NEW2_RESUM'
			AND 	EXP			= 	'ONGOING';
      	END;
    END IF;

	COMMIT;

END;

CREATE OR REPLACE PROCEDURE SWM.P_SET_SUM_ANSWER_HO_BIZKIND_NEW3_RESUM(
	--P_SDT					 IN VARCHAR2,	-- 시작일자
	--P_EDT				     IN VARCHAR2,	-- 종료일자
	I_ROW_DATE		IN VARCHAR2 --시작일자
--	O_SQL_ECODE		OUT	INT,
--    O_SQL_EMSG		OUT	VARCHAR2
)
IS


	
    O_SQL_ECODE number := 0; --초기값은 0으로..
    O_SQL_EMSG  VARCHAR2(200) := 'PROC P_SET_SUM_ANSWER_HO_BIZKIND_NEW3_RESUM ONGOING...'; --초기값은 정상 처리된걸로..

BEGIN



	BEGIN
		--데이터를 집계해야 하는경우 STAT_HIST 데이터를 M추가 한다.        
		--배치 시작전 기본 데이터를 세팅한다.(집계시작날짜, 집계구분, 집계타겟날짜, 타겟테이블/간략명, 타겟프로시저명, 결과, 결과MSG, 종료날짜)
        INSERT INTO  SWM.AX_STAT_HIST
        (STARTTIME, STATGUBUN, TARGETTIME, TARGETTABLE, EXPROC, EXP, EXPMSG, ENDTIME)
        VALUES	(
					SYSDATE,					--STARTTIME
					'31(DAY)',					--STATGUBUN
					SUBSTR(I_ROW_DATE,1,8),		--TARGETTIME
					'T_SUM_ANSWER_HO_BIZKIND_NEW3_RESUM',					--TARGETTABLE
					'P_SET_SUM_ANSWER_HO_BIZKIND_NEW3_RESUM',				--EXPROC
					'ONGOING',					--EXP
					NULL,						--EXPMSG
					NULL						--ENDTIME
				);

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] STAT_HIST INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리

    END;
	
    BEGIN
		--집계를 수행해야 하는 경우 순서대로 프로세스를 수행한다.
		--집계대상 시간대에 데이터가 집계되어 있다면 삭제
		DELETE
		FROM  	SWM.T_SUM_ANSWER_HO_BIZKIND_NEW3_BACKUP 
		WHERE	DT = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2);

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] T_SUM_ANSWER_HO_BIZKIND_NEW3_BACKUP DELETE.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


	BEGIN
		INSERT INTO T_SUM_ANSWER_HO_BIZKIND_NEW3_BACKUP 
		SELECT TO_CHAR(SYSDATE,'YYYY-MM-DD') AS RunDate
			  ,DT
			  ,Level1Cd
			  ,Level2Cd
			  ,Level2Nm
			  ,BizClasCd
			  ,MRP
			  ,ToAgent
			  ,Answer
			  ,IB
			  ,IB_TalkTime
			  ,OB_Answer
			  ,OB_TalkTime
			  ,UserCnt
			  ,VCUserCnt
			  ,NormalVCUserCnt
			  ,HalfVCUserCnt
			  ,TotWorkTime
			  ,ReasonTime
			  ,TotReasonTime
			  ,RealWorkTime
			  ,Worker
			  ,RealWorker
			  ,Note
			  ,MfyId
			  ,MfyDate
			  ,Reason1_Time
		  FROM t_Sum_Answer_Ho_Bizkind_New3 
		  WHERE DT = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
	 	;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] T_SUM_ANSWER_HO_BIZKIND_NEW3_BACKUP INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;
   
   
	BEGIN
		--집계를 수행해야 하는 경우 순서대로 프로세스를 수행한다.
		--집계대상 시간대에 데이터가 집계되어 있다면 삭제
		DELETE
		FROM  	SWM.T_SUM_ANSWER_HO_BIZKIND_NEW3
		WHERE	DT = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2);

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] T_SUM_ANSWER_HO_BIZKIND_NEW3 DELETE.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;   


	BEGIN
		insert into SWM.T_SUM_ANSWER_HO_BIZKIND_NEW3
		(
			DT, Level1Cd, Level2Cd, Level2Nm, BizClasCd, MRP, 
			ToAgent, Answer,
			IB,IB_TalkTime,
			OB_Answer, OB_TalkTime, 
			UserCnt, VCUserCnt, NormalVCUserCnt, HalfVCUserCnt, 
			TotWorkTime, ReasonTime, TotReasonTime, RealWorkTime, Worker, RealWorker,Reason1_Time
		)
		select 
			t1.DT,t2.Level1Cd,t2.Level2Cd,t2.Level2Nm,t2.BizClasCd,'' as MRP,
			t1.ToAgent,t1.Answer,
			t2.IB,
			t2.IB_TalkTime,
			t2.OB_Answer,t2.OB_TalkTime,
			t3.UserCnt,--상담사수(퇴사자제외)
			t3.VCUserCnt,--휴가자수(일반휴가+반차)
			t3.NormalVCUserCnt, --일반휴가자
			t3.HalfVCUserCnt,--반차자
			t3.TotWorkTime, --총근무시간=(총원-휴가자)*9시간+(반차)*4시간
			NVL(t3.ReasonTime,0) as ReasonTime, --인정시간(교육+식사+코칭)
			NVL(t3.TotReasonTime,0) as TotReasonTime, --총인정시간(교육+식사+코칭)
			NVL(t3.RealWorkTime,0) as RealWorkTime,--RW_총근무시간
			NVL(t3.Worker,0) as Worker,--근무인원	
			NVL(t3.RealWorker,0) as RealWorker, --실투입인원(근무인원*(실근무시간/총근무시간))
			NVL(t3.Reason1_Time,0) as Reason1_Time --후처리시간
		from (
			--인바운드 호현황
			
			SELECT
				BizClasCd
				,DT
				,WK
				,HD
				,sum(ToAgent) as ToAgent
				,sum(Answer)-sum(Callback) as Answer
			from (
				SELECT 
					/*업무구분 추출*/
					   case when I.DNIS='60006' or (I.DNIS ='60001' and I.Menu_Code<>'A04') then '01' --CS콜센터+080무료전화
							when I.DNIS='60008' or (I.DNIS='60001' and I.Menu_Code='A04') then '04' --사고보험금전담센터
							when I.DNIS='60002' then '03' else 'N/A' end as BizClasCd --임직원헬프데스크
	                  ,S.ROW_DATE AS DT
	 			      ,CASE WHEN S.ROW_WEEK = '일요일' THEN '1' 
					 		WHEN S.ROW_WEEK = '월요일' THEN '2' 
							WHEN S.ROW_WEEK = '화요일' THEN '3' 
							WHEN S.ROW_WEEK = '수요일' THEN '4' 
							WHEN S.ROW_WEEK = '목요일' THEN '5' 
							WHEN S.ROW_WEEK = '금요일' THEN '6' 
					       ELSE '7' END as WK				  
				      , CASE WHEN H.DESCRIPT = '공휴일' THEN 'Y' ELSE 'N' END AS HD
					  , I.DNIS AS MRP
	                  , N_TALK_ACD + N_AB_ACD + N_RING_AB_ACD AS ToAgent -- 상담사 연결 요청호 (N_ENTER : N_TALK_ACD + N_AB_ACD 응답+포기를 사용해야 함)
	                  , N_TALK_ACD   AS Answer -- 응답호
	                  , 0 AS  Callback
				FROM SWM.SKILL_DY S, 
					 SWM.T_HOLIDAY H,
					 SWM.T_IVR_DG_MAP I
	            WHERE --S.ROW_DATE >= P_SDT AND S.ROW_DATE < P_EDT
	            	S.ROW_DATE = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
			      AND S.DG_CODE = I.Menu_Code
			      AND S.ROW_DATE = H.DT(+)
			      AND I.DNIS IN ('60001','60002','60006','60008')
	            UNION ALL
				SELECT 
					/*업무구분 추출*/
					   case when I.DNIS='60006' or (I.DNIS ='60001' and I.Menu_Code<>'A04') then '01' --CS콜센터+080무료전화
							when I.DNIS='60008' or (I.DNIS='60001' and I.Menu_Code='A04') then '04' --사고보험금전담센터
							when I.DNIS='60002' then '03' else 'N/A' end as BizClasCd --임직원헬프데스크
	                  , CB.ROW_DATE AS DT
				     , CASE WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '일요일' THEN '1' 
							WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '월요일' THEN '2' 
							WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '화요일' THEN '3' 
							WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '수요일' THEN '4' 
							WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '목요일' THEN '5' 
							WHEN to_char(to_date(CB.ROW_DATE,'YYYY-MM-DD') , 'DAY') = '금요일' THEN '6' 
					       ELSE '7' END as WK
				      , CASE WHEN H.DESCRIPT = '공휴일' THEN 'Y' ELSE 'N' END AS HD
					  , I.DNIS AS MRP
	                  , 0 AS ToAgent -- 상담사 연결 요청호 (N_ENTER : N_TALK_ACD + N_AB_ACD 응답+포기를 사용해야 함)
	                  , 0 AS Answer -- 응답호
	                  , 0 AS  Callback
				FROM SWM.V_CALLBACK_FT CB, 
					 SWM.T_HOLIDAY H,
					 SWM.T_IVR_DG_MAP I
	            WHERE --CB.ROW_DATE >= P_SDT AND CB.ROW_DATE < P_EDT
	            	CB.ROW_DATE = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
			      AND CB.DG_CODE = I.Menu_Code
			      AND CB.ROW_DATE = H.DT(+)
			      AND I.DNIS IN ('60001','60002','60006','60008')
				) a	
			group by BizClasCd,DT,WK,HD
		) t1 
		inner join (
			--IB,OB실적
			select 
				 u.BizClasCd
				,A.ROW_DATE AS DT
				,A.Level1Cd AS Level1Cd
				,A.Level2Cd AS Level2Cd
				,o.Level2Nm
				,sum(case u.BizClasCd
					when '01' then (case when I.DNIS='60006' or (I.DNIS ='60001' and I.Menu_Code<>'A04') then a.N_TALK_ACD + a.N_RONA else 0 end)
					when '03' then (case when I.DNIS='60002' then a.N_TALK_ACD + a.N_RONA else 0 end)
					when '04' then (case when I.DNIS='60008' or (I.DNIS='60001' and I.Menu_Code='A04') then a.N_TALK_ACD + a.N_RONA else 0 end)
					else 0 end) as IB
				,sum(case u.BizClasCd
					when '01' then (case when I.DNIS='60006' or (I.DNIS ='60001' and I.Menu_Code<>'A04') then a.T_TALK_ACD else 0 end)
					when '03' then (case when I.DNIS='60002' then a.T_TALK_ACD else 0 end)
					when '04' then (case when I.DNIS='60008' or (I.DNIS='60001' and I.Menu_Code='A04') then a.T_TALK_ACD else 0 end)
					else 0 end) as IB_TalkTime
			    ,sum(N_TALK_OW_OB) as OB_Answer
				,sum(T_TALK_OW_OB) as OB_TalkTime			
			FROM SWM.AGENT_DY A, 
				 SWM.T_IVR_DG_MAP I,
				 SWM.v_OrgInf O,
				 SWM.t_User_day U
			WHERE --A.ROW_DATE >= P_SDT AND A.ROW_DATE < P_EDT
			A.ROW_DATE = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
			and u.SysKind='CS'
			and a.AGENT_LOGID  = u.PeripheralNumber
			AND a.row_date = u.dt
			and	u.BizClasCd in ('01','03','04') --인바운드,사고,헬프 	
			and	(to_char(u.RetiredDt,'YYYY-MM-DD') > a.ROW_DATE or u.RetiredDt is null) and to_char(u.JoinDt,'YYYY-MM-DD') <= a.ROW_DATE	
			and	u.EvltYN = 'Y'	
			AND (o.Level1Cd=a.Level1Cd and o.Level2Cd=a.Level2Cd and o.Level3Cd=a.Level3Cd)
			AND ( A.DG_CODE = I.MENU_CODE) -- 상담사의 OB 또는 상태 통계는 기본적으로 Z00 코드를 가지고 있음
	--        AND AGENT_LOGID = '51280093'
			group by a.ROW_DATE, a.Level1Cd, a.Level2Cd, o.Level2Nm, u.BizClasCd     
		--	 , AGENT_LOGID, I.DNIS, I.Menu_Code
		) t2 on (t1.DT=t2.DT and t1.BizClasCd=t2.BizClasCd)
		inner join (
		     select
				A.DT,
				A.Level1Cd,
				A.Level2Cd,
				A.BizClasCd,
				A.UserCnt,--상담사수(퇴사자제외)
				A.VCUserCnt,--휴가자수(일반휴가+반차)
				A.NormalVCUserCnt, --일반휴가자
				A.HalfVCUserCnt,--반차자
				A.TotWorkTime, --총근무시간=(총원-휴가자)*9시간+(반차)*4시간
				A.ReasonTime, --인정시간(교육+식사+코칭)
				A.Reason1_Time, --후처리시간
				A.TotReasonTime,  --총인정시간(교육+식사+코칭)
				A.RealWorkTime,--RW_총근무시간
				A.Worker,--근무인원	
				-- convert(numeric(12,2), round(A.Worker*(1.0*A.RealWorkTime/A.TotWorkTime),2)) as RealWorker --실투입인원(근무인원*(실근무시간/총근무시간))
	           round(A.Worker*(1.0*A.RealWorkTime/A.TotWorkTime),2) as RealWorker --실투입인원(근무인원*(실근무시간/총근무시간))
				from
			(
				select 
					a.DT,
					a.Level1Cd,
					a.Level2Cd,
					a.BizClasCd,
					UserCnt,--상담사수(퇴사자제외)
					NVL(b.VCUserCnt,0) as VCUserCnt,--휴가자수(일반휴가+반차)
					NVL(b.NormalVCUserCnt,0) as NormalVCUserCnt, --일반휴가자
					NVL(b.HalfVCUserCnt,0) as HalfVCUserCnt,--반차자
					(UserCnt-NVL(b.NormalVCUserCnt,0))*9*60*60 + NVL(b.HalfVCUserCnt,0)*4*60*60 as TotWorkTime, --총근무시간=(총원-휴가자)*9시간+(반차)*4시간
					c.Reason_Time as ReasonTime, --인정시간(교육+식사+코칭)
					c.Reason1_Time as Reason1_Time, --후처리시간
					d.Reason_Time as TotReasonTime, --총인정시간(교육+식사+코칭)
					((UserCnt-NVL(b.NormalVCUserCnt,0))*9*60*60 + NVL(b.HalfVCUserCnt,0)*4*60*60) as RealWorkTime,--RW_총근무시간[RW_TotWorkSecTime]상담사 실근무현황((근무인원(당일 팀내 사용자수-휴가자)*9시간)+(반차*4시간)
					UserCnt-NVL(b.VCUserCnt,0) as Worker--근무인원		
				from 
				(
					--그룹별 근무자(퇴사자제외)
					select 
						s.DT,
						u.Level1Cd,
						u.Level2Cd,
						u.BizClasCd,
						count(u.UserID) as UserCnt 					
					FROM (SELECT DISTINCT ROW_DATE AS DT 
					        FROM SWM.SKILL_DY
				           WHERE --ROW_DATE >= P_SDT and ROW_DATE < P_EDT
				           	ROW_DATE = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
				             AND ROW_DATE NOT IN (SELECT DT FROM SWM.T_HOLIDAY WHERE MRP = '60001')) S
					      , swm.t_User_day u
					where	u.SysKind='CS'
					and		u.BizClasCd in ('01','03','04') --인바운드,사고,헬프 	
					and		(to_char(u.RetiredDt,'YYYY-MM-DD') > S.DT or u.RetiredDt is null) and to_char(u.JoinDt,'YYYY-MM-DD') <= S.DT	
					and		u.EvltYN = 'Y'	
					AND s.dt = u.dt
					group by s.DT,u.Level1Cd,u.Level2Cd,u.BizClasCd
					ORDER BY s.DT
				) a
				left outer join (
					--그룹별 휴가인원정보
					select s.DT,u.Level1Cd,u.Level2Cd,u.BizClasCd, 
							count(*) as VCUserCnt, --휴가인원
							sum(case when HLDS_DV_CD not in ('015','016','018','019','022','023') then 1 else 0 end ) as NormalVCUserCnt, --일반휴가자 인원
							sum(case when HLDS_DV_CD in ('015','016','018','019','022','023') then 1 else 0 end ) as HalfVCUserCnt --오전반차/오후반차 인원	   
	                FROM (SELECT distinct ROW_DATE AS DT  
					        FROM SWM.SKILL_DY
				           WHERE --ROW_DATE >= P_SDT and ROW_DATE < P_EDT
					           ROW_DATE = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
				             AND ROW_DATE NOT IN (SELECT DT FROM SWM.T_HOLIDAY WHERE MRP = '60001')) S
				          , (SELECT to_char(hlds_dt,'YYYY-MM-DD') AS dt, uv.*
				             FROM SWM.t_appUserVacation uv
				            WHERE  --HLDS_DT >= '20250101'|| '000000'  and HLDS_DT < '20250103'|| '235959' 
				            HLDS_DT = TO_TIMESTAMP(I_ROW_DATE, 'YYYY-MM-DD') 
				            ) V
				          , swm.T_USER_DAY U
					where	u.SysKind='CS'
					and		u.BizClasCd in ('01','03','04') --인바운드,사고,헬프 	
					and		(to_char(u.RetiredDt,'YYYY-MM-DD') > s.DT or u.RetiredDt is null) and to_char(u.JoinDt,'YYYY-MM-DD') <= s.DT	
					and		u.EvltYN = 'Y'				
					AND    v.UserId=u.UserID
					AND	v.dt = u.dt
					group by s.DT,u.Level1Cd,u.Level2Cd,u.BizClasCd
					
				) b on (a.DT=b.DT and a.Level1Cd=b.Level1Cd and a.Level2Cd=b.Level2Cd and a.BizClasCd=b.BizClasCd)
			
				left outer join (
					--인정시간(교육+식사+코칭+사무)
					select 
						s.DT,
						s.Level1Cd,
						s.Level2Cd,
						u.BizClasCd,					
						sum(Reason_Time) as Reason_Time, -- 인정시간 	
						sum(Reason1_Time) as Reason1_Time				
					from (select 
							   ROW_DATE AS DT,
							   Level1Cd AS Level1Cd,
							   Level2Cd AS Level2Cd,
							   Level3Cd AS Level3Cd,
							   AGENT_LOGID AS PeripheralNumber,
							   SUM(a.T_TI_NREADY_2+a.T_TI_NREADY_3+a.T_TI_NREADY_5) as Reason_Time, --인정시간(교육(2)+식사(3)+코칭(5)), 20170412.양현정C요청
							   SUM(a.T_TI_NREADY_1) as Reason1_Time
							from SWM.AGENT_DY A 
	--						where DT>=@SDT and DT< @EDT
							where --ROW_DATE>=P_SDT and ROW_DATE< P_EDT
							 ROW_DATE = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
							group by ROW_DATE,Level1Cd,Level2Cd,Level3Cd,AGENT_LOGID					
					      ) s 
					inner join swm.t_User_day u on (s.dt = u.dt AND s.PeripheralNumber = u.PeripheralNumber)
					where	u.SysKind='CS'
					and		u.BizClasCd in ('01','03','04') --인바운드,사고,헬프 	
					and		(to_char(u.RetiredDt,'YYYY-MM-DD')  > s.DT or u.RetiredDt is null) and to_char(u.JoinDt,'YYYY-MM-DD') <= s.DT	
					and		u.EvltYN = 'Y'	
					group by s.DT,s.Level1Cd,s.Level2Cd,u.BizClasCd
				) c on (a.DT=c.DT and a.Level1Cd=c.Level1Cd and a.Level2Cd=c.Level2Cd and a.BizClasCd=c.BizClasCd)
	
				left outer join (
					--총인정시간(교육+식사+코칭+사무)
					select 
						s.DT,s.Level1Cd,u.BizClasCd,					
						sum(Reason_Time) as Reason_Time -- 인정시간 					
					from (
							select ROW_DATE AS DT,
							       Level1Cd,
							       Level2Cd,
							       Level3Cd,
							       AGENT_LOGID AS PeripheralNumber,
								SUM(a.T_TI_NREADY_2+a.T_TI_NREADY_3+a.T_TI_NREADY_5) as Reason_Time --인정시간(교육(2)+식사(3)+코칭(5)), 20170412.양현정C요청
	                         FROM SWM.AGENT_DY A
						-- 	where DT>=@SDT and DT< @EDT
	                        where --ROW_DATE>=P_SDT and ROW_DATE< P_EDT
	                         ROW_DATE = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
							group by ROW_DATE,Level1Cd,Level2Cd,Level3Cd,AGENT_LOGID					
					) s 			
					 inner join swm.t_User_day u  on (s.dt = u.dt AND s.PeripheralNumber = u.PeripheralNumber)
					where	u.SysKind='CS'
					and		u.BizClasCd in ('01','03','04') --인바운드,사고,헬프 	
					and		(to_char(u.RetiredDt,'YYYY-MM-DD') > s.DT or u.RetiredDt is null) and to_char(u.JoinDt,'YYYY-MM-DD') <= s.DT	
					and		u.EvltYN = 'Y'	
					group by s.DT,s.Level1Cd,u.BizClasCd
				) d on (a.DT=d.DT and a.Level1Cd=d.Level1Cd and a.BizClasCd=d.BizClasCd)
			) A
		) 
		t3 on (t2.DT=t3.DT and t2.Level1Cd=t3.Level1Cd and t2.Level2Cd=t3.Level2Cd and t2.BizClasCd=t3.BizClasCd);
	
		COMMIT;
	
		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] T_SUM_ANSWER_HO_BIZKIND_NEW3 INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
		ROLLBACK; 	--오류가 발생하여 롤백처리
	END;

	IF	O_SQL_ECODE  <>  0  THEN
		--오류가 발생한 경우 FAIL과 오류코드 및 시간을 업데이트 한다.
		BEGIN
			UPDATE	SWM.AX_STAT_HIST
			SET		EXP 		= 	'FAIL',
					EXPMSG 		= 	O_SQL_EMSG,
					ENDTIME 	= 	SYSDATE
			WHERE	STATGUBUN 	= 	'31(DAY)'
			AND		TARGETTIME 	= 	SUBSTR(I_ROW_DATE,1,8)
			AND 	TARGETTABLE = 	'T_SUM_ANSWER_HO_BIZKIND_NEW3_RESUM'
			AND		EXPROC		= 	'P_SET_SUM_ANSWER_HO_BIZKIND_NEW3_RESUM'
			AND 	EXP			= 	'ONGOING';
		END;
    ELSE
		--오류가 없는 경우 SUCCESS와 결과 시간을 업데이트 한다.
		BEGIN
			UPDATE	SWM.AX_STAT_HIST
			SET		EXP 		= 	'SUCCESS',
					EXPMSG 		= 	'SUCCESS',
					ENDTIME 	= 	SYSDATE
			WHERE	STATGUBUN 	= 	'31(DAY)'
			AND		TARGETTIME 	= 	SUBSTR(I_ROW_DATE,1,8)
			AND 	TARGETTABLE = 	'T_SUM_ANSWER_HO_BIZKIND_NEW3_RESUM'
			AND		EXPROC		= 	'P_SET_SUM_ANSWER_HO_BIZKIND_NEW3_RESUM'
			AND 	EXP			= 	'ONGOING';
      	END;
    END IF;

	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.p_set_sum_apptmagent_day(
	P_StdDTFrom			IN VARCHAR2,	-- 시작일자
	P_StdDTTo			IN VARCHAR2		-- 종료일자
)
IS
	v_StdDTFrom		VARCHAR2(10);
	v_StdDTTo		VARCHAR2(10);
BEGIN
	v_StdDTFrom := TO_CHAR(TO_TIMESTAMP(P_StdDTFrom, 'YYYY-MM-DD'), 'YYYYMMDD');
	v_StdDTTo := TO_CHAR(TO_TIMESTAMP(P_StdDTTo, 'YYYY-MM-DD'), 'YYYYMMDD');
	BEGIN
		MERGE INTO swm.t_apptmagent_day_mix trg
		USING(
			SELECT
				TO_CHAR(TO_TIMESTAMP(dt, 'YYYYMMDD'), 'YYYY-MM-DD') AS dt,
				u.level1cd				AS cntr_cd,
				u.level2cd				AS grp_cd,
				u.level3cd				AS team_cd,
				usr_id					AS usr_id,
				cont_st_nm				AS cont_st_nm,
				tot_cnt					AS tot_cnt,
				tot_amt					AS tot_amt,
				guarant_cnt				AS guarant_cnt,
				guarant_amt				AS guarant_amt,
				cpc						AS exchng_grd
			FROM (
				SELECT
					'1' 						AS cont_st_nm,	 
					psdt 						AS dt,
					clpPsnno 					AS usr_id,
					COUNT(poliNo) 				AS tot_cnt,
					SUM(TO_NUMBER(tprm)) 		AS tot_amt,
					SUM(CASE WHEN prodlccd='1' THEN 1 ELSE 0 END) 	AS guarant_cnt,
					SUM(CASE WHEN prodlccd='1' THEN TO_NUMBER(tprm) ELSE 0 END) AS guarant_amt,
					SUM(TO_NUMBER(nbptmmcvav)) 	AS cpc
				FROM swm.t_eaitmbizdata a
				INNER JOIN swm.t_cdinf h ON (clctbrofbrcd=h.smlcd AND h.lrgcd='BC_N092')
				LEFT OUTER JOIN swm.t_cdinf c ON (a.cntrdtlscd=c.smlcd and c.lrgcd='BC_CNTR')
				where 1=1		
				AND psdt>=v_StdDTFrom AND psdt<=v_StdDTTo
				AND c.dscnm IN ('1','2')		
				AND a.rvcyCd <> '00'
				GROUP BY psdt, clppsnno
				
				UNION ALL

				SELECT  	
					'2' 						AS cont_st_nm, 
					SUBSTR(lschgdtm,1,8)		AS dt,
					clpPsnno 					AS usr_id,
					COUNT(poliNo) 				AS tot_cnt,
					SUM(TO_NUMBER(tprm)) 		AS tot_amt,
					SUM(CASE WHEN prodlccd='1' THEN 1 ELSE 0 END) 	AS guarant_cnt,
					SUM(CASE WHEN prodlccd='1' THEN TO_NUMBER(tprm) ELSE 0 END) AS guarant_amt,
					SUM(TO_NUMBER(nbptmmcvav)) 	AS cpc
				from swm.t_eaiTMBizData a
				INNER JOIN swm.t_cdinf h ON (clctbrofbrcd=h.smlcd AND h.lrgcd='BC_N092')
				LEFT OUTER JOIN swm.t_cdinf c ON (a.cntrdtlscd=c.SmlCd AND c.lrgcd='BC_CNTR')
				WHERE 1=1		
				AND lschgdtm>=v_StdDTFrom||'000000' AND psdt<=v_StdDTTo||'999999'
				AND c.dscnm IN ('3')
				AND a.rvcycd <> '00'
				GROUP BY SUBSTR(lschgdtm,1,8), clpPsnno
				
				UNION ALL

				SELECT  	
					'3' 											AS cont_st_nm,
					NVL(a1.dt, a2.dt)								AS dt,
					NVL(a1.usr_id, a2.usr_id)						AS usr_id,
					SUM(NVL(a1.tot_cnt,0)-NVL(a2.tot_cnt,0)) 		AS tot_cnt,
					SUM(NVL(a1.tot_amt,0)-NVL(a2.tot_amt,0)) 		AS tot_amt,
					SUM(NVL(a1.guarant_cnt,0)-NVL(a2.guarant_cnt,0)) AS guarant_cnt,
					SUM(NVL(a1.guarant_amt,0)-NVL(a2.guarant_amt,0)) AS guarant_amt,
					SUM(NVL(a1.cpc,0)-NVL(a2.cpc,0)) 				AS cpc
				from (
					select  	
						'1' 						AS cont_st_nm,	 
						psdt 						AS dt,
						clpPsnno 					AS usr_id,
						COUNT(poliNo) 				AS tot_cnt,
						SUM(TO_NUMBER(tprm)) 		AS tot_amt,
						SUM(CASE WHEN prodlccd='1' THEN 1 ELSE 0 END) 	AS guarant_cnt,
						SUM(CASE WHEN prodlccd='1' THEN TO_NUMBER(tprm) ELSE 0 END) AS guarant_amt,
						SUM(TO_NUMBER(nbptmmcvav)) 	AS cpc
		
					FROM swm.t_eaitmbizdata a
					INNER JOIN swm.t_cdinf h ON (clctbrofbrcd=h.smlcd AND h.lrgcd='BC_N092')
					LEFT OUTER JOIN swm.t_cdinf c ON (a.cntrdtlscd=c.smlcd and c.lrgcd='BC_CNTR')
					where 1=1		
					AND psdt>=v_StdDTFrom AND psdt<=v_StdDTTo
					AND c.dscnm IN ('1','2')		
					AND a.rvcyCd <> '00'
					GROUP BY psdt, clppsnno
				) a1 full outer join (
					SELECT  	
						'2' 						AS cont_st_nm, 
						SUBSTR(lschgdtm,1,8)		AS dt,
						clpPsnno 					AS usr_id,
						COUNT(poliNo) 				AS tot_cnt,
						SUM(TO_NUMBER(tprm)) 		AS tot_amt,
						SUM(CASE WHEN prodlccd='1' THEN 1 ELSE 0 END) 	AS guarant_cnt,
						SUM(CASE WHEN prodlccd='1' THEN TO_NUMBER(tprm) ELSE 0 END) AS guarant_amt,
						SUM(TO_NUMBER(nbptmmcvav)) 	AS cpc
					from swm.t_eaiTMBizData a
					INNER JOIN swm.t_cdinf h ON (clctbrofbrcd=h.smlcd AND h.lrgcd='BC_N092')
					LEFT OUTER JOIN swm.t_cdinf c ON (a.cntrdtlscd=c.SmlCd AND c.lrgcd='BC_CNTR')
					WHERE 1=1		
					AND lschgdtm>=v_StdDTFrom||'000000' AND psdt<=v_StdDTTo||'999999'
					AND c.dscnm IN ('3')
					AND a.rvcycd <> '00'
					GROUP BY SUBSTR(lschgdtm,1,8), clpPsnno
				) a2 ON (a1.dt=a2.dt AND a1.usr_id=a2.usr_id)
				GROUP BY NVL(a1.dt, a2.dt), NVL(a1.usr_id, a2.usr_id)
			) t LEFT OUTER JOIN swm.t_user u ON (t.usr_id=u.userid)
		) src
		ON (src.dt = trg.dt AND src.usr_id = trg.usr_id 
		AND src.cont_st_nm = trg.cont_st_nm)
		WHEN MATCHED THEN
			UPDATE SET
				trg.cntr_cd    		= src.cntr_cd,
				trg.grp_cd    		= src.grp_cd,
				trg.team_cd    		= src.team_cd,
				trg.tot_cnt    		= src.tot_cnt,
				trg.tot_amt    		= src.tot_amt,
				trg.guarant_cnt    	= src.guarant_cnt,
				trg.guarant_amt    	= src.guarant_amt,
				trg.exchng_grd    	= src.exchng_grd,
				trg.lastmodifydate 	= SYSTIMESTAMP
		WHEN NOT MATCHED THEN
			INSERT (
				dt,
				cntr_cd,
				grp_cd,
				team_cd,
				usr_id,
				cont_st_nm,
				tot_cnt,
				tot_amt,
				guarant_cnt,
				guarant_amt,
				exchng_grd,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.dt,
				src.cntr_cd,
				src.grp_cd,
				src.team_cd,
				src.usr_id,
				src.cont_st_nm,
				src.tot_cnt,
				src.tot_amt,
				src.guarant_cnt,
				src.guarant_amt,
				src.exchng_grd,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			);
	END;
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.p_set_sum_apptmagent_time(
	P_StdDTFrom			IN VARCHAR2,	-- 시작일자
	P_StdDTTo			IN VARCHAR2		-- 종료일자
)
IS
	v_StdDTFrom		VARCHAR2(10);
	v_StdDTTo		VARCHAR2(10);
BEGIN
	v_StdDTFrom := TO_CHAR(TO_TIMESTAMP(P_StdDTFrom, 'YYYY-MM-DD'), 'YYYYMMDD');
	v_StdDTTo := TO_CHAR(TO_TIMESTAMP(P_StdDTTo, 'YYYY-MM-DD'), 'YYYYMMDD');
	BEGIN
		MERGE INTO swm.t_apptmagent_time_mix trg
		USING(
			SELECT
				TO_CHAR(TO_TIMESTAMP(dt, 'YYYYMMDD'), 'YYYY-MM-DD') AS dt,
				tm						AS tm,
				u.level1cd				AS cntr_cd,
				u.level2cd				AS grp_cd,
				u.level3cd				AS team_cd,
				usr_id					AS usr_id,
				cont_dv_nm				AS cont_dv_nm,
				cnt						AS cnt,
				insufee					AS insufee
			FROM (
				SELECT
					'1' 						AS cont_dv_nm,
					psdt 						AS dt,
					SUBSTR(sustrmsdtm,9,2) 	AS tm,
					clpPsnno 					AS usr_id,
					COUNT(poliNo) 				AS cnt,
					SUM(TO_NUMBER(tprm)) 		AS insufee
				FROM swm.t_eaitmbizdata a
				INNER JOIN swm.t_cdinf h ON (clctbrofbrcd=h.smlcd AND h.lrgcd='BC_N092')
				LEFT OUTER JOIN swm.t_cdinf c ON (a.cntrdtlscd=c.smlcd and c.lrgcd='BC_CNTR')
				where 1=1		
				AND psdt>=v_StdDTFrom AND psdt<=v_StdDTTo
				AND c.dscnm IN ('1','2')		
				AND a.rvcyCd <> '00'
				GROUP BY psdt, SUBSTR(sustrmsdtm,9,2), clppsnno
				
				UNION ALL

				SELECT  	
					'2' 						AS cont_dv_nm,	 
					psdt 						AS dt,
					SUBSTR(sustrmsdtm,9,2) 	AS tm,
					clpPsnno 					AS usr_id,
					COUNT(poliNo) 				AS cnt,
					SUM(TO_NUMBER(tprm)) 		AS insufee
				from swm.t_eaiTMBizData a
				INNER JOIN swm.t_cdinf h ON (clctbrofbrcd=h.smlcd AND h.lrgcd='BC_N092')
				LEFT OUTER JOIN swm.t_cdinf c ON (a.cntrdtlscd=c.SmlCd AND c.lrgcd='BC_CNTR')
				WHERE 1=1		
				AND psdt>=v_StdDTFrom AND psdt<=v_StdDTTo
				AND c.dscnm IN ('1','2')
				AND a.rvcycd <> '00'
				GROUP BY psdt, SUBSTR(sustrmsdtm,9,2), clppsnno
			) t LEFT OUTER JOIN swm.t_user u ON (t.usr_id=u.userid)
		) src
		ON (src.dt = trg.dt AND src.tm = trg.tm AND src.usr_id = trg.usr_id 
		AND src.cont_dv_nm = trg.cont_dv_nm)
		WHEN MATCHED THEN
			UPDATE SET
				trg.cntr_cd    		= src.cntr_cd,
				trg.grp_cd    		= src.grp_cd,
				trg.team_cd    		= src.team_cd,
				trg.cnt    			= src.cnt,
				trg.insufee    		= src.insufee,
				trg.lastmodifydate 	= SYSTIMESTAMP
		WHEN NOT MATCHED THEN
			INSERT (
				dt,
				tm,
				cntr_cd,
				grp_cd,
				team_cd,
				usr_id,
				cont_dv_nm,
				cnt,
				insufee,
				call_cnt,
				avg_time,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.dt,
				src.tm,
				src.cntr_cd,
				src.grp_cd,
				src.team_cd,
				src.usr_id,
				src.cont_dv_nm,
				src.cnt,
				src.insufee,
				0,
				0,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			);
	END; 
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.p_set_sum_apptmteam_day(
	P_StdDTFrom			IN VARCHAR2,	-- 시작일자
	P_StdDTTo			IN VARCHAR2		-- 종료일자
)
IS
BEGIN
	BEGIN
		MERGE INTO swm.t_apptmteam_day_mix trg
		USING(
			SELECT
				NVL(a.dt, b.dt) 			AS dt,
				NVL(a.cntr_cd, b.cntr_cd) 	AS cntr_cd,
				NVL(a.grp_cd, b.grp_cd) 	AS grp_cd,
				NVL(a.team_cd, b.team_cd) 	AS team_cd,
				NVL(a.subs_cnt, 0) 			AS subs_cnt,
				NVL(a.subs_amt, 0) 			AS subs_amt,
				NVL(a.cont_cnt ,0) 			AS cont_cnt,
				NVL(a.CONT_AMT ,0) 			AS cont_amt,
				NVL(b.etco_cnt ,0) 			AS etco_cnt,
				NVL(b.call_prs_cnt ,0) 		AS call_prs_cnt,
				NVL(b.tmr_cnt ,0) 			AS tmr_cnt,
				NVL(b.call_cnt ,0) 			AS call_cnt,
				NVL(b.cur_cnt ,0) 			AS cur_cnt,
				NVL(b.cust_rsps01 ,0) 		AS cust_rsps01,
				NVL(b.exec_cnt ,0) 			AS exec_cnt,
				NVL(b.lead_time ,0) 		AS lead_time
			FROM (
				SELECT 
					DT,	
					CNTR_CD,
					GRP_CD,
					TEAM_CD,
					SUM(CASE WHEN cont_dv_nm='1' THEN cnt ELSE 0 END) 		AS subs_cnt,
					SUM(CASE WHEN cont_dv_nm='1' THEN insufee ELSE 0 END) 	AS subs_amt,
					SUM(CASE WHEN cont_dv_nm='2' THEN cnt ELSE 0 END) 		AS cont_cnt,
					SUM(CASE WHEN cont_dv_nm='2' THEN insufee ELSE 0 END) 	AS cont_amt
				FROM swm.t_apptmagent_time_mix a
				INNER JOIN swm.v_orginf o 
					ON (o.level1cd=a.cntr_cd AND o.level2cd=a.grp_cd AND  o.level3cd=a.team_cd AND syskind='TM' AND o.divcd IN ('5'))	
				WHERE dt >= P_StdDTFrom AND dt <= P_StdDTTo
				AND cont_dv_nm IN ('1','2')
				GROUP BY dt, cntr_cd, grp_cd, team_cd
			) a
			FULL OUTER JOIN (
				SELECT a.* 
				FROM swm.t_apptmteam_day a
				INNER JOIN swm.v_orginf o 
					ON (o.level1cd=a.cntr_cd AND o.level2cd=a.grp_cd AND o.level3cd=a.team_cd AND syskind='TM' AND o.divcd IN ('5'))
				WHERE  a.dt >= P_StdDTFrom and a.dt <= P_StdDTTo
			) b ON (a.dt=b.dt and a.cntr_cd=b.cntr_cd and a.grp_cd=b.grp_cd and a.team_cd=b.team_cd)
		) src
		ON (src.dt = trg.dt AND src.cntr_cd = trg.cntr_cd AND src.grp_cd = trg.grp_cd 
		AND src.team_cd = trg.team_cd)
		WHEN MATCHED THEN
			UPDATE SET
				trg.subs_cnt    		= src.subs_cnt,
				trg.subs_amt    		= src.subs_amt,
				trg.cont_cnt    		= src.cont_cnt,
				trg.cont_amt    		= src.cont_amt,
				trg.etco_cnt    		= src.etco_cnt,
				trg.call_prs_cnt    	= src.call_prs_cnt,
				trg.tmr_cnt    			= src.tmr_cnt,
				trg.call_cnt    		= src.call_cnt,
				trg.cur_cnt    			= src.cur_cnt,
				trg.cust_rsps01    		= src.cust_rsps01,
				trg.exec_cnt    		= src.exec_cnt,
				trg.lead_time    		= src.lead_time,
				trg.lastmodifydate 		= SYSTIMESTAMP
		WHEN NOT MATCHED THEN
			INSERT (
				dt,
				cntr_cd,
				grp_cd,
				team_cd,
				subs_cnt,
				subs_amt,
				cont_cnt,
				cont_amt,
				etco_cnt,
				call_prs_cnt,
				tmr_cnt,
				call_cnt,
				cur_cnt,
				cust_rsps01,
				exec_cnt,
				lead_time,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.dt,
				src.cntr_cd,
				src.grp_cd,
				src.team_cd,
				src.subs_cnt,
				src.subs_amt,
				src.cont_cnt,
				src.cont_amt,
				src.etco_cnt,
				src.call_prs_cnt,
				src.tmr_cnt,
				src.call_cnt,
				src.cur_cnt,
				src.cust_rsps01,
				src.exec_cnt,
				src.lead_time,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			);
	END;
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.p_set_sum_apptmteam_month(
	P_StdDTFrom			IN VARCHAR2,	-- 시작일자
	P_StdDTTo			IN VARCHAR2		-- 종료일자
) 
IS
	v_StdDTFrom_dash		VARCHAR2(7);
	v_StdDTTo_dash		VARCHAR2(7);
	v_StdDTFrom			VARCHAR2(10);
	v_StdDTTo			VARCHAR2(10);
BEGIN
	v_StdDTFrom_dash := TO_CHAR(TO_TIMESTAMP(P_StdDTFrom, 'YYYY-MM-DD'), 'YYYY-MM');
	v_StdDTTo_dash  := TO_CHAR(TO_TIMESTAMP(P_StdDTTo, 'YYYY-MM-DD'), 'YYYY-MM');
	v_StdDTFrom := TO_CHAR(TO_TIMESTAMP(P_StdDTFrom, 'YYYY-MM-DD'), 'YYYYMM') || '01';
	v_StdDTTo := TO_CHAR(TO_TIMESTAMP(P_StdDTTo, 'YYYY-MM-DD'), 'YYYYMM') || '31';
	BEGIN
		MERGE INTO swm.t_apptmteam_month_mix trg
		USING(
			SELECT
				NVL(a.ym, b.ym) 			AS ym,
				NVL(a.cntr_cd, b.cntr_cd) 	AS cntr_cd,
				NVL(a.grp_cd, b.grp_cd) 	AS grp_cd,
				NVL(a.team_cd, b.team_cd) 	AS team_cd,
				NVL(b.etco_cnt, 0) 			AS etco_cnt,
				NVL(b.call_prs_cnt, 0) 		AS call_prs_cnt,
				NVL(a.subs_cnt, 0) 			AS subs_cnt,
				NVL(a.tot_mtpm_insufee ,0) 	AS tot_mtpm_insufee,
				NVL(a.guarant_mtpm_insufee ,0) AS guarant_mtpm_insufee
			FROM (
				SELECT 
					SUBSTR(psdt,1,4)||'-'||SUBSTR(psdt,5,2) AS ym,	
					u.level1cd 					AS cntr_cd, 
					u.level2cd 					AS grp_cd, 
					u.level3cd 					AS team_cd, 
					COUNT(DISTINCT clppsnno) 	AS subs_cnt,
					SUM(TO_NUMBER(tprm)) 		AS tot_mtpm_insufee,
					SUM(CASE WHEN a.prodlccd='1' THEN TO_NUMBER(tprm) ELSE 0 END) AS guarant_mtpm_insufee				
				FROM swm.t_eaitmbizdata a
				INNER JOIN swm.t_cdinf h ON (clctbrofbrcd=h.smlcd AND h.lrgcd='BC_N092')
				LEFT OUTER JOIN swm.t_cdinf c ON (a.cntrdtlscd=c.smlcd AND c.lrgcd='BC_CNTR')	
				LEFT OUTER JOIN swm.t_user u ON (a.clppsnno=u.userid)
				WHERE 1=1
				AND psdt>=v_StdDTFrom AND psdt<=v_StdDTTo
				AND c.dscnm IN ('1','2')
				AND a.rvcycd <> '00'
				GROUP BY SUBSTR(psdt,1,4)||'-'||SUBSTR(psdt,5,2), u.Level1Cd, u.Level2Cd, u.Level3Cd
			) a
			FULL OUTER JOIN (
				SELECT a.* 
				FROM swm.t_apptmteam_month a 
				INNER JOIN swm.v_orginf o 
					ON (o.level1cd=a.cntr_cd AND o.level2cd=a.grp_cd AND o.level3cd=a.team_cd AND syskind='TM' AND o.divcd IN ('5'))
				WHERE	a.ym >= v_StdDTFrom_dash AND a.ym <= v_StdDTTo_dash
			) b ON (b.ym >= v_StdDTFrom_dash AND b.ym <= v_StdDTTo_dash AND a.ym=b.ym AND a.cntr_cd=b.cntr_cd AND a.grp_cd=b.grp_cd AND a.team_cd=b.team_cd)
		) src
		ON (src.ym = trg.ym AND src.cntr_cd = trg.cntr_cd AND src.grp_cd = trg.grp_cd 
		AND src.team_cd = trg.team_cd)
		WHEN MATCHED THEN
			UPDATE SET
				trg.etco_cnt    			= src.etco_cnt,
				trg.call_prs_cnt    		= src.call_prs_cnt,
				trg.subs_cnt    			= src.subs_cnt,
				trg.tot_mtpm_insufee    	= src.tot_mtpm_insufee,
				trg.guarant_mtpm_insufee 	= src.guarant_mtpm_insufee,
				trg.lastmodifydate 			= SYSTIMESTAMP
		WHEN NOT MATCHED THEN
			INSERT (
				ym,
				cntr_cd,
				grp_cd,
				team_cd,
				etco_cnt,
				call_prs_cnt,
				subs_cnt,
				tot_mtpm_insufee,
				guarant_mtpm_insufee,
				createdate,
				lastmodifydate
			)
			VALUES (
				src.ym,
				src.cntr_cd,
				src.grp_cd,
				src.team_cd,
				src.etco_cnt,
				src.call_prs_cnt,
				src.subs_cnt,
				src.tot_mtpm_insufee,
				src.guarant_mtpm_insufee,
				SYSTIMESTAMP,
				SYSTIMESTAMP
			);
	END;
	COMMIT;
END;

CREATE OR REPLACE PROCEDURE SWM.p_tm_allchanneltop10(
	P_outCursor 		OUT SYS_REFCURSOR
)
IS

BEGIN
   OPEN P_outCursor FOR	
   SELECT *
	 FROM (
		SELECT 
				ROW_NUMBER() OVER(PARTITION BY t2.DivCd ORDER BY t1.amt DESC) ranking,
				t2.DivCd as chcd,
				t2.DivNm as chnm,
				t2.Level1Nm as lv1nm,
				t2.Level2Nm as lv2nm,
				t2.Level3Nm as lv3nm,
				CASE WHEN t1.lv1cd = 'TKL' OR t1.lv1cd = 'TKJ' OR t1.lv1cd = 'TKW' THEN t2.Level3Nm ELSE t2.Level1Nm || '' || t2.Level3Nm END teamnm,
				t1.userid,
				t3.Name as usernm,
				NVL(t1.cnt, 0) as cnt,
				NVL(t1.amt, 0) as amt,
--				NVL(STUFF(ROUND(t1.amt, -3, 0), LENGTH(t1.amt) -2, LENGTH(t1.amt), ''), 0) totAmt,
				 substr(round(t1.amt, -3) , 1, length(t1.amt)-3) totAmt,
				NVL(t1.cpc, 0) as cpc
			FROM (
					SELECT 
							t1.DT,
							t1.CNTR_CD lv1cd,
							t1.GRP_CD lv2cd,
							t1.TEAM_CD lv3cd,
							t1.USR_ID as userid,
							SUM(t1.TOT_CNT) as cnt, 
							SUM(t1.TOT_AMT) as amt,
							NVL(SUM(t1.EXCHNG_GRD), 0) as cpc
						FROM SWM.t_appTMAgent_Day_MIX t1 
						WHERE t1.DT = to_char(sysdate, 'YYYY-MM-DD')  
						AND t1.CONT_ST_NM = '1'-- 2016-12-15 청약 조건 추가
						GROUP BY t1.DT, t1.CNTR_CD, t1.GRP_CD, t1.TEAM_CD, t1.USR_ID
			) t1
			INNER JOIN SWM.v_OrgInf t2
				ON (t2.SysKind = 'TM' 
					AND t1.lv1cd = t2.Level1Cd 
					AND t1.lv2cd = t2.Level2Cd 
					AND t1.lv3cd = t2.Level3Cd)
			INNER JOIN SWM.t_User t3
					ON (t3.Deleted = 'N' 
						AND t1.userid = t3.UserID 
						AND t2.Level1Cd = t2.Level1Cd 
						AND t2.Level2Cd = t3.Level2Cd
						AND t2.Level3Cd = t3.Level3Cd) 
	) a
	WHERE ranking <= 10;
	
END;

CREATE OR REPLACE PROCEDURE SWM.p_tm_timegroup(
	P_outCursor 		OUT SYS_REFCURSOR
)
IS
BEGIN

	OPEN P_outCursor FOR
	
		/*지점별 시간대별 업적 현황*/
		SELECT  a.hh,
		        a.lv1cd,
				a.lv1nm,
		        SUM(a.nTotCnt) as nTotCnt,
				SUM(a.nTotAmt) as nTotAmt,
				SUM(a.eTotCnt) as eTotCnt,
				SUM(a.eTotAmt) as eTotAmt
		   FROM (
				 SELECT 
						CASE WHEN t1.TM >= '01' AND t1.TM <= '09' THEN '09'
							 WHEN t1.TM >= '19' AND t1.TM <= '23' THEN '18'
						ELSE t1.TM END AS hh,
						t1.CNTR_CD as lv1cd,
						t2.Level1Nm as lv1nm,
						NVL(SUM(CASE WHEN t1.CONT_DV_NM = 1 THEN t1.CNT ELSE 0 END), 0) nTotCnt,		-- 청약건수
						NVL(SUM(CASE WHEN t1.CONT_DV_NM = 1 THEN t1.INSUFEE ELSE 0 END), 0) nTotAmt,	-- 청약금액
						NVL(SUM(CASE WHEN t1.CONT_DV_NM = 2 THEN t1.CNT ELSE 0 END), 0) eTotCnt,		-- 계약건수
						NVL(SUM(CASE WHEN t1.CONT_DV_NM = 2 THEN t1.INSUFEE ELSE 0 END), 0) eTotAmt	-- 계약금액
				 FROM SWM.t_appTMAgent_Time_MIX t1
				 INNER JOIN swm.v_OrgInf t2
					   ON (t1.CNTR_CD = t2.Level1Cd AND t1.GRP_CD = t2.Level2Cd AND t1.TEAM_CD = t2.Level3Cd)
				 WHERE t1.DT =  to_char(sysdate, 'YYYY-MM-DD')
				 AND t2.DivCd IS NOT NULL
				 GROUP BY t1.DT, t1.TM, t1.CNTR_CD, t2.Level1Nm
			) a
			GROUP BY a.hh, a.lv1cd, a.lv1nm
			HAVING a.hh IS NOT NULL
			ORDER BY a.hh ASC;

END;

CREATE OR REPLACE PROCEDURE SWM.p_tm_top10group(
	P_OrgCd				IN VARCHAR2,
	P_outCursor 		OUT SYS_REFCURSOR
)
IS
BEGIN

	OPEN P_outCursor FOR

	  SELECT * FROM (
		SELECT *
		FROM (
          SELECT lv1cd, lv2cd, lv3cd,userid, lv1nm, lv2nm, lv3nm, teamNm,usernm, cnt, amt, cpc
          FROM (
 				SELECT -- TOP 10
					  t1.CNTR_CD lv1cd,
					  t1.GRP_CD lv2cd,
					  t1.TEAM_CD lv3cd,
					  t3.UserID userid,
					  t2.Level1Nm as lv1nm,
					  t2.Level2Nm as lv2nm,
					  t2.Level1Nm || ' ' || t2.Level3Nm as lv3nm,
					  t2.Level3Nm as teamNm,
					  MAX(t3.Name) usernm,
					  NVL(SUM(t1.TOT_CNT), 0) as cnt,
					  NVL(SUM(t1.TOT_AMT), 0) as amt,
					  NVL(SUM(t1.EXCHNG_GRD), 0) as cpc
				 FROM swm.t_appTMAgent_Day_MIX t1
				 LEFT OUTER JOIN SWM.v_OrgInf t2
					  ON (t2.SysKind = 'TM' 
						AND t1.CNTR_CD = t2.Level1Cd 
						AND t1.GRP_CD = t2.Level2Cd 
						AND t1.TEAM_CD = t2.Level3Cd)
				 LEFT OUTER JOIN SWM.t_User t3
					  ON (t3.Deleted = 'N' 
						AND t1.USR_ID = t3.UserID 
						AND t2.Level1Cd = t3.Level1Cd 
						AND t2.Level2Cd = t3.Level2Cd
						AND t2.Level3Cd = t3.Level3Cd)
				WHERE t1.DT =  to_char(sysdate,'YYYY-MM-DD') -- CONVERT(VARCHAR(10), GETDATE(), 121)
				  AND t3.UserID IS NOT NULL 
				  AND t1.CNTR_CD = P_OrgCd
				  AND t1.CONT_ST_NM = '3' --정산기준 3, 청약기준 1
				GROUP BY t1.CNTR_CD, 
						 t1.GRP_CD, 
						 t1.TEAM_CD, 
						 t2.Level1Nm, 
						 t2.Level2Nm, 
						 t2.Level3Nm, 
						 t3.UserID
				ORDER BY amt DESC
			) WHERE ROWNUM < 11

		union all
          SELECT lv1cd, lv2cd, lv3cd,userid, lv1nm, lv2nm, lv3nm, teamNm,usernm, cnt, amt, cpc
          FROM (
			SELECT -- TOP 10
			          t1.Level1Cd lv1cd,
					  t1.Level2Cd lv2cd,
					  t1.Level3Cd lv3cd,
					  t1.UserID userid,
					  t2.Level1Nm as lv1nm,
					  t2.Level2Nm as lv2nm,
					  t2.Level1Nm || ' ' || t2.Level3Nm as lv3nm,
					  t2.Level3Nm as teamNm,
					  MAX(t1.Name) usernm,
					  0 as cnt,
					  0 as amt,
					  0 as cpc
				 FROM SWM.t_User t1
				 LEFT OUTER JOIN SWM.v_OrgInf t2
					  ON (t2.SysKind = 'TM' 
						AND t1.Level1Cd = t2.Level1Cd 
						AND t1.Level2Cd = t2.Level2Cd 
						AND t1.Level3Cd = t2.Level3Cd)				 
				WHERE t1.UserID NOT IN (select t3.USR_ID from SWM.t_appTMAgent_Day_MIX t3 where t3.DT = to_date(sysdate,'yyyy-mm-dd'))
					AND t1.Deleted = 'N'					
					AND t1.Level1Cd = P_OrgCd
					AND t1.Level2Cd != 'TKH9'
				GROUP BY t1.Level1Cd, 
						 t1.Level2Cd, 
						 t1.Level3Cd, 
						 t2.Level1Nm, 
						 t2.Level2Nm, 
						 t2.Level3Nm, 
						 t1.UserID
				order by t1.UserID
               ) WHERE ROWNUM  < 11

--ORDER BY amt desc
		) a
		ORDER BY a.amt DESC
	)  WHERE ROWNUM  < 11 ;
		

	
END;

CREATE OR REPLACE PROCEDURE SWM.p_tm_top10group_agentcalltime(
	P_checkDT			IN VARCHAR2, --당일조회, 월간조회 구분
	P_arrAgentId1		IN VARCHAR2, --상담사ID
	P_arrAgentId2		IN VARCHAR2, --상담사ID
	P_arrAgentId3		IN VARCHAR2, --상담사ID
	P_arrAgentId4		IN VARCHAR2, --상담사ID
	P_arrAgentId5		IN VARCHAR2, --상담사ID
	P_arrAgentId6		IN VARCHAR2, --상담사ID
	P_arrAgentId7		IN VARCHAR2, --상담사ID
	P_arrAgentId8		IN VARCHAR2, --상담사ID
	P_arrAgentId9		IN VARCHAR2, --상담사ID
	P_arrAgentId10		IN VARCHAR2, --상담사ID
	P_outCursor 		OUT SYS_REFCURSOR
)
IS
BEGIN
	
	IF P_checkDT = '1' THEN
		OPEN P_outCursor FOR
	 -- 일간 상담사별 콜타임
		select NVL(SWM.f_Sec_Datetime(NVL(va1.ACWOUTTIME,0) + NVL(va1.AUXOUTTIME,0)), 0) as ClsSec, tu1.UserID as UserId
		from SWM.CAGENT va1, SWM.t_User tu1
		where va1.LOGID = tu1.UserID
			and tu1.UserID in(P_arrAgentId1, P_arrAgentId2, P_arrAgentId3, P_arrAgentId4, P_arrAgentId5, P_arrAgentId6, P_arrAgentId7, P_arrAgentId8, P_arrAgentId9, P_arrAgentId10);
     ELSE -- IF checkDT = '2'
		OPEN P_outCursor FOR
	 -- 월간 상담사별 콜타임
		select NVL(SWM.f_Sec_Datetime(SUM(ClsSec)), 0) as ClsSec, UserId
		from SWM.t_appCounsel
		where DT >= to_char(sysdate, 'YYYY-MM' ) || '-01' AND DT <= to_char(sysdate, 'YYYY-MM-DD' )
          and UserID in(P_arrAgentId1, P_arrAgentId2, P_arrAgentId3, P_arrAgentId4, P_arrAgentId5, P_arrAgentId6, P_arrAgentId7, P_arrAgentId8, P_arrAgentId9, P_arrAgentId10)
		group by UserId;
    END IF;
END;

CREATE OR REPLACE PROCEDURE SWM.p_tm_top10group_month_std(
	P_OrgCd				IN VARCHAR2,
	P_outCursor 		OUT SYS_REFCURSOR
)
IS
BEGIN

	OPEN P_outCursor FOR

SELECT *
  FROM (
   SELECT *
     FROM (
     SELECT  *	FROM	
		 (
			SELECT
				  t1.Level1Cd lv1cd,
				  t1.Level2Cd lv2cd,
				  t1.Level3Cd lv3cd,
				  t1.UserID userid,
				  t2.Level1Nm as lv1nm,
				  t2.Level2Nm as lv2nm,
				  t2.Level1Nm || ' ' || t2.Level3Nm as lv3nm,
				  t2.Level3Nm as teamNm,
				  t1.Name usernm,
				  0 as cnt,
				  0 as amt, 
				  NVL(SWM.F_SEC_DATETIME(ta1.CALL_TIME), 0) as ClsSec
			 
			 FROM SWM.t_User t1
			 LEFT OUTER JOIN SWM.v_OrgInf t2
				  ON (t2.SysKind = 'TM' 
					AND t1.Level1Cd = t2.Level1Cd 
					AND t1.Level2Cd = t2.Level2Cd 
					AND t1.Level3Cd = t2.Level3Cd)
   			 LEFT OUTER JOIN SWM.t_appTMAgent_Month ta1
				  ON (t1.UserID = ta1.USR_ID) 													
			WHERE t1.Deleted = 'N' 
			  AND t1.Level1Cd = P_OrgCd			  
			  AND ta1.STD_CNT = '0'
			  AND ta1.DT = (select MAX(DT) from SWM.t_appTMAgent_Month)
			order by ClsSec desc
            )  WHERE ROWNUM < 11
		union all
         SELECT * FROM (
			SELECT 
				  t1.CNTR_CD lv1cd,
				  t1.GRP_CD lv2cd,
				  t1.TEAM_CD lv3cd,
				  t3.UserID userid,
				  t2.Level1Nm as lv1nm,
				  t2.Level2Nm as lv2nm,
				  t2.Level1Nm || ' ' || t2.Level3Nm as lv3nm,
				  t2.Level3Nm as teamNm,
				  t3.Name usernm,
				  NVL(t1.STD_CNT, 0) as cnt,
				  NVL(t1.STD_AMT, 0) as amt,
				  NVL(SWM.f_Sec_Datetime(t1.CALL_TIME), 0) as ClsSec
			 FROM SWM.t_appTMAgent_Month t1 
			 LEFT OUTER JOIN SWM.v_OrgInf t2
				  ON (t2.SysKind = 'TM' 
					AND t1.CNTR_CD = t2.Level1Cd 
					AND t1.GRP_CD = t2.Level2Cd 
					AND t1.TEAM_CD = t2.Level3Cd)
			 LEFT OUTER JOIN SWM.t_User t3
				  ON (t3.Deleted = 'N' 
					AND t1.USR_ID = t3.UserID 
					AND t2.Level1Cd = t2.Level1Cd 
					AND t2.Level2Cd = t3.Level2Cd
					AND t2.Level3Cd = t3.Level3Cd)
			WHERE t1.DT = (select MAX(DT) from SWM.t_appTMAgent_Month)
			  AND t3.UserID IS NOT NULL
			  AND t1.CNTR_CD = P_OrgCd			  			  
			  AND t1.STD_CNT != '0'			 
			ORDER BY amt DESC
		  ) WHERE ROWNUM < 11
		) a
		ORDER BY a.amt desc, ClsSec desc, usernm ASC
     )  WHERE ROWNUM < 11;
    
END;

CREATE OR REPLACE PROCEDURE SWM.SP_15MIN_INIT
(
    I_ROW_DATE		IN	VARCHAR2,
    O_SQL_ECODE		OUT	INT,
    O_SQL_EMSG		OUT	VARCHAR2
)
IS

--TRUNCATE ��
V_TRUNC_TABLE_SQL VARCHAR2(3000);
--UTC TIME���� ����
UTC_ROW_DATE NUMBER;
--�� ���� �̷� ����
AG_ORG_CNT NUMBER;

BEGIN
	--ver.0.1
    O_SQL_ECODE := 0; --�ʱⰪ�� 0����..
    O_SQL_EMSG 	:= 'PROC SP_15MIN_INIT ONGOING...'; --�ʱⰪ�� ���� ó���Ȱɷ�..

	BEGIN
		--�����͸� �����ؾ� �ϴ°�� AX_STAT_HIST �����͸� �߰� �Ѵ�.        
		--��ġ ������ �⺻ �����͸� �����Ѵ�.(������۳�¥, ���豸��, ����Ÿ�ٳ�¥, Ÿ�����̺�/������, Ÿ�����ν�����, ���, ���MSG, ���ᳯ¥)
        INSERT INTO  SWM.AX_STAT_HIST
        (STARTTIME, STATGUBUN, TARGETTIME, TARGETTABLE, EXPROC, EXP, EXPMSG, ENDTIME)
        VALUES	(
					SYSDATE,					--STARTTIME
					'10(15MIN)',				--STATGUBUN
					SUBSTR(I_ROW_DATE,1,12),	--TARGETTIME
					'INIT(15MIN)',				--TARGETTABLE
					'SP_15MIN_INIT',			--EXPROC
					'ONGOING',					--EXP
					NULL,						--EXPMSG
					NULL						--ENDTIME
				);

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] AX_STAT_HIST INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--������ �߻��Ͽ� �ѹ�ó��

    END;


	--��ȯ���� ������ ��´� 20241202000000 -> 1711897200 �������� ��ȯ
	BEGIN
		SELECT K2U(I_ROW_DATE) INTO UTC_ROW_DATE FROM DUAL;
	END;


    --CALLSTAT_TEMP TRUNCATE/INSERT
	BEGIN
		V_TRUNC_TABLE_SQL := 'TRUNCATE TABLE CALLSTAT_TEMP';

		EXECUTE IMMEDIATE V_TRUNC_TABLE_SQL;

		INSERT
		INTO 	SWB.CALLSTAT_TEMP 
				SELECT	*
				FROM	SWB.CALLSTAT
				WHERE	ETIMETS >= UTC_ROW_DATE AND ETIMETS < UTC_ROW_DATE + 900;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] CALLSTAT_TEMP TRUNCATE/INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--������ �߻��Ͽ� �ѹ�ó��
    END;


    --AGENTSTATUS_TEMP TRUNCATE/INSERT
	BEGIN
		V_TRUNC_TABLE_SQL := 'TRUNCATE TABLE AGENTSTATUS_TEMP';

		EXECUTE IMMEDIATE V_TRUNC_TABLE_SQL;

		INSERT
		INTO 	SWB.AGENTSTATUS_TEMP 
				SELECT	*
				FROM	SWB.AGENTSTATUS
				WHERE	TTIMETS >= UTC_ROW_DATE AND TTIMETS < UTC_ROW_DATE + 900;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] AGENTSTATUS_TEMP TRUNCATE/INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--������ �߻��Ͽ� �ѹ�ó��
    END;


    --EXTENSION_TEMP TRUNCATE/INSERT
	BEGIN
		V_TRUNC_TABLE_SQL := 'TRUNCATE TABLE EXTENSION_TEMP';

		EXECUTE IMMEDIATE V_TRUNC_TABLE_SQL;

		INSERT 
		INTO 	SWB.EXTENSION_TEMP
		SELECT	PERSON, MAX(EXTENSION) AS EXTENSION
		FROM	(
					SELECT 	PERSON, MAX(THISDN) EXTENSION 
					FROM 	SWB.CALLSTAT_TEMP
					WHERE	AGENTID IS NOT NULL
					GROUP BY PERSON
					UNION ALL
					SELECT 	PERSON, MAX(THISDN) EXTENSION 
					FROM 	SWB.AGENTSTATUS_TEMP 
					GROUP BY PERSON
				)
		GROUP BY PERSON;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] EXTENSION_TEMP TRUNCATE/INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--������ �߻��Ͽ� �ѹ�ó��
    END;


    --AGENTLOGIN_IN_TEMP TRUNCATE/INSERT
	BEGIN
		V_TRUNC_TABLE_SQL := 'TRUNCATE TABLE AGENTLOGIN_IN_TEMP';

		EXECUTE IMMEDIATE V_TRUNC_TABLE_SQL;

		INSERT
		INTO 	SWB.AGENTLOGIN_IN_TEMP
				SELECT 	*
				FROM 	SWB.AGENTLOGIN
				WHERE 	LOGINTIMETS >= UTC_ROW_DATE AND LOGINTIMETS < UTC_ROW_DATE + 900;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] AGENTLOGIN_IN_TEMP TRUNCATE/INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--������ �߻��Ͽ� �ѹ�ó��
    END;


	--AGENTLOGIN_OUT_TEMP TRUNCATE/INSERT
	BEGIN
		V_TRUNC_TABLE_SQL := 'TRUNCATE TABLE AGENTLOGIN_OUT_TEMP';

		EXECUTE IMMEDIATE V_TRUNC_TABLE_SQL;

		INSERT
		INTO 	SWB.AGENTLOGIN_OUT_TEMP
				SELECT 	*
				FROM 	SWB.AGENTLOGIN
				WHERE 	LOGOUTTIMETS >= UTC_ROW_DATE AND LOGOUTTIMETS < UTC_ROW_DATE + 900;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] AGENTLOGIN_OUT_TEMP TRUNCATE/INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--������ �߻��Ͽ� �ѹ�ó��
    END;

	COMMIT;


    --QUEUESTATE_TEMP TRUNCATE/INSERT
	BEGIN
		V_TRUNC_TABLE_SQL := 'TRUNCATE TABLE QUEUESTATE_TEMP';

		EXECUTE IMMEDIATE V_TRUNC_TABLE_SQL;

		INSERT
		INTO 	SWB.QUEUESTATE_TEMP
				SELECT 	*
				FROM 	SWB.QUEUESTATE
				WHERE 	TIME  = UTC_ROW_DATE
				AND 	DNTYPE IN (1,5,6,7);

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] QUEUESTATE_TEMP TRUNCATE/INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--������ �߻��Ͽ� �ѹ�ó��
    END;


    --USERDATA_TEMP TRUNCATE/INSERT
	BEGIN
		V_TRUNC_TABLE_SQL := 'TRUNCATE TABLE USERDATA_TEMP';

		EXECUTE IMMEDIATE V_TRUNC_TABLE_SQL;

		INSERT
		INTO 	SWB.USERDATA_TEMP
				SELECT 	*
				FROM 	SWB.USERDATA
				WHERE	ETIMETS >= UTC_ROW_DATE AND ETIMETS < UTC_ROW_DATE + 900;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] USERDATA_TEMP TRUNCATE/INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--������ �߻��Ͽ� �ѹ�ó��
    END;


--SP_5MIN_INIT AX_SK_ORG -> IVR_DG_MAP ���� ���̺� ���� -> T_IVR_DG_MAP ���� ����

    --AGENT_INFO TRUNCATE/INSERT
	BEGIN
		V_TRUNC_TABLE_SQL := 'TRUNCATE TABLE AGENT_INFO';

		EXECUTE IMMEDIATE V_TRUNC_TABLE_SQL;

		INSERT 
		INTO 	SWB.AGENT_INFO 
				SELECT	AGT_ST.PERSON  			AS AGENT_DBID, 
						PS.EMPLOYEE_ID 			AS EMPLOYEE_ID,
						CAL.LOGIN_CODE			AS LOGIN_ID,
						CAG.GROUP_DBID			AS ORG_DBID,
						ORG_GROUP.DG_DBID 		AS AGROUP_DBID,
						ORG_GROUP.MENU_CODE		AS AGROUP_NAME,  
						ROW_NUMBER() OVER (PARTITION BY PERSON ORDER BY LEVEL_, ORG_CHANNEL.SORT, ORG_GROUP.SORT) AS AGROUP_SEQ,
						AGT_ST.LEVEL_ 			AS LEVEL_,
						AGT_ST.SKILL 			AS SKILL_DBID,
						ORG_GROUP.SK_KNAME      AS SKILL_NAME,  		-- 확인필요
						ORG_CHANNEL.DG_DBID     AS ORG_CHANNEL_DBID,  	-- 확인필요
						ORG_CHANNEL.NAME    	AS ORG_CHANNEL_NAME,  	-- 확인필요
						CAG.LEVEL1CD			AS ORG_LEVEL1,
						CAG.LEVEL2CD			AS ORG_LEVEL2,
						CAG.LEVEL3CD			AS ORG_LEVEL3,
						AGT_ST.VQ_NUM			AS VQ_NUM
						
				FROM  
				(	
					--로그인한 상담사와 호를 받은 상담사의 목록을 조회한다
					SELECT 	DISTINCT PERSON, 99 AS SKILL , 99 AS LEVEL_, 'VQ99' AS VQ_NUM
					FROM	SWB.AGENTSTATUS_TEMP
					WHERE 	PERSON > 0 AND SKILL = -1 
					UNION ALL
					SELECT 	DISTINCT PERSON, SKILL AS SKILL , LEVEL_ AS LEVEL_, 'VQ99' AS VQ_NUM
					FROM	SWB.AGENTSTATUS_TEMP, CC.CFG_SKILL_LEVEL
					WHERE 	PERSON > 0 AND SKILL > 0 
					AND 	AGENTSTATUS_TEMP.PERSON = CFG_SKILL_LEVEL.PERSON_DBID
					AND 	AGENTSTATUS_TEMP.SKILL = CFG_SKILL_LEVEL.SKILL_DBID
					AND		AGENTSTATUS_TEMP.SEVENT != 73 --로그인시 보유스킬로 REGI하여 제외해야함
					AND 	AGENTSTATUS_TEMP.TEVENT NOT IN (74, 152)
					UNION ALL
					SELECT 	DISTINCT PERSON, SKILL AS SKILL , LEVEL_ AS LEVEL_, QUEUEV AS VQ_NUM
					FROM 	SWB.CALLSTAT_TEMP, CC.CFG_SKILL_LEVEL
					WHERE 	PERSON > 0 AND SKILL > 0
					AND 	CALLSTAT_TEMP.PERSON = CFG_SKILL_LEVEL.PERSON_DBID
					AND 	CALLSTAT_TEMP.SKILL = CFG_SKILL_LEVEL.SKILL_DBID				
				) AGT_ST,
				(
					SELECT 	*  
					FROM 	SWM.T_IVR_DG_MAP  
					WHERE 	DG_DBID IS NOT NULL
					AND 	MAP_LEVELS = 2 --�����׷�
					AND		DELETED_AT IS NULL
				) ORG_GROUP, 
				(
					SELECT 	*  
					FROM 	SWM.T_IVR_DG_MAP 
					WHERE 	MAP_LEVELS = 1 --ä�� 
					AND		DELETED_AT IS NULL
				) ORG_CHANNEL, CC.CFG_PERSON PS, (	
													SELECT 	MAX(TU.ORGCD) GROUP_DBID, TU.LEVEL1CD, TU.LEVEL2CD, TU.LEVEL3CD, CP.DBID AGENT_DBID 
													FROM 	SWM.T_USER TU, CC.CFG_PERSON CP 
													WHERE 	TU.PERIPHERALNUMBER = CP.EMPLOYEE_ID 
													AND 	TU.CTICREATECD = 'Y'  
													--AND 	SYSKIND = 'CS'
													GROUP BY TU.LEVEL1CD, TU.LEVEL2CD, TU.LEVEL3CD, CP.DBID
												) CAG, CC.CFG_LOGIN_INFO CLI, CC.CFG_AGENT_LOGIN CAL
				WHERE	AGT_ST.PERSON = PS.DBID 
				AND		PS.DBID = CAG.AGENT_DBID
				AND		PS.DBID = CLI.PERSON_DBID
				AND 	CLI.AGENT_LOGIN_DBID = CAL.DBID
				--AND 	ORG_GROUP.SK_DBID = AGT_ST.SKILL
				AND 	ORG_GROUP.SK_DBID = AGT_ST.SKILL
				AND 	ORG_CHANNEL.ID = ORG_GROUP.PARENT_ID;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] AGENT_INFO TRUNCATE/INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--������ �߻��Ͽ� �ѹ�ó��
    END;


    --VDN_ORG_SKILL TRUNCATE/INSERT
	BEGIN
		V_TRUNC_TABLE_SQL := 'TRUNCATE TABLE VDN_ORG_SKILL';

		EXECUTE IMMEDIATE V_TRUNC_TABLE_SQL;

		INSERT 
		INTO 	SWB.VDN_ORG_SKILL 
				SELECT	DNG.VDN_DBID 			AS VDN_DBID,
						DNG.VDN_NAME 			AS VDN_NAME,
						DNG.VDN_NUMBER 			AS VDN_NUMBER, 
						DNG.VDN_TYPE 			AS VDN_TYPE, 
						ORG_GROUP.DG_DBID	 	AS AGROUP_DBID, 
						ORG_GROUP.MENU_CODE		AS GROUP_NAME, 
						SK.DBID 				AS SKILL_DBID,
						ORG_GROUP.SK_KNAME      AS SKILL_NAME,  		-- 확인필요
						ORG_CHANNEL.DG_DBID		AS ORG_CHANNEL_DBID ,  	-- 확인필요
						ORG_CHANNEL.NAME       	AS ORG_CHANNEL_NAME  	-- 확인필요						
				FROM   	CC.CFG_SKILL SK, 
				(
					SELECT	CG.DBID GROUP_DBID, CG.NAME GROUP_NAME, CD.DBID VDN_DBID, CD.NAME VDN_NAME, CD.NUMBER_ VDN_NUMBER, CD.TYPE VDN_TYPE 
					FROM	CC.CFG_DN_GROUP CDG, CC.CFG_GROUP CG, CC.CFG_DN CD
					WHERE	CDG.GROUP_DBID = CG.DBID 
					AND     CDG.DN_DBID = CD.DBID  
					AND     CG.GROUP_TYPE = 3
				) DNG,
				(
					SELECT 	*  
					FROM 	SWM.T_IVR_DG_MAP  
					WHERE 	DG_DBID IS NOT NULL
					AND 	MAP_LEVELS = 2 --����2
					AND		DELETED_AT IS NULL
				) ORG_GROUP, 
				(
					SELECT 	*  
					FROM 	SWM.T_IVR_DG_MAP 
					WHERE 	MAP_LEVELS = 1 --����1
					AND		DELETED_AT IS NULL
				) ORG_CHANNEL 
				WHERE	ORG_GROUP.DG_DBID = DNG.GROUP_DBID 
				AND 	ORG_CHANNEL.ID = ORG_GROUP.PARENT_ID 
				AND 	ORG_GROUP.SK_DBID = SK.DBID(+); 

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] VDN_ORG_SKILL TRUNCATE/INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--������ �߻��Ͽ� �ѹ�ó��
    END;


	--AG_ORG_CNT INSERT
	BEGIN
		--�̷��� �ױ����� ���� �ִ��� Ȯ���Ѵ� (ORG����)
		SELECT 	COUNT(*)
		INTO 	AG_ORG_CNT
		FROM	SWM.AG_ORG_HIST
		WHERE	ROW_DATE  = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
		AND		STARTTIME = '2345';


		--�ڵ�����ÿ� �ѹ� INSERT�ϰ� �� �� ���� ����ÿ��� INSERT���� �ʴ´� ORG���� �� ������ �����ϱ� ���� ����
		--�� ������ INSERT
		IF	AG_ORG_CNT  =  0   THEN
			IF	SUBSTR(I_ROW_DATE,9,4)  =  '2345'  THEN
				INSERT INTO SWM.AG_ORG_HIST
				SELECT 	SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2) AS ROW_DATE,
						SUBSTR(I_ROW_DATE,9,4) 	AS STARTTIME,
						ORG_DBID 				AS ORG_DBID,
						ORG_LEVEL1				AS ORG_LEVEL1,
						ORG_LEVEL2				AS ORG_LEVEL2,
						ORG_LEVEL3				AS ORG_LEVEL3,
						AGENT_DBID 				AS AGENT_DBID,
						EMPLOYEE_ID 			AS EMP_ID,
						LOGIN_ID				AS LOGIN_ID
				FROM	(
							SELECT 	DISTINCT TU.ORGCD AS ORG_DBID, TU.LEVEL1CD AS ORG_LEVEL1, TU.LEVEL2CD AS ORG_LEVEL2, TU.LEVEL3CD AS ORG_LEVEL3, 
											 CP.DBID AS AGENT_DBID, CP.EMPLOYEE_ID AS EMPLOYEE_ID, TU.USERID AS LOGIN_ID
							FROM 	SWM.T_USER TU, CC.CFG_PERSON CP
							WHERE 	TU.PERIPHERALNUMBER = CP.EMPLOYEE_ID 
							AND 	TU.CTICREATECD = 'Y'
						);
			END IF;
		END IF;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] AG_ORG_HIST INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--������ �߻��Ͽ� �ѹ�ó��
    END;


	IF	O_SQL_ECODE  <>  0  THEN
		--������ �߻��� ��� FAIL�� �����ڵ� �� �ð��� ������Ʈ �Ѵ�.
		BEGIN
			UPDATE	SWM.AX_STAT_HIST
			SET		EXP 		= 	'FAIL',
					EXPMSG 		= 	O_SQL_EMSG,
					ENDTIME 	= 	SYSDATE
			WHERE	STATGUBUN 	= 	'10(15MIN)'
			AND		TARGETTIME 	= 	SUBSTR(I_ROW_DATE,1,12)
			AND 	TARGETTABLE = 	'INIT(15MIN)'
			AND		EXPROC		= 	'SP_15MIN_INIT'
			AND 	EXP			= 	'ONGOING';
		END;
    ELSE
		--������ ���� ��� SUCCESS�� ��� �ð��� ������Ʈ �Ѵ�.
		BEGIN
			UPDATE	SWM.AX_STAT_HIST
			SET		EXP 		= 	'SUCCESS',
					EXPMSG 		= 	'SUCCESS',
					ENDTIME 	= 	SYSDATE
			WHERE	STATGUBUN 	= 	'10(15MIN)'
			AND		TARGETTIME 	= 	SUBSTR(I_ROW_DATE,1,12)
			AND 	TARGETTABLE = 	'INIT(15MIN)'
			AND		EXPROC		= 	'SP_15MIN_INIT'
			AND 	EXP			= 	'ONGOING';
      	END;
    END IF;

	COMMIT;

END;

CREATE OR REPLACE PROCEDURE SWM.SP_ABLE_EXC_RETURN
(
    I_ROW_DATE		IN	VARCHAR2,
	I_STAT_GUBUN	IN	VARCHAR2,	
	I_TARGET_TABLE	IN	VARCHAR2,
	I_EXPROC		IN	VARCHAR2,
    I_SQL_ECODE		IN	INT,
    I_SQL_EMSG		IN	VARCHAR2
)
IS

TARGET_DATE VARCHAR2(50);

BEGIN
	--ver.0.1
	CASE 	I_STAT_GUBUN
		WHEN 	'10(15MIN)'		THEN
				TARGET_DATE := SUBSTR(I_ROW_DATE,1,12);
		WHEN 	'20(HOUR)' 		THEN
				TARGET_DATE := SUBSTR(I_ROW_DATE,1,10);
		WHEN 	'30(DAY)' 		THEN
				TARGET_DATE := SUBSTR(I_ROW_DATE,1,8);
		WHEN 	'40(MONTH)' 	THEN
				TARGET_DATE := SUBSTR(I_ROW_DATE,1,6);
		WHEN 	'50(DAY)'		THEN
				TARGET_DATE := SUBSTR(I_ROW_DATE,1,8);		
		ELSE
				TARGET_DATE := SUBSTR(I_ROW_DATE,1,12);
		END
	CASE;


	IF	I_SQL_ECODE  <>  0  THEN
		--������ �߻��� ��� FAIL�� �����ڵ� �� �ð��� ������Ʈ �Ѵ�.	
		BEGIN
			UPDATE	SWM.AX_STAT_HIST
			SET		EXP 		= 	'FAIL',
					EXPMSG 		= 	I_SQL_EMSG,
					ENDTIME 	= 	SYSDATE
			WHERE	STATGUBUN 	= 	I_STAT_GUBUN
			AND		TARGETTIME 	= 	TARGET_DATE
			AND 	TARGETTABLE = 	I_TARGET_TABLE
			AND		EXPROC		= 	I_EXPROC
			AND 	EXP			= 	'ONGOING';
		END;
    ELSE
		--������ ���� ��� SUCCESS�� ��� �ð��� ������Ʈ �Ѵ�.
		BEGIN
			UPDATE	SWM.AX_STAT_HIST
			SET		EXP 		= 	'SUCCESS',
					EXPMSG 		= 	'SUCCESS',
					ENDTIME 	= 	SYSDATE
			WHERE	STATGUBUN 	= 	I_STAT_GUBUN
			AND		TARGETTIME 	= 	TARGET_DATE
			AND 	TARGETTABLE = 	I_TARGET_TABLE
			AND		EXPROC		= 	I_EXPROC
			AND 	EXP			= 	'ONGOING';
      	END;
    END IF;

	COMMIT;

END;

CREATE OR REPLACE PROCEDURE SWM.SP_DAY_PURGE
(
    I_ROW_DATE		IN	VARCHAR2,
    O_SQL_ECODE		OUT	INT,
    O_SQL_EMSG		OUT	VARCHAR2
)
IS

BEGIN
	--ver.0.1
    O_SQL_ECODE := 0; --초기값은 0으로..
    O_SQL_EMSG 	:= 'PROC SP_DAY_PURGE ONGOING...'; --초기값은 정상 처리된걸로..

	BEGIN
		--데이터를 집계해야 하는경우 STAT_HIST 데이터를 추가 한다.        
		--배치 시작전 기본 데이터를 세팅한다.(집계시작날짜, 집계구분, 집계타겟날짜, 타겟테이블/간략명, 타겟프로시저명, 결과, 결과MSG, 종료날짜)
        INSERT INTO  SWM.AX_STAT_HIST
        (STARTTIME, STATGUBUN, TARGETTIME, TARGETTABLE, EXPROC, EXP, EXPMSG, ENDTIME)
        VALUES	(
					SYSDATE,					--STARTTIME
					'50(PURGE)',				--STATGUBUN
					TO_CHAR(ADD_MONTHS(TO_DATE(SUBSTR(I_ROW_DATE,1,8),'YYYY-MM-DD'), -60),'YYYYMMDD'),	--TARGETTIME
					'PURGE(DAY)',				--TARGETTABLE
					'SP_DAY_PURGE',				--EXPROC
					'ONGOING',					--EXP
					NULL,						--EXPMSG
					NULL						--ENDTIME
				);

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] STAT_HIST INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리

    END;


	--일 단위 / AGENT PURGE(5년)
	BEGIN
		DELETE
		FROM 	SWM.AGENT_DY
        WHERE   ROW_DATE  = TO_CHAR(ADD_MONTHS(TO_DATE(SUBSTR(I_ROW_DATE,1,8),'YYYY-MM-DD'), -60),'YYYY-MM-DD')
        ;--AND		STARTTIME = SUBSTR(I_ROW_DATE,9,4);

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] AGENT_DY PURGE.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


	--일 단위 / SKILL PURGE(5년)
    BEGIN
		DELETE
		FROM 	SWM.SKILL_DY
        WHERE   ROW_DATE  = TO_CHAR(ADD_MONTHS(TO_DATE(SUBSTR(I_ROW_DATE,1,8),'YYYY-MM-DD'), -60),'YYYY-MM-DD')
        ;--AND		STARTTIME = SUBSTR(I_ROW_DATE,9,4);

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] SKILL_DY PURGE.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;

	-- 배치 성공 이력 (5일)
	BEGIN
		DELETE FROM swm.t_historybatch
		WHERE starttime < SYSTIMESTAMP-5 AND status='Y';
	END;
	-- 배치 실패 이력 (60일)
	BEGIN
		DELETE FROM swm.t_historybatch
		WHERE starttime < SYSTIMESTAMP-60 AND status='N';
	END;


 	IF	O_SQL_ECODE  <>  0  THEN
		--오류가 발생한 경우 FAIL과 오류코드 및 시간을 업데이트 한다.
		BEGIN
			UPDATE	SWM.AX_STAT_HIST
			SET		EXP 		= 	'FAIL',
					EXPMSG 		= 	O_SQL_EMSG,
					ENDTIME 	= 	SYSDATE
			WHERE	STATGUBUN 	= 	'50(PURGE)'
			AND		TARGETTIME 	= 	TO_CHAR(ADD_MONTHS(TO_DATE(SUBSTR(I_ROW_DATE,1,8),'YYYY-MM-DD'), -60),'YYYYMMDD')
			AND 	TARGETTABLE = 	'PURGE(DAY)'
			AND		EXPROC		= 	'SP_DAY_PURGE'
			AND 	EXP			= 	'ONGOING';
		END;
    ELSE
		--오류가 없는 경우 SUCCESS와 결과 시간을 업데이트 한다.
		BEGIN
			UPDATE	SWM.AX_STAT_HIST
			SET		EXP 		= 	'SUCCESS',
					EXPMSG 		= 	'SUCCESS',
					ENDTIME 	= 	SYSDATE
			WHERE	STATGUBUN 	= 	'50(PURGE)'
			AND		TARGETTIME 	= 	TO_CHAR(ADD_MONTHS(TO_DATE(SUBSTR(I_ROW_DATE,1,8),'YYYY-MM-DD'), -60),'YYYYMMDD')
			AND 	TARGETTABLE = 	'PURGE(DAY)'
			AND		EXPROC		= 	'SP_DAY_PURGE'
			AND 	EXP			= 	'ONGOING';
      	END;
    END IF;

	COMMIT;

END;

CREATE OR REPLACE PROCEDURE SWM.SP_DAY_SKILL
(
    I_ROW_DATE		IN	VARCHAR2,
    O_SQL_ECODE		OUT	INT,
    O_SQL_EMSG		OUT	VARCHAR2
)
IS

BEGIN
	--ver.0.1
    O_SQL_ECODE := 0; --초기값은 0으로..
    O_SQL_EMSG 	:= 'PROC SP_DAY_SKILL ONGOING...'; --초기값은 정상 처리된걸로..

	BEGIN
		--데이터를 집계해야 하는경우 STAT_HIST 데이터를 추가 한다.        
		--배치 시작전 기본 데이터를 세팅한다.(집계시작날짜, 집계구분, 집계타겟날짜, 타겟테이블/간략명, 타겟프로시저명, 결과, 결과MSG, 종료날짜)
        INSERT INTO  SWM.AX_STAT_HIST
        (STARTTIME, STATGUBUN, TARGETTIME, TARGETTABLE, EXPROC, EXP, EXPMSG, ENDTIME)
        VALUES	(
					SYSDATE,					--STARTTIME
					'30(DAY)',					--STATGUBUN
					SUBSTR(I_ROW_DATE,1,8),		--TARGETTIME
					'SKILL_DY',					--TARGETTABLE
					'SP_DAY_SKILL',				--EXPROC
					'ONGOING',					--EXP
					NULL,						--EXPMSG
					NULL						--ENDTIME
				);

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] STAT_HIST INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리

    END;


	BEGIN
		--집계를 수행해야 하는 경우 순서대로 프로세스를 수행한다.
		--집계대상 시간대에 데이터가 집계되어 있다면 삭제
		DELETE
		FROM  	SWM.SKILL_DY
		WHERE	ROW_DATE = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2);

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] SKILL_DY DELETE.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


	BEGIN
		INSERT 
		INTO 	SWM.SKILL_DY
		(
			SELECT	SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2) AS ROW_DATE,
					DECODE(TO_CHAR(to_date(SUBSTR(I_ROW_DATE,1,4) || SUBSTR(I_ROW_DATE,5,2) || SUBSTR(I_ROW_DATE,7,2)), 'd'),1, '일', 2, '월', 3, '화', 4, '수', 5, '목', 6, '금', 7, '토') || '요일' AS ROW_WEEK,
					DG_DBID 							AS 		DG_DBID,
					DG_CODE								AS		DG_CODE,
					----QUEUE 최대기준값----
					MAX(N_MAX_AGT) 						AS 		N_MAX_AGT,
					MAX(N_MAX_ACD) 						AS 		N_MAX_ACD,
					MAX(T_MAX_ACD) 						AS 		T_MAX_ACD,
					----QUEUE 분배호----
					SUM(N_DISTRIB) 						AS 		N_DISTRIB,
					SUM(T_DISTRIB) 						AS 		T_DISTRIB,
					----QUEUE 총인입호----
					SUM(N_TOT_ACD_WAIT) 				AS 		N_TOT_ACD_WAIT,
					SUM(T_TOT_ACD_WAIT)					AS 		T_TOT_ACD_WAIT,
					----QUEUE 상담사인입호----
					SUM(N_ENTER_ACD)						AS 		N_ENTER_ACD,
					SUM(T_ENTER_ACD)					AS 		T_ENTER_ACD,
					----QUEUE N초 이상 N초 미만 상담사 응답호----
					SUM(N_WAIT_ACD) 					AS 		N_WAIT_ACD,
					SUM(N_WAIT_ACD_0) 					AS 		N_WAIT_ACD_0,
					SUM(N_WAIT_ACD_1) 					AS 		N_WAIT_ACD_1,
					SUM(N_WAIT_ACD_2) 					AS 		N_WAIT_ACD_2,
					SUM(N_WAIT_ACD_3) 					AS 		N_WAIT_ACD_3,
					SUM(N_WAIT_ACD_4) 					AS 		N_WAIT_ACD_4,
					SUM(N_WAIT_ACD_5) 					AS 		N_WAIT_ACD_5,
					SUM(N_WAIT_ACD_6) 					AS 		N_WAIT_ACD_6,
					SUM(N_WAIT_ACD_7) 					AS 		N_WAIT_ACD_7,
					SUM(N_WAIT_ACD_8) 					AS 		N_WAIT_ACD_8,
					SUM(N_WAIT_ACD_9) 					AS 		N_WAIT_ACD_9,
					SUM(N_WAIT_ACD_10) 					AS 		N_WAIT_ACD_10,
					SUM(N_WAIT_ACD_11) 					AS 		N_WAIT_ACD_11,
					----QUEUE N초 이상 N초미만 상담사 응답 누적 시간----
					SUM(T_WAIT_ACD) 					AS 		T_WAIT_ACD,
					SUM(T_WAIT_ACD_0) 					AS 		T_WAIT_ACD_0,
					SUM(T_WAIT_ACD_1) 					AS 		T_WAIT_ACD_1,
					SUM(T_WAIT_ACD_2) 					AS 		T_WAIT_ACD_2,
					SUM(T_WAIT_ACD_3) 					AS 		T_WAIT_ACD_3,
					SUM(T_WAIT_ACD_4) 					AS 		T_WAIT_ACD_4,
					SUM(T_WAIT_ACD_5) 					AS 		T_WAIT_ACD_5,
					SUM(T_WAIT_ACD_6) 					AS 		T_WAIT_ACD_6,
					SUM(T_WAIT_ACD_7) 					AS 		T_WAIT_ACD_7,
					SUM(T_WAIT_ACD_8) 					AS 		T_WAIT_ACD_8,
					SUM(T_WAIT_ACD_9) 					AS 		T_WAIT_ACD_9,
					SUM(T_WAIT_ACD_10) 					AS 		T_WAIT_ACD_10,
					SUM(T_WAIT_ACD_11) 					AS 		T_WAIT_ACD_11,
					MAX(T_MAX_WAIT_ACD) 				AS 		T_MAX_WAIT_ACD,
					----QUEUE N초 이상 N초 미만 큐 포기호----
					SUM(N_AB_ACD) 						AS 		N_AB_ACD,
					SUM(N_AB_ACD_0) 					AS 		N_AB_ACD_0,
					SUM(N_AB_ACD_1) 					AS 		N_AB_ACD_1,
					SUM(N_AB_ACD_2) 					AS 		N_AB_ACD_2,
					SUM(N_AB_ACD_3) 					AS 		N_AB_ACD_3,
					SUM(N_AB_ACD_4) 					AS 		N_AB_ACD_4,
					SUM(N_AB_ACD_5) 					AS 		N_AB_ACD_5,
					SUM(N_AB_ACD_6) 					AS 		N_AB_ACD_6,
					SUM(N_AB_ACD_7) 					AS 		N_AB_ACD_7,
					SUM(N_AB_ACD_8) 					AS 		N_AB_ACD_8,
					SUM(N_AB_ACD_9) 					AS 		N_AB_ACD_9,
					SUM(N_AB_ACD_10) 					AS 		N_AB_ACD_10,
					SUM(N_AB_ACD_11) 					AS 		N_AB_ACD_11,
					----QUEUE N초 이상 N초미만 큐 포기 누적 시간----
					SUM(T_AB_ACD) 						AS 		T_AB_ACD,
					SUM(T_AB_ACD_0) 					AS 		T_AB_ACD_0,
					SUM(T_AB_ACD_1) 					AS 		T_AB_ACD_1,
					SUM(T_AB_ACD_2) 					AS 		T_AB_ACD_2,
					SUM(T_AB_ACD_3) 					AS 		T_AB_ACD_3,
					SUM(T_AB_ACD_4) 					AS 		T_AB_ACD_4,
					SUM(T_AB_ACD_5) 					AS 		T_AB_ACD_5,
					SUM(T_AB_ACD_6) 					AS 		T_AB_ACD_6,
					SUM(T_AB_ACD_7) 					AS 		T_AB_ACD_7,
					SUM(T_AB_ACD_8) 					AS 		T_AB_ACD_8,
					SUM(T_AB_ACD_9) 					AS 		T_AB_ACD_9,
					SUM(T_AB_ACD_10) 					AS 		T_AB_ACD_10,
					SUM(T_AB_ACD_11) 					AS 		T_AB_ACD_11,
					MAX(T_MAX_AB_ACD) 					AS 		T_MAX_AB_ACD,
					----QUEUE 플로우인 아웃----
					SUM(N_FLOW_IN) 						AS 		N_FLOW_IN,
					SUM(T_FLOW_IN) 						AS 		T_FLOW_IN,
					SUM(N_FLOW_OUT) 					AS 		N_FLOW_OUT,
					SUM(T_FLOW_OUT) 					AS 		T_FLOW_OUT,
					----QUEUE 기타 건수 시간----
					SUM(N_RONA) 						AS 		N_RONA,
					SUM(N_NON_SERVICE) 					AS 		N_NON_SERVICE,
					SUM(T_NON_SERVICE) 					AS 		T_NON_SERVICE,
					MAX(T_MAX_NON_SERVICE) 				AS 		T_MAX_NON_SERVICE,
					----QUEUE 인입기준 건수----
					SUM(N_ENTER) 						AS 		N_ENTER,
					SUM(N_ANSW) 						AS 		N_ANSW,
					SUM(N_ABAN) 						AS 		N_ABAN,
					----AGENT SUMMARY----
					----AGENT 로그인건수, 로그인시간, 최초 로그인, 최종 로그아웃 시각----
					SUM(N_LOGGD_IN) 					AS 		N_LOGGD_IN,
					SUM(N_LOGGD_OUT) 					AS 		N_LOGGD_OUT,
					SUM(T_I_LOGIN) 						AS 		T_I_LOGIN,
					----AGENT 대기건수, 대기시간----
					SUM(N_READY) 						AS 		N_READY,
					SUM(T_I_READY) 						AS 		T_I_READY,
					----AGENT 후처리건수, 후처리시간 (TI - 메인스킬, I - 보유스킬)----
					SUM(N_ACW) 							AS 		N_ACW,
					SUM(T_I_ACW) 						AS 		T_I_ACW,
					SUM(T_I_ACW_IT)						AS 		T_I_ACW_IT,
					SUM(T_I_ACW_IB)						AS 		T_I_ACW_IB,
					SUM(T_I_ACW_OB)						AS 		T_I_ACW_OB,
					SUM(T_I_ACW_CO)						AS 		T_I_ACW_CO,
					----AGENT 이석건수, 이석시간 (TI - 메인스킬, I - 보유스킬)----
					SUM(N_NREADY) 						AS 		N_NREADY,
					SUM(N_NREADY_0) 					AS 		N_NREADY_0,
					SUM(N_NREADY_1) 					AS 		N_NREADY_1,
					SUM(N_NREADY_2) 					AS 		N_NREADY_2,
					SUM(N_NREADY_3) 					AS 		N_NREADY_3,
					SUM(N_NREADY_4) 					AS 		N_NREADY_4,
					SUM(N_NREADY_5) 					AS 		N_NREADY_5,
					SUM(N_NREADY_6) 					AS 		N_NREADY_6,
					SUM(N_NREADY_7) 					AS 		N_NREADY_7,
					SUM(N_NREADY_8) 					AS 		N_NREADY_8,
					SUM(N_NREADY_9) 					AS 		N_NREADY_9,
					SUM(T_I_NREADY) 					AS 		T_I_NREADY,
					SUM(T_I_NREADY_0) 					AS 		T_I_NREADY_0,
					SUM(T_I_NREADY_1) 					AS 		T_I_NREADY_1,
					SUM(T_I_NREADY_2) 					AS 		T_I_NREADY_2,
					SUM(T_I_NREADY_3) 					AS 		T_I_NREADY_3,
					SUM(T_I_NREADY_4) 					AS 		T_I_NREADY_4,
					SUM(T_I_NREADY_5) 					AS 		T_I_NREADY_5,
					SUM(T_I_NREADY_6) 					AS 		T_I_NREADY_6,
					SUM(T_I_NREADY_7) 					AS 		T_I_NREADY_7,
					SUM(T_I_NREADY_8) 					AS 		T_I_NREADY_8,
					SUM(T_I_NREADY_9) 					AS 		T_I_NREADY_9,
					----AGENT 기타시간 (TI - 메인스킬, I - 보유스킬)----
					SUM(T_I_OTHER) 						AS 		T_I_OTHER,
					----AGENT 상담사 벨울림건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_ACD) 					AS 		N_RING_ACD,
					SUM(N_RING_ACD_1) 					AS 		N_RING_ACD_1,
					SUM(N_RING_ACD_2) 					AS 		N_RING_ACD_2,
					SUM(N_RING_ACD_3) 					AS 		N_RING_ACD_3,
					SUM(T_RING_ACD) 					AS 		T_RING_ACD,
					SUM(T_RING_ACD_E) 					AS 		T_RING_ACD_E,
					SUM(T_RING_ACD_1) 					AS 		T_RING_ACD_1,
					SUM(T_RING_ACD_2) 					AS 		T_RING_ACD_2,
					SUM(T_RING_ACD_3) 					AS 		T_RING_ACD_3,
					MAX(T_RING_MAX_ACD) 				AS 		T_RING_MAX_ACD,
					----AGENT 상담사 응답건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_TALK_ACD) 					AS 		N_TALK_ACD,
					SUM(N_TALK_ACD_0)					AS 		N_TALK_ACD_0,
					SUM(N_TALK_ACD_1)					AS 		N_TALK_ACD_1,
					SUM(N_TALK_ACD_2)					AS 		N_TALK_ACD_2,
					SUM(N_TALK_ACD_3)					AS 		N_TALK_ACD_3,
					SUM(N_TALK_ACD_4)					AS 		N_TALK_ACD_4,
					SUM(N_TALK_ACD_5)					AS 		N_TALK_ACD_5,
					SUM(N_TALK_ACD_6)					AS 		N_TALK_ACD_6,
					SUM(N_TALK_ACD_7)					AS 		N_TALK_ACD_7,
					SUM(N_TALK_ACD_8)					AS 		N_TALK_ACD_8,
					SUM(N_TALK_ACD_9)					AS 		N_TALK_ACD_9,
					SUM(N_TALK_ACD_10)					AS 		N_TALK_ACD_10,
					SUM(N_TALK_ACD_11)					AS 		N_TALK_ACD_11,
					SUM(N_TALK_ACD_NOTR)				AS 		N_TALK_ACD_NOTR,
					SUM(T_TALK_ACD) 					AS 		T_TALK_ACD,
					SUM(T_TALK_ACD_E) 					AS 		T_TALK_ACD_E,
					MAX(T_TALK_MAX_ACD) 				AS 		T_TALK_MAX_ACD,
					----AGENT 상담사 벨울림 포기건수, 시간----
					SUM(N_RING_AB_ACD) 					AS 		N_RING_AB_ACD,
					SUM(N_RING_AB_ACD_1) 				AS 		N_RING_AB_ACD_1,
					SUM(N_RING_AB_ACD_2) 				AS 		N_RING_AB_ACD_2,
					SUM(N_RING_AB_ACD_3) 				AS 		N_RING_AB_ACD_3,
					SUM(T_RING_AB_ACD) 					AS 		T_RING_AB_ACD,
					SUM(T_RING_AB_ACD_1) 				AS 		T_RING_AB_ACD_1,
					SUM(T_RING_AB_ACD_2) 				AS 		T_RING_AB_ACD_2,
					SUM(T_RING_AB_ACD_3) 				AS 		T_RING_AB_ACD_3,
					MAX(T_RING_MAX_AB_ACD) 				AS 		T_RING_MAX_AB_ACD,
					----AGENT 상담사 NACD 수신건수, 시간----
					SUM(N_RING_NACD) 					AS 		N_RING_NACD,
					SUM(N_TALK_NACD_IN) 				AS 		N_TALK_NACD_IN,
					SUM(N_RING_AB_NACD) 				AS 		N_RING_AB_NACD,
					SUM(T_RING_NACD) 					AS 		T_RING_NACD,
					SUM(T_TALK_NACD_IN) 				AS 		T_TALK_NACD_IN,
					SUM(T_RING_AB_NACD) 				AS 		T_RING_AB_NACD,
					SUM(N_DIAL_NACD) 					AS 		N_DIAL_NACD,
					SUM(N_TALK_NACD_OUT) 				AS 		N_TALK_NACD_OUT,
					SUM(T_DIAL_NACD) 					AS 		T_DIAL_NACD,
					SUM(T_TALK_NACD_OUT) 				AS 		T_TALK_NACD_OUT,
					----AGENT 상담사 DACD 수신건수, 시간----
					SUM(N_RING_DACD) 					AS 		N_RING_DACD,
					SUM(N_TALK_DACD_IN) 				AS 		N_TALK_DACD_IN,
					SUM(N_RING_AB_DACD) 				AS 		N_RING_AB_DACD,
					SUM(T_RING_DACD) 					AS 		T_RING_DACD,
					SUM(T_TALK_DACD_IN)	 				AS 		T_TALK_DACD_IN,
					SUM(T_RING_AB_DACD) 				AS 		T_RING_AB_DACD,
					SUM(N_DIAL_DACD) 					AS 		N_DIAL_DACD,
					SUM(N_TALK_DACD_OUT) 				AS 		N_TALK_DACD_OUT,
					SUM(T_DIAL_DACD) 					AS 		T_DIAL_DACD,
					SUM(T_TALK_DACD_OUT) 				AS 		T_TALK_DACD_OUT,
					----AGENT 총 보류 건수, 시간----
					SUM(N_HOLD) 						AS 		N_HOLD,
					SUM(N_HOLD_AB) 						AS 		N_HOLD_AB,
					SUM(N_HOLD_ACD) 					AS 		N_HOLD_ACD,
					SUM(N_HOLD_AB_ACD) 					AS 		N_HOLD_AB_ACD,
					SUM(N_HOLD_NACD) 					AS 		N_HOLD_NACD,
					SUM(N_HOLD_AB_NACD) 				AS 		N_HOLD_AB_NACD,
					SUM(T_HOLD) 						AS 		T_HOLD,
					SUM(T_HOLD_ACD) 					AS 		T_HOLD_ACD,
					SUM(T_HOLD_NACD) 					AS 		T_HOLD_NACD,
					----AGENT 콜타입별 보류 건수, 시간----
					SUM(N_HOLD_IT) 						AS 		N_HOLD_IT,
					SUM(N_HOLD_ACD_IT) 					AS 		N_HOLD_ACD_IT,
					SUM(T_HOLD_IT) 						AS 		T_HOLD_IT,
					SUM(T_HOLD_ACD_IT) 					AS 		T_HOLD_ACD_IT,
					SUM(N_HOLD_IB) 						AS 		N_HOLD_IB,
					SUM(N_HOLD_ACD_IB) 					AS 		N_HOLD_ACD_IB,
					SUM(T_HOLD_IB) 						AS 		T_HOLD_IB,
					SUM(T_HOLD_ACD_IB) 					AS 		T_HOLD_ACD_IB,
					SUM(N_HOLD_OB) 						AS 		N_HOLD_OB,
					SUM(N_HOLD_ACD_OB) 					AS 		N_HOLD_ACD_OB,
					SUM(T_HOLD_OB) 						AS 		T_HOLD_OB,
					SUM(T_HOLD_ACD_OB) 					AS 		T_HOLD_ACD_OB,
					SUM(N_HOLD_CO) 						AS 		N_HOLD_CO,
					SUM(N_HOLD_ACD_CO) 					AS 		N_HOLD_ACD_CO,
					SUM(T_HOLD_CO) 						AS 		T_HOLD_CO,
					SUM(T_HOLD_ACD_CO) 					AS 		T_HOLD_ACD_CO,
					----AGENT 1. INTERNAL 수신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_IT) 						AS 		N_RING_IT,
					SUM(N_RING_AB_IT) 					AS 		N_RING_AB_IT,
					SUM(N_RING_IW_TRST_IT) 				AS 		N_RING_IW_TRST_IT,
					SUM(N_TALK_IW_IT) 					AS 		N_TALK_IW_IT,
					SUM(N_TALK_IW_TRST_IT) 				AS 		N_TALK_IW_TRST_IT,
					SUM(N_TALK_IW_TRSM_IT) 				AS 		N_TALK_IW_TRSM_IT,
					SUM(N_TALK_CONFJ_IT) 				AS 		N_TALK_CONFJ_IT,
					SUM(N_TALK_CONFM_IT) 				AS 		N_TALK_CONFM_IT,
					SUM(T_RING_IT) 						AS 		T_RING_IT,
					SUM(T_RING_IT_E) 					AS 		T_RING_IT_E,
					SUM(T_RING_AB_IT) 					AS 		T_RING_AB_IT,
					SUM(T_RING_IW_TRST_IT) 				AS 		T_RING_IW_TRST_IT,
					SUM(T_TALK_IW_IT) 					AS 		T_TALK_IW_IT,
					SUM(T_TALK_IW_IT_E) 				AS 		T_TALK_IW_IT_E,
					SUM(T_TALK_IW_TRST_IT) 				AS 		T_TALK_IW_TRST_IT,
					SUM(T_TALK_IW_TRSM_IT) 				AS 		T_TALK_IW_TRSM_IT,
					SUM(T_TALK_CONFJ_IT) 				AS 		T_TALK_CONFJ_IT,
					SUM(T_TALK_CONFM_IT) 				AS 		T_TALK_CONFM_IT,
					----AGENT 1. INTERNAL ACD 수신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_IT_ACD) 					AS 		N_RING_IT_ACD,
					SUM(N_RING_AB_IT_ACD) 				AS 		N_RING_AB_IT_ACD,
					SUM(N_RING_IW_TRST_IT_ACD) 			AS 		N_RING_IW_TRST_IT_ACD,
					SUM(N_TALK_IW_IT_ACD) 				AS 		N_TALK_IW_IT_ACD,
					SUM(N_TALK_IW_TRST_IT_ACD) 			AS 		N_TALK_IW_TRST_IT_ACD,
					SUM(N_TALK_IW_TRSM_IT_ACD) 			AS 		N_TALK_IW_TRSM_IT_ACD,
					SUM(N_TALK_CONFJ_IT_ACD) 			AS 		N_TALK_CONFJ_IT_ACD,
					SUM(N_TALK_CONFM_IT_ACD) 			AS 		N_TALK_CONFM_IT_ACD,
					SUM(T_RING_IT_ACD) 					AS 		T_RING_IT_ACD,
					SUM(T_RING_IT_ACD_E) 				AS 		T_RING_IT_ACD_E,
					SUM(T_RING_AB_IT_ACD) 				AS 		T_RING_AB_IT_ACD,
					SUM(T_RING_IW_TRST_IT_ACD) 			AS 		T_RING_IW_TRST_IT_ACD,
					SUM(T_TALK_IW_IT_ACD) 				AS 		T_TALK_IW_IT_ACD,
					SUM(T_TALK_IW_IT_ACD_E) 			AS 		T_TALK_IW_IT_ACD_E,
					SUM(T_TALK_IW_TRST_IT_ACD) 			AS 		T_TALK_IW_TRST_IT_ACD,
					SUM(T_TALK_IW_TRSM_IT_ACD) 			AS 		T_TALK_IW_TRSM_IT_ACD,
					SUM(T_TALK_CONFJ_IT_ACD) 			AS 		T_TALK_CONFJ_IT_ACD,
					SUM(T_TALK_CONFM_IT_ACD) 			AS 		T_TALK_CONFM_IT_ACD,
					----AGENT 1. INTERNAL 발신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_DIAL_IT) 						AS 		N_DIAL_IT,
					SUM(N_TALK_OW_IT) 					AS 		N_TALK_OW_IT,
					SUM(N_TALK_OW_TRSM_IT) 				AS 		N_TALK_OW_TRSM_IT,
					SUM(T_DIAL_IT) 						AS 		T_DIAL_IT,
					SUM(T_DIAL_IT_E) 					AS 		T_DIAL_IT_E,
					SUM(T_TALK_OW_IT) 					AS 		T_TALK_OW_IT,
					SUM(T_TALK_OW_IT_E) 				AS 		T_TALK_OW_IT_E,
					SUM(T_TALK_OW_TRSM_IT) 				AS 		T_TALK_OW_TRSM_IT,
					----AGENT 2. INBOUND 수신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_IB) 						AS 		N_RING_IB,
					SUM(N_RING_AB_IB) 					AS 		N_RING_AB_IB,
					SUM(N_RING_IW_TRST_IB) 				AS 		N_RING_IW_TRST_IB,
					SUM(N_TALK_IW_TRST_IB) 				AS 		N_TALK_IW_TRST_IB,
					SUM(N_TALK_IW_IB) 					AS 		N_TALK_IW_IB,
					SUM(N_TALK_IW_TRSM_IB) 				AS 		N_TALK_IW_TRSM_IB,
					SUM(N_TALK_CONFJ_IB) 				AS 		N_TALK_CONFJ_IB,
					SUM(N_TALK_CONFM_IB) 				AS 		N_TALK_CONFM_IB,
					SUM(T_RING_IB) 						AS 		T_RING_IB,
					SUM(T_RING_IB_E) 					AS 		T_RING_IB_E,
					SUM(T_RING_AB_IB) 					AS 		T_RING_AB_IB,
					SUM(T_RING_IW_TRST_IB) 				AS 		T_RING_IW_TRST_IB,
					SUM(T_TALK_IW_IB) 					AS 		T_TALK_IW_IB,
					SUM(T_TALK_IW_IB_E)					AS 		T_TALK_IW_IB_E,
					SUM(T_TALK_IW_TRST_IB) 				AS 		T_TALK_IW_TRST_IB,
					SUM(T_TALK_IW_TRSM_IB) 				AS 		T_TALK_IW_TRSM_IB,
					SUM(T_TALK_CONFJ_IB) 				AS 		T_TALK_CONFJ_IB,
					SUM(T_TALK_CONFM_IB) 				AS 		T_TALK_CONFM_IB,
					----AGENT 2. INBOUND ACD 수신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_IB_ACD) 					AS 		N_RING_IB_ACD,
					SUM(N_RING_AB_IB_ACD) 				AS 		N_RING_AB_IB_ACD,
					SUM(N_RING_IW_TRST_IB_ACD) 			AS 		N_RING_IW_TRST_IB_ACD,
					SUM(N_TALK_IW_IB_ACD) 				AS 		N_TALK_IW_IB_ACD,
					SUM(N_TALK_IW_TRST_IB_ACD) 			AS 		N_TALK_IW_TRST_IB_ACD,
					SUM(N_TALK_IW_TRSM_IB_ACD) 			AS 		N_TALK_IW_TRSM_IB_ACD,
					SUM(N_TALK_CONFJ_IB_ACD) 			AS 		N_TALK_CONFJ_IB_ACD,
					SUM(N_TALK_CONFM_IB_ACD) 			AS 		N_TALK_CONFM_IB_ACD,
					SUM(T_RING_IB_ACD) 					AS 		T_RING_IB_ACD,
					SUM(T_RING_IB_ACD_E) 				AS 		T_RING_IB_ACD_E,
					SUM(T_RING_AB_IB_ACD) 				AS 		T_RING_AB_IB_ACD,
					SUM(T_RING_IW_TRST_IB_ACD) 			AS 		T_RING_IW_TRST_IB_ACD,
					SUM(T_TALK_IW_IB_ACD) 				AS 		T_TALK_IW_IB_ACD,
					SUM(T_TALK_IW_IB_ACD_E) 			AS 		T_TALK_IW_IB_ACD_E,
					SUM(T_TALK_IW_TRST_IB_ACD) 			AS 		T_TALK_IW_TRST_IB_ACD,
					SUM(T_TALK_IW_TRSM_IB_ACD) 			AS 		T_TALK_IW_TRSM_IB_ACD,
					SUM(T_TALK_CONFJ_IB_ACD) 			AS 		T_TALK_CONFJ_IB_ACD,
					SUM(T_TALK_CONFM_IB_ACD) 			AS 		T_TALK_CONFM_IB_ACD,
					----AGENT 3. OUTBOUND 수신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_OB) 						AS 		N_RING_OB,
					SUM(N_RING_AB_OB) 					AS 		N_RING_AB_OB,
					SUM(N_RING_IW_TRST_OB) 				AS 		N_RING_IW_TRST_OB,
					SUM(N_TALK_IW_OB) 					AS 		N_TALK_IW_OB,
					SUM(N_TALK_IW_TRST_OB) 				AS 		N_TALK_IW_TRST_OB,
					SUM(N_TALK_IW_TRSM_OB) 				AS 		N_TALK_IW_TRSM_OB,
					SUM(N_TALK_CONFJ_OB) 				AS 		N_TALK_CONFJ_OB,
					SUM(N_TALK_CONFM_OB) 				AS 		N_TALK_CONFM_OB,
					SUM(T_RING_OB) 						AS 		T_RING_OB,
					SUM(T_RING_OB_E) 					AS 		T_RING_OB_E,
					SUM(T_RING_AB_OB) 					AS 		T_RING_AB_OB,
					SUM(T_RING_IW_TRST_OB) 				AS 		T_RING_IW_TRST_OB,
					SUM(T_TALK_IW_OB) 					AS 		T_TALK_IW_OB,
					SUM(T_TALK_IW_OB_E) 				AS 		T_TALK_IW_OB_E,
					SUM(T_TALK_IW_TRST_OB) 				AS 		T_TALK_IW_TRST_OB,
					SUM(T_TALK_IW_TRSM_OB) 				AS 		T_TALK_IW_TRSM_OB,
					SUM(T_TALK_CONFJ_OB) 				AS 		T_TALK_CONFJ_OB,
					SUM(T_TALK_CONFM_OB) 				AS 		T_TALK_CONFM_OB,
					----AGENT 3. OUTBOUND ACD 수신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_OB_ACD) 					AS 		N_RING_OB_ACD,
					SUM(N_RING_AB_OB_ACD) 				AS 		N_RING_AB_OB_ACD,
					SUM(N_RING_IW_TRST_OB_ACD) 			AS 		N_RING_IW_TRST_OB_ACD,
					SUM(N_TALK_IW_OB_ACD) 				AS 		N_TALK_IW_OB_ACD,
					SUM(N_TALK_IW_TRST_OB_ACD) 			AS 		N_TALK_IW_TRST_OB_ACD,
					SUM(N_TALK_IW_TRSM_OB_ACD) 			AS 		N_TALK_IW_TRSM_OB_ACD,
					SUM(N_TALK_CONFJ_OB_ACD) 			AS 		N_TALK_CONFJ_OB_ACD,
					SUM(N_TALK_CONFM_OB_ACD) 			AS 		N_TALK_CONFM_OB_ACD,					
					SUM(T_RING_OB_ACD) 					AS 		T_RING_OB_ACD,
					SUM(T_RING_OB_ACD_E) 				AS 		T_RING_OB_ACD_E,
					SUM(T_RING_AB_OB_ACD) 				AS 		T_RING_AB_OB_ACD,
					SUM(T_RING_IW_TRST_OB_ACD) 			AS 		T_RING_IW_TRST_OB_ACD,
					SUM(T_TALK_IW_OB_ACD) 				AS 		T_TALK_IW_OB_ACD,
					SUM(T_TALK_IW_OB_ACD_E) 			AS 		T_TALK_IW_OB_ACD_E,
					SUM(T_TALK_IW_TRST_OB_ACD) 			AS 		T_TALK_IW_TRST_OB_ACD,
					SUM(T_TALK_IW_TRSM_OB_ACD) 			AS 		T_TALK_IW_TRSM_OB_ACD,
					SUM(T_TALK_CONFJ_OB_ACD) 			AS 		T_TALK_CONFJ_OB_ACD,
					SUM(T_TALK_CONFM_OB_ACD) 			AS 		T_TALK_CONFM_OB_ACD,
					----AGENT 3. OUTBOUND 발신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_DIAL_OB) 						AS 		N_DIAL_OB,
					SUM(N_TALK_OW_OB) 					AS 		N_TALK_OW_OB,
					SUM(N_TALK_OW_TRSM_OB) 				AS 		N_TALK_OW_TRSM_OB,
					SUM(T_DIAL_OB) 						AS 		T_DIAL_OB,
					SUM(T_DIAL_OB_E) 					AS 		T_DIAL_OB_E,
					SUM(T_TALK_OW_OB) 					AS 		T_TALK_OW_OB,
					SUM(T_TALK_OW_OB_E) 				AS 		T_TALK_OW_OB_E,
					SUM(T_TALK_OW_TRSM_OB) 				AS 		T_TALK_OW_TRSM_OB,
					----AGENT 4. CONSULT 수신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_CO) 						AS 		N_RING_CO,
					SUM(N_RING_AB_CO) 					AS 		N_RING_AB_CO,
					SUM(N_RING_IW_TRST_CO) 				AS 		N_RING_IW_TRST_CO,
					SUM(N_TALK_IW_CO) 					AS 		N_TALK_IW_CO,
					SUM(N_TALK_IW_TRST_CO) 				AS 		N_TALK_IW_TRST_CO,
					SUM(N_TALK_IW_TRSM_CO) 				AS 		N_TALK_IW_TRSM_CO,
					SUM(N_TALK_CONFJ_CO)	 			AS 		N_TALK_CONFJ_CO,
					SUM(N_TALK_CONFM_CO)	 			AS 		N_TALK_CONFM_CO,
					SUM(T_RING_CO) 						AS 		T_RING_CO,
					SUM(T_RING_CO_E) 					AS 		T_RING_CO_E,
					SUM(T_RING_AB_CO) 					AS 		T_RING_AB_CO,
					SUM(T_RING_IW_TRST_CO) 				AS 		T_RING_IW_TRST_CO,
					SUM(T_TALK_IW_CO) 					AS 		T_TALK_IW_CO,
					SUM(T_TALK_IW_CO_E) 				AS 		T_TALK_IW_CO_E,
					SUM(T_TALK_IW_TRST_CO) 				AS 		T_TALK_IW_TRST_CO,
					SUM(T_TALK_IW_TRSM_CO) 				AS 		T_TALK_IW_TRSM_CO,
					SUM(T_TALK_CONFJ_CO) 				AS 		T_TALK_CONFJ_CO,
					SUM(T_TALK_CONFM_CO) 				AS 		T_TALK_CONFM_CO,
					----AGENT 4. CONSULT ACD 수신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_CO_ACD) 					AS 		N_RING_CO_ACD,
					SUM(N_RING_AB_CO_ACD) 				AS 		N_RING_AB_CO_ACD,
					SUM(N_RING_IW_TRST_CO_ACD) 			AS 		N_RING_IW_TRST_CO_ACD,
					SUM(N_TALK_IW_CO_ACD) 				AS 		N_TALK_IW_CO_ACD,
					SUM(N_TALK_IW_TRST_CO_ACD) 			AS 		N_TALK_IW_TRST_CO_ACD,
					SUM(N_TALK_IW_TRSM_CO_ACD) 			AS 		N_TALK_IW_TRSM_CO_ACD,
					SUM(N_TALK_CONFJ_CO_ACD) 			AS 		N_TALK_CONFJ_CO_ACD,
					SUM(N_TALK_CONFM_CO_ACD) 			AS 		N_TALK_CONFM_CO_ACD,
					SUM(T_RING_CO_ACD) 					AS 		T_RING_CO_ACD,
					SUM(T_RING_CO_ACD_E) 				AS 		T_RING_CO_ACD_E,
					SUM(T_RING_AB_CO_ACD) 				AS 		T_RING_AB_CO_ACD,
					SUM(T_RING_IW_TRST_CO_ACD) 			AS 		T_RING_IW_TRST_CO_ACD,
					SUM(T_TALK_IW_CO_ACD) 				AS 		T_TALK_IW_CO_ACD,
					SUM(T_TALK_IW_CO_ACD_E) 			AS 		T_TALK_IW_CO_ACD_E,
					SUM(T_TALK_IW_TRST_CO_ACD) 			AS 		T_TALK_IW_TRST_CO_ACD,
					SUM(T_TALK_IW_TRSM_CO_ACD) 			AS 		T_TALK_IW_TRSM_CO_ACD,
					SUM(T_TALK_CONFJ_CO_ACD) 			AS 		T_TALK_CONFJ_CO_ACD,
					SUM(T_TALK_CONFM_CO_ACD) 			AS 		T_TALK_CONFM_CO_ACD,
					----AGENT 4. CONSULT 발신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_DIAL_CO) 						AS 		N_DIAL_CO,
					SUM(N_TALK_OW_CO) 					AS 		N_TALK_OW_CO,
					SUM(N_TALK_OW_TRSM_CO) 				AS 		N_TALK_OW_TRSM_CO,
					SUM(T_DIAL_CO) 						AS 		T_DIAL_CO,
					SUM(T_DIAL_CO_E) 					AS 		T_DIAL_CO_E,
					SUM(T_TALK_OW_CO) 					AS 		T_TALK_OW_CO,
					SUM(T_TALK_OW_CO_E) 				AS 		T_TALK_OW_CO_E,
					SUM(T_TALK_OW_TRSM_CO) 				AS 		T_TALK_OW_TRSM_CO,
					----AGENT 전환호----
					SUM(N_TRANS) 						AS 		N_TRANS,
					SUM(N_TRANS_TRST) 					AS 		N_TRANS_TRST,
					SUM(N_TRANS_TRSM)	 				AS 		N_TRANS_TRSM,
					----AGENT 회의호----
					SUM(N_CONF) 						AS 		N_CONF,
					SUM(N_CONF_CONFJ) 					AS 		N_CONF_CONFJ,
					SUM(N_CONF_CONFM) 					AS 		N_CONF_CONFM,
					SUM(T_CONF) 						AS 		T_CONF,
					SUM(T_CONF_CONFJ) 					AS 		T_CONF_CONFJ,
					SUM(T_CONF_CONFM) 					AS 		T_CONF_CONFM,
					----SKILL 여분 필드---
					SUM(ETC1) 							AS 		ETC1,
					SUM(ETC2) 							AS 		ETC2,
					SUM(ETC3) 							AS 		ETC3,
					SUM(ETC4) 							AS 		ETC4,
					SUM(ETC5) 							AS 		ETC5
			FROM	SWM.SKILL_HR
			WHERE	ROW_DATE = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2)
			GROUP BY DG_DBID, DG_CODE
		);

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] SKILL_DY INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


 	IF	O_SQL_ECODE  <>  0  THEN
		--오류가 발생한 경우 FAIL과 오류코드 및 시간을 업데이트 한다.
		BEGIN
			UPDATE	SWM.AX_STAT_HIST
			SET		EXP 		= 	'FAIL',
					EXPMSG 		= 	O_SQL_EMSG,
					ENDTIME 	= 	SYSDATE
			WHERE	STATGUBUN 	= 	'30(DAY)'
			AND		TARGETTIME 	= 	SUBSTR(I_ROW_DATE,1,8)
			AND 	TARGETTABLE = 	'SKILL_DY'
			AND		EXPROC		= 	'SP_DAY_SKILL'
			AND 	EXP			= 	'ONGOING';
		END;
    ELSE
		--오류가 없는 경우 SUCCESS와 결과 시간을 업데이트 한다.
		BEGIN
			UPDATE	SWM.AX_STAT_HIST
			SET		EXP 		= 	'SUCCESS',
					EXPMSG 		= 	'SUCCESS',
					ENDTIME 	= 	SYSDATE
			WHERE	STATGUBUN 	= 	'30(DAY)'
			AND		TARGETTIME 	= 	SUBSTR(I_ROW_DATE,1,8)
			AND 	TARGETTABLE = 	'SKILL_DY'
			AND		EXPROC		= 	'SP_DAY_SKILL'
			AND 	EXP			= 	'ONGOING';
      	END;
    END IF;

	COMMIT;

END;

CREATE OR REPLACE PROCEDURE SWM.SP_MONTH_SKILL
(
    I_ROW_DATE		IN	VARCHAR2,
    O_SQL_ECODE		OUT	INT,
    O_SQL_EMSG		OUT	VARCHAR2
)
IS

BEGIN
	--ver.0.1
    O_SQL_ECODE := 0; --초기값은 0으로..
    O_SQL_EMSG 	:= 'PROC SP_MONTH_SKILL ONGOING...'; --초기값은 정상 처리된걸로..

	BEGIN
		--데이터를 집계해야 하는경우 STAT_HIST 데이터를 추가 한다.        
		--배치 시작전 기본 데이터를 세팅한다.(집계시작날짜, 집계구분, 집계타겟날짜, 타겟테이블/간략명, 타겟프로시저명, 결과, 결과MSG, 종료날짜)
        INSERT INTO  SWM.AX_STAT_HIST
        (STARTTIME, STATGUBUN, TARGETTIME, TARGETTABLE, EXPROC, EXP, EXPMSG, ENDTIME)
        VALUES	(
					SYSDATE,					--STARTTIME
					'40(MONTH)',				--STATGUBUN
					SUBSTR(I_ROW_DATE,1,6),		--TARGETTIME
					'SKILL_MN',					--TARGETTABLE
					'SP_MONTH_SKILL',			--EXPROC
					'ONGOING',					--EXP
					NULL,						--EXPMSG
					NULL						--ENDTIME
				);

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] STAT_HIST INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리

    END;


	BEGIN
		--집계를 수행해야 하는 경우 순서대로 프로세스를 수행한다.
		--집계대상 시간대에 데이터가 집계되어 있다면 삭제
		DELETE
		FROM  	SWM.SKILL_MN
		WHERE	ROW_DATE = SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2);

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] SKILL_MN DELETE.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


	BEGIN
		INSERT 
		INTO 	SWM.SKILL_MN
		(
			SELECT	SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '-' || SUBSTR(I_ROW_DATE,7,2) AS ROW_DATE,
					DG_DBID 							AS 		DG_DBID,
					DG_CODE								AS 		DG_CODE,
					----QUEUE 최대기준값----
					MAX(N_MAX_AGT) 						AS 		N_MAX_AGT,
					MAX(N_MAX_ACD) 						AS 		N_MAX_ACD,
					MAX(T_MAX_ACD) 						AS 		T_MAX_ACD,
					----QUEUE 분배호----
					SUM(N_DISTRIB) 						AS 		N_DISTRIB,
					SUM(T_DISTRIB) 						AS 		T_DISTRIB,
					----QUEUE 총인입호----
					SUM(N_TOT_ACD_WAIT) 				AS 		N_TOT_ACD_WAIT,
					SUM(T_TOT_ACD_WAIT)					AS 		T_TOT_ACD_WAIT,
					----QUEUE 상담사인입호----
					SUM(N_ENTER_ACD)					AS 		N_ENTER_ACD,
					SUM(T_ENTER_ACD)					AS 		T_ENTER_ACD,
					----QUEUE N초 이상 N초 미만 상담사 응답호----
					SUM(N_WAIT_ACD) 					AS 		N_WAIT_ACD,
					SUM(N_WAIT_ACD_0) 					AS 		N_WAIT_ACD_0,
					SUM(N_WAIT_ACD_1) 					AS 		N_WAIT_ACD_1,
					SUM(N_WAIT_ACD_2) 					AS 		N_WAIT_ACD_2,
					SUM(N_WAIT_ACD_3) 					AS 		N_WAIT_ACD_3,
					SUM(N_WAIT_ACD_4) 					AS 		N_WAIT_ACD_4,
					SUM(N_WAIT_ACD_5) 					AS 		N_WAIT_ACD_5,
					SUM(N_WAIT_ACD_6) 					AS 		N_WAIT_ACD_6,
					SUM(N_WAIT_ACD_7) 					AS 		N_WAIT_ACD_7,
					SUM(N_WAIT_ACD_8) 					AS 		N_WAIT_ACD_8,
					SUM(N_WAIT_ACD_9) 					AS 		N_WAIT_ACD_9,
					SUM(N_WAIT_ACD_10) 					AS 		N_WAIT_ACD_10,
					SUM(N_WAIT_ACD_11) 					AS 		N_WAIT_ACD_11,
					----QUEUE N초 이상 N초미만 상담사 응답 누적 시간----
					SUM(T_WAIT_ACD) 					AS 		T_WAIT_ACD,
					SUM(T_WAIT_ACD_0) 					AS 		T_WAIT_ACD_0,
					SUM(T_WAIT_ACD_1) 					AS 		T_WAIT_ACD_1,
					SUM(T_WAIT_ACD_2) 					AS 		T_WAIT_ACD_2,
					SUM(T_WAIT_ACD_3) 					AS 		T_WAIT_ACD_3,
					SUM(T_WAIT_ACD_4) 					AS 		T_WAIT_ACD_4,
					SUM(T_WAIT_ACD_5) 					AS 		T_WAIT_ACD_5,
					SUM(T_WAIT_ACD_6) 					AS 		T_WAIT_ACD_6,
					SUM(T_WAIT_ACD_7) 					AS 		T_WAIT_ACD_7,
					SUM(T_WAIT_ACD_8) 					AS 		T_WAIT_ACD_8,
					SUM(T_WAIT_ACD_9) 					AS 		T_WAIT_ACD_9,
					SUM(T_WAIT_ACD_10) 					AS 		T_WAIT_ACD_10,
					SUM(T_WAIT_ACD_11) 					AS 		T_WAIT_ACD_11,
					MAX(T_MAX_WAIT_ACD) 				AS 		T_MAX_WAIT_ACD,
					----QUEUE N초 이상 N초 미만 큐 포기호----
					SUM(N_AB_ACD) 						AS 		N_AB_ACD,
					SUM(N_AB_ACD_0) 					AS 		N_AB_ACD_0,
					SUM(N_AB_ACD_1) 					AS 		N_AB_ACD_1,
					SUM(N_AB_ACD_2) 					AS 		N_AB_ACD_2,
					SUM(N_AB_ACD_3) 					AS 		N_AB_ACD_3,
					SUM(N_AB_ACD_4) 					AS 		N_AB_ACD_4,
					SUM(N_AB_ACD_5) 					AS 		N_AB_ACD_5,
					SUM(N_AB_ACD_6) 					AS 		N_AB_ACD_6,
					SUM(N_AB_ACD_7) 					AS 		N_AB_ACD_7,
					SUM(N_AB_ACD_8) 					AS 		N_AB_ACD_8,
					SUM(N_AB_ACD_9) 					AS 		N_AB_ACD_9,
					SUM(N_AB_ACD_10) 					AS 		N_AB_ACD_10,
					SUM(N_AB_ACD_11) 					AS 		N_AB_ACD_11,
					----QUEUE N초 이상 N초미만 큐 포기 누적 시간----
					SUM(T_AB_ACD) 						AS 		T_AB_ACD,
					SUM(T_AB_ACD_0) 					AS 		T_AB_ACD_0,
					SUM(T_AB_ACD_1) 					AS 		T_AB_ACD_1,
					SUM(T_AB_ACD_2) 					AS 		T_AB_ACD_2,
					SUM(T_AB_ACD_3) 					AS 		T_AB_ACD_3,
					SUM(T_AB_ACD_4) 					AS 		T_AB_ACD_4,
					SUM(T_AB_ACD_5) 					AS 		T_AB_ACD_5,
					SUM(T_AB_ACD_6) 					AS 		T_AB_ACD_6,
					SUM(T_AB_ACD_7) 					AS 		T_AB_ACD_7,
					SUM(T_AB_ACD_8) 					AS 		T_AB_ACD_8,
					SUM(T_AB_ACD_9) 					AS 		T_AB_ACD_9,
					SUM(T_AB_ACD_10) 					AS 		T_AB_ACD_10,
					SUM(T_AB_ACD_11) 					AS 		T_AB_ACD_11,
					MAX(T_MAX_AB_ACD) 					AS 		T_MAX_AB_ACD,
					----QUEUE 플로우인 아웃----
					SUM(N_FLOW_IN) 						AS 		N_FLOW_IN,
					SUM(T_FLOW_IN) 						AS 		T_FLOW_IN,
					SUM(N_FLOW_OUT) 					AS 		N_FLOW_OUT,
					SUM(T_FLOW_OUT) 					AS 		T_FLOW_OUT,
					----QUEUE 기타 건수 시간----
					SUM(N_RONA) 						AS 		N_RONA,
					SUM(N_NON_SERVICE) 					AS 		N_NON_SERVICE,
					SUM(T_NON_SERVICE) 					AS 		T_NON_SERVICE,
					MAX(T_MAX_NON_SERVICE) 				AS 		T_MAX_NON_SERVICE,
					----QUEUE 인입기준 건수----
					SUM(N_ENTER) 						AS 		N_ENTER,
					SUM(N_ANSW) 						AS 		N_ANSW,
					SUM(N_ABAN) 						AS 		N_ABAN,
					----AGENT SUMMARY----
					----AGENT 로그인건수, 로그인시간, 최초 로그인, 최종 로그아웃 시각----
					SUM(N_LOGGD_IN) 					AS 		N_LOGGD_IN,
					SUM(N_LOGGD_OUT) 					AS 		N_LOGGD_OUT,
					SUM(T_I_LOGIN) 						AS 		T_I_LOGIN,
					----AGENT 대기건수, 대기시간----
					SUM(N_READY) 						AS 		N_READY,
					SUM(T_I_READY) 						AS 		T_I_READY,
					----AGENT 후처리건수, 후처리시간 (TI - 메인스킬, I - 보유스킬)----
					SUM(N_ACW) 							AS 		N_ACW,
					SUM(T_I_ACW) 						AS 		T_I_ACW,
					SUM(T_I_ACW_IT)						AS 		T_I_ACW_IT,
					SUM(T_I_ACW_IB)						AS 		T_I_ACW_IB,
					SUM(T_I_ACW_OB)						AS 		T_I_ACW_OB,
					SUM(T_I_ACW_CO)						AS 		T_I_ACW_CO,
					----AGENT 이석건수, 이석시간 (TI - 메인스킬, I - 보유스킬)----
					SUM(N_NREADY) 						AS 		N_NREADY,
					SUM(N_NREADY_0) 					AS 		N_NREADY_0,
					SUM(N_NREADY_1) 					AS 		N_NREADY_1,
					SUM(N_NREADY_2) 					AS 		N_NREADY_2,
					SUM(N_NREADY_3) 					AS 		N_NREADY_3,
					SUM(N_NREADY_4) 					AS 		N_NREADY_4,
					SUM(N_NREADY_5) 					AS 		N_NREADY_5,
					SUM(N_NREADY_6) 					AS 		N_NREADY_6,
					SUM(N_NREADY_7) 					AS 		N_NREADY_7,
					SUM(N_NREADY_8) 					AS 		N_NREADY_8,
					SUM(N_NREADY_9) 					AS 		N_NREADY_9,
					SUM(T_I_NREADY) 					AS 		T_I_NREADY,
					SUM(T_I_NREADY_0) 					AS 		T_I_NREADY_0,
					SUM(T_I_NREADY_1) 					AS 		T_I_NREADY_1,
					SUM(T_I_NREADY_2) 					AS 		T_I_NREADY_2,
					SUM(T_I_NREADY_3) 					AS 		T_I_NREADY_3,
					SUM(T_I_NREADY_4) 					AS 		T_I_NREADY_4,
					SUM(T_I_NREADY_5) 					AS 		T_I_NREADY_5,
					SUM(T_I_NREADY_6) 					AS 		T_I_NREADY_6,
					SUM(T_I_NREADY_7) 					AS 		T_I_NREADY_7,
					SUM(T_I_NREADY_8) 					AS 		T_I_NREADY_8,
					SUM(T_I_NREADY_9) 					AS 		T_I_NREADY_9,
					----AGENT 기타시간 (TI - 메인스킬, I - 보유스킬)----
					SUM(T_I_OTHER) 						AS 		T_I_OTHER,
					----AGENT 상담사 벨울림건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_ACD) 					AS 		N_RING_ACD,
					SUM(N_RING_ACD_1) 					AS 		N_RING_ACD_1,
					SUM(N_RING_ACD_2) 					AS 		N_RING_ACD_2,
					SUM(N_RING_ACD_3) 					AS 		N_RING_ACD_3,
					SUM(T_RING_ACD) 					AS 		T_RING_ACD,
					SUM(T_RING_ACD_E) 					AS 		T_RING_ACD_E,
					SUM(T_RING_ACD_1) 					AS 		T_RING_ACD_1,
					SUM(T_RING_ACD_2) 					AS 		T_RING_ACD_2,
					SUM(T_RING_ACD_3) 					AS 		T_RING_ACD_3,
					MAX(T_RING_MAX_ACD) 				AS 		T_RING_MAX_ACD,
					----AGENT 상담사 응답건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_TALK_ACD) 					AS 		N_TALK_ACD,
					SUM(N_TALK_ACD_0)					AS 		N_TALK_ACD_0,
					SUM(N_TALK_ACD_1)					AS 		N_TALK_ACD_1,
					SUM(N_TALK_ACD_2)					AS 		N_TALK_ACD_2,
					SUM(N_TALK_ACD_3)					AS 		N_TALK_ACD_3,
					SUM(N_TALK_ACD_4)					AS 		N_TALK_ACD_4,
					SUM(N_TALK_ACD_5)					AS 		N_TALK_ACD_5,
					SUM(N_TALK_ACD_6)					AS 		N_TALK_ACD_6,
					SUM(N_TALK_ACD_7)					AS 		N_TALK_ACD_7,
					SUM(N_TALK_ACD_8)					AS 		N_TALK_ACD_8,
					SUM(N_TALK_ACD_9)					AS 		N_TALK_ACD_9,
					SUM(N_TALK_ACD_10)					AS 		N_TALK_ACD_10,
					SUM(N_TALK_ACD_11)					AS 		N_TALK_ACD_11,
					SUM(N_TALK_ACD_NOTR)				AS 		N_TALK_ACD_NOTR,
					SUM(T_TALK_ACD) 					AS 		T_TALK_ACD,
					SUM(T_TALK_ACD_E) 					AS 		T_TALK_ACD_E,
					MAX(T_TALK_MAX_ACD) 				AS 		T_TALK_MAX_ACD,
					----AGENT 상담사 벨울림 포기건수, 시간----
					SUM(N_RING_AB_ACD) 					AS 		N_RING_AB_ACD,
					SUM(N_RING_AB_ACD_1) 				AS 		N_RING_AB_ACD_1,
					SUM(N_RING_AB_ACD_2) 				AS 		N_RING_AB_ACD_2,
					SUM(N_RING_AB_ACD_3) 				AS 		N_RING_AB_ACD_3,
					SUM(T_RING_AB_ACD) 					AS 		T_RING_AB_ACD,
					SUM(T_RING_AB_ACD_1) 				AS 		T_RING_AB_ACD_1,
					SUM(T_RING_AB_ACD_2) 				AS 		T_RING_AB_ACD_2,
					SUM(T_RING_AB_ACD_3) 				AS 		T_RING_AB_ACD_3,
					MAX(T_RING_MAX_AB_ACD) 				AS 		T_RING_MAX_AB_ACD,
					----AGENT 상담사 NACD 수신건수, 시간----
					SUM(N_RING_NACD) 					AS 		N_RING_NACD,
					SUM(N_TALK_NACD_IN) 				AS 		N_TALK_NACD_IN,
					SUM(N_RING_AB_NACD) 				AS 		N_RING_AB_NACD,
					SUM(T_RING_NACD) 					AS 		T_RING_NACD,
					SUM(T_TALK_NACD_IN) 				AS 		T_TALK_NACD_IN,
					SUM(T_RING_AB_NACD) 				AS 		T_RING_AB_NACD,
					SUM(N_DIAL_NACD) 					AS 		N_DIAL_NACD,
					SUM(N_TALK_NACD_OUT) 				AS 		N_TALK_NACD_OUT,
					SUM(T_DIAL_NACD) 					AS 		T_DIAL_NACD,
					SUM(T_TALK_NACD_OUT) 				AS 		T_TALK_NACD_OUT,
					----AGENT 상담사 DACD 수신건수, 시간----
					SUM(N_RING_DACD) 					AS 		N_RING_DACD,
					SUM(N_TALK_DACD_IN) 				AS 		N_TALK_DACD_IN,
					SUM(N_RING_AB_DACD) 				AS 		N_RING_AB_DACD,
					SUM(T_RING_DACD) 					AS 		T_RING_DACD,
					SUM(T_TALK_DACD_IN)	 				AS 		T_TALK_DACD_IN,
					SUM(T_RING_AB_DACD) 				AS 		T_RING_AB_DACD,
					SUM(N_DIAL_DACD) 					AS 		N_DIAL_DACD,
					SUM(N_TALK_DACD_OUT) 				AS 		N_TALK_DACD_OUT,
					SUM(T_DIAL_DACD) 					AS 		T_DIAL_DACD,
					SUM(T_TALK_DACD_OUT) 				AS 		T_TALK_DACD_OUT,
					----AGENT 총 보류 건수, 시간----
					SUM(N_HOLD) 						AS 		N_HOLD,
					SUM(N_HOLD_AB) 						AS 		N_HOLD_AB,
					SUM(N_HOLD_ACD) 					AS 		N_HOLD_ACD,
					SUM(N_HOLD_AB_ACD) 					AS 		N_HOLD_AB_ACD,
					SUM(N_HOLD_NACD) 					AS 		N_HOLD_NACD,
					SUM(N_HOLD_AB_NACD) 				AS 		N_HOLD_AB_NACD,
					SUM(T_HOLD) 						AS 		T_HOLD,
					SUM(T_HOLD_ACD) 					AS 		T_HOLD_ACD,
					SUM(T_HOLD_NACD) 					AS 		T_HOLD_NACD,
					----AGENT 콜타입별 보류 건수, 시간----
					SUM(N_HOLD_IT) 						AS 		N_HOLD_IT,
					SUM(N_HOLD_ACD_IT) 					AS 		N_HOLD_ACD_IT,
					SUM(T_HOLD_IT) 						AS 		T_HOLD_IT,
					SUM(T_HOLD_ACD_IT) 					AS 		T_HOLD_ACD_IT,
					SUM(N_HOLD_IB) 						AS 		N_HOLD_IB,
					SUM(N_HOLD_ACD_IB) 					AS 		N_HOLD_ACD_IB,
					SUM(T_HOLD_IB) 						AS 		T_HOLD_IB,
					SUM(T_HOLD_ACD_IB) 					AS 		T_HOLD_ACD_IB,
					SUM(N_HOLD_OB) 						AS 		N_HOLD_OB,
					SUM(N_HOLD_ACD_OB) 					AS 		N_HOLD_ACD_OB,
					SUM(T_HOLD_OB) 						AS 		T_HOLD_OB,
					SUM(T_HOLD_ACD_OB) 					AS 		T_HOLD_ACD_OB,
					SUM(N_HOLD_CO) 						AS 		N_HOLD_CO,
					SUM(N_HOLD_ACD_CO) 					AS 		N_HOLD_ACD_CO,
					SUM(T_HOLD_CO) 						AS 		T_HOLD_CO,
					SUM(T_HOLD_ACD_CO) 					AS 		T_HOLD_ACD_CO,
					----AGENT 1. INTERNAL 수신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_IT) 						AS 		N_RING_IT,
					SUM(N_RING_AB_IT) 					AS 		N_RING_AB_IT,
					SUM(N_RING_IW_TRST_IT) 				AS 		N_RING_IW_TRST_IT,
					SUM(N_TALK_IW_IT) 					AS 		N_TALK_IW_IT,
					SUM(N_TALK_IW_TRST_IT) 				AS 		N_TALK_IW_TRST_IT,
					SUM(N_TALK_IW_TRSM_IT) 				AS 		N_TALK_IW_TRSM_IT,
					SUM(N_TALK_CONFJ_IT) 				AS 		N_TALK_CONFJ_IT,
					SUM(N_TALK_CONFM_IT) 				AS 		N_TALK_CONFM_IT,
					SUM(T_RING_IT) 						AS 		T_RING_IT,
					SUM(T_RING_IT_E) 					AS 		T_RING_IT_E,
					SUM(T_RING_AB_IT) 					AS 		T_RING_AB_IT,
					SUM(T_RING_IW_TRST_IT) 				AS 		T_RING_IW_TRST_IT,
					SUM(T_TALK_IW_IT) 					AS 		T_TALK_IW_IT,
					SUM(T_TALK_IW_IT_E) 				AS 		T_TALK_IW_IT_E,
					SUM(T_TALK_IW_TRST_IT) 				AS 		T_TALK_IW_TRST_IT,
					SUM(T_TALK_IW_TRSM_IT) 				AS 		T_TALK_IW_TRSM_IT,
					SUM(T_TALK_CONFJ_IT) 				AS 		T_TALK_CONFJ_IT,
					SUM(T_TALK_CONFM_IT) 				AS 		T_TALK_CONFM_IT,
					----AGENT 1. INTERNAL ACD 수신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_IT_ACD) 					AS 		N_RING_IT_ACD,
					SUM(N_RING_AB_IT_ACD) 				AS 		N_RING_AB_IT_ACD,
					SUM(N_RING_IW_TRST_IT_ACD) 			AS 		N_RING_IW_TRST_IT_ACD,
					SUM(N_TALK_IW_IT_ACD) 				AS 		N_TALK_IW_IT_ACD,
					SUM(N_TALK_IW_TRST_IT_ACD) 			AS 		N_TALK_IW_TRST_IT_ACD,
					SUM(N_TALK_IW_TRSM_IT_ACD) 			AS 		N_TALK_IW_TRSM_IT_ACD,
					SUM(N_TALK_CONFJ_IT_ACD) 			AS 		N_TALK_CONFJ_IT_ACD,
					SUM(N_TALK_CONFM_IT_ACD) 			AS 		N_TALK_CONFM_IT_ACD,
					SUM(T_RING_IT_ACD) 					AS 		T_RING_IT_ACD,
					SUM(T_RING_IT_ACD_E) 				AS 		T_RING_IT_ACD_E,
					SUM(T_RING_AB_IT_ACD) 				AS 		T_RING_AB_IT_ACD,
					SUM(T_RING_IW_TRST_IT_ACD) 			AS 		T_RING_IW_TRST_IT_ACD,
					SUM(T_TALK_IW_IT_ACD) 				AS 		T_TALK_IW_IT_ACD,
					SUM(T_TALK_IW_IT_ACD_E) 			AS 		T_TALK_IW_IT_ACD_E,
					SUM(T_TALK_IW_TRST_IT_ACD) 			AS 		T_TALK_IW_TRST_IT_ACD,
					SUM(T_TALK_IW_TRSM_IT_ACD) 			AS 		T_TALK_IW_TRSM_IT_ACD,
					SUM(T_TALK_CONFJ_IT_ACD) 			AS 		T_TALK_CONFJ_IT_ACD,
					SUM(T_TALK_CONFM_IT_ACD) 			AS 		T_TALK_CONFM_IT_ACD,
					----AGENT 1. INTERNAL 발신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_DIAL_IT) 						AS 		N_DIAL_IT,
					SUM(N_TALK_OW_IT) 					AS 		N_TALK_OW_IT,
					SUM(N_TALK_OW_TRSM_IT) 				AS 		N_TALK_OW_TRSM_IT,
					SUM(T_DIAL_IT) 						AS 		T_DIAL_IT,
					SUM(T_DIAL_IT_E) 					AS 		T_DIAL_IT_E,
					SUM(T_TALK_OW_IT) 					AS 		T_TALK_OW_IT,
					SUM(T_TALK_OW_IT_E) 				AS 		T_TALK_OW_IT_E,
					SUM(T_TALK_OW_TRSM_IT) 				AS 		T_TALK_OW_TRSM_IT,
					----AGENT 2. INBOUND 수신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_IB) 						AS 		N_RING_IB,
					SUM(N_RING_AB_IB) 					AS 		N_RING_AB_IB,
					SUM(N_RING_IW_TRST_IB) 				AS 		N_RING_IW_TRST_IB,
					SUM(N_TALK_IW_TRST_IB) 				AS 		N_TALK_IW_TRST_IB,
					SUM(N_TALK_IW_IB) 					AS 		N_TALK_IW_IB,
					SUM(N_TALK_IW_TRSM_IB) 				AS 		N_TALK_IW_TRSM_IB,
					SUM(N_TALK_CONFJ_IB) 				AS 		N_TALK_CONFJ_IB,
					SUM(N_TALK_CONFM_IB) 				AS 		N_TALK_CONFM_IB,
					SUM(T_RING_IB) 						AS 		T_RING_IB,
					SUM(T_RING_IB_E) 					AS 		T_RING_IB_E,
					SUM(T_RING_AB_IB) 					AS 		T_RING_AB_IB,
					SUM(T_RING_IW_TRST_IB) 				AS 		T_RING_IW_TRST_IB,
					SUM(T_TALK_IW_IB) 					AS 		T_TALK_IW_IB,
					SUM(T_TALK_IW_IB_E)					AS 		T_TALK_IW_IB_E,
					SUM(T_TALK_IW_TRST_IB) 				AS 		T_TALK_IW_TRST_IB,
					SUM(T_TALK_IW_TRSM_IB) 				AS 		T_TALK_IW_TRSM_IB,
					SUM(T_TALK_CONFJ_IB) 				AS 		T_TALK_CONFJ_IB,
					SUM(T_TALK_CONFM_IB) 				AS 		T_TALK_CONFM_IB,
					----AGENT 2. INBOUND ACD 수신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_IB_ACD) 					AS 		N_RING_IB_ACD,
					SUM(N_RING_AB_IB_ACD) 				AS 		N_RING_AB_IB_ACD,
					SUM(N_RING_IW_TRST_IB_ACD) 			AS 		N_RING_IW_TRST_IB_ACD,
					SUM(N_TALK_IW_IB_ACD) 				AS 		N_TALK_IW_IB_ACD,
					SUM(N_TALK_IW_TRST_IB_ACD) 			AS 		N_TALK_IW_TRST_IB_ACD,
					SUM(N_TALK_IW_TRSM_IB_ACD) 			AS 		N_TALK_IW_TRSM_IB_ACD,
					SUM(N_TALK_CONFJ_IB_ACD) 			AS 		N_TALK_CONFJ_IB_ACD,
					SUM(N_TALK_CONFM_IB_ACD) 			AS 		N_TALK_CONFM_IB_ACD,
					SUM(T_RING_IB_ACD) 					AS 		T_RING_IB_ACD,
					SUM(T_RING_IB_ACD_E) 				AS 		T_RING_IB_ACD_E,
					SUM(T_RING_AB_IB_ACD) 				AS 		T_RING_AB_IB_ACD,
					SUM(T_RING_IW_TRST_IB_ACD) 			AS 		T_RING_IW_TRST_IB_ACD,
					SUM(T_TALK_IW_IB_ACD) 				AS 		T_TALK_IW_IB_ACD,
					SUM(T_TALK_IW_IB_ACD_E) 			AS 		T_TALK_IW_IB_ACD_E,
					SUM(T_TALK_IW_TRST_IB_ACD) 			AS 		T_TALK_IW_TRST_IB_ACD,
					SUM(T_TALK_IW_TRSM_IB_ACD) 			AS 		T_TALK_IW_TRSM_IB_ACD,
					SUM(T_TALK_CONFJ_IB_ACD) 			AS 		T_TALK_CONFJ_IB_ACD,
					SUM(T_TALK_CONFM_IB_ACD) 			AS 		T_TALK_CONFM_IB_ACD,
					----AGENT 3. OUTBOUND 수신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_OB) 						AS 		N_RING_OB,
					SUM(N_RING_AB_OB) 					AS 		N_RING_AB_OB,
					SUM(N_RING_IW_TRST_OB) 				AS 		N_RING_IW_TRST_OB,
					SUM(N_TALK_IW_OB) 					AS 		N_TALK_IW_OB,
					SUM(N_TALK_IW_TRST_OB) 				AS 		N_TALK_IW_TRST_OB,
					SUM(N_TALK_IW_TRSM_OB) 				AS 		N_TALK_IW_TRSM_OB,
					SUM(N_TALK_CONFJ_OB) 				AS 		N_TALK_CONFJ_OB,
					SUM(N_TALK_CONFM_OB) 				AS 		N_TALK_CONFM_OB,
					SUM(T_RING_OB) 						AS 		T_RING_OB,
					SUM(T_RING_OB_E) 					AS 		T_RING_OB_E,
					SUM(T_RING_AB_OB) 					AS 		T_RING_AB_OB,
					SUM(T_RING_IW_TRST_OB) 				AS 		T_RING_IW_TRST_OB,
					SUM(T_TALK_IW_OB) 					AS 		T_TALK_IW_OB,
					SUM(T_TALK_IW_OB_E) 				AS 		T_TALK_IW_OB_E,
					SUM(T_TALK_IW_TRST_OB) 				AS 		T_TALK_IW_TRST_OB,
					SUM(T_TALK_IW_TRSM_OB) 				AS 		T_TALK_IW_TRSM_OB,
					SUM(T_TALK_CONFJ_OB) 				AS 		T_TALK_CONFJ_OB,
					SUM(T_TALK_CONFM_OB) 				AS 		T_TALK_CONFM_OB,
					----AGENT 3. OUTBOUND ACD 수신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_OB_ACD) 					AS 		N_RING_OB_ACD,
					SUM(N_RING_AB_OB_ACD) 				AS 		N_RING_AB_OB_ACD,
					SUM(N_RING_IW_TRST_OB_ACD) 			AS 		N_RING_IW_TRST_OB_ACD,
					SUM(N_TALK_IW_OB_ACD) 				AS 		N_TALK_IW_OB_ACD,
					SUM(N_TALK_IW_TRST_OB_ACD) 			AS 		N_TALK_IW_TRST_OB_ACD,
					SUM(N_TALK_IW_TRSM_OB_ACD) 			AS 		N_TALK_IW_TRSM_OB_ACD,
					SUM(N_TALK_CONFJ_OB_ACD) 			AS 		N_TALK_CONFJ_OB_ACD,
					SUM(N_TALK_CONFM_OB_ACD) 			AS 		N_TALK_CONFM_OB_ACD,					
					SUM(T_RING_OB_ACD) 					AS 		T_RING_OB_ACD,
					SUM(T_RING_OB_ACD_E) 				AS 		T_RING_OB_ACD_E,
					SUM(T_RING_AB_OB_ACD) 				AS 		T_RING_AB_OB_ACD,
					SUM(T_RING_IW_TRST_OB_ACD) 			AS 		T_RING_IW_TRST_OB_ACD,
					SUM(T_TALK_IW_OB_ACD) 				AS 		T_TALK_IW_OB_ACD,
					SUM(T_TALK_IW_OB_ACD_E) 			AS 		T_TALK_IW_OB_ACD_E,
					SUM(T_TALK_IW_TRST_OB_ACD) 			AS 		T_TALK_IW_TRST_OB_ACD,
					SUM(T_TALK_IW_TRSM_OB_ACD) 			AS 		T_TALK_IW_TRSM_OB_ACD,
					SUM(T_TALK_CONFJ_OB_ACD) 			AS 		T_TALK_CONFJ_OB_ACD,
					SUM(T_TALK_CONFM_OB_ACD) 			AS 		T_TALK_CONFM_OB_ACD,
					----AGENT 3. OUTBOUND 발신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_DIAL_OB) 						AS 		N_DIAL_OB,
					SUM(N_TALK_OW_OB) 					AS 		N_TALK_OW_OB,
					SUM(N_TALK_OW_TRSM_OB) 				AS 		N_TALK_OW_TRSM_OB,
					SUM(T_DIAL_OB) 						AS 		T_DIAL_OB,
					SUM(T_DIAL_OB_E) 					AS 		T_DIAL_OB_E,
					SUM(T_TALK_OW_OB) 					AS 		T_TALK_OW_OB,
					SUM(T_TALK_OW_OB_E) 				AS 		T_TALK_OW_OB_E,
					SUM(T_TALK_OW_TRSM_OB) 				AS 		T_TALK_OW_TRSM_OB,
					----AGENT 4. CONSULT 수신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_CO) 						AS 		N_RING_CO,
					SUM(N_RING_AB_CO) 					AS 		N_RING_AB_CO,
					SUM(N_RING_IW_TRST_CO) 				AS 		N_RING_IW_TRST_CO,
					SUM(N_TALK_IW_CO) 					AS 		N_TALK_IW_CO,
					SUM(N_TALK_IW_TRST_CO) 				AS 		N_TALK_IW_TRST_CO,
					SUM(N_TALK_IW_TRSM_CO) 				AS 		N_TALK_IW_TRSM_CO,
					SUM(N_TALK_CONFJ_CO)	 			AS 		N_TALK_CONFJ_CO,
					SUM(N_TALK_CONFM_CO)	 			AS 		N_TALK_CONFM_CO,
					SUM(T_RING_CO) 						AS 		T_RING_CO,
					SUM(T_RING_CO_E) 					AS 		T_RING_CO_E,
					SUM(T_RING_AB_CO) 					AS 		T_RING_AB_CO,
					SUM(T_RING_IW_TRST_CO) 				AS 		T_RING_IW_TRST_CO,
					SUM(T_TALK_IW_CO) 					AS 		T_TALK_IW_CO,
					SUM(T_TALK_IW_CO_E) 				AS 		T_TALK_IW_CO_E,
					SUM(T_TALK_IW_TRST_CO) 				AS 		T_TALK_IW_TRST_CO,
					SUM(T_TALK_IW_TRSM_CO) 				AS 		T_TALK_IW_TRSM_CO,
					SUM(T_TALK_CONFJ_CO) 				AS 		T_TALK_CONFJ_CO,
					SUM(T_TALK_CONFM_CO) 				AS 		T_TALK_CONFM_CO,
					----AGENT 4. CONSULT ACD 수신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_RING_CO_ACD) 					AS 		N_RING_CO_ACD,
					SUM(N_RING_AB_CO_ACD) 				AS 		N_RING_AB_CO_ACD,
					SUM(N_RING_IW_TRST_CO_ACD) 			AS 		N_RING_IW_TRST_CO_ACD,
					SUM(N_TALK_IW_CO_ACD) 				AS 		N_TALK_IW_CO_ACD,
					SUM(N_TALK_IW_TRST_CO_ACD) 			AS 		N_TALK_IW_TRST_CO_ACD,
					SUM(N_TALK_IW_TRSM_CO_ACD) 			AS 		N_TALK_IW_TRSM_CO_ACD,
					SUM(N_TALK_CONFJ_CO_ACD) 			AS 		N_TALK_CONFJ_CO_ACD,
					SUM(N_TALK_CONFM_CO_ACD) 			AS 		N_TALK_CONFM_CO_ACD,
					SUM(T_RING_CO_ACD) 					AS 		T_RING_CO_ACD,
					SUM(T_RING_CO_ACD_E) 				AS 		T_RING_CO_ACD_E,
					SUM(T_RING_AB_CO_ACD) 				AS 		T_RING_AB_CO_ACD,
					SUM(T_RING_IW_TRST_CO_ACD) 			AS 		T_RING_IW_TRST_CO_ACD,
					SUM(T_TALK_IW_CO_ACD) 				AS 		T_TALK_IW_CO_ACD,
					SUM(T_TALK_IW_CO_ACD_E) 			AS 		T_TALK_IW_CO_ACD_E,
					SUM(T_TALK_IW_TRST_CO_ACD) 			AS 		T_TALK_IW_TRST_CO_ACD,
					SUM(T_TALK_IW_TRSM_CO_ACD) 			AS 		T_TALK_IW_TRSM_CO_ACD,
					SUM(T_TALK_CONFJ_CO_ACD) 			AS 		T_TALK_CONFJ_CO_ACD,
					SUM(T_TALK_CONFM_CO_ACD) 			AS 		T_TALK_CONFM_CO_ACD,
					----AGENT 4. CONSULT 발신 건수, 시간 (_E(없음) - INTERVAL, _E(있음) - 호 종료)----
					SUM(N_DIAL_CO) 						AS 		N_DIAL_CO,
					SUM(N_TALK_OW_CO) 					AS 		N_TALK_OW_CO,
					SUM(N_TALK_OW_TRSM_CO) 				AS 		N_TALK_OW_TRSM_CO,
					SUM(T_DIAL_CO) 						AS 		T_DIAL_CO,
					SUM(T_DIAL_CO_E) 					AS 		T_DIAL_CO_E,
					SUM(T_TALK_OW_CO) 					AS 		T_TALK_OW_CO,
					SUM(T_TALK_OW_CO_E) 				AS 		T_TALK_OW_CO_E,
					SUM(T_TALK_OW_TRSM_CO) 				AS 		T_TALK_OW_TRSM_CO,
					----AGENT 전환호----
					SUM(N_TRANS) 						AS 		N_TRANS,
					SUM(N_TRANS_TRST) 					AS 		N_TRANS_TRST,
					SUM(N_TRANS_TRSM)	 				AS 		N_TRANS_TRSM,
					----AGENT 회의호----
					SUM(N_CONF) 						AS 		N_CONF,
					SUM(N_CONF_CONFJ) 					AS 		N_CONF_CONFJ,
					SUM(N_CONF_CONFM) 					AS 		N_CONF_CONFM,
					SUM(T_CONF) 						AS 		T_CONF,
					SUM(T_CONF_CONFJ) 					AS 		T_CONF_CONFJ,
					SUM(T_CONF_CONFM) 					AS 		T_CONF_CONFM,
					----SKILL 여분 필드---
					SUM(ETC1) 							AS 		ETC1,
					SUM(ETC2) 							AS 		ETC2,
					SUM(ETC3) 							AS 		ETC3,
					SUM(ETC4) 							AS 		ETC4,
					SUM(ETC5) 							AS 		ETC5
			FROM	SWM.SKILL_DY
			WHERE	ROW_DATE LIKE SUBSTR(I_ROW_DATE,1,4) || '-' || SUBSTR(I_ROW_DATE,5,2) || '%'
					AND ROW_WEEK NOT IN ('토요일','일요일')					
			GROUP BY DG_DBID, DG_CODE
		);

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] SKILL_MN INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


 	IF	O_SQL_ECODE  <>  0  THEN
		--오류가 발생한 경우 FAIL과 오류코드 및 시간을 업데이트 한다.
		BEGIN
			UPDATE	SWM.AX_STAT_HIST
			SET		EXP 		= 	'FAIL',
					EXPMSG 		= 	O_SQL_EMSG,
					ENDTIME 	= 	SYSDATE
			WHERE	STATGUBUN 	= 	'40(MONTH)'
			AND		TARGETTIME 	= 	SUBSTR(I_ROW_DATE,1,6)
			AND 	TARGETTABLE = 	'SKILL_MN'
			AND		EXPROC		= 	'SP_MONTH_SKILL'
			AND 	EXP			= 	'ONGOING';
		END;
    ELSE
		--오류가 없는 경우 SUCCESS와 결과 시간을 업데이트 한다.
		BEGIN
			UPDATE	SWM.AX_STAT_HIST
			SET		EXP 		= 	'SUCCESS',
					EXPMSG 		= 	'SUCCESS',
					ENDTIME 	= 	SYSDATE
			WHERE	STATGUBUN 	= 	'40(MONTH)'
			AND		TARGETTIME 	= 	SUBSTR(I_ROW_DATE,1,6)
			AND 	TARGETTABLE = 	'SKILL_MN'
			AND		EXPROC		= 	'SP_MONTH_SKILL'
			AND 	EXP			= 	'ONGOING';
      	END;
    END IF;

	COMMIT;

END;

CREATE OR REPLACE PROCEDURE SWM.SP_SWB_SW_AGGJOB
(
    I_ROW_SDATE		IN	VARCHAR2,
	I_ROW_EDATE		IN	VARCHAR2,
    O_SQL_ECODE		OUT	INT,
    O_SQL_EMSG		OUT	VARCHAR2
)
IS

--UTC TIME으로 변경
UTC_ROW_SDATE NUMBER;
UTC_ROW_EDATE NUMBER;

BEGIN
	--ver.0.1
    O_SQL_ECODE := 0; --초기값은 0으로..
    O_SQL_EMSG 	:= 'PROC SP_SWB_SW_AGGJOB ONGOING...'; --초기값은 정상 처리된걸로..

	BEGIN
		--데이터를 집계해야 하는경우 STAT_HIST 데이터를 추가 한다.        
		--배치 시작전 기본 데이터를 세팅한다.(집계시작날짜, 집계구분, 집계타겟날짜, 타겟테이블/간략명, 타겟프로시저명, 결과, 결과MSG, 종료날짜)
        INSERT INTO  SWM.AX_STAT_HIST
        (STARTTIME, STATGUBUN, TARGETTIME, TARGETTABLE, EXPROC, EXP, EXPMSG, ENDTIME)
        VALUES	(
					SYSDATE,					--STARTTIME
					'SWB->SW',				--STATGUBUN
					SUBSTR(I_ROW_SDATE,1,14),	--TARGETTIME
					'WINK',						--TARGETTABLE
					'SP_SWB_SW_AGGJOB',	--EXPROC
					'ONGOING',					--EXP
					NULL,						--EXPMSG
					NULL						--ENDTIME
				);

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG	:= 	'[ERROR] STAT_HIST INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리

    END;


	--변환값을 변수에 담는다 20240401000000 -> 1711897200 형식으로 변환
	BEGIN
		SELECT SW.K2U(I_ROW_SDATE) INTO UTC_ROW_SDATE FROM dual;
		
		SELECT SW.K2U(I_ROW_EDATE) INTO UTC_ROW_EDATE FROM dual;
	END;


	--CALLINFO DELETE
	BEGIN
		DELETE
		FROM	SW.CALLINFO
		WHERE 	ETIMETS > UTC_ROW_SDATE AND ETIMETS <= UTC_ROW_EDATE;
		
		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] CALLINFO DELETE.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


	--CALLINFO INSERT
	BEGIN
		INSERT 
		INTO 	SW.CALLINFO
		(		ID           ,
				CONNID       ,
				CONNIDMTS    ,
				CONNIDTRS    ,
				CONNIDFST    ,
				UUID         ,
				UCID         ,
				CALLID       ,
				SEVENT       ,
				TEVENT       ,
				CALLTYPE     ,
				CALLTYPELOC  ,
				CALLTYPEPRP  ,
				CALLTYPEEXT  ,
				SWITCHID     ,
				THISDN       ,
				OTHERDN      ,
				THISTRUNK    ,
				DNIS         ,
				ANI          ,
				LASTID       ,
				LASTPERSON   ,
				LASTDN       ,
				ABANDONED    ,
				TALKED       ,
				CONFERRED    ,
				CONFERMADE   ,
				TRANSFERRED  ,
				TRANSFERMADE ,
				REDIRECTED   ,
				FORWARDED    ,
				RING         ,
				DIAL         ,
				HOLD         ,
				QUEUED       ,
				DIVERTED     ,
				ACW          ,
				TRING        ,
				TDIAL        ,
				TTALK        ,
				TDIALHOLD    ,
				TTALKHOLD    ,
				TQUEUED      ,
				TVQUEUED     ,
				TACW         ,
				RPARTY       ,
				STIME        ,
				TTIME        ,
				STIMETS      ,
				TTIMETS      ,
				ETIMETS      ,
				IRING        ,
				ITALKED      ,
				ICONFERRED   ,
				ITRANSFERRED ,
				ITTALK
		)
		SELECT	ID           ,
				CONNID       ,
				CONNIDMTS    ,
				CONNIDTRS    ,
				CONNIDFST    ,
				UUID         ,
				'' UCID      ,
				CALLID       ,
				SEVENT       ,
				TEVENT       ,
				CALLTYPE     ,
				CALLTYPELOC  ,
				CALLTYPEPRP  ,
				CALLTYPEEXT  ,
				SWITCHID     ,
				THISDN       ,
				OTHERDN      ,
				THISTRUNK    ,
				DNIS         ,
				ANI          ,
				LASTID       ,
				LASTPERSON   ,
				LASTDN       ,
				ABANDONED    ,
				TALKED       ,
				CONFERRED    ,
				CONFERMADE   ,
				TRANSFERRED  ,
				TRANSFERMADE ,
				REDIRECTED   ,
				FORWARDED    ,
				RING         ,
				DIAL         ,
				HOLD         ,
				QUEUED       ,
				DIVERTED     ,
				ACW          ,
				TRING        ,
				TDIAL        ,
				TTALK        ,
				TDIALHOLD    ,
				TTALKHOLD    ,
				TQUEUED      ,
				TVQUEUED     ,
				TACW         ,
				RPARTY       ,
				STIME        ,
				TTIME        ,
				STIMETS      ,
				TTIMETS      ,
				ETIMETS      ,
				IRING        ,
				ITALKED      ,
				ICONFERRED   ,
				ITRANSFERRED ,
				ITTALK
		FROM	SWB.CALLINFO
		WHERE 	ETIMETS > UTC_ROW_SDATE AND ETIMETS <= UTC_ROW_EDATE;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] CALLINFO INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


	--CALLDETAIL DELETE
	BEGIN
		DELETE
		FROM	SW.CALLDETAIL
		WHERE 	ETIMETS > UTC_ROW_SDATE AND ETIMETS <= UTC_ROW_EDATE;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] CALLDETAIL DELETE.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


	--CALLDETAIL INSERT
	BEGIN
		INSERT 
		INTO 	SW.CALLDETAIL
		(		ID             ,
				SEQ            ,
				CALLSEQ        ,
				EVENTSEQ       ,
				TIME           ,
				TIMETS         ,
				TIMEUTS        ,
				ETIMETS        ,
				CONNID         ,
				CONNIDPRE      ,
				CONNIDTRS      ,
				CONNIDORG      ,
				CONNIDMTS      ,
				CALLID         ,
				CALLTYPE       ,
				CALLTYPELOC    ,
				CALLTYPEPRP    ,
				CALLTYPEEXT    ,
				CALLSTATE      ,
				EVENT          ,
				SWITCHID       ,
				AGENTID        ,
				PERSON         ,
				THISDN         ,
				OTHERDN        ,
				THIRDDN        ,
				THISROLE       ,
				OTHERROLE      ,
				THIRDROLE      ,
				THISQUEUE      ,
				OTHERQUEUE     ,
				THIRDQUEUE     ,
				THISTRUNK      ,
				OTHERTRUNK     ,
				THIRDTRUNK     ,
				DNIS           ,
				ANI            ,
				RPARTY         ,
				NETWORKCALLID  ,
				NETWORKROLE    ,
				NETWORKDEST    ,
				FIRSTTRSDN     ,
				FIRSTTRSCONNID ,
				FIRSTTRSLOC    ,
				LASTTRSDN      ,
				LASTTRSCONNID  ,
				LASTTRSLOC
		)
		SELECT 	ID             ,
				SEQ            ,
				CALLSEQ        ,
				EVENTSEQ       ,
				TIME           ,
				TIMETS         ,
				TIMEUTS        ,
				ETIMETS        ,
				CONNID         ,
				CONNIDPRE      ,
				CONNIDTRS      ,
				CONNIDORG      ,
				CONNIDMTS      ,
				CALLID         ,
				CALLTYPE       ,
				CALLTYPELOC    ,
				CALLTYPEPRP    ,
				CALLTYPEEXT    ,
				CALLSTATE      ,
				EVENT          ,
				SWITCHID       ,
				AGENTID        ,
				PERSON         ,
				THISDN         ,
				OTHERDN        ,
				THIRDDN        ,
				THISROLE       ,
				OTHERROLE      ,
				THIRDROLE      ,
				THISQUEUE      ,
				OTHERQUEUE     ,
				THIRDQUEUE     ,
				THISTRUNK      ,
				OTHERTRUNK     ,
				THIRDTRUNK     ,
				DNIS           ,
				ANI            ,
				RPARTY         ,
				NETWORKCALLID  ,
				NETWORKROLE    ,
				NETWORKDEST    ,
				FIRSTTRSDN     ,
				FIRSTTRSCONNID ,
				FIRSTTRSLOC    ,
				LASTTRSDN      ,
				LASTTRSCONNID  ,
				LASTTRSLOC
		FROM	SWB.CALLDETAIL
		WHERE	ETIMETS > UTC_ROW_SDATE AND ETIMETS <= UTC_ROW_EDATE;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] CALLDETAIL INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


	--CALLSTAT DELETE
	BEGIN
		DELETE 
		FROM 	SW.CALLSTAT
		WHERE 	ETIMETS > UTC_ROW_SDATE AND ETIMETS <= UTC_ROW_EDATE;
		
		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] CALLSTAT DELETE.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


	--CALLSTAT INSERT	
	BEGIN
		INSERT 
		INTO 	SW.CALLSTAT 
		(		ID            ,
				SEQ           ,
				STATSEQ       ,
				CONNID        ,
				QDID          ,
				PCID          ,
				THISDN        ,
				AGENTID       ,
				SWITCHID      ,
				PERSON        ,
				CALLTYPE      ,
				CALLTYPELOC   ,
				CALLTYPEPRP   ,
				CALLTYPEEXT   ,
				ROLE          ,
				FLOWIN        ,
				FLOWOUT       ,
				SCALLSTATE    ,
				TCALLSTATE    ,
				QUEUE1        ,
				QUEUE2        ,
				QUEUEV        ,
				QUEUEO        ,
				SKILL         ,
				SKILLO        ,
				DACD          ,
				NWAITS        ,
				NWAITT        ,
				NRING         ,
				NDIAL         ,
				NTALK         ,
				NDIALHOLD     ,
				NTALKHOLD     ,
				NQUEUE        ,
				NACW          ,
				NCONF         ,
				NHOLDAB       ,
				TRING         ,
				TDIAL         ,
				TTALK         ,
				TDIALHOLD     ,
				TTALKHOLD     ,
				TRINGCONF     ,
				TDIALCONF     ,
				TTALKCONF     ,
				TDIALHOLDCONF ,
				TTALKHOLDCONF ,
				TQUEUE        ,
				TACW          ,
				THOLDAB       ,
				SEVENT        ,
				TEVENT        ,
				RPARTY        ,
				EXTRNL        ,
				BUSY          ,
				RTRQ          ,
				STIME         ,
				TTIME         ,
				STIMETS       ,
				TTIMETS       ,
				ETIMETS       ,
				DNTYPE        ,
				NTRS          ,
				STATUS        ,
				WORKMODE      ,
				REASONCODE    ,
				ORG           ,
				SROLE         ,
				TROLE         ,
				STATE         ,
				OTHERDN       ,
				SWITCHIDQ1    ,
				SWITCHIDQ2    ,
				SWITCHIDQV
		)
		SELECT	ID            ,
				SEQ           ,
				STATSEQ       ,
				CONNID        ,
				QDID          ,
				PCID          ,
				THISDN        ,
				AGENTID       ,
				SWITCHID      ,
				PERSON        ,
				CALLTYPE      ,
				CALLTYPELOC   ,
				CALLTYPEPRP   ,
				CALLTYPEEXT   ,
				ROLE          ,
				FLOWIN        ,
				FLOWOUT       ,
				SCALLSTATE    ,
				TCALLSTATE    ,
				QUEUE1        ,
				QUEUE2        ,
				QUEUEV        ,
				QUEUEO        ,
				SKILL         ,
				SKILLO        ,
				DACD          ,
				NWAITS        ,
				NWAITT        ,
				NRING         ,
				NDIAL         ,
				NTALK         ,
				NDIALHOLD     ,
				NTALKHOLD     ,
				NQUEUE        ,
				NACW          ,
				NCONF         ,
				NHOLDAB       ,
				TRING         ,
				TDIAL         ,
				TTALK         ,
				TDIALHOLD     ,
				TTALKHOLD     ,
				TRINGCONF     ,
				TDIALCONF     ,
				TTALKCONF     ,
				TDIALHOLDCONF ,
				TTALKHOLDCONF ,
				TQUEUE        ,
				TACW          ,
				THOLDAB       ,
				SEVENT        ,
				TEVENT        ,
				RPARTY        ,
				EXTRNL        ,
				BUSY          ,
				RTRQ          ,
				STIME         ,
				TTIME         ,
				STIMETS       ,
				TTIMETS       ,
				ETIMETS       ,
				DNTYPE        ,
				NTRS          ,
				STATUS        ,
				WORKMODE      ,
				REASONCODE    ,
				ORG           ,
				SROLE         ,
				TROLE         ,
				STATE         ,
				OTHERDN       ,
				SWITCHIDQ1    ,
				SWITCHIDQ2    ,
				SWITCHIDQV
		FROM 	SWB.CALLSTAT
		WHERE	ETIMETS > UTC_ROW_SDATE AND ETIMETS <= UTC_ROW_EDATE;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] CALLSTAT INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


	--USERDATA DELETE
	BEGIN
		DELETE
		FROM 	SW.USERDATA
		WHERE 	ETIMETS > UTC_ROW_SDATE AND ETIMETS <= UTC_ROW_EDATE;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] USERDATA DELETE.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


	--USERDATA INSERT
	BEGIN
		INSERT 
		INTO SW.USERDATA
		(		ID      ,
				TIME    ,
				TIMETS  ,
				ETIMETS ,
				CONNID  ,
				SEQ     ,
				CALLSEQ ,
				THISDN  ,
				SWITCHID,
				EVENT   ,
				DATA1   ,
				DATA2   ,
				DATA3
		)
		SELECT 	ID      ,
				TIME    ,
				TIMETS  ,
				ETIMETS ,
				CONNID  ,
				SEQ     ,
				CALLSEQ ,
				THISDN  ,
				SWITCHID,
				EVENT   ,
				DATA1   ,
				DATA2   ,
				DATA3
		FROM 	SWB.USERDATA
		WHERE	ETIMETS > UTC_ROW_SDATE AND ETIMETS <= UTC_ROW_EDATE;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] USERDATA INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


	--QUEUESTATE DELETE
	BEGIN
		DELETE
		FROM 	SW.QUEUESTATE
		WHERE 	TIME > UTC_ROW_SDATE AND TIME <= UTC_ROW_EDATE;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] QUEUESTATE DELETE.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


	--QUEUESTATE INSERT
	BEGIN
		INSERT 
		INTO SW.QUEUESTATE
		(		TIME      ,
				THISDN    ,
				DBID      ,
				SWITCHID  ,
				DNTYPE    ,
				NWAIT     ,
				NWAITMIN  ,
				NWAITMAX  ,
				TWAITMIN  ,
				TWAITMAX  ,
				NLOGIN    ,
				NLOGINMIN ,
				NLOGINMAX ,
				NENTER    ,
				NANSW     ,
				NABAN     ,
				NANSL1    ,
				NANSL2    ,
                NANSL3    ,
                NTALK     ,
                NTALKMIN  ,
                NTALKMAX
		)
		SELECT	TIME      ,
				THISDN    ,
				DBID      ,
				SWITCHID  ,
				DNTYPE    ,
				NWAIT     ,
				NWAITMIN  ,
				NWAITMAX  ,
				TWAITMIN  ,
				TWAITMAX  ,
				NLOGIN    ,
				NLOGINMIN ,
				NLOGINMAX ,
				NENTER    ,
				NANSW     ,
				NABAN     ,
				NANSL1    ,
				NANSL2    ,
                NANSL3    ,
                NTALK     ,
                NTALKMIN  ,
                NTALKMAX
		FROM 	SWB.QUEUESTATE
		WHERE 	TIME > UTC_ROW_SDATE AND TIME <= UTC_ROW_EDATE;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] QUEUESTATE INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;
	

	--AGENTLOGIN DELETE
	BEGIN
		DELETE
		FROM 	SW.AGENTLOGIN
		WHERE 	LOGINTIMETS > UTC_ROW_SDATE AND LOGINTIMETS <= UTC_ROW_EDATE;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] AGENTLOGIN DELETE.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


	--AGENTLOGIN INSERT
	BEGIN
		INSERT 
		INTO 	SW.AGENTLOGIN
		(		ID           ,
				SEQ          ,
				AGENTID      ,
				PERSON       ,
				THISDN       ,
				THISQUEUE    ,
				SKILL        ,
				SWITCHID     ,
				LOGINTIME    ,
				LOGINTIMETS  ,
				LOGOUTTIME   ,
				LOGOUTTIMETS
		)
		SELECT 	ID           ,
				SEQ          ,
				AGENTID      ,
				PERSON       ,
				THISDN       ,
				THISQUEUE    ,
				SKILL        ,
				SWITCHID     ,
				LOGINTIME    ,
				LOGINTIMETS  ,
				LOGOUTTIME   ,
				LOGOUTTIMETS
		FROM 	SWB.AGENTLOGIN
		WHERE	LOGINTIMETS > UTC_ROW_SDATE AND LOGINTIMETS <= UTC_ROW_EDATE;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] AGENTLOGIN INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


	--AGENTSTATUS DELETE
	BEGIN
		DELETE
		FROM 	SW.AGENTSTATUS
		WHERE 	TTIMETS > UTC_ROW_SDATE AND TTIMETS <= UTC_ROW_EDATE;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] AGENTSTATUS DELETE.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


	--AGENTSTATUS INSERT
	BEGIN
		INSERT 
		INTO 	SW.AGENTSTATUS
		(		ID          ,
				AGENTID     ,
				PERSON      ,
				THISDN      ,
				SWITCHID    ,
				THISQUEUE   ,
				VQUEUE      ,
				QUEUE1      ,
				SKILL       ,
				STIME       ,
				STIMETS     ,
				TTIME       ,
				TTIMETS     ,
				ITIMETS     ,
				SEVENT      ,
				TEVENT      ,
				STATUS      ,
				WORKMODE    ,
				REASONCODE  ,
				REASONS     ,
				CALLS       ,
				CONNID      ,
				CALLTYPE    ,
				CALLTYPEEXT ,
				STATE       ,
				HOLD        ,
				ROLE        ,
				QUERY       ,
				NTRS        ,
				ORG         ,
				SCALLSTATE  ,
				TCALLSTATE  ,
				SROLE       ,
				TROLE
		)
		SELECT	ID          ,
				AGENTID     ,
				PERSON      ,
				THISDN      ,
				SWITCHID    ,
				THISQUEUE   ,
				VQUEUE      ,
				QUEUE1      ,
				SKILL       ,
				STIME       ,
				STIMETS     ,
				TTIME       ,
				TTIMETS     ,
				ITIMETS     ,
				SEVENT      ,
				TEVENT      ,
				STATUS      ,
				WORKMODE    ,
				REASONCODE  ,
				REASONS     ,
				CALLS       ,
				CONNID      ,
				CALLTYPE    ,
				CALLTYPEEXT ,
				STATE       ,
				HOLD        ,
				ROLE        ,
				QUERY       ,
				NTRS        ,
				ORG         ,
				SCALLSTATE  ,
				TCALLSTATE  ,
				SROLE       ,
				TROLE
		FROM 	SWB.AGENTSTATUS
		WHERE	TTIMETS > UTC_ROW_SDATE AND TTIMETS <= UTC_ROW_EDATE;

		COMMIT;

		EXCEPTION WHEN OTHERS THEN
			O_SQL_ECODE	:=	ABS(SQLCODE);
			O_SQL_EMSG 	:= '[ERROR] AGENTSTATUS INSERT.. [' || O_SQL_ECODE || '][' || SQLERRM || ']';
			ROLLBACK; 	--오류가 발생하여 롤백처리
	END;


 	IF	O_SQL_ECODE  <>  0  THEN
		--오류가 발생한 경우 FAIL과 오류코드 및 시간을 업데이트 한다.
		BEGIN
			UPDATE	SWM.AX_STAT_HIST
			SET		EXP 		= 	'FAIL',
					EXPMSG 		= 	O_SQL_EMSG,
					ENDTIME 	= 	SYSDATE
			WHERE	STATGUBUN 	= 	'SWB->SW'
			AND		TARGETTIME 	= 	SUBSTR(I_ROW_SDATE,1,14)
			AND 	TARGETTABLE = 	'MUFFIN'
			AND		EXPROC		= 	'SP_SWB_SW_AGGJOB'
			AND 	EXP			= 	'ONGOING';
		END;
    ELSE
		--오류가 없는 경우 SUCCESS와 결과 시간을 업데이트 한다.
		BEGIN
			UPDATE	SWM.AX_STAT_HIST
			SET		EXP 		= 	'SUCCESS',
					EXPMSG 		= 	'SUCCESS',
					ENDTIME 	= 	SYSDATE
			WHERE	STATGUBUN 	= 	'SWB->SW'
			AND		TARGETTIME 	= 	SUBSTR(I_ROW_SDATE,1,14)
			AND 	TARGETTABLE = 	'MUFFIN'
			AND		EXPROC		= 	'SP_SWB_SW_AGGJOB'
			AND 	EXP			= 	'ONGOING';
      	END;

		O_SQL_EMSG := 'SUCCESS';

    END IF;

	COMMIT;
END;