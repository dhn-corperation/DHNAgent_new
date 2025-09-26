package com.dhn.client.service;

import com.dhn.client.bean.FTDataBean;
import com.dhn.client.bean.ImageBean;
import com.dhn.client.bean.Msg_Log;
import com.dhn.client.bean.SQLParameter;

import java.util.List;

public interface FTRequestService {

    public int selectFTRequestCount(SQLParameter param) throws Exception;

    public void updateFTGroupNo(SQLParameter param) throws Exception;

    public List<FTDataBean> selectFTRequests(SQLParameter param) throws Exception;

    public void updateFTInvalidData(List<String> invalidList, Msg_Log ml) throws Exception;

    public void updateFTSendComplete(SQLParameter param) throws Exception;

    public void updateFTSendInit(SQLParameter param) throws Exception;

    // (구) 친구톡
    public int selectFtImageCount(SQLParameter param) throws Exception;

    public void updateFTImageGroup(SQLParameter param) throws Exception;

    public List<ImageBean> selectFtImage(SQLParameter param) throws Exception;

    public void updateFTImageFail(SQLParameter param) throws Exception;

    public void updateFTImageUrl(SQLParameter param) throws Exception;

    public int selectOldFTRequestCount(SQLParameter param) throws Exception;

    public void updateOldFTGroupNo(SQLParameter param) throws Exception;

    public List<FTDataBean> selectOldFTRequests(SQLParameter param) throws Exception;
}
