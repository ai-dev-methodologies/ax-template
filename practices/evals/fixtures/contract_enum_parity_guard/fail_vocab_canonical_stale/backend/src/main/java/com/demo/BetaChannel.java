package com.demo;

/** wire_source: java_method_returns — the second SPI adapter. */
public class BetaChannel implements DemoChannel {

    @Override
    public String channelName() { return "chan-y"; }
}
