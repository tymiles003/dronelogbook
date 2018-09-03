package ugcs.ucsHub.forms;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WaitForm extends JDialog {

    private final JLabel messageLabel;

    @FunctionalInterface
    public interface Action {
        void run() throws Exception;
    }

    private WaitForm() {
        super((JFrame) null, true);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        setTitle("Please wait...");
        final ImageIcon icon = new ImageIcon(WaitForm.class.getResource("/graphics/Loading_icon.gif"));
        this.messageLabel = new JLabel("Loading", icon, SwingConstants.RIGHT);
        add(new JPanel().add(messageLabel).getParent());

        pack();
    }

    private static volatile WaitForm instance;

    public static WaitForm waitForm() {
        if (instance == null) {
            synchronized (WaitForm.class) {
                if (instance == null) {
                    instance = new WaitForm();
                }
            }
        }
        return instance;
    }

    public void waitOnAction(String message, Action action, Component parentOrNull) {
        waitOnCallable(message, () -> {
                    action.run();
                    return 0;
                }, parentOrNull
        );
    }

    public <T> T waitOnCallable(String message, Callable<T> callable, Component parentOrNull) {
        final ExecutorService executorService = Executors.newSingleThreadExecutor();

        final Future<T> resultFuture = executorService.submit(() -> {
            try {
                SwingUtilities.invokeLater(() -> {
                    messageLabel.setText(message);
                    pack();
                    setLocationRelativeTo(parentOrNull);
                });
                return callable.call();
            } catch (Exception toRethrow) {
                throw new RuntimeException(toRethrow);
            } finally {
                SwingUtilities.invokeLater(() -> setVisible(false));
            }
        });

        setVisible(true);
        try {
            return resultFuture.get();
        } catch (InterruptedException | ExecutionException toRethrow) {
            throw new RuntimeException(toRethrow);
        } finally {
            executorService.shutdown();
        }
    }
}