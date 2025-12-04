package com.dhn.client.dao;

import com.dhn.client.bean.BMDataBean;
import com.dhn.client.bean.BMRequestBean;
import com.dhn.client.bean.Msg_Log;
import com.dhn.client.bean.SQLParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
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
    @Transactional(rollbackFor = Exception.class)
    public void updateInvalidData(List<String> invalidList, Msg_Log ml) throws Exception {
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
    @Transactional(rollbackFor = Exception.class)
    public void updateExpectedFail(Msg_Log ml) throws Exception {
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
}
