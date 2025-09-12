package com.dhn.client.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Head {
    private String header;
    private String content;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("url_mobile")
    private String urlMobile;

    @JsonProperty("url_pc")
    private String urlPc;

    @JsonProperty("scheme_android")
    private String schemeAndroid;

    @JsonProperty("scheme_ios")
    private String schemeIos;
}
