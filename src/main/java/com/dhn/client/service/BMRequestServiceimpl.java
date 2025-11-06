package com.dhn.client.service;

import com.dhn.client.bean.*;
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

    @Override
    public int selectBDRequestCount(SQLParameter param) throws Exception {
        return bmRequestDAO.selectBDRequestCount(param);
    }

    @Override
    public void updateBDGroupNo(SQLParameter param) throws Exception {
        bmRequestDAO.updateBDGroupNo(param);
    }

    @Override
    public List<BMRequestBean> selectBDRequests(SQLParameter param) throws Exception {
        return bmRequestDAO.selectBDRequests(param);
    }

    @Override
    public int selectIBMImageCount(SQLParameter param) throws Exception {
        return bmRequestDAO.selectIBMImageCount(param);
    }

    @Override
    public void updateIBMImageGroup(SQLParameter param) throws Exception {
        bmRequestDAO.updateIBMImageGroup(param);
    }

    @Override
    public List<ImageBean> selectIBMImage(SQLParameter param) throws Exception {
        return bmRequestDAO.selectIBMImage(param);
    }

    @Override
    public void updateIBMImageFail(SQLParameter param) throws Exception {
        bmRequestDAO.updateIBMImageFail(param);
    }

    @Override
    public void updateIBMImageUploadFail(SQLParameter param) throws Exception {
        bmRequestDAO.updateIBMImageUploadFail(param);
    }

    @Override
    public void updateIBMImageUrl(SQLParameter param) throws Exception {
        bmRequestDAO.updateIBMImageUrl(param);
    }

    @Override
    public int selectIBMRequestCount(SQLParameter param) throws Exception {
        return bmRequestDAO.selectIBMRequestCount(param);
    }

    @Override
    public void updateIBMGroupNo(SQLParameter param) throws Exception {
        bmRequestDAO.updateIBMGroupNo(param);
    }

    @Override
    public List<BMDataBean> selectIBMRequests(SQLParameter param) throws Exception {
        return bmRequestDAO.selectIBMRequests(param);
    }

}
