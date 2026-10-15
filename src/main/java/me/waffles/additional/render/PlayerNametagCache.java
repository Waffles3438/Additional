package me.waffles.additional.render;

import net.minecraft.text.Text;

/** Cache data belongs to the player entity and is reclaimed with it. */
public interface PlayerNametagCache {
    Text additional$getNametagText(int tick);
    Boolean additional$getBot(long generation);
    void additional$setBot(long generation, boolean bot);
}
