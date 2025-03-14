package com.dhn.client.dao;

import com.dhn.client.bean.SQLParameter;
import com.dhn.client.bean.SendData;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class RequestDAOImpl implements RequestDAO{

    @Autowired
    private SqlSession sqlSession;

    @Override
    public int kaoSendDataCount(SQLParameter param) throws Exception {
        return sqlSession.selectOne("com.dhn.client.kakao.mapper.SendRequest.kaoSendDataCount", param);
    }

    @Override
    public void kaoGroupUpdate(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.kaoGroupUpdate", param);
    }

    @Override
    public List<SendData> kaoSendDataList(SQLParameter param) throws Exception {
        return sqlSession.selectList("com.dhn.client.kakao.mapper.SendRequest.kaoSendDataList", param);
    }

    @Override
    public void kaoSendSuccess(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.kaoSendSuccess", param);
    }

    @Override
    public void kaoSendFail(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.kaoSendFail", param);
    }

    @Override
    public void kaoSendRetry(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.kakao.mapper.SendRequest.kaoSendRetry", param);
    }

    @Override
    public int msgSendDataCount(SQLParameter param) throws Exception {
        return sqlSession.selectOne("com.dhn.client.msg.mapper.SendRequest.msgSendDataCount", param);
    }

    @Override
    public void msgGroupUpdate(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.msgGroupUpdate", param);
    }

    @Override
    public List<SendData> msgSendDataList(SQLParameter param) throws Exception {
        return sqlSession.selectList("com.dhn.client.msg.mapper.SendRequest.msgSendDataList",param);
    }

    @Override
    public void msgSendSuccess(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.msgSendSuccess",param);
    }

    @Override
    public void msgSendFail(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.msgSendFailUpdate",param);
        sqlSession.insert("com.dhn.client.msg.mapper.SendRequest.msgSendFailInsert",param);
        sqlSession.delete("com.dhn.client.msg.mapper.SendRequest.msgSendFailDelete",param);
    }

    @Override
    public void msgSendRetry(SQLParameter param) throws Exception {
        sqlSession.update("com.dhn.client.msg.mapper.SendRequest.msgSendRetry",param);
    }
}
