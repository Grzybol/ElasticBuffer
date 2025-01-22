package org.betterbox.elasticBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Plugin(name = "CustomConsoleInjector", category = "Core", elementType = "appender", printObject = true)
public class CustomConsoleInjector extends AbstractAppender {
    private final JavaPlugin plugin;
    private final ElasticBufferAPI api;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private boolean removed = false;

    public CustomConsoleInjector(JavaPlugin plugin, ElasticBufferAPI api) {
        super("CustomConsoleInjector", null, null, true);
        this.plugin = plugin;
        this.api = api;
        api.log("CustomConsoleInjector created", "INFO", "Console", "", "", "Console");

        // Start the appender
        start();

        ((Logger) LogManager.getRootLogger()).addAppender(this);
        api.log("((Logger) LogManager.getRootLogger()).addAppender(this); done", "INFO", "Console", "", "", "Console");

        // Start asynchronous task
        startAsyncTask();
    }

    private void startAsyncTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            //api.log("STARTING TASK", "INFO", "Console", "", "", "Console");
            final StringBuilder buffer = new StringBuilder();
            String curLine;
            try {
                while ((curLine = messageQueue.poll()) != null) {
                    if (buffer.length() + curLine.length() > 2000 - 2) { // Ustal limit długości wiadomości
                        if (isValidMessage(buffer.toString())) {
                            api.log(buffer.toString(), "INFO", "Console", "", "", "Console");
                        } else {
                            api.log("Invalid message detected, skipping log entry.", "WARN", "Console", "", "", "Console");
                        }
                        buffer.setLength(0);
                    } else {
                        buffer.append("\n").append(curLine);
                    }
                }
                if (buffer.length() != 0) {
                    if (isValidMessage(buffer.toString())) {
                        api.log(buffer.toString(), "INFO", "Console", "", "", "Console");
                    } else {
                        api.log("Invalid message detected, skipping log entry.", "WARN", "Console", "", "", "Console");
                    }
                }
            } catch (Exception e) {
                api.log("Error in task: " + e.getMessage(), "ERROR", "Console", "", "", "Console");
            }
        }, 20, 20 * 5);
        api.log("task done", "DEBUG", "Console", "", "", "Console");
    }

    private boolean isValidMessage(String message) {
        // Sprawdź, czy długość wiadomości nie przekracza 32KB
        return message.length() <= 32 * 1024;
    }
    @Override
    public void append(LogEvent event) {
        String message = event.getMessage().getFormattedMessage();
        String level = event.getLevel().toString();
        String loggerName = event.getLoggerName();

        // Jeśli jest wyjątek, pobierz stacktrace
        final String throwableString;
        if (event.getThrown() != null) {
            StringWriter sw = new StringWriter();
            event.getThrown().printStackTrace(new PrintWriter(sw));
            throwableString = sw.toString();
        } else {
            throwableString = null;
        }

        // Dodaj wiadomość do kolejki tylko jeśli nie jest pusta
        if (message != null && !message.trim().isEmpty()) {
            // Usunięcie nieprawidłowych znaków
            message = message.replaceAll("[\\x00-\\x1F\\x7F]", "");

            // Usunięcie sekwencji ANSI i innych specjalnych formatowań
            message = message.replaceAll("\\e\\[[\\d;]*[^\\d;]", "");

            // Finalne formatowanie wiadomości
            String formattedMessage = message + (throwableString != null ? " " + throwableString : "");

            // Sprawdź, czy wiadomość nie przekracza limitu 32KB
            if (isValidMessage(formattedMessage)) {
                messageQueue.add(formattedMessage);
            } else {
                api.log("Message too large, skipping log entry.", "WARN", "Console", "", "", "Console");
            }
        }
    }

    public void remove() {
        removed = true;
        ((Logger) LogManager.getRootLogger()).removeAppender(this);
        messageQueue.clear();
    }

    public boolean isRemoved() {
        return removed;
    }
}