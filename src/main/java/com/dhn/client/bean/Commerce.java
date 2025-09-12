package com.dhn.client.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Commerce {
    private String title;

    @JsonProperty("regular_price")
    private Integer regularPrice;

    @JsonProperty("discount_price")
    private Integer discountPrice;

    @JsonProperty("discount_rate")
    private Integer discountRate;

    @JsonProperty("discount_fixed")
    private Integer discountFixed;
}
