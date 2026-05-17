package com.ax.template.authblueprint.practices;

import org.springframework.stereotype.Service;

@Service
public class OrderEventPublisher {

    private final MessagePublisher publisher;

    public OrderEventPublisher(MessagePublisher publisher) {
        this.publisher = publisher;
    }

    public void publishOrderPlaced(OrderPlacedEvent event) {
        publisher.publish(MessageTopics.ORDER_PLACED, event);
    }
}
