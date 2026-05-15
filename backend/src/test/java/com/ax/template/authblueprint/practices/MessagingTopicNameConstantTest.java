package com.ax.template.authblueprint.practices;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("PRACTICES")
@Tag("PRACTICES-MESSAGING-003")
class MessagingTopicNameConstantTest {

    @Test
    void practices_MESSAGING_003_topicNamesAreDeclaredAsPublicStaticFinalConstants() throws Exception {
        // Topic / routing-key names spread as inline string literals across the codebase
        // are a rename-breakage waiting to happen: one publisher and one consumer pick
        // different spellings of the same logical topic, the bug surfaces only in prod
        // when the queue silently goes empty. Forcing every topic through a single
        // constants class makes the topic vocabulary explicit, greppable, and renameable.
        Field f = MessageTopics.class.getDeclaredField("ORDER_PLACED");
        int mods = f.getModifiers();
        assertThat(Modifier.isPublic(mods)).as("ORDER_PLACED must be public").isTrue();
        assertThat(Modifier.isStatic(mods)).as("ORDER_PLACED must be static").isTrue();
        assertThat(Modifier.isFinal(mods)).as("ORDER_PLACED must be final").isTrue();
        assertThat(f.getType()).as("ORDER_PLACED must be a String constant").isEqualTo(String.class);

        assertThat(Modifier.isFinal(MessageTopics.class.getModifiers()))
                .as("MessageTopics must be a final class so the constants holder cannot be extended")
                .isTrue();
    }
}
