package com.dhn.client.dao;

import com.dhn.client.bean.SQLParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
}
