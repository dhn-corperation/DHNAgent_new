package com.dhn.client.service;

import com.dhn.client.bean.BMDataBean;
import com.dhn.client.bean.BMRequestBean;
import com.dhn.client.bean.Msg_Log;
import com.dhn.client.bean.SQLParameter;
import com.dhn.client.dao.BMRequestDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class BMRequestServiceimpl implements BMRequestService {

    @Autowired
    private BMRequestDAO bmRequestDAO;

    @Override
    public int selectBMRequestCount(SQLParameter param) throws Exception {
        return bmRequestDAO.selectBMRequestCount(param);
    }

    @Override
    public void updateBMGroupNo(SQLParameter param) throws Exception {
        bmRequestDAO.updateBMGroupNo(param);
    }

    @Override
    public List<BMDataBean> selectBMRequests(SQLParameter param) throws Exception {
        return bmRequestDAO.selectBMRequests(param);
    }

    @Override
    public void updateBMSendComplete(SQLParameter param) throws Exception {
        bmRequestDAO.updateBMSendComplete(param);
    }

    @Override
    public void updateBMSendInit(SQLParameter param) throws Exception {
        bmRequestDAO.updateBMSendInit(param);
    }

    @Override
    public void updateInvalidData(List<String> invalidList, Msg_Log ml) throws Exception {
        bmRequestDAO.updateInvalidData(invalidList,ml);
    }

    @Override
    public List<BMDataBean> selectBCRequests(SQLParameter param) throws Exception {
        return bmRequestDAO.selectBCRequests(param);
    }

}
