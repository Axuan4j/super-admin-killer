package com.sak.service.util;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class ValidUtil {

    private ValidUtil() {
    }

    public static Chain check() {
        return new Chain();
    }

    public static final class Chain {

        public Chain notNull(Object value, String message) {
            return isTrue(value != null, message);
        }

        public Chain hasText(String value, String message) {
            return isTrue(StringUtils.hasText(value), message);
        }

        public Chain notEmpty(Collection<?> value, String message) {
            return isTrue(!CollectionUtils.isEmpty(value), message);
        }

        public Chain minLength(String value, int minLength, String message) {
            return isTrue(value != null && value.trim().length() >= minLength, message);
        }

        public Chain maxLength(String value, int maxLength, String message) {
            return isTrue(value == null || value.length() <= maxLength, message);
        }

        public Chain matches(String value, String regex, String message) {
            return isTrue(value != null && value.matches(regex), message);
        }

        public Chain notEquals(Object left, Object right, String message) {
            return isTrue(!Objects.equals(left, right), message);
        }

        public Chain custom(BooleanSupplier supplier, String message) {
            return isTrue(supplier.getAsBoolean(), message);
        }

        public Chain isTrue(boolean expression, String message) {
            if (!expression) {
                throw new IllegalArgumentException(message);
            }
            return this;
        }
    }
}
