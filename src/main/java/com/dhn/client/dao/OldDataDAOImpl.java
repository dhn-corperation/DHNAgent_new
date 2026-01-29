package com.dhn.client.dao;

import com.dhn.client.bean.Msg_Log;
import com.dhn.client.bean.SQLParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
public class OldDataDAOImpl implements OldDataDAO{

    @Autowired
    private SqlSession sqlSession;

    @Override
    public int old_data_count(SQLParameter param) throws Exception {
        int cnt = 0;
        cnt = sqlSession.selectOne("com.dhn.client.olddata.mapper.SendRequest.old_data_count",param);
        return cnt;
    }

    @Override
    public void old_data_group_update(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.olddata.mapper.SendRequest.old_data_group_update",param);

    }

    @Override
    public void old_data_result(SQLParameter param) throws Exception {
        int retry = 0;
        int maxRetry = 5;

        while (true) {
            try {
                ((OldDataDAO) AopContext.currentProxy()).doold_data_resultTx(param);
                return;
            } catch (Exception e) {
                retry++;
                log.warn("[RETRY] old_data_result retry {}/{}/{}",retry,maxRetry,e);
                if (!isRetryable(e) || retry >= maxRetry) {
                    log.error("[FAIL] old_data_result failed after {} retries", retry, e);
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
    public void doold_data_resultTx(SQLParameter param) {
        sqlSession.update("com.dhn.client.olddata.mapper.SendRequest.old_data_update",param);
        sqlSession.insert("com.dhn.client.olddata.mapper.SendRequest.old_data_insert",param);
        sqlSession.delete("com.dhn.client.olddata.mapper.SendRequest.old_data_delete",param);
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
