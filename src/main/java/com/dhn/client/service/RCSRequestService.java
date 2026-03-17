package com.dhn.client.service;

import com.dhn.client.bean.RequestBean;
import com.dhn.client.bean.SQLParameter;

import java.util.List;

public interface RCSRequestService {

    public int selectRCSRequestCount(SQLParameter param) throws Exception;

    public void updateRCSGroupNo(SQLParameter param) throws Exception;

    public List<RequestBean> selectRCSRequests(SQLParameter param) throws Exception;

    public void updateRCSSendComplete(SQLParameter param) throws Exception;

    public void updateRCSSendInit(SQLParameter param) throws Exception;
}
