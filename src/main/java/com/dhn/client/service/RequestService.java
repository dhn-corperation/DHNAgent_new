package com.dhn.client.service;

import com.dhn.client.bean.SQLParameter;
import com.dhn.client.bean.SendData;

import java.util.List;

public interface RequestService {

    // 알림톡
    int kaoSendDataCount(SQLParameter param) throws Exception;

    void kaoGroupUpdate(SQLParameter param)throws Exception;

    List<SendData> kaoSendDataList(SQLParameter param) throws Exception;

    void kaoSendSuccess(SQLParameter param) throws Exception;

    void kaoSendFail(SQLParameter param) throws Exception;

    void kaoSendRetry(SQLParameter param) throws Exception;

    // 문자
    int msgSendDataCount(SQLParameter param) throws Exception;

    void msgGroupUpdate(SQLParameter param) throws Exception;

    List<SendData> msgSendDataList(SQLParameter param) throws Exception;

    void msgSendSuccess(SQLParameter param) throws Exception;

    void msgSendFail(SQLParameter param) throws Exception;

    void msgSendRetry(SQLParameter param) throws Exception;

}
