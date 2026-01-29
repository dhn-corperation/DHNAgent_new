package com.dhn.client.dao;

import com.dhn.client.bean.ImageBean;
import com.dhn.client.bean.Msg_Log;
import com.dhn.client.bean.RequestBean;
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
public class MSGRequestDAOimpl implements MSGRequestDAO{

    @Autowired
    private SqlSession sqlSession;

    @Override
    public int selectSMSRequestCount(SQLParameter param) throws Exception {
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
    public void msgResultInsert(Msg_Log ml) throws Exception {
        int retry = 0;
        int maxRetry = 5;

        while (true) {
            try {
                ((MSGRequestDAO) AopContext.currentProxy()).domsgResultInsertTx(ml);
                return;
            } catch (Exception e) {
                retry++;
                log.warn("[RETRY] msgResultInsert retry {}/{} msgid={} / {}",retry,maxRetry,ml.getMsgid(),e);
                if (!isRetryable(e) || retry >= maxRetry) {
                    log.error("[FAIL] msgResultInsert failed after {} retries", retry, e);
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
    public void domsgResultInsertTx(Msg_Log ml) {
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

        int retry = 0;
        int maxRetry = 5;

        while (true) {
            try {
                ((MSGRequestDAO) AopContext.currentProxy()).dolog_moveTx(param);
                return;
            } catch (Exception e) {
                retry++;
                log.warn("[RETRY] msg_log_move retry {}/{}/{}",retry,maxRetry,e);
                if (!isRetryable(e) || retry >= maxRetry) {
                    log.error("[FAIL] msg_log_move failed after {} retries", retry, e);
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
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.log_move_insert", param);
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.log_move_delete", param);
    }

    @Override
    public int selectLMSRequestCount(SQLParameter param) throws Exception {
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
    public int selectMMSRequestCount(SQLParameter param) throws Exception {
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
    public List<ImageBean> selectMMSImage(SQLParameter param) throws Exception {
        return sqlSession.selectList("com.dhn.client.msg.mapper.SendRequest.req_mms_image", param);
    }

    @Override
    public void updateMMSImageGroup(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.req_mms_key_update", param);
    }

    @Override
    public void updateMMSImageFail(SQLParameter param) throws Exception {

        int retry = 0;
        int maxRetry = 5;

        while (true) {
            try {
                ((MSGRequestDAO) AopContext.currentProxy()).doupdateMMSImageFailTx(param);
                return;
            } catch (Exception e) {
                retry++;
                log.warn("[RETRY] updateMMSImageFail retry {}/{} msgid={} / {}",retry,maxRetry,param.getMsgid(),e);
                if (!isRetryable(e) || retry >= maxRetry) {
                    log.error("[FAIL] updateMMSImageFail failed after {} retries", retry, e);
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
    public void doupdateMMSImageFailTx(SQLParameter param) {
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.mms_image_fail_update",param);
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.mms_image_fail_log_Insert", param);
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.mms_image_fail_delete", param);
    }

    @Override
    public int selectOTPRequestCount(SQLParameter param) throws Exception {
        return sqlSession.selectOne("com.dhn.client.msg.mapper.SendRequest.req_otp_count",param);
    }

    @Override
    public void updateOTPGroupNo(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.req_otp_group_update",param);
    }

    @Override
    public List<RequestBean> selectOTPRequests(SQLParameter param) throws Exception {
        return sqlSession.selectList("com.dhn.client.msg.mapper.SendRequest.req_otp_select",param);
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
