package com.dhn.client.service;

import com.dhn.client.bean.ImageBean;
import com.dhn.client.bean.KAORequestBean;
import com.dhn.client.bean.Msg_Log;
import com.dhn.client.bean.SQLParameter;
import com.dhn.client.dao.KAORequestDAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class KAORequestServiceimpl implements KAORequestService{

    @Autowired
    private KAORequestDAO kaoRequestDAO;

    @Override
    public int selectKAORequestCount(SQLParameter param) throws Exception {
        return kaoRequestDAO.selectKAORequestCount(param);
    }

    @Override
    public void updateKAOGroupNo(SQLParameter param) throws Exception {
        kaoRequestDAO.updateKAOGroupNo(param);
    }

    @Override
    public List<KAORequestBean> selectKAORequests(SQLParameter param) throws Exception {
        return kaoRequestDAO.selectKAORequests(param);
    }

    @Override
    public void updateKAOSendComplete(SQLParameter param) throws Exception {
        kaoRequestDAO.updateKAOSendComplete(param);
    }

    @Override
    public void updateKAOSendInit(SQLParameter param) throws Exception {
        kaoRequestDAO.updateKAOSendInit(param);
    }

    @Override
    public void updateKAOAuthFail(SQLParameter param) throws Exception {
        kaoRequestDAO.updateKAOAuthFail(param);
    }

    @Override
    public void kaoJsonErrMessage(SQLParameter param, List<String> jsonErrMsgid) throws Exception {
        kaoRequestDAO.kaoJsonErrMessage(param, jsonErrMsgid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void kaoResultInsert(Msg_Log ml) throws Exception {
        kaoRequestDAO.kaoResultInsert(ml);
    }

    @Override
    public int log_move_count(SQLParameter param) throws Exception {
        return kaoRequestDAO.log_move_count(param);
    }

    @Override
    public void update_log_move_groupNo(SQLParameter param) throws Exception {
        kaoRequestDAO.update_log_move_groupNo(param);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void log_move(SQLParameter param) throws Exception {
        kaoRequestDAO.log_move(param);
    }

    @Override
    public int selectFTRequestCount(SQLParameter param) throws Exception {
        return kaoRequestDAO.selectFTRequestCount(param);
    }

    @Override
    public void updateFTGroupNo(SQLParameter param) throws Exception {
        kaoRequestDAO.updateFTGroupNo(param);
    }

    @Override
    public List<KAORequestBean> selectFTRequests(SQLParameter param) throws Exception {
        return kaoRequestDAO.selectFTRequests(param);
    }

    @Override
    public int selectFtImageCount(SQLParameter param) throws Exception {
        return kaoRequestDAO.selectFtImageCount(param);
    }

    @Override
    public List<ImageBean> selectFtImage(SQLParameter param) throws Exception {
        return kaoRequestDAO.selectFtImage(param);
    }

    @Override
    public void updateFTImageUrl(SQLParameter param) throws Exception {
        kaoRequestDAO.updateFTImageUrl(param);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFTImageFail(SQLParameter param) throws Exception {
        kaoRequestDAO.updateFTImageFail(param);
    }

}
