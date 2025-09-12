package com.dhn.client.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CarouselItem {

    private String header;
    private String message;

    @JsonProperty("additional_content")
    private String additionalContent;

    private Attachments attachment;

}
