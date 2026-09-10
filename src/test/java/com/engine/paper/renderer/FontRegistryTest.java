package com.engine.paper.renderer;

import com.engine.paper.domain.style.FontRegistry;
import com.engine.paper.domain.style.TextStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FontRegistryTest {

    @Test
    @DisplayName("Should register in-memory font definition and retrieve correctly with fallback")
    void testFontRegistration() {
        FontRegistry registry = new FontRegistry();
        byte[] dummyFontBytes = new byte[]{1, 2, 3, 4, 5};

        registry.registerFont("Inter", dummyFontBytes);

        assertThat(registry.hasFont("Inter")).isTrue();
        assertThat(registry.hasFont("inter")).isTrue();
        assertThat(registry.hasFont("Roboto")).isFalse();

        FontRegistry.FontDefinition def = registry.getFont("Inter", false, false);
        assertThat(def).isNotNull();
        assertThat(def.fontData()).isEqualTo(dummyFontBytes);

        // Fallback to regular when bold variant not specifically registered
        FontRegistry.FontDefinition boldDef = registry.getFont("Inter", true, false);
        assertThat(boldDef).isNotNull();
        assertThat(boldDef.fontData()).isEqualTo(dummyFontBytes);
    }
}
