package com.dhn.client.service;

import com.dhn.client.bean.RequestBean;
import com.dhn.client.bean.SQLParameter;
import com.dhn.client.dao.RCSRequestDAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class RCSRequestServiceImpl implements RCSRequestService {

    @Autowired
    private RCSRequestDAO rcsRequestDAO;

    @Override
    public int selectRCSRequestCount(SQLParameter param) throws Exception {
        return rcsRequestDAO.selectRCSRequestCount(param);
    }

    @Override
    public void updateRCSGroupNo(SQLParameter param) throws Exception {
        rcsRequestDAO.updateRCSGroupNo(param);
    }

    @Override
    public List<RequestBean> selectRCSRequests(SQLParameter param) throws Exception {
        return rcsRequestDAO.selectRCSRequests(param);
    }

    @Override
    public void updateRCSSendComplete(SQLParameter param) throws Exception {
        rcsRequestDAO.updateRCSSendComplete(param);
    }

    @Override
    public void updateRCSSendInit(SQLParameter param) throws Exception {
        rcsRequestDAO.updateRCSSendInit(param);
    }
}
