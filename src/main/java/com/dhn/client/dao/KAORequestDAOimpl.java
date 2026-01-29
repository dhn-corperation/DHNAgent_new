package com.dhn.client.dao;

import com.dhn.client.bean.ImageBean;
import com.dhn.client.bean.KAORequestBean;
import com.dhn.client.bean.Msg_Log;
import com.dhn.client.bean.SQLParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class KAORequestDAOimpl implements KAORequestDAO{

    @Autowired
    private SqlSession sqlSession;

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
    public void kaoResultInsert(Msg_Log ml) throws Exception {
        int retry = 0;
        int maxRetry = 5;

        while (true) {
            try {
                ((KAORequestDAO) AopContext.currentProxy()).dokaoResultInsertTx(ml);
                return;
            } catch (Exception e) {
                retry++;
                log.warn("[RETRY] kaoResultInsert retry {}/{} msgid={} / {}",retry,maxRetry,ml.getMsgid(),e);
                if (!isRetryable(e) || retry >= maxRetry) {
                    log.error("[FAIL] kaoResultInsert failed after {} retries", retry, e);
                    throw e;
                }
                Thread.sleep(200 * retry);
            }
        }
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRES_NEW
    )
    public void dokaoResultInsertTx(Msg_Log ml) {
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
        int retry = 0;
        int maxRetry = 5;

        while (true) {
            try {
                ((KAORequestDAO) AopContext.currentProxy()).dolog_moveTx(param);
                return;
            } catch (Exception e) {
                retry++;
                log.warn("[RETRY] kao_log_move retry {}/{}/{}",retry,maxRetry,e);
                if (!isRetryable(e) || retry >= maxRetry) {
                    log.error("[FAIL] kao_log_move failed after {} retries", retry, e);
                    throw e;
                }
                Thread.sleep(200 * retry);
            }
        }
    }

    @Override
    @Transactional(
            rollbackFor = Exception.class,
            propagation = Propagation.REQUIRES_NEW
    )
    public void dolog_moveTx(SQLParameter param) {
        sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.log_move_insert", param);
        sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.log_move_delete", param);
    }

    private boolean isRetryable(Exception e) {
        Throwable t = e;
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null) {
                msg = msg.toLowerCase();
                if (msg.contains("deadlock")
                        || msg.contains("lock wait timeout")) {
                    return true;
                }
                if (msg.contains("ora-00060")
                        || msg.contains("ora-30006")) {
                    return true;
                }
            }

            if (t instanceof java.sql.SQLException) {
                String state = ((java.sql.SQLException)t).getSQLState();
                if ("40001".equals(state)) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

}
