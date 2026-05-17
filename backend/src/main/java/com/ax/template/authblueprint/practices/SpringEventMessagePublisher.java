package com.ax.template.authblueprint.practices;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class SpringEventMessagePublisher implements MessagePublisher {

    private final ApplicationEventPublisher events;

    public SpringEventMessagePublisher(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Override
    public void publish(String topic, Object payload) {
        events.publishEvent(new TopicMessageEvent(topic, payload));
    }
}
