package com.dhn.client.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Image {

    @JsonProperty("img_url")
    private String imgUrl;

    @JsonProperty("img_link")
    private String imgLink;
}
