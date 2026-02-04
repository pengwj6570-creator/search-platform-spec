package com.search.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Logback configuration utility for search platform.
 * Provides programmatic configuration of logging without external XML files.
 * Thread-safe for concurrent initialization.
 */
public class LoggingConfig {

    private static final String DEFAULT_LOG_PATTERN =
            "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";

    private static final AtomicBoolean configured = new AtomicBoolean(false);

    private LoggingConfig() {
        // Utility class - prevent instantiation
    }

    /**
     * Initialize default logging configuration with console output.
     * This should be called once at application startup.
     * Thread-safe - only the first call will configure logging.
     */
    public static void init() {
        init(Level.INFO);
    }

    /**
     * Initialize logging configuration with specified root log level.
     * Thread-safe - only the first call will configure logging.
     *
     * @param rootLevel the root logger level
     */
    public static void init(Level rootLevel) {
        if (!configured.compareAndSet(false, true)) {
            return;
        }

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();

        // Configure console appender
        ConsoleAppender<ILoggingEvent> consoleAppender = createConsoleAppender(loggerContext);
        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(consoleAppender);
        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(rootLevel);
    }

    /**
     * Initialize logging configuration with console and file appenders.
     * Thread-safe - only the first call will configure logging.
     *
     * @param rootLevel  the root logger level
     * @param logFilePath the path to the log file
     */
    public static void initWithFile(Level rootLevel, String logFilePath) {
        if (!configured.compareAndSet(false, true)) {
            return;
        }

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();

        // Configure console appender
        ConsoleAppender<ILoggingEvent> consoleAppender = createConsoleAppender(loggerContext);
        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(consoleAppender);

        // Configure file appender
        RollingFileAppender<ILoggingEvent> fileAppender = createFileAppender(loggerContext, logFilePath);
        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(fileAppender);

        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(rootLevel);
    }

    /**
     * Set the log level for a specific package or class.
     *
     * @param loggerName the name of the logger (package or class)
     * @param level      the log level to set
     */
    public static void setLevel(String loggerName, Level level) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(loggerName);
        logger.setLevel(level);
    }

    /**
     * Set the root log level.
     *
     * @param level the log level to set
     */
    public static void setRootLevel(Level level) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(level);
    }

    /**
     * Create a console appender with pattern layout.
     *
     * @param loggerContext the logger context
     * @return the configured console appender
     */
    private static ConsoleAppender<ILoggingEvent> createConsoleAppender(LoggerContext loggerContext) {
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(loggerContext);
        consoleAppender.setName("CONSOLE");

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern(DEFAULT_LOG_PATTERN);
        encoder.start();

        consoleAppender.setEncoder(encoder);
        consoleAppender.start();

        return consoleAppender;
    }

    /**
     * Create a rolling file appender with pattern layout.
     *
     * @param loggerContext the logger context
     * @param logFilePath   the path to the log file
     * @return the configured rolling file appender
     */
    private static RollingFileAppender<ILoggingEvent> createFileAppender(LoggerContext loggerContext, String logFilePath) {
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("FILE");
        fileAppender.setFile(logFilePath);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern(DEFAULT_LOG_PATTERN);
        encoder.start();

        SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
        rollingPolicy.setContext(loggerContext);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(logFilePath.replace(".log", ".%d{yyyy-MM-dd}.%i.log"));
        rollingPolicy.setMaxFileSize(FileSize.valueOf("10MB"));
        rollingPolicy.setMaxHistory(30);
        rollingPolicy.setTotalSizeCap(FileSize.valueOf("1GB"));
        rollingPolicy.start();

        fileAppender.setEncoder(encoder);
        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.start();

        return fileAppender;
    }

    /**
     * Check if logging has been configured.
     *
     * @return true if configured, false otherwise
     */
    public static boolean isConfigured() {
        return configured.get();
    }

    /**
     * Reset the configuration state (primarily for testing).
     */
    static void reset() {
        configured.set(false);
    }
}
