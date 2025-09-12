package com.dhn.client.dao;

import com.dhn.client.bean.BMDataBean;
import com.dhn.client.bean.BMRequestBean;
import com.dhn.client.bean.SQLParameter;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
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
}
