package com.dhn.client.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Tail {

    @JsonProperty("url_pc")
    private String urlPc;

    @JsonProperty("url_mobile")
    private String urlMobile;

    @JsonProperty("scheme_ios")
    private String schemeIos;

    @JsonProperty("scheme_android")
    private String schemeAndroid;
}
