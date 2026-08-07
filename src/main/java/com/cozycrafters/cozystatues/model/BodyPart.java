package com.cozycrafters.cozystatues.model;

/** The six boxes that make up a standing Minecraft player model. */
public enum BodyPart {
    RIGHT_LEG,
    LEFT_LEG,
    TORSO,
    RIGHT_ARM,
    LEFT_ARM,
    HEAD;

    /** True for the two arms, the only parts whose width depends on the skin model. */
    public boolean isArm() {
        return this == RIGHT_ARM || this == LEFT_ARM;
    }
}
