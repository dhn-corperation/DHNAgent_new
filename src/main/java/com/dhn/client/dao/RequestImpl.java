package com.dhn.client.dao;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dhn.client.bean.*;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RequestImpl implements RequestDAO{

	@Autowired
	private SqlSession sqlSession;

	/*
	@Override
	public int atTableCheck(SQLParameter param) throws Exception {
		return sqlSession.selectOne("com.dhn.client.create.mapper.SendRequest.atTableCheck", param);
	}

	@Override
	public void atTableCreate(SQLParameter param) throws Exception {

		if(param.getDatabase().equals("oracle")){
			sqlSession.update("com.dhn.client.create.mapper.SendRequest.createSequence_oracle", param);
			sqlSession.update("com.dhn.client.create.mapper.SendRequest.createAtTable_oracle", param);
			sqlSession.update("com.dhn.client.create.mapper.SendRequest.createIndex1_oracle", param);
			sqlSession.update("com.dhn.client.create.mapper.SendRequest.createIndex2_oracle", param);
		}else if(param.getDatabase().equals("mysql") || param.getDatabase().equals("mariadb")){
			sqlSession.update("com.dhn.client.create.mapper.SendRequest.createAtTable_mysql", param);
		}
	}


	@Override
	public int selectKAORequestCount(SQLParameter param) throws Exception {
		int cnt = 0;
		cnt = sqlSession.selectOne("com.dhn.client.kakao.mapper.SendRequest.req_kao_count",param);
		return cnt;
	}

	@Override
	public void updateKAOGroupNo(SQLParameter param) throws Exception {
		sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.req_kao_group_update",param);
	}

	@Override
	public List<KAORequestBean> selectKAORequests(SQLParameter param) throws Exception {
		return sqlSession.selectList("com.dhn.client.kakao.mapper.SendRequest.req_kao_select", param);
	}

	@Override
	public void updateKAOSendComplete(SQLParameter param) throws Exception {
		sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.req_sent_complete", param);
	}

	@Override
	public void updateKAOSendInit(SQLParameter param) throws Exception {
		sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.req_sent_init", param);
	}

	@Override
	public int selectSMSReqeustCount(SQLParameter param) throws Exception {
		int cnt = 0;
		cnt = sqlSession.selectOne("com.dhn.client.nkakao.mapper.SendRequest.req_sms_count",param);
		return cnt;
	}

	@Override
	public void updateSMSGroupNo(SQLParameter param) throws Exception {
		sqlSession.update("com.dhn.client.nkakao.mapper.SendRequest.req_sms_group_update",param);
	}

	@Override
	public List<RequestBean> selectSMSRequests(SQLParameter param) throws Exception {
		return sqlSession.selectList("com.dhn.client.nkakao.mapper.SendRequest.req_sms_select", param);
	}

	@Override
	public void updateSMSSendComplete(SQLParameter param) throws Exception {
		sqlSession.update("com.dhn.client.nkakao.mapper.SendRequest.req_sent_complete",param);
	}

	@Override
	public void updateSMSSendInit(SQLParameter param) throws Exception {
		sqlSession.update("com.dhn.client.nkakao.mapper.SendRequest.req_sent_init", param);
	}

	@Override
	public int selectLMSReqeustCount(SQLParameter param) throws Exception {
		int cnt = 0;
		cnt = sqlSession.selectOne("com.dhn.client.nkakao.mapper.SendRequest.req_lms_count", param);
		return cnt;
	}

	@Override
	public void updateLMSGroupNo(SQLParameter param) throws Exception {
		sqlSession.update("com.dhn.client.nkakao.mapper.SendRequest.req_lms_group_update", param);
	}

	@Override
	public List<RequestBean> selectLMSRequests(SQLParameter param) throws Exception {
		return sqlSession.selectList("com.dhn.client.nkakao.mapper.SendRequest.req_lms_select", param);
	}

	@Override
	public int selectMMSReqeustCount(SQLParameter param) throws Exception {
		int cnt ;
		cnt = sqlSession.selectOne("com.dhn.client.nkakao.mapper.SendRequest.req_mms_count", param);
		return cnt;
	}

	@Override
	public void updateMMSGroupNo(SQLParameter param) throws Exception {
		sqlSession.update("com.dhn.client.nkakao.mapper.SendRequest.req_mms_group_update", param);
	}

	@Override
	public List<RequestBean> selectMMSRequests(SQLParameter param) throws Exception {
		return sqlSession.selectList("com.dhn.client.nkakao.mapper.SendRequest.req_mms_select", param);
	}

	@Override
	public int selectMMSImageCount(SQLParameter param) throws Exception {
		return sqlSession.selectOne("com.dhn.client.nkakao.mapper.SendRequest.req_mms_image_count",param);
	}

	@Override
	public List<MMSImageBean> selectMMSImage(SQLParameter param) throws Exception {
		return sqlSession.selectList("com.dhn.client.nkakao.mapper.SendRequest.req_mms_image", param);
	}

	@Override
	public void updateMMSImageGroup(SQLParameter param) throws Exception {
		sqlSession.update("com.dhn.client.nkakao.mapper.SendRequest.req_mms_key_update", param);
	}

	@Override
	public LMSTableBean kakao_to_sms_select(Msg_Log ml) throws Exception {
		return sqlSession.selectOne("com.dhn.client.nkakao.mapper.SendRequest.kakao_to_sms_select",ml);
	}

	@Override
	public void Insert_msg_log(Msg_Log _ml) throws Exception {
		sqlSession.update("com.dhn.client.nkakao.mapper.SendRequest.result_log_insert1", _ml);
		sqlSession.update("com.dhn.client.nkakao.mapper.SendRequest.result_log_insert2", _ml);
		sqlSession.update("com.dhn.client.nkakao.mapper.SendRequest.result_log_insert3", _ml);
	}

	@Override
	public void insert_sms(LMSTableBean lmsBean) throws Exception {
		sqlSession.insert("com.dhn.client.nkakao.mapper.SendRequest.kao_to_sms_insert",lmsBean);
	}

	@Override
	public int log_move_count(SQLParameter param) throws Exception {
		return sqlSession.selectOne("com.dhn.client.nkakao.mapper.SendRequest.log_move_count",param);
	}

	@Override
	public void update_log_move_groupNo(SQLParameter param) throws Exception {
		sqlSession.update("com.dhn.client.nkakao.mapper.SendRequest.log_move_group_update",param);
	}

	@Override
	public void log_move(SQLParameter param) throws Exception {
		sqlSession.update("com.dhn.client.nkakao.mapper.SendRequest.log_move_insert",param);
		sqlSession.update("com.dhn.client.nkakao.mapper.SendRequest.log_move_delete",param);
	}

	@Override
	public void jsonErrMessage(SQLParameter param, List<String> jsonErrMsgid) throws Exception {
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("param", param);
		paramMap.put("jsonErrMsgid", jsonErrMsgid);
		if(param.getMsg_type().equals("A")){
			sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.kao_json_err_message",paramMap);

		}else{
			sqlSession.update("com.dhn.client.nkakao.mapper.SendRequest.msg_json_err_message",paramMap);
		}
	}

	 */
}
