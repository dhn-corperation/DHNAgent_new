package com.dhn.client.dao;

import com.dhn.client.bean.KAORequestBean;
import com.dhn.client.bean.Msg_Log;
import com.dhn.client.bean.SQLParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class KAORequestDAOimpl implements KAORequestDAO{

    @Autowired
    private SqlSession sqlSession;

    @Override
    public int atTableCheck(SQLParameter param) throws Exception {
        return sqlSession.selectOne("com.dhn.client.create.mapper.SendRequest.atTableCheck", param);
    }

    @Override
    public void atTableCreate(SQLParameter param) throws Exception {

        if(param.getDatabase().equals("oracle")){
            int seqcnt = sqlSession.selectOne("com.dhn.client.create.mapper.SendRequest.atSeqCheck_oracle",param);

            if(seqcnt == 0){
                sqlSession.update("com.dhn.client.create.mapper.SendRequest.createSequence_oracle", param);
            }
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.createAtTable_oracle", param);
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.createIndex1_oracle", param);
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.createIndex2_oracle", param);
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.createIndex3_oracle", param);
        }else if(param.getDatabase().equals("mysql") || param.getDatabase().equals("mariadb")){
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.createAtTable_mysql", param);
        }
    }

    @Override
    public void atLogTableCheck(String atTable, String atLogTable, String database) {
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");

        String lastMonth = now.minusMonths(1).format(formatter);
        String currentMonth = now.format(formatter);
        String nextMonth = now.plusMonths(1).format(formatter);

        String atLogTableLast = atLogTable+"_"+lastMonth;
        String atLogTableCurrent = atLogTable+"_"+currentMonth;
        String atLogTableNext = atLogTable+"_"+nextMonth;

        Map<String, String> map = new HashMap<>();
        map.put("atTable", atTable);
        map.put("database",database);

        map.put("atLogTable",atLogTableLast);
        int result_last = sqlSession.selectOne("com.dhn.client.create.mapper.SendRequest.atLogTableCheck", map);
        if(result_last == 0){
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.createAtLogTable", map);
            log.info("{} 테이블 생성",map.get("atLogTable"));
        }

        map.put("atLogTable",atLogTableCurrent);
        int result_current = sqlSession.selectOne("com.dhn.client.create.mapper.SendRequest.atLogTableCheck", map);
        if(result_current == 0){
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.createAtLogTable", map);
            log.info("{} 테이블 생성",map.get("atLogTable"));

        }

        map.put("atLogTable",atLogTableNext);
        int result_next = sqlSession.selectOne("com.dhn.client.create.mapper.SendRequest.atLogTableCheck", map);
        if(result_next == 0){
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.createAtLogTable", map);
            log.info("{} 테이블 생성",map.get("atLogTable"));

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
    public void updateKAOAuthFail(SQLParameter paramCopy) throws Exception {
        sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.kao_oauth_fail", paramCopy);
    }

    @Override
    public void kaoJsonErrMessage(SQLParameter param, List<String> jsonErrMsgid) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("param", param);
        paramMap.put("jsonErrMsgid", jsonErrMsgid);
        sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.kao_json_err_message",paramMap);
    }

    @Override
    public void kaoResultInsert(Msg_Log ml) throws Exception {
        sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.kaoResultUpdate", ml);
        sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.kaoLogInsert", ml);
        sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.kaoResultDelete", ml);
    }

    @Override
    public int log_move_count(SQLParameter param) throws Exception {
        return sqlSession.selectOne("com.dhn.client.kakao.mapper.SendRequest.kao_log_move_count", param);
    }

    @Override
    public void update_log_move_groupNo(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.update_log_move_groupNo", param);
    }

    @Override
    public void log_move(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.log_move_insert", param);
        sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.log_move_delete", param);
    }

}
