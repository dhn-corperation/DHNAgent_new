package com.dhn.client.dao;

import com.dhn.client.bean.SQLParameter;

public interface MSGRequestDAO {

    public int msgTableCheck(SQLParameter param) throws Exception;

    public void msgTableCreate(SQLParameter param) throws Exception;

    public void msgLogTableCheck(String msgTable, String msgLogTable, String database) throws Exception;
}
