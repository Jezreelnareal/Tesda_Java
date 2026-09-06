import javax.swing.SwingUtilities;
import shared.ui.JCashFrame;
import shared.ui.UiTheme;

public class Main {

    public static void main(String[] args) {
        UiTheme.install();
        SwingUtilities.invokeLater(() -> {
            new JCashFrame().setVisible(true);
        });
    }
}
