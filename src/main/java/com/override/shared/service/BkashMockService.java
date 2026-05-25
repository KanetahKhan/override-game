package com.override.shared.service;

import com.override.shared.model.GameState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * MOCK bKash payment flow.
 *
 * IMPORTANT: This is NOT a real payment integration. Real bKash integration
 * requires a registered merchant account, the bKash PGW credentials, and a
 * server-side webhook. For a semester project we simulate the UX flow only:
 *
 *   1. Enter bKash mobile number  (any 11 digits — no SMS sent)
 *   2. Enter OTP                  (any 4-6 digits — accepted)
 *   3. Enter PIN                  (any 4-5 digits — accepted)
 *   4. Confirmation screen        (coins added to GameState)
 *
 * Replace with real bKash PGW calls in production. Suggested classes:
 *   - BkashAuthClient (token grant)
 *   - BkashPaymentClient (create / execute payment)
 *   - PaymentController (Spring Boot REST endpoint that calls bKash API)
 */
public class BkashMockService {

    /** Static class. */
    private BkashMockService() {}

    /** Coin packages for sale, keyed by BDT price. */
    public record CoinPackage(int coins, int priceBdt, String label) {}

    public static CoinPackage[] packages() {
        return new CoinPackage[] {
            new CoinPackage(100,  50,  "Starter"),
            new CoinPackage(500,  200, "Resistance"),
            new CoinPackage(1200, 400, "Override"),   // best value
            new CoinPackage(3000, 800, "Symbiosis"),
        };
    }

    /**
     * Open the bKash mock dialog. On success, the requested coins are added
     * to GameState. The {@code onComplete} callback fires after the dialog
     * closes — pass it to refresh the calling screen.
     */
    public static void purchase(CoinPackage pkg, Runnable onComplete) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("bKash Payment (Simulated)");
        dialog.setResizable(false);

        // ----- Step 1: phone number -----
        Label header = new Label("bKash Payment");
        header.getStyleClass().add("bkash-header");

        Label warn = new Label("⚠ MOCK PAYMENT — NO REAL TRANSACTION");
        warn.getStyleClass().add("bkash-warning");

        Label info = new Label("Pay ৳" + pkg.priceBdt() + " for " + pkg.coins() + " coins (" + pkg.label() + ")");
        info.getStyleClass().add("bkash-info");

        Label step = new Label("Step 1 of 3 — Enter your bKash account number");
        step.getStyleClass().add("bkash-step");

        TextField input = new TextField();
        input.setPromptText("01XXXXXXXXX");
        input.setMaxWidth(260);

        Label error = new Label();
        error.getStyleClass().add("bkash-error");

        Button next = new Button("Proceed");
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("bkash-cancel");

        next.setOnAction(e -> {
            String v = input.getText().trim();
            if (!v.matches("\\d{11}")) {
                error.setText("Please enter an 11-digit number.");
                return;
            }
            // Step 2 — OTP
            step.setText("Step 2 of 3 — Enter the 6-digit OTP we 'sent' to " + v);
            input.clear();
            input.setPromptText("OTP (any 4-6 digits)");
            error.setText("");
            next.setText("Verify");

            next.setOnAction(e2 -> {
                String otp = input.getText().trim();
                if (!otp.matches("\\d{4,6}")) {
                    error.setText("Enter 4 to 6 digits.");
                    return;
                }
                // Step 3 — PIN
                step.setText("Step 3 of 3 — Enter your bKash PIN");
                input.clear();
                input.setPromptText("PIN (any 4-5 digits)");
                error.setText("");
                next.setText("Pay ৳" + pkg.priceBdt());

                next.setOnAction(e3 -> {
                    String pin = input.getText().trim();
                    if (!pin.matches("\\d{4,5}")) {
                        error.setText("Enter 4 to 5 digits.");
                        return;
                    }
                    // Success
                    GameState.get().addCoins(pkg.coins());

                    step.setText("✓ Payment successful");
                    info.setText("+" + pkg.coins() + " coins added to your account.");
                    error.setText("Transaction ID: TX" + System.currentTimeMillis());
                    error.getStyleClass().setAll("bkash-success");
                    input.setVisible(false);
                    next.setText("Done");
                    next.setOnAction(e4 -> {
                        dialog.close();
                        if (onComplete != null) onComplete.run();
                    });
                });
            });
        });

        cancel.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(10, next, cancel);
        buttons.setAlignment(Pos.CENTER);

        VBox root = new VBox(12, header, warn, info, step, input, error, buttons);
        root.setPadding(new Insets(28));
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("bkash-root");
        root.setPrefSize(440, 360);

        Scene s = new Scene(root);
        s.getStylesheets().add(
            BkashMockService.class.getResource("/styles/main.css").toExternalForm()
        );
        dialog.setScene(s);
        dialog.showAndWait();
    }
}
