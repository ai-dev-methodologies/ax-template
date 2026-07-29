package com.demo;

/**
 * The SPI itself: an abstract declaration has no body, so it contributes no literal —
 * the interface must never widen the extracted set.
 */
public interface DemoChannel {

    String channelName();
}
