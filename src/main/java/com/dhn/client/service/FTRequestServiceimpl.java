package com.dhn.client.service;

import com.dhn.client.bean.FTDataBean;
import com.dhn.client.bean.ImageBean;
import com.dhn.client.bean.Msg_Log;
import com.dhn.client.bean.SQLParameter;
import com.dhn.client.dao.FTRequestDAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class FTRequestServiceimpl implements FTRequestService {

    @Autowired
    private FTRequestDAO ftRequestDAO;

    @Override
    public int selectFTRequestCount(SQLParameter param) throws Exception {
        return ftRequestDAO.selectFTRequestCount(param);
    }

    @Override
    public void updateFTGroupNo(SQLParameter param) throws Exception {
        ftRequestDAO.updateFTGroupNo(param);
    }

    @Override
    public List<FTDataBean> selectFTRequests(SQLParameter param) throws Exception {
        return ftRequestDAO.selectFTRequests(param);
    }

    @Override
    public void updateFTInvalidData(List<String> invalidList, Msg_Log ml) throws Exception {
        ftRequestDAO.updateFTInvalidData(invalidList,ml);
    }

    @Override
    public void updateFTSendComplete(SQLParameter param) throws Exception {
        ftRequestDAO.updateFTSendComplete(param);
    }

    @Override
    public void updateFTSendInit(SQLParameter param) throws Exception {
        ftRequestDAO.updateFTSendInit(param);
    }

    @Override
    public int selectFtImageCount(SQLParameter param) throws Exception {
        return ftRequestDAO.selectFtImageCount(param);
    }

    @Override
    public void updateFTImageGroup(SQLParameter param) throws Exception {
        ftRequestDAO.updateFTImageGroup(param);
    }

    @Override
    public List<ImageBean> selectFtImage(SQLParameter param) throws Exception {
        return ftRequestDAO.selectFtImage(param);
    }

    @Override
    public void updateFTImageFail(SQLParameter param) throws Exception {
        ftRequestDAO.updateFTImageFail(param);
    }

    @Override
    public void updateFTImageUrl(SQLParameter param) throws Exception {
        ftRequestDAO.updateFTImageUrl(param);
    }

    @Override
    public int selectOldFTRequestCount(SQLParameter param) throws Exception {
        return ftRequestDAO.selectOldFTRequestCount(param);
    }

    @Override
    public void updateOldFTGroupNo(SQLParameter param) throws Exception {
        ftRequestDAO.updateOldFTGroupNo(param);
    }

    @Override
    public List<FTDataBean> selectOldFTRequests(SQLParameter param) throws Exception {
        return ftRequestDAO.selectOldFTRequests(param);
    }
}
