package com.dhn.client.bean;

import lombok.Data;

@Data
public class RCSRequestBean {
    private String msgid;
    private String messagetype;
    private String msg;
    private String msgsms;
    private String pinvoice;
    private String phn;
    private String regdt;
    private String reservedt;
    private String smskind;
    private String smslmstit;
    private String smssender;
    private String kisacode;
    private String title;
    private String rcsbaseid;
    private String rcsbutton;
    private String rcsagencyid;
    private String rcsagencykey;
    private String rcsbrandkey;
}
