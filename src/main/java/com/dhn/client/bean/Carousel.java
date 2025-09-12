package com.dhn.client.bean;

import lombok.Data;

import java.util.List;

@Data
public class Carousel {
    private Head head;
    private List<CarouselItem> list;
    private Tail tail;
}
