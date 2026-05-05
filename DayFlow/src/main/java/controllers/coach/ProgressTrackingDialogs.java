package controllers.coach;

import dto.coaching_session.ProgressReport;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import model.coaching_session.SessionEvaluation;

import java.util.Optional;

final class ProgressTrackingDialogs {

    private ProgressTrackingDialogs() {
    }

    static Optional<UserFeedbackInput> showUserFeedbackDialog() {
        Dialog<UserFeedbackInput> dialog = new Dialog<>();
        dialog.setTitle("Feedback session");
        dialog.setHeaderText("Donnez votre retour sur la session");
        ButtonType submit = new ButtonType("Envoyer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submit, ButtonType.CANCEL);

        ComboBox<Integer> ratingCombo = new ComboBox<>();
        ratingCombo.getItems().addAll(1, 2, 3, 4, 5);
        ratingCombo.setPromptText("Note du coach");

        TextArea feedbackArea = new TextArea();
        feedbackArea.setPromptText("Feedback global...");
        feedbackArea.setPrefRowCount(3);

        TextArea commentArea = new TextArea();
        commentArea.setPromptText("Commentaire detaille...");
        commentArea.setPrefRowCount(4);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.add(new Label("Note coach (1-5):"), 0, 0);
        grid.add(ratingCombo, 1, 0);
        grid.add(new Label("Feedback:"), 0, 1);
        grid.add(feedbackArea, 1, 1);
        grid.add(new Label("Commentaire:"), 0, 2);
        grid.add(commentArea, 1, 2);
        GridPane.setHgrow(feedbackArea, Priority.ALWAYS);
        GridPane.setHgrow(commentArea, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> {
            if (button == submit) {
                return new UserFeedbackInput(
                        ratingCombo.getValue(),
                        trimOrNull(feedbackArea.getText()),
                        trimOrNull(commentArea.getText())
                );
            }
            return null;
        });
        return dialog.showAndWait();
    }

    static Optional<SessionEvaluation> showCoachEvaluationDialog(String recommendationHint) {
        Dialog<SessionEvaluation> dialog = new Dialog<>();
        dialog.setTitle("Evaluation coach");
        dialog.setHeaderText("Evaluation de progression");
        ButtonType submit = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submit, ButtonType.CANCEL);

        TextField disciplineField = new TextField();
        disciplineField.setPromptText("0-100");
        TextField goalField = new TextField();
        goalField.setPromptText("0-100");
        TextField evolutionField = new TextField();
        evolutionField.setPromptText("0-100");
        TextField coachFeedbackScoreField = new TextField();
        coachFeedbackScoreField.setPromptText("0-100");
        TextField deltaField = new TextField();
        deltaField.setPromptText("-30 a +30");

        TextArea remarksArea = new TextArea();
        remarksArea.setPrefRowCount(2);
        remarksArea.setPromptText("Remarques du coach...");

        TextArea recommendationArea = new TextArea();
        recommendationArea.setPrefRowCount(2);
        recommendationArea.setPromptText("Recommandations personnalisees...");
        if (recommendationHint != null && !recommendationHint.isBlank()) {
            recommendationArea.setText(recommendationHint);
        }

        TextArea actionArea = new TextArea();
        actionArea.setPrefRowCount(2);
        actionArea.setPromptText("Prochaine action conseillee...");

        TextArea adjustmentArea = new TextArea();
        adjustmentArea.setPrefRowCount(2);
        adjustmentArea.setPromptText("Ajustement du programme...");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.add(new Label("Discipline:"), 0, 0);
        grid.add(disciplineField, 1, 0);
        grid.add(new Label("Objectifs atteints:"), 0, 1);
        grid.add(goalField, 1, 1);
        grid.add(new Label("Evolution:"), 0, 2);
        grid.add(evolutionField, 1, 2);
        grid.add(new Label("Feedback coach score:"), 0, 3);
        grid.add(coachFeedbackScoreField, 1, 3);
        grid.add(new Label("Delta progression:"), 0, 4);
        grid.add(deltaField, 1, 4);
        grid.add(new Label("Remarques:"), 0, 5);
        grid.add(remarksArea, 1, 5);
        grid.add(new Label("Recommandations:"), 0, 6);
        grid.add(recommendationArea, 1, 6);
        grid.add(new Label("Prochaine action:"), 0, 7);
        grid.add(actionArea, 1, 7);
        grid.add(new Label("Ajustement programme:"), 0, 8);
        grid.add(adjustmentArea, 1, 8);
        GridPane.setHgrow(remarksArea, Priority.ALWAYS);
        GridPane.setHgrow(recommendationArea, Priority.ALWAYS);
        GridPane.setHgrow(actionArea, Priority.ALWAYS);
        GridPane.setHgrow(adjustmentArea, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> {
            if (button != submit) {
                return null;
            }
            SessionEvaluation e = new SessionEvaluation();
            e.setDisciplineScore(parseNullableInt(disciplineField.getText()));
            e.setGoalAchievementScore(parseNullableInt(goalField.getText()));
            e.setEvolutionScore(parseNullableInt(evolutionField.getText()));
            e.setCoachFeedbackScore(parseNullableInt(coachFeedbackScoreField.getText()));
            e.setProgressDelta(parseNullableInt(deltaField.getText()));
            e.setCoachRemarks(trimOrNull(remarksArea.getText()));
            e.setRecommendations(trimOrNull(recommendationArea.getText()));
            e.setNextAction(trimOrNull(actionArea.getText()));
            e.setProgramAdjustment(trimOrNull(adjustmentArea.getText()));
            return e;
        });
        return dialog.showAndWait();
    }

    static void showProgressReportDialog(ProgressReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Score actuel: ").append(report.currentScore())
                .append(" (precedent: ").append(report.previousScore())
                .append(", variation: ").append(report.scoreChange() >= 0 ? "+" : "")
                .append(report.scoreChange()).append(")\n\n");
        sb.append("Recommandation globale: ").append(report.overallRecommendation()).append("\n\n");
        sb.append("Historique sessions:\n");
        for (ProgressReport.ProgressSnapshot snap : report.sessions()) {
            sb.append("- Session #").append(snap.sessionId())
                    .append(" [").append(snap.sessionStatus()).append("]");
            if (snap.scoreAfter() != null) {
                sb.append(" score=").append(snap.scoreAfter());
            }
            if (snap.scoreChange() != null) {
                sb.append(" (").append(snap.scoreChange() >= 0 ? "+" : "").append(snap.scoreChange()).append(")");
            }
            sb.append("\n");
            if (snap.userFeedback() != null && !snap.userFeedback().isBlank()) {
                sb.append("  Feedback user: ").append(snap.userFeedback()).append("\n");
            }
            if (snap.coachRecommendations() != null && !snap.coachRecommendations().isBlank()) {
                sb.append("  Reco coach: ").append(snap.coachRecommendations()).append("\n");
            }
            if (snap.nextAction() != null && !snap.nextAction().isBlank()) {
                sb.append("  Prochaine action: ").append(snap.nextAction()).append("\n");
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Rapport de progression");
        alert.setHeaderText("Coaching Request #" + report.coachingRequestId());
        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(700);
        textArea.setPrefHeight(420);
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    private static Integer parseNullableInt(String value) {
        String trimmed = trimOrNull(value);
        if (trimmed == null) {
            return null;
        }
        return Integer.parseInt(trimmed);
    }

    private static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    record UserFeedbackInput(Integer coachRating, String feedback, String comment) {
    }
}
