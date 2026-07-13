package org.fen.fen.util;

import java.util.List;

public class Formatters {
    public static String f(String template, Object... parameters) {
        if (List.of(parameters).isEmpty()) {
            return template;
        }
        return String.format(template, parameters);
    }
}
