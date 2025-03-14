package com.dhn.client.dao;

import com.dhn.client.bean.SQLParameter;
import com.dhn.client.bean.SendData;

import java.util.List;

public interface RequestDAO {

    int kaoSendDataCount(SQLParameter param) throws Exception;

    void kaoGroupUpdate(SQLParameter param) throws Exception;

    List<SendData> kaoSendDataList(SQLParameter param) throws Exception;

    void kaoSendSuccess(SQLParameter param) throws Exception;

    void kaoSendFail(SQLParameter param) throws Exception;

    void kaoSendRetry(SQLParameter param) throws Exception;

    int msgSendDataCount(SQLParameter param) throws Exception;

    void msgGroupUpdate(SQLParameter param) throws Exception;

    List<SendData> msgSendDataList(SQLParameter param) throws Exception;

    void msgSendSuccess(SQLParameter param) throws Exception;

    void msgSendFail(SQLParameter param) throws Exception;

    void msgSendRetry(SQLParameter param) throws Exception;

}
