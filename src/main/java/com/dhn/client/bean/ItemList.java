package com.dhn.client.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ItemList {

    private String title;

    @JsonProperty("img_url")
    private String imgUrl;

    @JsonProperty("scheme_android")
    private String schemeAndroid;

    @JsonProperty("scheme_ios")
    private String schemeIos;

    @JsonProperty("url_mobile")
    private String urlMobile;

    @JsonProperty("url_pc")
    private String urlPc;
}
