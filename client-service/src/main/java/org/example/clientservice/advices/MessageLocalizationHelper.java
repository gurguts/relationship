package org.example.clientservice.advices;

import lombok.NonNull;
import org.springframework.context.MessageSource;

import java.util.Locale;
import java.util.Objects;

final class MessageLocalizationHelper {

    private MessageLocalizationHelper() {
    }

    static String getLocalizedMessage(@NonNull MessageSource messageSource,
                                     @NonNull String messageKey,
                                     String defaultMessage,
                                     @NonNull Locale locale) {
        return messageSource.getMessage(messageKey, null, defaultMessage, locale);
    }

    static String getLocalizedErrorCodeMessage(@NonNull MessageSource messageSource,
                                               @NonNull String errorCode,
                                               String fallbackMessage,
                                               @NonNull Locale locale) {
        String messageKey = String.format(ErrorConstants.MESSAGE_KEY_PREFIX_SOURCE_ERROR, errorCode.toUpperCase());
        String localizedMessage = messageSource.getMessage(messageKey, null, null, locale);
        
        if (isValidLocalizedMessage(localizedMessage, messageKey)) {
            return localizedMessage;
        }
        
        return Objects.toString(fallbackMessage, "");
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

