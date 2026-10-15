package me.waffles.additional.render;

import net.minecraft.text.Formatting;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class NametagTextTest {
    @Test
    public void frameLocalDecorationsDoNotAccumulateOnCachedNames() {
        NametagText cached = new NametagText(() -> new LiteralText("Player"));
        String base = cached.getFormattedString();
        cached.beginRender().append(" Badge");
        assertNotEquals(base, cached.getFormattedString());
        assertEquals(base, cached.beginRender().getFormattedString());
        cached.append(" Badge");
        assertEquals("Player Badge", cached.getString());
    }

    @Test
    public void formattedColorsAndSiblingsMatchOriginalText() {
        Text original = new LiteralText("Player");
        original.getStyle().setColor(Formatting.RED);
        original.append(new LiteralText(" Guild"));
        NametagText cached = new NametagText(original);
        assertEquals(original.getFormattedString(), cached.getFormattedString());
        assertEquals(original.getString(), cached.getString());
        assertEquals(original.getFormattedString(), cached.getFormattedString());
    }

    @Test
    public void rendererStyleAndSiblingChangesInvalidateFormattedText() {
        Text original = new LiteralText("Player");
        NametagText cached = new NametagText(original);
        String first = cached.getFormattedString();
        cached.getStyle().setBold(true);
        cached.getSiblings().add(new LiteralText(" Updated"));
        assertEquals(original.getFormattedString(), cached.getFormattedString());
        assertNotEquals(first, cached.getFormattedString());
        Text copy = cached.copy();
        copy.append(" Copy");
        assertNotEquals(copy.getFormattedString(), cached.getFormattedString());
    }
}
