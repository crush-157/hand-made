package com.example.fn;

import com.oracle.emeatechnology.icecream.*;

public class PriceList {

    public String handleRequest(String input) {
        return new OnlineShop().getPriceListAsString();
    }
}
