package com.demo;

/** wire_source: java_method_returns — one SPI adapter. */
public class AlphaChannel implements DemoChannel {

    @Override
    public String channelName() { return "chan-x"; }
}
