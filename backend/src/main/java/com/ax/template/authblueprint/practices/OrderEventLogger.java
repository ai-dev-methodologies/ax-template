package com.ax.template.authblueprint.practices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventLogger {

    private static final Logger log = LoggerFactory.getLogger(OrderEventLogger.class);

    @EventListener
    public void onTopicMessage(TopicMessageEvent event) {
        if (MessageTopics.ORDER_PLACED.equals(event.topic())
                && event.payload() instanceof OrderPlacedEvent placed) {
            log.info("order placed: id={} amount={}cents", placed.orderId(), placed.amountCents());
        }
    }
}
