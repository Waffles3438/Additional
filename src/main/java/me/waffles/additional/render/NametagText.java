package me.waffles.additional.render;

import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/** Render-only text: the original component's styles are already in this string. */
public final class NametagText implements Text {
    private final Supplier<Text> source;
    private final String baseline;
    private Text delegate;
    private String formatted;

    public NametagText(Text delegate) {
        this(() -> delegate);
    }

    public NametagText(Supplier<Text> source) {
        this.source = source;
        baseline = source.get().getFormattedString();
        formatted = baseline;
    }

    public NametagText beginRender() {
        // A renderer's temporary style/sibling changes must not accumulate on
        // later frames. The normal draw path retains only the formatted string.
        delegate = null;
        formatted = baseline;
        return this;
    }

    private Text delegate() {
        if (delegate == null) delegate = source.get();
        return delegate;
    }

    @Override
    public String getFormattedString() {
        if (formatted == null) formatted = delegate().getFormattedString();
        return formatted;
    }

    @Override public Text setStyle(Style style) { formatted = null; delegate().setStyle(style); return this; }
    @Override public Style getStyle() { formatted = null; return delegate().getStyle(); }
    @Override public Text append(String text) { formatted = null; delegate().append(text); return this; }
    @Override public Text append(Text text) { formatted = null; delegate().append(text); return this; }
    @Override public String getContent() { return delegate().getContent(); }
    @Override public String getString() { return delegate().getString(); }
    @Override public List<Text> getSiblings() { formatted = null; return delegate().getSiblings(); }
    @Override public Text copy() { return new NametagText(delegate().copy()); }
    @Override public Iterator<Text> iterator() { formatted = null; return delegate().iterator(); }
}
