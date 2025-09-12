package com.dhn.client.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Button {

    private String name;
    private String type;

    @JsonProperty("scheme_android")
    private String schemeAndroid;

    @JsonProperty("scheme_ios")
    private String schemeIos;

    @JsonProperty("url_mobile")
    private String urlMobile;

    @JsonProperty("url_pc")
    private String urlPc;

    @JsonProperty("chat_extra")
    private String chatExtra;

    @JsonProperty("chat_event")
    private String chatEvent;

    @JsonProperty("biz_form_key")
    private String bizFormKey;

}
