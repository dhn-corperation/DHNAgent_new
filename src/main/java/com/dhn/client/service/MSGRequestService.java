package com.dhn.client.service;

import com.dhn.client.bean.SQLParameter;

public interface MSGRequestService {

    public void msgTableCheck(SQLParameter param) throws Exception;

    public void msgLogTableCheck(String msgTable, String msgLogTable, String database) throws Exception;
}
