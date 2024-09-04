package com.dhn.client.dao;

import com.dhn.client.bean.MMSImageBean;
import com.dhn.client.bean.Msg_Log;
import com.dhn.client.bean.RequestBean;
import com.dhn.client.bean.SQLParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class MSGRequestDAOimpl implements MSGRequestDAO{

    @Autowired
    private SqlSession sqlSession;

    @Override
    public int msgTableCheck(SQLParameter param) throws Exception {
        return sqlSession.selectOne("com.dhn.client.create.mapper.SendRequest.msgTableCheck", param);
    }

    @Override
    public void msgTableCreate(SQLParameter param) throws Exception {
        if(param.getDatabase().equals("oracle")){

            int seqcnt = sqlSession.selectOne("com.dhn.client.create.mapper.SendRequest.msgSeqCheck_oracle",param);

            if(seqcnt == 0){
                sqlSession.update("com.dhn.client.create.mapper.SendRequest.createMsgSequence_oracle", param);
            }

            sqlSession.update("com.dhn.client.create.mapper.SendRequest.createMsgTable_oracle", param);
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.createMsgIndex1_oracle", param);
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.createMsgIndex2_oracle", param);
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.createMsgIndex3_oracle", param);
        }else if(param.getDatabase().equals("mysql") || param.getDatabase().equals("mariadb")){
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.createMsgTable_mysql", param);
        }
    }

    @Override
    public void msgLogTableCheck(String msgTable, String msgLogTable, String database) throws Exception {
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");

        String lastMonth = now.minusMonths(1).format(formatter);
        String currentMonth = now.format(formatter);
        String nextMonth = now.plusMonths(1).format(formatter);

        String msgLogTableLast = msgLogTable+"_"+lastMonth;
        String msgLogTableCurrent = msgLogTable+"_"+currentMonth;
        String msgLogTableNext = msgLogTable+"_"+nextMonth;

        Map<String, String> map = new HashMap<>();
        map.put("msgTable", msgTable);
        map.put("database",database);

        map.put("msgLogTable",msgLogTableLast);
        int result_last = sqlSession.selectOne("com.dhn.client.create.mapper.SendRequest.msgLogTableCheck", map);
        if(result_last == 0){
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.createMsgLogTable", map);
            log.info("{} 테이블 생성",map.get("msgLogTable"));
        }

        map.put("msgLogTable",msgLogTableCurrent);
        int result_current = sqlSession.selectOne("com.dhn.client.create.mapper.SendRequest.msgLogTableCheck", map);
        if(result_current == 0){
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.createMsgLogTable", map);
            log.info("{} 테이블 생성",map.get("msgLogTable"));

        }

        map.put("msgLogTable",msgLogTableNext);
        int result_next = sqlSession.selectOne("com.dhn.client.create.mapper.SendRequest.msgLogTableCheck", map);
        if(result_next == 0){
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.createMsgLogTable", map);
            log.info("{} 테이블 생성",map.get("msgLogTable"));

        }
    }

    @Override
    public int selectSMSReqeustCount(SQLParameter param) throws Exception {
        return sqlSession.selectOne("com.dhn.client.msg.mapper.SendRequest.req_sms_count",param);
    }

    @Override
    public void updateSMSGroupNo(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.req_sms_group_update",param);
    }

    @Override
    public List<RequestBean> selectSMSRequests(SQLParameter param) throws Exception {
        return sqlSession.selectList("com.dhn.client.msg.mapper.SendRequest.req_sms_select",param);
    }

    @Override
    public void updateSMSSendComplete(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.req_sent_complete",param);
    }

    @Override
    public void updateSMSSendInit(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.req_sent_init",param);
    }

    @Override
    public void updateMSGAuthFail(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.msg_auth_fail",param);
    }

    @Override
    public void jsonErrMessage(SQLParameter param, List<String> jsonErrMsgid) throws Exception {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("param", param);
        paramMap.put("jsonErrMsgid", jsonErrMsgid);
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.msg_json_err_message",paramMap);
    }

    @Override
    public void msgResultInsert(Msg_Log ml) throws Exception {
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.msgResultUpdate", ml);
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.msgLogInsert", ml);
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.msgResultDelete", ml);
    }

    @Override
    public int log_move_count(SQLParameter param) throws Exception {
        return sqlSession.selectOne("com.dhn.client.msg.mapper.SendRequest.msg_log_move_count", param);
    }

    @Override
    public void update_log_move_groupNo(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.update_log_move_groupNo", param);
    }

    @Override
    public void log_move(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.log_move_insert", param);
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.log_move_delete", param);
    }

    @Override
    public int selectLMSReqeustCount(SQLParameter param) throws Exception {
        return sqlSession.selectOne("com.dhn.client.msg.mapper.SendRequest.req_lms_count",param);
    }

    @Override
    public void updateLMSGroupNo(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.req_lms_group_update",param);
    }

    @Override
    public List<RequestBean> selectLMSRequests(SQLParameter param) throws Exception {
        return sqlSession.selectList("com.dhn.client.msg.mapper.SendRequest.req_lms_select",param);
    }

    @Override
    public int selectMMSReqeustCount(SQLParameter param) throws Exception {
        return sqlSession.selectOne("com.dhn.client.msg.mapper.SendRequest.req_mms_count",param);
    }

    @Override
    public void updateMMSGroupNo(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.req_mms_group_update",param);
    }

    @Override
    public List<RequestBean> selectMMSRequests(SQLParameter param) throws Exception {
        return sqlSession.selectList("com.dhn.client.msg.mapper.SendRequest.req_mms_select",param);
    }

    @Override
    public int selectMMSImageCount(SQLParameter param) throws Exception {
        return sqlSession.selectOne("com.dhn.client.msg.mapper.SendRequest.req_mms_image_count",param);
    }

    @Override
    public List<MMSImageBean> selectMMSImage(SQLParameter param) throws Exception {
        return sqlSession.selectList("com.dhn.client.msg.mapper.SendRequest.req_mms_image", param);
    }

    @Override
    public void updateMMSImageGroup(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.req_mms_key_update", param);
    }
}
