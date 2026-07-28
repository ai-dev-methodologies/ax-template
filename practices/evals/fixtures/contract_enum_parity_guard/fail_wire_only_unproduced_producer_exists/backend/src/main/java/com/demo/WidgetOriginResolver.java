package com.demo;

/**
 * The producer the `origin` entry's absence probe asserts does NOT exist. Its arrival is
 * exactly the event that must revoke an `unproduced` exemption: the block is now backed
 * by real code and has to be bound to it.
 */
public class WidgetOriginResolver {

    public String resolve() { return "inbound"; }
}
