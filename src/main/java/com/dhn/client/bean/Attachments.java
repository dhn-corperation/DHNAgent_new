package com.dhn.client.bean;

import lombok.Data;

import java.util.List;

@Data
public class Attachments {
    private List<Button> button;
    private Image image;
    private Item item;
    private Coupon coupon;
    private Commerce commerce;
    private Video video;

}
