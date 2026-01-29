package com.dhn.client.dao;

import com.dhn.client.bean.BMDataBean;
import com.dhn.client.bean.BMRequestBean;
import com.dhn.client.bean.Msg_Log;
import com.dhn.client.bean.SQLParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Slf4j
public class BMRequestDAOimpl implements BMRequestDAO {

    @Autowired
    private SqlSession sqlSession;

    @Override
    public int selectBMRequestCount(SQLParameter param) throws Exception {
        int cnt = 0;
        cnt = sqlSession.selectOne("com.dhn.client.brand.mapper.SendRequest.bm_kao_count",param);
        return cnt;
    }

    @Override
    public void updateBMGroupNo(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.brand.mapper.SendRequest.req_bm_group_update",param);
    }

    @Override
    public List<BMDataBean> selectBMRequests(SQLParameter param) throws Exception {
        return sqlSession.selectList("com.dhn.client.brand.mapper.SendRequest.req_bm_select", param);
    }

    @Override
    public void updateBMSendComplete(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.brand.mapper.SendRequest.req_bm_sent_complete", param);
    }

    @Override
    public void updateBMSendInit(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.brand.mapper.SendRequest.req_bm_sent_init", param);
    }

    @Override
    public void updateInvalidData(List<String> invalidList, Msg_Log ml) throws Exception {
        int retry = 0;
        int maxRetry = 5;

        while (true) {

            try {
                ((BMRequestDAO) AopContext.currentProxy()).doUpdateInvalidDataTx(invalidList, ml);
                return;
            } catch (Exception e) {
                retry++;
                log.warn("[RETRY] updateInvalidData retry {}/{} invalidCnt={} / {}",retry,maxRetry,invalidList.size(),e);
                if (!isRetryable(e) || retry >= maxRetry) {
                    log.error("[FAIL] updateInvalidData failed after {} retries", retry, e);
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
    public void doUpdateInvalidDataTx(List<String> invalidList, Msg_Log ml) {

        Map<String, Object> param = new HashMap<>();
        param.put("list", invalidList);
        param.put("ml", ml);

        sqlSession.update("com.dhn.client.brand.mapper.SendRequest.bmInvalidUpdate", param);
        sqlSession.insert("com.dhn.client.brand.mapper.SendRequest.bmInvalidLogInsert", param);
        sqlSession.delete("com.dhn.client.brand.mapper.SendRequest.bmInvalidResultDelete", param);
    }

    @Override
    public List<BMDataBean> selectBCRequests(SQLParameter param) throws Exception {
        return sqlSession.selectList("com.dhn.client.brand.mapper.SendRequest.req_bc_select", param);
    }

    @Override
    public int selectBDRequestCount(SQLParameter param) throws Exception {
        int cnt = 0;
        cnt = sqlSession.selectOne("com.dhn.client.brand.mapper.SendRequest.bd_kao_count",param);
        return cnt;
    }

    @Override
    public void updateBDGroupNo(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.brand.mapper.SendRequest.req_bd_group_update",param);
    }

    @Override
    public List<BMRequestBean> selectBDRequests(SQLParameter param) throws Exception {
        return sqlSession.selectList("com.dhn.client.brand.mapper.SendRequest.req_bd_select", param);
    }

    @Override
    public void updateExpectedFail(Msg_Log ml) throws Exception {
        int retry = 0;
        int maxRetry = 5;

        while (true) {
            try {
                ((BMRequestDAO) AopContext.currentProxy()).doUpdateExpectedFailTx(ml);
                return;

            } catch (Exception e) {
                retry++;
                log.warn("[RETRY] updateExpectedFail retry {}/{} msgid={} / {}",retry,maxRetry,ml.getMsgid(),e);
                if (!isRetryable(e) || retry >= maxRetry) {
                    log.error("[FAIL] updateExpectedFail failed after {} retries", retry, e);
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
    public void doUpdateExpectedFailTx(Msg_Log ml) {

        sqlSession.update("com.dhn.client.brand.mapper.SendRequest.bmExpectedUpdate", ml);
        sqlSession.insert("com.dhn.client.brand.mapper.SendRequest.bmExpectedLogInsert", ml);
        sqlSession.delete("com.dhn.client.brand.mapper.SendRequest.bmExpectedDelete", ml);
    }

    @Override
    public void retryBmData(List<String> retryList, Msg_Log ml) throws Exception {

        Map<String, Object> param = new HashMap<>();
        param.put("list", retryList);
        param.put("ml", ml);

        sqlSession.update("com.dhn.client.brand.mapper.SendRequest.bmRetryUpdate", param);
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
