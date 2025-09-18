package com.dhn.client.service;

import com.dhn.client.bean.BMDataBean;
import com.dhn.client.bean.BMRequestBean;
import com.dhn.client.bean.Msg_Log;
import com.dhn.client.bean.SQLParameter;

import java.util.List;

public interface BMRequestService {

    public int selectBMRequestCount(SQLParameter param) throws Exception;

    public void updateBMGroupNo(SQLParameter param) throws Exception;

    public List<BMDataBean> selectBMRequests(SQLParameter param) throws Exception;

    public void updateBMSendComplete(SQLParameter param) throws Exception;

    public void updateBMSendInit(SQLParameter param) throws Exception;

    public void updateInvalidData(List<String> invalidList, Msg_Log ml) throws Exception;

    public List<BMDataBean> selectBCRequests(SQLParameter param) throws Exception;

    public int selectBDRequestCount(SQLParameter param) throws Exception;

    public void updateBDGroupNo(SQLParameter param)throws Exception;

    public List<BMRequestBean> selectBDRequests(SQLParameter param) throws Exception;
}
