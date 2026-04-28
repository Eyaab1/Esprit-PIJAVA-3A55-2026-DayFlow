package utils;

import javafx.scene.control.*;
import javafx.scene.layout.Region;

import java.time.LocalDate;
import java.util.function.Predicate;

public class FormValidator {

    private static final String ERROR_STYLE = "-fx-border-color: #dc2626; -fx-border-width: 2;";
    private static final String SUCCESS_STYLE = "-fx-border-color: #059669; -fx-border-width: 2;";
    private static final String DEFAULT_STYLE = "-fx-border-color: #e2e8f0; -fx-border-width: 1;";

    /**
     * Valide un champ texte avec une condition et affiche un message d'erreur
     */
    public static boolean validateTextField(TextField field, Predicate<String> validator, String errorMessage) {
        String value = field.getText();
        boolean isValid = validator.test(value);
        
        if (isValid) {
            field.setStyle(SUCCESS_STYLE);
            field.setTooltip(null);
        } else {
            field.setStyle(ERROR_STYLE);
            Tooltip tooltip = new Tooltip(errorMessage);
            tooltip.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;");
            field.setTooltip(tooltip);
        }
        
        return isValid;
    }

    /**
     * Valide un TextArea
     */
    public static boolean validateTextArea(TextArea area, Predicate<String> validator, String errorMessage) {
        String value = area.getText();
        boolean isValid = validator.test(value);
        
        if (isValid) {
            area.setStyle(SUCCESS_STYLE);
            area.setTooltip(null);
        } else {
            area.setStyle(ERROR_STYLE);
            Tooltip tooltip = new Tooltip(errorMessage);
            tooltip.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;");
            area.setTooltip(tooltip);
        }
        
        return isValid;
    }

    /**
     * Valide un DatePicker
     */
    public static boolean validateDatePicker(DatePicker picker, Predicate<LocalDate> validator, String errorMessage) {
        LocalDate value = picker.getValue();
        boolean isValid = validator.test(value);
        
        if (isValid) {
            picker.setStyle(SUCCESS_STYLE);
            picker.setTooltip(null);
        } else {
            picker.setStyle(ERROR_STYLE);
            Tooltip tooltip = new Tooltip(errorMessage);
            tooltip.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;");
            picker.setTooltip(tooltip);
        }
        
        return isValid;
    }

    /**
     * Valide un ChoiceBox
     */
    public static boolean validateChoiceBox(ChoiceBox<?> choiceBox, String errorMessage) {
        boolean isValid = choiceBox.getValue() != null;
        
        if (isValid) {
            choiceBox.setStyle(SUCCESS_STYLE);
            choiceBox.setTooltip(null);
        } else {
            choiceBox.setStyle(ERROR_STYLE);
            Tooltip tooltip = new Tooltip(errorMessage);
            tooltip.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;");
            choiceBox.setTooltip(tooltip);
        }
        
        return isValid;
    }

    /**
     * Réinitialise le style d'un contrôle
     */
    public static void resetStyle(Control control) {
        control.setStyle(DEFAULT_STYLE);
        control.setTooltip(null);
    }

    /**
     * Validateurs communs
     */
    public static class Validators {
        public static Predicate<String> notEmpty() {
            return s -> s != null && !s.trim().isEmpty();
        }

        public static Predicate<String> minLength(int min) {
            return s -> s != null && s.trim().length() >= min;
        }

        public static Predicate<String> maxLength(int max) {
            return s -> s == null || s.length() <= max;
        }

        public static Predicate<String> lengthBetween(int min, int max) {
            return s -> s != null && s.trim().length() >= min && s.length() <= max;
        }

        public static Predicate<LocalDate> notNull() {
            return date -> date != null;
        }

        public static Predicate<LocalDate> afterDate(LocalDate reference) {
            return date -> date != null && reference != null && date.isAfter(reference);
        }

        public static Predicate<LocalDate> beforeDate(LocalDate reference) {
            return date -> date != null && reference != null && date.isBefore(reference);
        }

        public static Predicate<String> isNumeric() {
            return s -> {
                if (s == null || s.trim().isEmpty()) return false;
                try {
                    Integer.parseInt(s.trim());
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            };
        }

        public static Predicate<String> isPositiveNumber() {
            return s -> {
                if (s == null || s.trim().isEmpty()) return false;
                try {
                    return Integer.parseInt(s.trim()) > 0;
                } catch (NumberFormatException e) {
                    return false;
                }
            };
        }

        public static Predicate<String> isTimeFormat() {
            return s -> {
                if (s == null || s.trim().isEmpty()) return false;
                return s.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$");
            };
        }
    }
}
