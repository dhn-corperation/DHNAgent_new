package com.dhn.client.dao;

import com.dhn.client.bean.RequestBean;
import com.dhn.client.bean.SQLParameter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@Slf4j
public class RCSRequestDAOImpl implements RCSRequestDAO {

    @Autowired
    private SqlSession sqlSession;

    @Override
    public int selectRCSRequestCount(SQLParameter param) throws Exception {
        int cnt = 0;
        cnt = sqlSession.selectOne("com.dhn.client.rcs.mapper.SendRequest.req_rcs_count",param);
        return cnt;
    }

    @Override
    public void updateRCSGroupNo(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.rcs.mapper.SendRequest.req_rcs_group_update",param);
    }

    @Override
    public List<RequestBean> selectRCSRequests(SQLParameter param) throws Exception {
        return sqlSession.selectList("com.dhn.client.rcs.mapper.SendRequest.req_rcs_select", param);
    }

    @Override
    public void updateRCSSendComplete(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.rcs.mapper.SendRequest.req_rcs_sent_complete", param);
    }

    @Override
    public void updateRCSSendInit(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.rcs.mapper.SendRequest.req_rcs_sent_init", param);
    }
}
