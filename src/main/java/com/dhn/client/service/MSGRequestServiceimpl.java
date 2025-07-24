package com.dhn.client.service;

import com.dhn.client.bean.ImageBean;
import com.dhn.client.bean.Msg_Log;
import com.dhn.client.bean.RequestBean;
import com.dhn.client.bean.SQLParameter;
import com.dhn.client.dao.MSGRequestDAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class MSGRequestServiceimpl implements MSGRequestService {

    @Autowired
    private MSGRequestDAO  msgRequestDAO;

    @Override
    public int selectSMSRequestCount(SQLParameter param) throws Exception {
        return msgRequestDAO.selectSMSRequestCount(param);
    }

    @Override
    public void updateSMSGroupNo(SQLParameter param) throws Exception {
        msgRequestDAO.updateSMSGroupNo(param);
    }

    @Override
    public List<RequestBean> selectSMSRequests(SQLParameter param) throws Exception {
        return msgRequestDAO.selectSMSRequests(param);
    }

    @Override
    public void updateSMSSendComplete(SQLParameter param) throws Exception {
        msgRequestDAO.updateSMSSendComplete(param);
    }

    @Override
    public void updateSMSSendInit(SQLParameter param) throws Exception {
        msgRequestDAO.updateSMSSendInit(param);
    }

    @Override
    public void msgResultInsert(Msg_Log ml) throws Exception {
        msgRequestDAO.msgResultInsert(ml);
    }

    @Override
    public int log_move_count(SQLParameter param) throws Exception {
        return msgRequestDAO.log_move_count(param);
    }

    @Override
    public void update_log_move_groupNo(SQLParameter param) throws Exception {
        msgRequestDAO.update_log_move_groupNo(param);
    }

    @Override
    public void log_move(SQLParameter param) throws Exception {
        msgRequestDAO.log_move(param);
    }

    @Override
    public int selectLMSRequestCount(SQLParameter param) throws Exception {
        return msgRequestDAO.selectLMSRequestCount(param);
    }

    @Override
    public void updateLMSGroupNo(SQLParameter param) throws Exception {
        msgRequestDAO.updateLMSGroupNo(param);
    }

    @Override
    public List<RequestBean> selectLMSRequests(SQLParameter param) throws Exception {
        return msgRequestDAO.selectLMSRequests(param);
    }

    @Override
    public int selectMMSRequestCount(SQLParameter param) throws Exception {
        return msgRequestDAO.selectMMSRequestCount(param);
    }

    @Override
    public void updateMMSGroupNo(SQLParameter param) throws Exception {
        msgRequestDAO.updateMMSGroupNo(param);
    }

    @Override
    public List<RequestBean> selectMMSRequests(SQLParameter param) throws Exception {
        return msgRequestDAO.selectMMSRequests(param);
    }

    @Override
    public int selectMMSImageCount(SQLParameter param) throws Exception {
        return msgRequestDAO.selectMMSImageCount(param);
    }

    @Override
    public List<ImageBean> selectMMSImage(SQLParameter param) throws Exception {
        return msgRequestDAO.selectMMSImage(param);
    }

    @Override
    public void updateMMSImageGroup(SQLParameter param) throws Exception {
        msgRequestDAO.updateMMSImageGroup(param);
    }

    @Override
    public void updateMMSImageFail(SQLParameter param) throws Exception {
        msgRequestDAO.updateMMSImageFail(param);
    }

    @Override
    public int selectOTPRequestCount(SQLParameter param) throws Exception {
        return msgRequestDAO.selectOTPRequestCount(param);
    }

    @Override
    public void updateOTPGroupNo(SQLParameter param) throws Exception {
        msgRequestDAO.updateOTPGroupNo(param);
    }

    @Override
    public List<RequestBean> selectOTPRequests(SQLParameter param) throws Exception {
        return msgRequestDAO.selectOTPRequests(param);
    }
}
