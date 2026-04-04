package com.ax.template.authblueprint.auth;

public record OAuthLinkRequest(String provider, String code, String state) {}
