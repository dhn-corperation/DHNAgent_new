package com.dhn.client.dao;

import com.dhn.client.bean.BMDataBean;
import com.dhn.client.bean.BMRequestBean;
import com.dhn.client.bean.SQLParameter;

import java.util.List;

public interface BMRequestDAO {

    public int selectBMRequestCount(SQLParameter param) throws Exception;

    public void updateBMGroupNo(SQLParameter param) throws Exception;

    public List<BMDataBean> selectBMRequests(SQLParameter param) throws Exception;
}
