package io.github.tt432.ccaforge;

import dev.onyxstudios.cca.internal.base.ComponentsInternals;
import dev.onyxstudios.cca.internal.entity.CardinalComponentsEntity;
import io.github.tt432.ccaforge.net.CcaNetHandler;
import net.minecraftforge.fml.common.Mod;


@Mod(Ccaforge.MOD_ID)
public class Ccaforge {
    public static final String MOD_ID = "ccaforge";

    public Ccaforge() {
        CcaNetHandler.init();
        CardinalComponentsEntity.init();
        ComponentsInternals.init();
    }
}
