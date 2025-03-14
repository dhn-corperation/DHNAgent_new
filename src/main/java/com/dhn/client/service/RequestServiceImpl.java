package com.dhn.client.service;

import com.dhn.client.bean.SQLParameter;
import com.dhn.client.bean.SendData;
import com.dhn.client.dao.RequestDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class RequestServiceImpl implements RequestService{

    @Autowired
    private RequestDAO requestDAO;

    @Override
    public int kaoSendDataCount(SQLParameter param) throws Exception {
        return requestDAO.kaoSendDataCount(param);
    }

    @Override
    public void kaoGroupUpdate(SQLParameter param) throws Exception {
        requestDAO.kaoGroupUpdate(param);
    }

    @Override
    public List<SendData> kaoSendDataList(SQLParameter param) throws Exception {
        return requestDAO.kaoSendDataList(param);
    }

    @Override
    public void kaoSendSuccess(SQLParameter param) throws Exception {
        requestDAO.kaoSendSuccess(param);
    }

    @Override
    public void kaoSendFail(SQLParameter param) throws Exception {
        requestDAO.kaoSendFail(param);
    }

    @Override
    public void kaoSendRetry(SQLParameter param) throws Exception {
        requestDAO.kaoSendRetry(param);
    }

    @Override
    public int msgSendDataCount(SQLParameter param) throws Exception {
        return requestDAO.msgSendDataCount(param);
    }

    @Override
    public void msgGroupUpdate(SQLParameter param) throws Exception {
        requestDAO.msgGroupUpdate(param);
    }

    @Override
    public List<SendData> msgSendDataList(SQLParameter param) throws Exception {
        return requestDAO.msgSendDataList(param);
    }

    @Override
    public void msgSendSuccess(SQLParameter param) throws Exception {
        requestDAO.msgSendSuccess(param);
    }

    @Override
    public void msgSendFail(SQLParameter param) throws Exception {
        requestDAO.msgSendFail(param);
    }

    @Override
    public void msgSendRetry(SQLParameter param) throws Exception {
        requestDAO.msgSendRetry(param);
    }
}
