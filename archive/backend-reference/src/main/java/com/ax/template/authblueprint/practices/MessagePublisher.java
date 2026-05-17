package com.ax.template.authblueprint.practices;

public interface MessagePublisher {

    void publish(String topic, Object payload);
}
