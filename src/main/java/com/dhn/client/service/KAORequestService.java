package com.dhn.client.service;

import com.dhn.client.bean.KAORequestBean;
import com.dhn.client.bean.Msg_Log;
import com.dhn.client.bean.SQLParameter;

import java.util.List;

public interface KAORequestService {

    public void atTableCheck(SQLParameter param) throws Exception;

    public void atLogTableCheck(String atTable, String atLogTable, String database) throws Exception;

    public int selectKAORequestCount(SQLParameter param) throws Exception;

    public void updateKAOGroupNo(SQLParameter param) throws Exception;

    public List<KAORequestBean> selectKAORequests(SQLParameter param) throws Exception;

    public void updateKAOSendComplete(SQLParameter param) throws Exception;

    public void updateKAOSendInit(SQLParameter param) throws Exception;

    public void updateKAOAuthFail(SQLParameter paramCopy) throws Exception;

    public void kaoJsonErrMessage(SQLParameter param, List<String> jsonErrMsgid) throws Exception;

    public void kaoResultInsert(Msg_Log ml) throws Exception;

    public int log_move_count(SQLParameter param) throws Exception;

    public void update_log_move_groupNo(SQLParameter param) throws Exception;

    public void log_move(SQLParameter param) throws Exception;

}
