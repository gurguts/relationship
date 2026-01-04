package org.example.containerservice.advice;

import lombok.NonNull;
import org.springframework.context.MessageSource;

import java.util.Locale;

final class MessageLocalizationHelper {

    private MessageLocalizationHelper() {
    }

    static String getLocalizedMessage(@NonNull MessageSource messageSource,
                                     @NonNull String messageKey,
                                     String defaultMessage,
                                     @NonNull Locale locale) {
        return messageSource.getMessage(messageKey, null, defaultMessage, locale);
    }

    static boolean isValidLocalizedMessage(String localizedMessage, String messageKey) {
        return localizedMessage != null && !localizedMessage.equals(messageKey);
    }

    static String extractFirstLine(@NonNull String message) {
        String[] lines = message.split("\n");
        return lines.length > 0 ? lines[0] : message;
    }

    static boolean isComplexMessage(String message) {
        if (message == null) {
            return false;
        }
        return message.contains("\n") || message.length() > ErrorConstants.MAX_MESSAGE_LENGTH_FOR_DETAILS;
    }
}

