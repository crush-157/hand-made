package com.example.fn;

import com.oracle.emeatechnology.icecream.*;

public class OnlineOrder {

    public Double handleRequest(OrderMessage input) {
      Shop s = new OnlineShop();
      return s.order(input.flavour, input.quantity);
    }
}
