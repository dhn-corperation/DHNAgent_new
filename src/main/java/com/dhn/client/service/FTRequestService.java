package com.dhn.client.service;

import com.dhn.client.bean.FTDataBean;
import com.dhn.client.bean.Msg_Log;
import com.dhn.client.bean.SQLParameter;

import java.util.List;

public interface FTRequestService {

    public int selectFTRequestCount(SQLParameter param) throws Exception;

    public void updateFTGroupNo(SQLParameter param) throws Exception;

    public List<FTDataBean> selectFTRequests(SQLParameter param) throws Exception;

    public void updateFTInvalidData(List<String> invalidList, Msg_Log ml) throws Exception;

    public void updateFTSendComplete(SQLParameter param) throws Exception;

    public void updateFTSendInit(SQLParameter param) throws Exception;
}
