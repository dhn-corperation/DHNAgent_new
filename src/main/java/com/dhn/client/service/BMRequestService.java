package com.dhn.client.service;

import com.dhn.client.bean.*;

import java.util.List;

public interface BMRequestService {

    public int selectBMRequestCount(SQLParameter param) throws Exception;

    public void updateBMGroupNo(SQLParameter param) throws Exception;

    public List<BMDataBean> selectBMRequests(SQLParameter param) throws Exception;

    public void updateBMSendComplete(SQLParameter param) throws Exception;

    public void updateBMSendInit(SQLParameter param) throws Exception;

    public void updateInvalidData(List<String> invalidList, Msg_Log ml) throws Exception;

    public List<BMDataBean> selectBCRequests(SQLParameter param) throws Exception;

    public int selectBDRequestCount(SQLParameter param) throws Exception;

    public void updateBDGroupNo(SQLParameter param)throws Exception;

    public List<BMRequestBean> selectBDRequests(SQLParameter param) throws Exception;

    // 이미지 파일 업로드 형식
    public int selectIBMImageCount(SQLParameter param) throws Exception;

    public void updateIBMImageGroup(SQLParameter param) throws Exception;

    public List<ImageBean> selectIBMImage(SQLParameter param) throws Exception;

    public void updateIBMImageFail(SQLParameter param) throws Exception;

    public void updateIBMImageUploadFail(SQLParameter param) throws Exception;

    public void updateIBMImageUrl(SQLParameter param) throws Exception;
}
