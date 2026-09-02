import javax.swing.SwingUtilities;
import ui.JCashFrame;
import ui.UiTheme;

public class Main {

    public static void main(String[] args) {
        UiTheme.install();
        SwingUtilities.invokeLater(() -> {
            new JCashFrame().setVisible(true);
        });
    }
}
