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
public class CreateDAOImpl implements CreateDAO{

    @Autowired
    private SqlSession sqlSession;

    @Override
    public int tableCheck(SQLParameter param) throws Exception {
        return sqlSession.selectOne("com.dhn.client.create.mapper.SendRequest.tableCheck", param);
    }

    @Override
    public void tableCreate(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.create.mapper.SendRequest.tableCreate", param);
    }

    @Override
    public void logTableCheck(SQLParameter param) throws Exception {
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");

        String lastMonth = now.minusMonths(1).format(formatter);
        String currentMonth = now.format(formatter);
        String nextMonth = now.plusMonths(1).format(formatter);

        String logTableLast = param.getLog_table()+"_"+lastMonth;
        String logTableCurrent = param.getLog_table()+"_"+currentMonth;
        String logTableNext = param.getLog_table()+"_"+nextMonth;

        Map<String, String> map = new HashMap<>();
        map.put("msg_table", param.getMsg_table());

        map.put("log_table",logTableLast);
        int result_last = sqlSession.selectOne("com.dhn.client.create.mapper.SendRequest.logTableCheck", map);
        if(result_last == 0){
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.logTableCreate", map);
            log.info("{} 테이블 생성",map.get("log_table"));
        }

        map.put("log_table",logTableCurrent);
        int result_current = sqlSession.selectOne("com.dhn.client.create.mapper.SendRequest.logTableCheck", map);
        if(result_current == 0){
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.logTableCreate", map);
            log.info("{} 테이블 생성",map.get("log_table"));

        }

        map.put("log_table",logTableNext);
        int result_next = sqlSession.selectOne("com.dhn.client.create.mapper.SendRequest.logTableCheck", map);
        if(result_next == 0){
            sqlSession.update("com.dhn.client.create.mapper.SendRequest.logTableCreate", map);
            log.info("{} 테이블 생성",map.get("log_table"));

        }
    }
}
