package dev.apexban.velocity;

import dev.apexban.core.platform.PlatformLogger;
import org.slf4j.Logger;

final class VelocityLogger implements PlatformLogger {
    private final Logger logger;

    VelocityLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }
}
