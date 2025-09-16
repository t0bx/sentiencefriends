package de.t0bx.sentiencefriends.api;

import lombok.Getter;

import java.util.logging.Logger;

public class PacketController {
    @Getter
    private static final PacketController instance = new PacketController();

    @Getter
    private final Logger logger = Logger.getLogger("PacketController");
}
