package ugcs.ucsHub.ui;

import ugcs.common.identity.Identity;
import ugcs.common.operation.Operation;
import ugcs.exceptions.logbook.LogbookAuthorizationFailed;
import ugcs.ucsHub.ui.components.JHyperlink;
import ugcs.upload.logbook.DroneLogbookResponse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.awt.Color.decode;
import static java.awt.Color.getHSBColor;
import static java.lang.String.format;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static ugcs.ucsHub.Settings.settings;

/**
 * Representation for results of flights upload {@link Operation}
 */
final class UploadReportForm extends JPanel {
    private static Color SUCCESS_COLOR = getHSBColor(0.269f, 0.1f, 1.0f);
    private static Color WARNING_COLOR = getHSBColor(0.147f, 0.14f, 1.0f);
    private static Color ERROR_COLOR = decode("#ffcece");

    private final Collection<Operation<Identity<?>, DroneLogbookResponse>> uploadResponses;

    static void showReport(Component parentComponent, List<Operation<Identity<?>, DroneLogbookResponse>> uploadResponses) {
        final UploadReportForm reportForm = new UploadReportForm(uploadResponses);
        showMessageDialog(parentComponent, reportForm, "Upload result", PLAIN_MESSAGE, reportForm.getMessageIcon());
    }

    private UploadReportForm(Collection<Operation<Identity<?>, DroneLogbookResponse>> uploadResponses) {
        this.uploadResponses = uploadResponses;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        if (isLogbookAuthorizationError()) {
            formAuthorizationFailedReport();
        } else {
            formReport();
        }
    }

    private boolean isLogbookAuthorizationError() {
        final long authorizationErrorCount = uploadResponses.stream()
                .map(Operation::getError)
                .filter(Optional::isPresent)
                .filter(error -> error.get() instanceof LogbookAuthorizationFailed)
                .count();

        return authorizationErrorCount > 0;
    }

    private Icon getMessageIcon() {
        final long errorsCount = uploadResponses.stream()
                .map(Operation::getError)
                .filter(Optional::isPresent)
                .count();

        final long warningCount = uploadResponses.stream()
                .map(Operation::getResult)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(DroneLogbookResponse::isWarning)
                .count();

        if (errorsCount > 0) {
            return settings().getErrorIcon();
        }

        if (warningCount > 0) {
            return settings().getWarningIcon();
        }

        return settings().getSuccessIcon();
    }

    private void formAuthorizationFailedReport() {
        final JPanel reportRow = new JPanel();

        reportRow.add(new JLabel("DroneLogbook authorization failed."));

        final JLabel changeCredentialsLabel =
                new JLabel(format("<html><a href=\\\"_\\\">%s</a></html>", "click to change credentials"));
        changeCredentialsLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        changeCredentialsLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showLogbookAuthorizationForm();
            }
        });
        reportRow.add(changeCredentialsLabel);

        this.add(reportRow);
    }

    private void showLogbookAuthorizationForm() {
        final JPanel mainPanel = new JPanel(new GridLayout(0, 1));

        final JTextField loginDlbField = new JTextField(settings().getUploadServerLogin(), 25);
        mainPanel.add(new JLabel("DroneLogbook login:"));
        mainPanel.add(new JPanel().add(loginDlbField).getParent());

        final JTextField passwordDlbField = new JPasswordField(settings().getUploadServerPassword(), 25);
        mainPanel.add(new JLabel("DroneLogbook password:"));
        mainPanel.add(new JPanel().add(passwordDlbField).getParent());

        final int dialogResult = showConfirmDialog(this, mainPanel, "DroneLogbook credentials", OK_CANCEL_OPTION);
        if (dialogResult == OK_OPTION) {
            settings().storeUploadServerLogin(loginDlbField.getText());
            settings().storeUploadServerPassword(passwordDlbField.getText());
        }
    }

    private void formReport() {
        uploadResponses.forEach(operationResult -> {
            final JPanel reportRow = new JPanel();
            reportRow.setLayout(new BoxLayout(reportRow, BoxLayout.X_AXIS));

            final String identityTextRepresentation = operationResult.getId().toString();
            final String statusString = operationStatusString(operationResult);
            reportRow.add(new JLabel(format("[ %s ] - %s:", identityTextRepresentation, statusString)));

            operationResult.getResult()
                    .ifPresent(uploadResponse -> formReportRow(uploadResponse, reportRow));

            operationResult.getError().ifPresent(error -> formReportRow(error, reportRow));

            reportRow.add(Box.createHorizontalGlue());
            this.add(reportRow);
        });
    }

    private void formReportRow(DroneLogbookResponse droneLogbookResponse, JPanel rowContainer) {
        final String description = droneLogbookResponse.getDescription()
                .map(String::trim)
                .map(s -> s.endsWith(".") ? s : s.concat("."))
                .map(s -> s.concat(" "))
                .orElse("No description. ");

        rowContainer.add(new JLabel(description));
        droneLogbookResponse.getUrl()
                .ifPresent(url -> rowContainer.add(new JHyperlink(url, "Click to view on DroneLogbook.")));
        rowContainer.setBackground(getResponseColor(droneLogbookResponse));
    }

    private void formReportRow(Throwable error, JPanel rowContainer) {
        rowContainer.add(new JLabel(error.getMessage()));
        rowContainer.setBackground(ERROR_COLOR);
    }

    private Color getResponseColor(DroneLogbookResponse droneLogbookResponse) {
        if (droneLogbookResponse.isUploadSucceed()) {
            return SUCCESS_COLOR;
        }

        return WARNING_COLOR;
    }

    private String operationStatusString(Operation<Identity<?>, DroneLogbookResponse> operationResult) {
        return operationResult.getResult().map(uploadResponse -> {
            if (uploadResponse.isUploadSucceed()) {
                return "uploaded";
            }
            if (uploadResponse.isWarning()) {
                return "upload rejected";
            }
            return "upload error";
        }).orElse("upload error");
    }
}