package com.github.bhavuklabs.exceptions.config;

import com.github.bhavuklabs.exceptions.Research4jException;

public class ConfigurationException extends Research4jException {

    public ConfigurationException(String message) {
        super("CONFIG_ERROR", message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super("CONFIG_ERROR", message, cause);
    }

    public ConfigurationException(String message, String configKey) {
        super("CONFIG_ERROR", message, configKey);
    }
}
