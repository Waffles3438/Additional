package me.waffles.additional.mixin;

import me.waffles.additional.render.NametagText;
import me.waffles.additional.render.PlayerNametagCache;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.scoreboard.team.AbstractTeam;
import net.minecraft.scoreboard.team.Team;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;
import java.util.function.Supplier;

@Mixin(PlayerEntity.class)
public abstract class PlayerNametagCacheMixin implements PlayerNametagCache {
    @Shadow public abstract Text getDisplayName();
    @Shadow public abstract String getName();
    @Shadow public abstract AbstractTeam getScoreboardTeam();

    @Unique private int additional$nameTick;
    @Unique private NametagText additional$nameText;
    @Unique private final Supplier<Text> additional$displayName = this::getDisplayName;
    @Unique private AbstractTeam additional$nameTeam;
    @Unique private String additional$name, additional$prefix, additional$suffix;
    @Unique private volatile long additional$botState = Long.MIN_VALUE;

    @Override
    public Text additional$getNametagText(int tick) {
        AbstractTeam team = getScoreboardTeam();
        // Preserve custom team implementations without assuming their formatting.
        if (team != null && !(team instanceof Team)) return getDisplayName();
        String name = getName();
        String prefix = team == null ? null : ((Team) team).getPrefix();
        String suffix = team == null ? null : ((Team) team).getSuffix();
        if (additional$nameText == null || additional$nameTick != tick
                || additional$nameTeam != team || !Objects.equals(additional$name, name)
                || !Objects.equals(additional$prefix, prefix) || !Objects.equals(additional$suffix, suffix)) {
            // The original method still runs, including other mods' changes.
            // Cache only its render string; chat retains the original hover/click data.
            additional$nameText = new NametagText(additional$displayName);
            additional$nameTick = tick;
            additional$nameTeam = team;
            additional$name = name;
            additional$prefix = prefix;
            additional$suffix = suffix;
        }
        return additional$nameText.beginRender();
    }

    @Override
    public Boolean additional$getBot(long generation) {
        long state = additional$botState;
        return (state >>> 1) == generation ? (state & 1L) != 0 : null;
    }

    @Override
    public void additional$setBot(long generation, boolean bot) {
        additional$botState = (generation << 1) | (bot ? 1L : 0L);
    }
}
