package com.kazeshukufuku.centralvintage.content.pickling;

import net.minecraft.core.Direction;

import java.util.List;

public interface FermentingJarOutputController {
    List<Direction> centralvintage$getDisabledOutputs();

    Direction centralvintage$getPreferredOutput();

    void centralvintage$onWrenched(Direction face);
}
