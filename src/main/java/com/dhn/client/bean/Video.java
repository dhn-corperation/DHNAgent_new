package com.dhn.client.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Video {

    @JsonProperty("video_url")
    private String videoUrl;

    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;
}
