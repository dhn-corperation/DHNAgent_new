package com.dhn.client.service;

import com.dhn.client.bean.MMSImageBean;
import com.dhn.client.bean.Msg_Log;
import com.dhn.client.bean.RequestBean;
import com.dhn.client.bean.SQLParameter;
import com.dhn.client.dao.MSGRequestDAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class MSGRequestServiceimpl implements MSGRequestService {

    @Autowired
    private MSGRequestDAO  msgRequestDAO;

    @Override
    public void msgTableCheck(SQLParameter param) throws Exception {
        int result = msgRequestDAO.msgTableCheck(param);

        try{
            if(result == 0){
                msgRequestDAO.msgTableCreate(param);
                log.info("{} 테이블 생성 완료",param.getMsg_table());
            }else{
                log.info("{} 테이블이 존재합니다.",param.getMsg_table());
            }
        }catch (Exception e){
            log.error("{} 테이블 생성 중 오류 발생: {}", param.getMsg_table(), e.getMessage());
            throw e;
        }
    }

    @Override
    public void msgLogTableCheck(String msgTable, String msgLogTable, String database) throws Exception{
        msgRequestDAO.msgLogTableCheck(msgTable, msgLogTable, database);
    }

    @Override
    public int selectSMSReqeustCount(SQLParameter param) throws Exception {
        return msgRequestDAO.selectSMSReqeustCount(param);
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
    public void updateMSGAuthFail(SQLParameter param) throws Exception {
        msgRequestDAO.updateMSGAuthFail(param);
    }

    @Override
    public void jsonErrMessage(SQLParameter param, List<String> jsonErrMsgid) throws Exception {
        msgRequestDAO.jsonErrMessage(param,jsonErrMsgid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
    public void log_move(SQLParameter param) throws Exception {
        msgRequestDAO.log_move(param);
    }

    @Override
    public int selectLMSReqeustCount(SQLParameter param) throws Exception {
        return msgRequestDAO.selectLMSReqeustCount(param);
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
    public int selectMMSReqeustCount(SQLParameter param) throws Exception {
        return msgRequestDAO.selectMMSReqeustCount(param);
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
    public List<MMSImageBean> selectMMSImage(SQLParameter param) throws Exception {
        return msgRequestDAO.selectMMSImage(param);
    }

    @Override
    public void updateMMSImageGroup(SQLParameter param) throws Exception {
        msgRequestDAO.updateMMSImageGroup(param);
    }
}
