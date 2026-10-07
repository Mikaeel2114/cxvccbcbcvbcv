package dev.apexban.bukkit;

import dev.apexban.core.platform.PlatformLogger;

import java.util.logging.Level;
import java.util.logging.Logger;

final class BukkitLogger implements PlatformLogger {
    private final Logger logger;

    BukkitLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warn(String message) {
        logger.warning(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }
}
