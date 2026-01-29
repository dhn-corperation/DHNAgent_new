package com.dhn.client.dao;

import com.dhn.client.bean.FTDataBean;
import com.dhn.client.bean.ImageBean;
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
public class FTRequestDAOimpl implements FTRequestDAO {

    @Autowired
    private SqlSession sqlSession;

    @Override
    public int selectFTRequestCount(SQLParameter param) throws Exception {
        int cnt = 0;
        cnt = sqlSession.selectOne("com.dhn.client.friend.mapper.SendRequest.ft_kao_count",param);
        return cnt;
    }

    @Override
    public void updateFTGroupNo(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.friend.mapper.SendRequest.req_ft_group_update",param);
    }

    @Override
    public List<FTDataBean> selectFTRequests(SQLParameter param) throws Exception {
        return sqlSession.selectList("com.dhn.client.friend.mapper.SendRequest.req_ft_select", param);
    }

    @Override
    public void updateFTInvalidData(List<String> invalidList, Msg_Log ml) throws Exception {
        int retry = 0;
        int maxRetry = 5;

        while (true) {

            try {
                ((FTRequestDAO) AopContext.currentProxy()).doupdateFTInvalidDataTx(invalidList, ml);
                return;
            } catch (Exception e) {
                retry++;
                log.warn("[RETRY] updateFTInvalidData retry {}/{} invalidCnt={} / {}",retry,maxRetry,invalidList.size(),e);
                if (!isRetryable(e) || retry >= maxRetry) {
                    log.error("[FAIL] updateFTInvalidData failed after {} retries", retry, e);
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
    public void doupdateFTInvalidDataTx(List<String> invalidList, Msg_Log ml) {

        Map<String, Object> param = new HashMap<>();
        param.put("list", invalidList);
        param.put("ml", ml);

        sqlSession.update("com.dhn.client.friend.mapper.SendRequest.ftInvalidUpdate", param);
        sqlSession.insert("com.dhn.client.friend.mapper.SendRequest.ftInvalidLogInsert", param);
        sqlSession.delete("com.dhn.client.friend.mapper.SendRequest.ftInvalidResultDelete", param);
    }

    @Override
    public void updateFTSendComplete(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.friend.mapper.SendRequest.req_ft_sent_complete", param);
    }

    @Override
    public void updateFTSendInit(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.friend.mapper.SendRequest.req_ft_sent_init", param);
    }

    @Override
    public int selectFtImageCount(SQLParameter param) throws Exception {
        return sqlSession.selectOne("com.dhn.client.friend.mapper.SendRequest.ft_kao_img_count",param);
    }

    @Override
    public void updateFTImageGroup(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.friend.mapper.SendRequest.ft_kao_img_group",param);
    }

    @Override
    public List<ImageBean> selectFtImage(SQLParameter param) throws Exception {
        return sqlSession.selectList("com.dhn.client.friend.mapper.SendRequest.ft_kao_image_list",param);
    }

    @Override
    public void updateFTImageFail(SQLParameter param) throws Exception {
        int retry = 0;
        int maxRetry = 5;

        while (true) {

            try {
                ((FTRequestDAO) AopContext.currentProxy()).doupdateFTImageFailTx(param);
                return;
            } catch (Exception e) {
                retry++;
                log.warn("[RETRY] updateFTImageFail retry {}/{} msgid={} / {}",retry,maxRetry,param.getMsgid(),e);
                if (!isRetryable(e) || retry >= maxRetry) {
                    log.error("[FAIL] updateFTImageFail failed after {} retries", retry, e);
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
    public void doupdateFTImageFailTx(SQLParameter param) {

        sqlSession.update("com.dhn.client.friend.mapper.SendRequest.ft_image_fail_update", param);
        sqlSession.insert("com.dhn.client.friend.mapper.SendRequest.ft_image_fail_log_Insert", param);
        sqlSession.delete("com.dhn.client.friend.mapper.SendRequest.ft_image_fail_delete", param);
    }

    @Override
    public void updateFTImageUrl(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.friend.mapper.SendRequest.ft_image_url_update",param);
    }

    @Override
    public int selectOldFTRequestCount(SQLParameter param) throws Exception {
        return sqlSession.selectOne("com.dhn.client.friend.mapper.SendRequest.ft_old_kao_count",param);
    }

    @Override
    public void updateOldFTGroupNo(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.friend.mapper.SendRequest.req_ft_old_group_update",param);
    }

    @Override
    public List<FTDataBean> selectOldFTRequests(SQLParameter param) throws Exception {
        return sqlSession.selectList("com.dhn.client.friend.mapper.SendRequest.req_old_ft_select", param);
    }

    @Override
    public void updateFTImageUploadFail(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.friend.mapper.SendRequest.ft_image_upload_fail",param);
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
