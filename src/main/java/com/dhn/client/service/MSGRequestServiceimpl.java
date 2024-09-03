package com.dhn.client.service;

import com.dhn.client.bean.SQLParameter;
import com.dhn.client.dao.MSGRequestDAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
