package com.dhn.client.service;

import com.dhn.client.bean.BMDataBean;
import com.dhn.client.bean.BMRequestBean;
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

}
