package ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import model.CashInTransaction;
import model.Admin;
import model.AdminCreditTransaction;
import model.AdminDebitTransaction;
import model.Transaction;
import model.TransferTransaction;
import model.User;
import model.WithdrawalTransaction;
import service.Auth;
import service.AdminAccountService;
import service.Balance;
import service.CashIn;
import service.Logs;
import service.Transfer;
import service.Withdrawal;
import util.DatabaseConnection;

public final class JCashFrame extends JFrame {

    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final String LANDING_CARD = "landing";
    private static final String LOGIN_CARD = "login";
    private static final String ADMIN_LOGIN_CARD = "admin-login";
    private static final String REGISTRATION_CARD = "registration";
    private static final String DASHBOARD_CARD = "dashboard";
    private static final String ADMIN_DASHBOARD_CARD = "admin-dashboard";
    private static final String OVERVIEW_CARD = "overview";
    private static final String TRANSACTIONS_CARD = "transactions";
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Auth auth = new Auth();
    private final Balance balance = new Balance();
    private final CashIn cashIn = new CashIn();
    private final Withdrawal withdrawal = new Withdrawal();
    private final Transfer transfer = new Transfer();
    private final Logs logs = new Logs();
    private final AdminAccountService adminAccountService =
            new AdminAccountService();

    private final CardLayout applicationCards = new CardLayout();
    private final JPanel applicationPanel = new JPanel(applicationCards);
    private final CardLayout contentCards = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentCards);
    private final List<JButton> dashboardControls = new ArrayList<>();
    private final List<JButton> navigationButtons = new ArrayList<>();

    private JTextField mobileNumberField;
    private JPasswordField pinField;
    private JButton loginButton;
    private JButton registrationButton;
    private JButton retryConnectionButton;
    private JLabel loginStatusLabel;
    private JLabel attemptsLabel;
    private JLabel welcomeLabel;
    private JLabel mobileLabel;
    private JLabel balanceLabel;
    private DefaultTableModel transactionTableModel;
    private JTable transactionTable;
    private DefaultTableModel recentTableModel;
    private JTable recentTable;
    private TableRowSorter<DefaultTableModel> transactionSorter;
    private JTextField transactionSearchField;
    private JComboBox<String> transactionTypeFilter;
    private JPanel userSidebar;
    private JLabel userSidebarBrand;
    private JButton overviewNavigationButton;
    private JButton transactionsNavigationButton;
    private JButton detailsNavigationButton;
    private JTextField adminUsernameField;
    private JPasswordField adminPinField;
    private JButton adminLoginButton;
    private JButton adminRetryConnectionButton;
    private JLabel adminLoginStatusLabel;
    private JLabel adminAttemptsLabel;
    private JTextField registrationNameField;
    private JTextField registrationMobileField;
    private JPasswordField registrationPinField;
    private JPasswordField registrationConfirmPinField;
    private JButton registrationSubmitButton;
    private JButton registrationRetryConnectionButton;
    private JLabel registrationStatusLabel;
    private AdminDashboardPanel adminDashboardPanel;

    private User currentUser;
    private Admin currentAdmin;
    private int failedLoginAttempts;
    private int failedAdminLoginAttempts;
    private boolean databaseAvailable;

    public JCashFrame() {
        super("JCash Wallet");
        configureWindow();
        applicationPanel.add(createLandingPanel(), LANDING_CARD);
        applicationPanel.add(createLoginPanel(), LOGIN_CARD);
        applicationPanel.add(createAdminLoginPanel(), ADMIN_LOGIN_CARD);
        applicationPanel.add(
                createRegistrationPanel(),
                REGISTRATION_CARD
        );
        applicationPanel.add(createDashboardPanel(), DASHBOARD_CARD);
        adminDashboardPanel = new AdminDashboardPanel(
                this,
                adminAccountService,
                balance,
                this::adminLogout
        );
        applicationPanel.add(
                adminDashboardPanel,
                ADMIN_DASHBOARD_CARD
        );
        setContentPane(applicationPanel);
        applicationCards.show(applicationPanel, LANDING_CARD);
        checkDatabaseConnection();
    }

    private void configureWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(860, 600));
        setSize(1120, 720);
        setLocationRelativeTo(null);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateResponsiveLayout();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                DatabaseConnection.shutdown();
            }
        });
    }

    private JPanel createLandingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTheme.BACKGROUND);
        panel.add(createBrandPanel(), BorderLayout.WEST);

        JPanel area = new JPanel(new GridBagLayout());
        area.setBackground(UiTheme.BACKGROUND);
        JPanel card = new JPanel();
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(410, 390));
        card.setBorder(new EmptyBorder(40, 44, 38, 44));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        addLeftAligned(card, UiTheme.label(
                "Welcome to JCash",
                UiTheme.NAVY,
                UiTheme.FONT_TITLE
        ));
        card.add(Box.createVerticalStrut(8));
        addLeftAligned(card, UiTheme.label(
                "Choose how you want to continue.",
                UiTheme.MUTED,
                UiTheme.FONT_PLAIN
        ));
        card.add(Box.createVerticalStrut(32));

        JButton userButton = fullWidthButton(
                UiTheme.primaryButton("User login")
        );
        JButton adminButton = fullWidthButton(
                UiTheme.darkButton("Admin login")
        );
        registrationButton = fullWidthButton(
                UiTheme.textButton("Create account")
        );
        userButton.addActionListener(event -> showUserLogin());
        adminButton.addActionListener(event -> showAdminLogin());
        registrationButton.addActionListener(
                event -> showRegistration()
        );
        card.add(userButton);
        card.add(Box.createVerticalStrut(14));
        card.add(adminButton);
        card.add(Box.createVerticalStrut(14));
        card.add(registrationButton);
        area.add(card);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        JPanel themeBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 16));
        themeBar.setOpaque(false);
        themeBar.add(UiTheme.themeButton(this));
        wrapper.add(themeBar, BorderLayout.NORTH);
        wrapper.add(area, BorderLayout.CENTER);
        panel.add(wrapper, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTheme.BACKGROUND);
        panel.add(createBrandPanel(), BorderLayout.WEST);
        panel.add(withThemeBar(createLoginFormArea()), BorderLayout.CENTER);
        return panel;
    }

    private JPanel withThemeBar(JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        JPanel themeBar = new JPanel(new BorderLayout());
        themeBar.setOpaque(false);
        themeBar.setBorder(new EmptyBorder(16, 18, 16, 18));
        themeBar.add(createBackButton(), BorderLayout.WEST);
        themeBar.add(UiTheme.themeButton(this), BorderLayout.EAST);
        wrapper.add(themeBar, BorderLayout.NORTH);
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    private JButton createBackButton() {
        JButton button = UiTheme.lightButton("");
        Dimension size = new Dimension(42, 42);
        button.setIcon(new UiIcon(UiIcon.Kind.CHEVRON_LEFT, 20));
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        button.setToolTipText("Back to welcome");
        button.getAccessibleContext().setAccessibleName("Back to welcome");
        button.getAccessibleContext().setAccessibleDescription(
                "Return to the welcome screen"
        );
        button.addActionListener(
                event -> {
                    hideAllPasswordFields();
                    applicationCards.show(applicationPanel, LANDING_CARD);
                }
        );
        return button;
    }

    private JPanel createBrandPanel() {
        JPanel brandPanel = new JPanel();
        brandPanel.setPreferredSize(new Dimension(360, 0));
        brandPanel.setBackground(UiTheme.NAVY);
        brandPanel.setBorder(new EmptyBorder(52, 42, 52, 42));
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));

        JLabel logo = UiTheme.label(
                "JCASH",
                Color.WHITE,
                UiTheme.FONT_TITLE.deriveFont(32f)
        );
        JLabel tagline = UiTheme.label(
                "Your money, made simple.",
                new Color(203, 213, 225),
                UiTheme.FONT_PLAIN.deriveFont(17f)
        );
        JLabel description = UiTheme.label(
                "<html>Cash in, send funds, and review every transaction "
                        + "from one secure wallet.</html>",
                new Color(148, 163, 184),
                UiTheme.FONT_PLAIN
        );

        brandPanel.add(logo);
        brandPanel.add(Box.createVerticalStrut(14));
        brandPanel.add(tagline);
        brandPanel.add(Box.createVerticalStrut(32));
        brandPanel.add(description);
        brandPanel.add(Box.createVerticalGlue());
        brandPanel.add(UiTheme.label(
                "Secure JDBC banking demo",
                new Color(148, 163, 184),
                UiTheme.FONT_PLAIN.deriveFont(12f)
        ));
        return brandPanel;
    }

    private JPanel createLoginFormArea() {
        JPanel formArea = new JPanel(new GridBagLayout());
        formArea.setBackground(UiTheme.BACKGROUND);
        formArea.setBorder(new EmptyBorder(10, 35, 50, 35));

        JPanel card = new JPanel();
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(390, 400));
        card.setBorder(new EmptyBorder(12, 42, 12, 42));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel title = UiTheme.label(
                "Welcome back",
                UiTheme.NAVY,
                UiTheme.FONT_TITLE
        );
        JLabel subtitle = UiTheme.label(
                "Sign in to continue to your wallet.",
                UiTheme.MUTED,
                UiTheme.FONT_PLAIN
        );

        mobileNumberField = new JTextField();
        pinField = new JPasswordField();
        UiTheme.styleField(mobileNumberField);
        UiTheme.styleField(pinField);
        UiTheme.installPasswordVisibilityToggle(pinField);
        installDigitsOnlyFilter(mobileNumberField, 11);
        installDigitsOnlyFilter(pinField, 4);

        loginButton = UiTheme.primaryButton("Sign in");
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginButton.setEnabled(false);
        loginButton.addActionListener(event -> attemptLogin());
        pinField.addActionListener(event -> attemptLogin());

        retryConnectionButton = UiTheme.lightButton("Retry connection");
        retryConnectionButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        retryConnectionButton.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 42)
        );
        retryConnectionButton.setVisible(false);
        retryConnectionButton.addActionListener(
                event -> checkDatabaseConnection()
        );
        loginStatusLabel = UiTheme.label(
                "Connecting to JCash...",
                UiTheme.MUTED,
                UiTheme.FONT_PLAIN.deriveFont(12f)
        );
        attemptsLabel = UiTheme.label(
                "You have 3 login attempts.",
                UiTheme.MUTED,
                UiTheme.FONT_PLAIN.deriveFont(12f)
        );

        card.add(Box.createVerticalGlue());
        addLeftAligned(card, title);
        card.add(Box.createVerticalStrut(6));
        addLeftAligned(card, subtitle);
        card.add(Box.createVerticalStrut(28));
        addField(card, "Mobile number", mobileNumberField);
        card.add(Box.createVerticalStrut(16));
        addField(card, "4-digit PIN", pinField);
        card.add(Box.createVerticalStrut(11));
        addLeftAligned(card, attemptsLabel);
        card.add(Box.createVerticalStrut(18));
        card.add(loginButton);
        card.add(Box.createVerticalStrut(10));
        addLeftAligned(card, loginStatusLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(retryConnectionButton);
        card.add(Box.createVerticalGlue());

        formArea.add(card);
        return formArea;
    }

    private JPanel createAdminLoginPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTheme.BACKGROUND);
        panel.add(createBrandPanel(), BorderLayout.WEST);

        JPanel area = new JPanel(new GridBagLayout());
        area.setBackground(UiTheme.BACKGROUND);
        area.setBorder(new EmptyBorder(10, 35, 50, 35));
        JPanel card = new JPanel();
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(390, 400));
        card.setBorder(new EmptyBorder(12, 42, 12, 42));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        adminUsernameField = UiTheme.styleField(new JTextField());
        adminPinField = new JPasswordField();
        UiTheme.styleField(adminPinField);
        UiTheme.installPasswordVisibilityToggle(adminPinField);
        installDigitsOnlyFilter(adminPinField, 4);
        adminLoginButton = fullWidthButton(
                UiTheme.primaryButton("Sign in as admin")
        );
        adminLoginButton.setEnabled(false);
        adminLoginButton.addActionListener(event -> attemptAdminLogin());
        adminPinField.addActionListener(event -> attemptAdminLogin());
        adminRetryConnectionButton = fullWidthButton(
                UiTheme.lightButton("Retry connection")
        );
        adminRetryConnectionButton.setVisible(false);
        adminRetryConnectionButton.addActionListener(
                event -> checkDatabaseConnection()
        );
        adminLoginStatusLabel = UiTheme.label(
                "Connecting to JCash...",
                UiTheme.MUTED,
                UiTheme.FONT_PLAIN.deriveFont(12f)
        );
        adminAttemptsLabel = UiTheme.label(
                "You have 3 admin login attempts.",
                UiTheme.MUTED,
                UiTheme.FONT_PLAIN.deriveFont(12f)
        );

        card.add(Box.createVerticalGlue());
        addLeftAligned(card, UiTheme.label(
                "Administrator access",
                UiTheme.NAVY,
                UiTheme.FONT_TITLE
        ));
        card.add(Box.createVerticalStrut(6));
        addLeftAligned(card, UiTheme.label(
                "Sign in with your admin username and PIN.",
                UiTheme.MUTED,
                UiTheme.FONT_PLAIN
        ));
        card.add(Box.createVerticalStrut(24));
        addField(card, "Admin username", adminUsernameField);
        card.add(Box.createVerticalStrut(14));
        addField(card, "4-digit PIN", adminPinField);
        card.add(Box.createVerticalStrut(10));
        addLeftAligned(card, adminAttemptsLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(adminLoginButton);
        card.add(Box.createVerticalStrut(9));
        addLeftAligned(card, adminLoginStatusLabel);
        card.add(Box.createVerticalStrut(9));
        card.add(adminRetryConnectionButton);
        card.add(Box.createVerticalGlue());
        area.add(card);
        panel.add(withThemeBar(area), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRegistrationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTheme.BACKGROUND);
        panel.add(createBrandPanel(), BorderLayout.WEST);

        JPanel area = new JPanel(new GridBagLayout());
        area.setBackground(UiTheme.BACKGROUND);
        area.setBorder(new EmptyBorder(0, 35, 20, 35));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(430, 465));
        form.setBorder(new EmptyBorder(8, 36, 8, 36));
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        registrationNameField = UiTheme.styleField(new JTextField());
        registrationMobileField = UiTheme.styleField(new JTextField());
        registrationPinField = new JPasswordField();
        registrationConfirmPinField = new JPasswordField();
        UiTheme.styleField(registrationPinField);
        UiTheme.styleField(registrationConfirmPinField);
        UiTheme.installPasswordVisibilityToggle(registrationPinField);
        UiTheme.installPasswordVisibilityToggle(registrationConfirmPinField);
        installDigitsOnlyFilter(registrationMobileField, 11);
        installDigitsOnlyFilter(registrationPinField, 4);
        installDigitsOnlyFilter(registrationConfirmPinField, 4);

        registrationSubmitButton = fullWidthButton(
                UiTheme.primaryButton("Create account")
        );
        registrationSubmitButton.setEnabled(false);
        registrationSubmitButton.addActionListener(
                event -> attemptRegistration()
        );
        registrationConfirmPinField.addActionListener(
                event -> attemptRegistration()
        );

        registrationRetryConnectionButton = fullWidthButton(
                UiTheme.lightButton("Retry connection")
        );
        registrationRetryConnectionButton.setVisible(false);
        registrationRetryConnectionButton.addActionListener(
                event -> checkDatabaseConnection()
        );
        registrationStatusLabel = UiTheme.label(
                "Connecting to JCash...",
                UiTheme.MUTED,
                UiTheme.FONT_PLAIN.deriveFont(12f)
        );

        addLeftAligned(form, UiTheme.label(
                "Create your account",
                UiTheme.NAVY,
                UiTheme.FONT_TITLE
        ));
        form.add(Box.createVerticalStrut(5));
        addLeftAligned(form, UiTheme.label(
                "Start with a secure, zero-balance JCash account.",
                UiTheme.MUTED,
                UiTheme.FONT_PLAIN
        ));
        form.add(Box.createVerticalStrut(16));
        addField(form, "Full name", registrationNameField);
        form.add(Box.createVerticalStrut(8));
        addField(form, "Mobile number", registrationMobileField);
        form.add(Box.createVerticalStrut(8));
        addField(form, "4-digit PIN", registrationPinField);
        form.add(Box.createVerticalStrut(8));
        addField(form, "Confirm PIN", registrationConfirmPinField);
        form.add(Box.createVerticalStrut(14));
        form.add(registrationSubmitButton);
        form.add(Box.createVerticalStrut(8));
        addLeftAligned(form, registrationStatusLabel);
        form.add(Box.createVerticalStrut(8));
        form.add(registrationRetryConnectionButton);

        area.add(form);
        panel.add(withThemeBar(area), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDashboardPanel() {
        JPanel dashboard = new JPanel(new BorderLayout());
        dashboard.setBackground(UiTheme.BACKGROUND);
        dashboard.add(createSidebar(), BorderLayout.WEST);
        dashboard.add(createDashboardBody(), BorderLayout.CENTER);
        return dashboard;
    }

    private JPanel createSidebar() {
        userSidebar = new JPanel();
        userSidebar.setPreferredSize(new Dimension(230, 0));
        userSidebar.setBackground(UiTheme.NAVY);
        userSidebar.setBorder(new EmptyBorder(30, 22, 28, 22));
        userSidebar.setLayout(new BoxLayout(userSidebar, BoxLayout.Y_AXIS));

        userSidebarBrand = UiTheme.label(
                "JCASH",
                Color.WHITE,
                UiTheme.FONT_TITLE.deriveFont(25f)
        );
        userSidebar.add(userSidebarBrand);
        userSidebar.add(Box.createVerticalStrut(40));

        overviewNavigationButton = createNavigationButton("Overview", UiIcon.Kind.HOME);
        transactionsNavigationButton = createNavigationButton("Transactions", UiIcon.Kind.HISTORY);
        detailsNavigationButton = createNavigationButton("Account details", UiIcon.Kind.USER);
        JButton logoutButton = createNavigationButton("Log out", UiIcon.Kind.LOGOUT);
        overviewNavigationButton.addActionListener(event -> showOverview());
        transactionsNavigationButton.addActionListener(event -> showTransactions());
        detailsNavigationButton.addActionListener(event -> showAccountDetails());
        logoutButton.addActionListener(event -> logout());

        dashboardControls.add(overviewNavigationButton);
        dashboardControls.add(transactionsNavigationButton);
        dashboardControls.add(detailsNavigationButton);
        dashboardControls.add(logoutButton);
        navigationButtons.addAll(List.of(overviewNavigationButton,
                transactionsNavigationButton, detailsNavigationButton));

        userSidebar.add(overviewNavigationButton);
        userSidebar.add(Box.createVerticalStrut(10));
        userSidebar.add(transactionsNavigationButton);
        userSidebar.add(Box.createVerticalStrut(10));
        userSidebar.add(detailsNavigationButton);
        userSidebar.add(Box.createVerticalGlue());
        userSidebar.add(logoutButton);
        UiTheme.setNavigationActive(overviewNavigationButton, navigationButtons);
        return userSidebar;
    }

    private JButton createNavigationButton(String text, UiIcon.Kind icon) {
        JButton button = UiTheme.darkButton(text);
        button.putClientProperty("jcash.fullText", text);
        button.setIcon(new UiIcon(icon));
        button.setIconTextGap(12);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        return button;
    }

    private JPanel createDashboardBody() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(UiTheme.BACKGROUND);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(24, 30, 16, 30));

        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        welcomeLabel = UiTheme.label(
                "Welcome",
                UiTheme.NAVY,
                UiTheme.FONT_TITLE.deriveFont(22f)
        );
        mobileLabel = UiTheme.label(
                "",
                UiTheme.MUTED,
                UiTheme.FONT_PLAIN.deriveFont(12f)
        );
        identity.add(welcomeLabel);
        identity.add(Box.createVerticalStrut(3));
        identity.add(mobileLabel);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerActions.setOpaque(false);
        headerActions.add(UiTheme.themeButton(this));
        header.add(identity, BorderLayout.WEST);
        header.add(headerActions, BorderLayout.EAST);

        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(5, 30, 28, 30));
        contentPanel.add(createOverviewPanel(), OVERVIEW_CARD);
        contentPanel.add(createTransactionsPanel(), TRANSACTIONS_CARD);

        body.add(header, BorderLayout.NORTH);
        body.add(contentPanel, BorderLayout.CENTER);
        return body;
    }

    private JPanel createOverviewPanel() {
        JPanel overview = new JPanel(new GridBagLayout());
        overview.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.insets = new Insets(0, 0, 20, 0);
        overview.add(createBalanceCard(), constraints);

        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.weightx = 0.333;
        constraints.weighty = 0.55;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, 0, 0, 10);
        overview.add(createActionCard(
                "Cash in",
                "Add funds securely to your JCash balance.",
                "Add funds",
                this::showCashInDialog
        ), constraints);

        constraints.gridx = 1;
        constraints.insets = new Insets(0, 10, 0, 10);
        overview.add(createActionCard(
                "Withdraw",
                "Withdraw available funds from your JCash balance.",
                "Withdraw funds",
                this::showWithdrawalDialog
        ), constraints);

        constraints.gridx = 2;
        constraints.insets = new Insets(0, 10, 0, 0);
        overview.add(createActionCard(
                "Send money",
                "Transfer funds to another registered mobile number.",
                "Make transfer",
                this::showTransferDialog
        ), constraints);

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        constraints.weighty = 0.45;
        constraints.insets = new Insets(18, 0, 0, 0);
        overview.add(createRecentActivityPanel(), constraints);
        return overview;
    }

    private JPanel createRecentActivityPanel() {
        UiTheme.RoundedPanel panel = new UiTheme.RoundedPanel(UiTheme.SURFACE, 20);
        panel.setLayout(new BorderLayout(0, 12));
        panel.setBorder(new EmptyBorder(18, 20, 18, 20));
        panel.add(UiTheme.label("Recent activity", UiTheme.NAVY,
                UiTheme.FONT_TITLE.deriveFont(18f)), BorderLayout.NORTH);
        recentTableModel = new DefaultTableModel(
                new String[]{"Date & time", "Activity", "Amount"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        recentTable = new JTable(recentTableModel);
        configureTransactionTable(recentTable);
        recentTable.setRowHeight(30);
        JScrollPane scroll = new JScrollPane(recentTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBalanceCard() {
        UiTheme.RoundedPanel card = new UiTheme.RoundedPanel(
                UiTheme.NAVY,
                24
        );
        card.setBorder(new EmptyBorder(26, 30, 27, 30));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(UiTheme.label(
                "AVAILABLE BALANCE",
                new Color(148, 210, 202),
                UiTheme.FONT_MEDIUM.deriveFont(12f)
        ));
        card.add(Box.createVerticalStrut(9));
        balanceLabel = UiTheme.label(
                "PHP 0.00",
                Color.WHITE,
                UiTheme.FONT_BALANCE
        );
        card.add(balanceLabel);
        card.add(Box.createVerticalStrut(9));
        card.add(UiTheme.label(
                "Updated after every successful transaction",
                new Color(203, 213, 225),
                UiTheme.FONT_PLAIN.deriveFont(12f)
        ));
        return card;
    }

    private JPanel createActionCard(
            String title,
            String description,
            String buttonText,
            Runnable action
    ) {
        UiTheme.RoundedPanel card = new UiTheme.RoundedPanel(
                UiTheme.SURFACE,
                22
        );
        card.setBorder(new EmptyBorder(28, 26, 26, 26));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel titleLabel = UiTheme.label(
                title,
                UiTheme.NAVY,
                UiTheme.FONT_TITLE.deriveFont(20f)
        );
        JLabel descriptionLabel = UiTheme.label(
                "<html>" + description + "</html>",
                UiTheme.MUTED,
                UiTheme.FONT_PLAIN
        );
        JButton button = UiTheme.primaryButton(buttonText);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(event -> action.run());
        dashboardControls.add(button);

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(13));
        card.add(descriptionLabel);
        card.add(Box.createVerticalGlue());
        card.add(button);
        return card;
    }

    private JPanel createTransactionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);

        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setOpaque(false);
        heading.add(UiTheme.label(
                "Transaction history",
                UiTheme.NAVY,
                UiTheme.FONT_TITLE.deriveFont(21f)
        ), BorderLayout.WEST);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filters.setOpaque(false);
        transactionSearchField = UiTheme.styleField(new JTextField(14));
        transactionSearchField.putClientProperty(
                com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT,
                "Search transactions");
        transactionTypeFilter = new JComboBox<>(new String[]{"All types",
                "Cash in", "Withdrawal", "Transfer sent", "Transfer received",
                "Admin credit", "Admin debit"});
        JButton refreshButton = UiTheme.lightButton("Refresh");
        refreshButton.addActionListener(event -> loadTransactions());
        dashboardControls.add(refreshButton);
        filters.add(transactionSearchField);
        filters.add(transactionTypeFilter);
        filters.add(refreshButton);
        heading.add(filters, BorderLayout.EAST);

        transactionTableModel = new DefaultTableModel(
                new String[]{
                    "Date & time",
                    "Type",
                    "Amount",
                    "Details",
                    "Counterparty"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        transactionTable = new JTable(transactionTableModel);
        configureTransactionTable(transactionTable);
        transactionSorter = new TableRowSorter<>(transactionTableModel);
        transactionTable.setRowSorter(transactionSorter);
        transactionSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { applyTransactionFilter(); }
            public void removeUpdate(DocumentEvent event) { applyTransactionFilter(); }
            public void changedUpdate(DocumentEvent event) { applyTransactionFilter(); }
        });
        transactionTypeFilter.addActionListener(event -> applyTransactionFilter());
        transactionTable.getTableHeader().setBackground(
                new Color(232, 240, 247)
        );
        transactionTable.getTableHeader().setForeground(UiTheme.NAVY);
        transactionTable.getColumnModel().getColumn(2)
                .setCellRenderer(new AmountCellRenderer());

        JScrollPane scrollPane = new JScrollPane(transactionTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scrollPane.getViewport().setBackground(UiTheme.SURFACE);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void configureTransactionTable(JTable table) {
        table.setFont(UiTheme.FONT_PLAIN.deriveFont(13f));
        table.setForeground(UiTheme.TEXT);
        table.setBackground(UiTheme.SURFACE);
        table.setRowHeight(36);
        table.setShowVerticalLines(false);
        table.setGridColor(UiTheme.BORDER);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(UiTheme.FONT_MEDIUM);

        DefaultTableCellRenderer centeredCells =
                new DefaultTableCellRenderer();
        centeredCells.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, centeredCells);

        TableCellRenderer headerRenderer =
                table.getTableHeader().getDefaultRenderer();
        table.getTableHeader().setDefaultRenderer((headerTable, value,
                selected, focused, row, column) -> {
            Component component = headerRenderer.getTableCellRendererComponent(
                    headerTable,
                    value,
                    selected,
                    focused,
                    row,
                    column
            );
            if (component instanceof JLabel label) {
                label.setHorizontalAlignment(SwingConstants.CENTER);
            }
            return component;
        });
    }

    private void applyTransactionFilter() {
        if (transactionSorter == null) {
            return;
        }
        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        String query = transactionSearchField.getText().trim();
        if (!query.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
        }
        String type = (String) transactionTypeFilter.getSelectedItem();
        if (type != null && !"All types".equals(type)) {
            filters.add(RowFilter.regexFilter("^" + java.util.regex.Pattern.quote(type) + "$", 1));
        }
        transactionSorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
    }

    private void checkDatabaseConnection() {
        databaseAvailable = false;
        loginButton.setEnabled(false);
        adminLoginButton.setEnabled(false);
        registrationSubmitButton.setEnabled(false);
        retryConnectionButton.setVisible(false);
        adminRetryConnectionButton.setVisible(false);
        registrationRetryConnectionButton.setVisible(false);
        setLoginStatus("Connecting to JCash...", UiTheme.MUTED);
        setAdminLoginStatus("Connecting to JCash...", UiTheme.MUTED);
        setRegistrationStatus("Connecting to JCash...", UiTheme.MUTED);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws SQLException {
                DatabaseConnection.verifyConnection();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    databaseAvailable = true;
                    loginButton.setEnabled(
                            failedLoginAttempts < MAX_LOGIN_ATTEMPTS
                    );
                    adminLoginButton.setEnabled(
                            failedAdminLoginAttempts < MAX_LOGIN_ATTEMPTS
                    );
                    registrationSubmitButton.setEnabled(true);
                    setLoginStatus("Database connected", UiTheme.SUCCESS);
                    setAdminLoginStatus(
                            "Database connected",
                            UiTheme.SUCCESS
                    );
                    setRegistrationStatus(
                            "Database connected",
                            UiTheme.SUCCESS
                    );
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showConnectionFailure();
                } catch (ExecutionException exception) {
                    showConnectionFailure();
                }
            }
        }.execute();
    }

    private void showConnectionFailure() {
        databaseAvailable = false;
        loginButton.setEnabled(false);
        adminLoginButton.setEnabled(false);
        registrationSubmitButton.setEnabled(false);
        retryConnectionButton.setVisible(true);
        adminRetryConnectionButton.setVisible(true);
        registrationRetryConnectionButton.setVisible(true);
        setLoginStatus("Database unavailable", UiTheme.DANGER);
        setAdminLoginStatus("Database unavailable", UiTheme.DANGER);
        setRegistrationStatus("Database unavailable", UiTheme.DANGER);
        showError(
                "Connection unavailable",
                "Start MySQL and verify the JCash database configuration."
        );
    }

    private void showUserLogin() {
        applicationCards.show(applicationPanel, LOGIN_CARD);
        mobileNumberField.requestFocusInWindow();
    }

    private void showAdminLogin() {
        applicationCards.show(applicationPanel, ADMIN_LOGIN_CARD);
        adminUsernameField.requestFocusInWindow();
    }

    private void showRegistration() {
        applicationCards.show(applicationPanel, REGISTRATION_CARD);
        registrationNameField.requestFocusInWindow();
    }

    private void attemptRegistration() {
        if (!databaseAvailable) {
            setRegistrationStatus(
                    "Database unavailable. Retry the connection.",
                    UiTheme.DANGER
            );
            return;
        }

        String fullName = registrationNameField.getText().trim();
        String mobileNumber = registrationMobileField.getText().trim();
        String pin = new String(registrationPinField.getPassword()).trim();
        String confirmation = new String(
                registrationConfirmPinField.getPassword()
        ).trim();
        if (fullName.isBlank()) {
            setRegistrationStatus("Enter your full name.", UiTheme.DANGER);
            registrationNameField.requestFocusInWindow();
            return;
        }
        if (fullName.length() > 100) {
            setRegistrationStatus(
                    "Full name cannot exceed 100 characters.",
                    UiTheme.DANGER
            );
            registrationNameField.requestFocusInWindow();
            return;
        }
        if (!mobileNumber.matches("09\\d{9}")) {
            setRegistrationStatus(
                    "Enter 11 digits beginning with 09.",
                    UiTheme.DANGER
            );
            registrationMobileField.requestFocusInWindow();
            return;
        }
        if (!pin.matches("\\d{4}")) {
            setRegistrationStatus("Enter a four-digit PIN.", UiTheme.DANGER);
            registrationPinField.requestFocusInWindow();
            return;
        }
        if (!pin.equals(confirmation)) {
            setRegistrationStatus("Enter the same PIN twice.", UiTheme.DANGER);
            registrationConfirmPinField.requestFocusInWindow();
            return;
        }

        registerUser(fullName, mobileNumber, pin);
    }

    private void registerUser(
            String fullName,
            String mobileNumber,
            String pin
    ) {
        setRegistrationBusy(true);
        setRegistrationStatus("Creating your account...", UiTheme.MUTED);
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws SQLException {
                return adminAccountService.createAccount(
                        fullName,
                        mobileNumber,
                        pin
                );
            }

            @Override
            protected void done() {
                try {
                    User registeredUser = get();
                    mobileNumberField.setText(
                            registeredUser.getMobileNumber()
                    );
                    pinField.setText("");
                    clearRegistrationForm();
                    setLoginBusy(false);
                    applicationCards.show(applicationPanel, LOGIN_CARD);
                    boolean loginLocked = failedLoginAttempts
                            >= MAX_LOGIN_ATTEMPTS;
                    setLoginStatus(loginLocked
                                    ? "Account created. Restart JCash to sign in."
                                    : "Account created. Enter your PIN to sign in.",
                            loginLocked ? UiTheme.WARNING : UiTheme.SUCCESS);
                    UiDialogs.toast(JCashFrame.this, "Account created");
                    if (!loginLocked) {
                        pinField.requestFocusInWindow();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showRegistrationFailure(exception);
                } catch (ExecutionException exception) {
                    showRegistrationFailure(exception.getCause());
                } finally {
                    setRegistrationBusy(false);
                }
            }
        }.execute();
    }

    private void showRegistrationFailure(Throwable exception) {
        String message;
        if (exception instanceof IllegalArgumentException
                && exception.getMessage() != null) {
            message = exception.getMessage();
        } else {
            message = "The account could not be created. Check the database "
                    + "connection and try again.";
        }
        setRegistrationStatus(message, UiTheme.DANGER);
    }

    private void attemptLogin() {
        if (!databaseAvailable
                || failedLoginAttempts >= MAX_LOGIN_ATTEMPTS) {
            return;
        }

        String mobileNumber = mobileNumberField.getText().trim();
        String pin = new String(pinField.getPassword()).trim();
        if (!mobileNumber.matches("09\\d{9}")) {
            setLoginStatus("Enter an 11-digit mobile number starting with 09",
                    UiTheme.DANGER);
            mobileNumberField.requestFocusInWindow();
            return;
        }
        if (!pin.matches("\\d{4}")) {
            setLoginStatus("Enter your 4-digit PIN", UiTheme.DANGER);
            pinField.requestFocusInWindow();
            return;
        }
        setLoginBusy(true);
        setLoginStatus("Checking your credentials...", UiTheme.MUTED);

        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws SQLException {
                return auth.authenticate(mobileNumber, pin);
            }

            @Override
            protected void done() {
                try {
                    User authenticatedUser = get();
                    if (authenticatedUser == null) {
                        registerFailedLogin();
                        return;
                    }

                    failedLoginAttempts = 0;
                    currentUser = authenticatedUser;
                    clearLoginForm();
                    updateDashboardIdentity();
                    showOverview();
                    applicationCards.show(
                            applicationPanel,
                            DASHBOARD_CARD
                    );
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showAuthenticationFailure();
                } catch (ExecutionException exception) {
                    showAuthenticationFailure();
                } finally {
                    if (currentUser == null) {
                        setLoginBusy(false);
                    }
                }
            }
        }.execute();
    }

    private void registerFailedLogin() {
        failedLoginAttempts++;
        pinField.setText("");
        int remaining = MAX_LOGIN_ATTEMPTS - failedLoginAttempts;

        if (remaining == 0) {
            attemptsLabel.setText(
                    "JCash is locked for this application session."
            );
            attemptsLabel.setForeground(UiTheme.DANGER);
            setLoginStatus("Too many failed attempts", UiTheme.DANGER);
            loginButton.setEnabled(false);
            showError(
                    "Session locked",
                    "Three unsuccessful login attempts were recorded. "
                            + "Close and reopen JCash to try again."
            );
            return;
        }

        attemptsLabel.setText(
                remaining + (remaining == 1
                        ? " login attempt remaining."
                        : " login attempts remaining.")
        );
        attemptsLabel.setForeground(UiTheme.WARNING);
        setLoginStatus("Invalid mobile number or PIN", UiTheme.DANGER);
        pinField.requestFocusInWindow();
    }

    private void attemptAdminLogin() {
        if (!databaseAvailable
                || failedAdminLoginAttempts >= MAX_LOGIN_ATTEMPTS) {
            return;
        }

        String username = adminUsernameField.getText().trim();
        String pin = new String(adminPinField.getPassword()).trim();
        if (username.isBlank()) {
            setAdminLoginStatus("Enter your admin username", UiTheme.DANGER);
            adminUsernameField.requestFocusInWindow();
            return;
        }
        if (!pin.matches("\\d{4}")) {
            setAdminLoginStatus("Enter your 4-digit PIN", UiTheme.DANGER);
            adminPinField.requestFocusInWindow();
            return;
        }
        setAdminLoginBusy(true);
        setAdminLoginStatus("Checking admin credentials...", UiTheme.MUTED);

        new SwingWorker<Admin, Void>() {
            @Override
            protected Admin doInBackground() throws SQLException {
                return auth.authenticateAdmin(username, pin);
            }

            @Override
            protected void done() {
                try {
                    Admin authenticatedAdmin = get();
                    if (authenticatedAdmin == null) {
                        registerFailedAdminLogin();
                        return;
                    }

                    failedAdminLoginAttempts = 0;
                    currentAdmin = authenticatedAdmin;
                    clearAdminLoginForm();
                    adminDashboardPanel.showFor(authenticatedAdmin);
                    applicationCards.show(
                            applicationPanel,
                            ADMIN_DASHBOARD_CARD
                    );
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showAdminAuthenticationFailure();
                } catch (ExecutionException exception) {
                    showAdminAuthenticationFailure();
                } finally {
                    if (currentAdmin == null) {
                        setAdminLoginBusy(false);
                    }
                }
            }
        }.execute();
    }

    private void registerFailedAdminLogin() {
        failedAdminLoginAttempts++;
        adminPinField.setText("");
        int remaining = MAX_LOGIN_ATTEMPTS - failedAdminLoginAttempts;
        if (remaining == 0) {
            adminAttemptsLabel.setText(
                    "Admin login is locked for this application session."
            );
            adminAttemptsLabel.setForeground(UiTheme.DANGER);
            setAdminLoginStatus("Too many failed attempts", UiTheme.DANGER);
            adminLoginButton.setEnabled(false);
            showError(
                    "Admin login locked",
                    "Three unsuccessful admin login attempts were recorded. "
                            + "Close and reopen JCash to try again."
            );
            return;
        }

        adminAttemptsLabel.setText(
                remaining + (remaining == 1
                        ? " admin login attempt remaining."
                        : " admin login attempts remaining.")
        );
        adminAttemptsLabel.setForeground(UiTheme.WARNING);
        setAdminLoginStatus("Invalid admin username or PIN", UiTheme.DANGER);
        adminPinField.requestFocusInWindow();
    }

    private void showAdminAuthenticationFailure() {
        setAdminLoginStatus("Authentication unavailable", UiTheme.DANGER);
        showError(
                "Unable to sign in",
                "The database request failed. Check MySQL and try again."
        );
    }

    private void showAuthenticationFailure() {
        setLoginStatus("Authentication unavailable", UiTheme.DANGER);
        showError(
                "Unable to sign in",
                "The database request failed. Check MySQL and try again."
        );
    }

    private void showOverview() {
        contentCards.show(contentPanel, OVERVIEW_CARD);
        UiTheme.setNavigationActive(overviewNavigationButton, navigationButtons);
        updateDashboardIdentity();
        refreshTransactionsSilently();
    }

    private void showTransactions() {
        contentCards.show(contentPanel, TRANSACTIONS_CARD);
        UiTheme.setNavigationActive(transactionsNavigationButton, navigationButtons);
        loadTransactions();
    }

    private void showCashInDialog() {
        JTextField amountField = UiTheme.styleField(new JTextField());
        JPanel form = createDialogForm(
                "Enter the amount you want to add.",
                "Amount",
                amountField
        );

        if (!UiDialogs.form(this, "Cash in", form, "Continue")) {
            return;
        }

        BigDecimal amount = parseAmount(amountField.getText());
        if (amount == null) {
            showError(
                    "Invalid amount",
                    "Enter a positive amount with no more than two decimals."
            );
            return;
        }

        if (!UiDialogs.confirm(this, "Confirm cash-in",
                "Add " + balance.formatAmount(amount) + " to your wallet?",
                "Add funds")) {
            return;
        }

        BigDecimal previousBalance = currentUser.getBalance();
        runDashboardTask(
                "Processing cash-in...",
                () -> cashIn.cashIn(currentUser, amount),
                updatedUser -> {
                    currentUser = updatedUser;
                    updateDashboardIdentity();
                    showSuccess(
                            "Cash-in complete",
                            receiptText(
                                    "Amount deposited",
                                    amount,
                                    previousBalance,
                                    updatedUser.getBalance()
                            )
                    );
                }
        );
    }

    private void showWithdrawalDialog() {
        JTextField amountField = UiTheme.styleField(new JTextField());
        JPanel form = createDialogForm(
                "Enter the amount you want to withdraw.",
                "Amount",
                amountField
        );
        if (!UiDialogs.form(this, "Withdraw money", form, "Continue")) {
            return;
        }

        BigDecimal amount = parseAmount(amountField.getText());
        if (amount == null) {
            showError(
                    "Invalid amount",
                    "Enter a positive amount with no more than two decimals."
            );
            return;
        }
        if (!UiDialogs.confirm(this, "Confirm withdrawal",
                "Withdraw " + balance.formatAmount(amount) + "?",
                "Withdraw")) {
            return;
        }

        BigDecimal previousBalance = currentUser.getBalance();
        runDashboardTask(
                "Processing withdrawal...",
                () -> withdrawal.withdraw(currentUser, amount),
                updatedUser -> {
                    currentUser = updatedUser;
                    updateDashboardIdentity();
                    showSuccess(
                            "Withdrawal complete",
                            receiptText(
                                    "Amount withdrawn",
                                    amount,
                                    previousBalance,
                                    updatedUser.getBalance()
                            )
                    );
                }
        );
    }

    private void showTransferDialog() {
        JTextField receiverField = UiTheme.styleField(new JTextField());
        JTextField amountField = UiTheme.styleField(new JTextField());
        installDigitsOnlyFilter(receiverField, 11);

        JPanel form = new JPanel();
        form.setBackground(UiTheme.SURFACE);
        form.setBorder(new EmptyBorder(8, 8, 8, 8));
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        addLeftAligned(form, UiTheme.label(
                "Send funds to another JCash customer.",
                UiTheme.MUTED,
                UiTheme.FONT_PLAIN
        ));
        form.add(Box.createVerticalStrut(16));
        addField(form, "Receiver mobile number", receiverField);
        form.add(Box.createVerticalStrut(14));
        addField(form, "Amount", amountField);

        if (!UiDialogs.form(this, "Transfer money", form, "Continue")) {
            return;
        }

        String receiver = receiverField.getText().trim();
        if (!receiver.matches("09\\d{9}")) {
            showError(
                    "Invalid receiver",
                    "Enter an 11-digit mobile number starting with 09."
            );
            return;
        }

        BigDecimal amount = parseAmount(amountField.getText());
        if (amount == null) {
            showError(
                    "Invalid amount",
                    "Enter a positive amount with no more than two decimals."
            );
            return;
        }

        if (!UiDialogs.confirm(this, "Confirm transfer",
                "Send " + balance.formatAmount(amount) + " to " + receiver + "?",
                "Send money")) {
            return;
        }

        runDashboardTask(
                "Sending transfer...",
                () -> transfer.transferWithReceipt(
                        currentUser,
                        receiver,
                        amount
                ),
                transferReceipt -> {
                    currentUser = transferReceipt.sender();
                    updateDashboardIdentity();
                    showSuccess(
                            "Transfer complete",
                            "Recipient: "
                                    + transferReceipt.receiver().getFullName()
                                    + " (" + receiver + ")\n"
                                    + receiptText(
                                            "Amount transferred",
                                            amount,
                                            transferReceipt
                                                    .previousSenderBalance(),
                                            transferReceipt.sender()
                                                    .getBalance()
                                    )
                    );
                }
        );
    }

    private void showAccountDetails() {
        if (currentUser == null) {
            return;
        }
        UiTheme.setNavigationActive(detailsNavigationButton, navigationButtons);
        UiDialogs.message(this, "Account details",
                "Full name: " + currentUser.getFullName()
                        + "\nAccount/mobile number: "
                        + currentUser.getMobileNumber()
                        + "\nCurrent balance: "
                        + balance.formatBalance(currentUser), false);
    }

    private void loadTransactions() {
        if (currentUser == null) {
            return;
        }
        runDashboardTask(
                "Loading transactions...",
                () -> logs.getTransactions(currentUser),
                this::populateTransactionTable
        );
    }

    private void populateTransactionTable(List<Transaction> transactions) {
        transactionTableModel.setRowCount(0);
        recentTableModel.setRowCount(0);
        int recentCount = 0;
        for (Transaction transaction : transactions) {
            Object[] row = toTableRow(transaction);
            transactionTableModel.addRow(row);
            if (recentCount++ < 5) {
                recentTableModel.addRow(new Object[]{row[0], row[1], row[2]});
            }
        }
    }

    private void refreshTransactionsSilently() {
        if (currentUser == null) {
            return;
        }
        User requestedFor = currentUser;
        new SwingWorker<List<Transaction>, Void>() {
            @Override
            protected List<Transaction> doInBackground() throws SQLException {
                return logs.getTransactions(requestedFor);
            }

            @Override
            protected void done() {
                try {
                    if (currentUser == requestedFor) {
                        populateTransactionTable(get());
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ignored) {
                    // A silent refresh leaves the existing table unchanged.
                }
            }
        }.execute();
    }

    private Object[] toTableRow(Transaction transaction) {
        String type;
        String amountPrefix;
        String counterparty = "-";

        if (transaction instanceof CashInTransaction) {
            type = "Cash in";
            amountPrefix = "+";
        } else if (transaction instanceof WithdrawalTransaction) {
            type = "Withdrawal";
            amountPrefix = "-";
        } else if (transaction instanceof AdminCreditTransaction credit) {
            type = "Admin credit";
            amountPrefix = "+";
            counterparty = "Admin " + credit.getAdminUsername();
        } else if (transaction instanceof AdminDebitTransaction debit) {
            type = "Admin debit";
            amountPrefix = "-";
            counterparty = "Admin " + debit.getAdminUsername();
        } else if (transaction instanceof TransferTransaction transferRecord) {
            if (currentUser.getMobileNumber().equals(
                    transferRecord.getSenderMobileNumber()
            )) {
                type = "Transfer sent";
                amountPrefix = "-";
                counterparty = "To "
                        + transferRecord.getReceiverMobileNumber();
            } else {
                type = "Transfer received";
                amountPrefix = "+";
                counterparty = "From "
                        + transferRecord.getSenderMobileNumber();
            }
        } else {
            throw new IllegalArgumentException(
                    "Unsupported transaction class: "
                            + transaction.getClass().getName()
            );
        }

        return new Object[]{
            transaction.getDateTime().format(DATE_TIME_FORMAT),
            type,
            amountPrefix + balance.formatAmount(transaction.getAmount()),
            transaction.getDetails(),
            counterparty
        };
    }

    private void logout() {
        currentUser = null;
        transactionTableModel.setRowCount(0);
        recentTableModel.setRowCount(0);
        clearLoginForm();
        resetLoginFeedback();
        setLoginBusy(false);
        applicationCards.show(applicationPanel, LANDING_CARD);
    }

    private void adminLogout() {
        currentAdmin = null;
        clearAdminLoginForm();
        resetAdminLoginFeedback();
        setAdminLoginBusy(false);
        applicationCards.show(applicationPanel, LANDING_CARD);
    }

    private void clearLoginForm() {
        mobileNumberField.setText("");
        pinField.setText("");
        UiTheme.hidePassword(pinField);
    }

    private void clearAdminLoginForm() {
        adminUsernameField.setText("");
        adminPinField.setText("");
        UiTheme.hidePassword(adminPinField);
    }

    private void clearRegistrationForm() {
        registrationNameField.setText("");
        registrationMobileField.setText("");
        registrationPinField.setText("");
        registrationConfirmPinField.setText("");
        UiTheme.hidePassword(registrationPinField);
        UiTheme.hidePassword(registrationConfirmPinField);
    }

    private void resetLoginFeedback() {
        failedLoginAttempts = 0;
        attemptsLabel.setText("You have 3 login attempts.");
        attemptsLabel.setForeground(UiTheme.MUTED);
        setLoginStatus(
                databaseAvailable ? "Database connected" : "Database unavailable",
                databaseAvailable ? UiTheme.SUCCESS : UiTheme.DANGER
        );
        loginButton.setEnabled(databaseAvailable);
    }

    private void resetAdminLoginFeedback() {
        failedAdminLoginAttempts = 0;
        adminAttemptsLabel.setText("You have 3 admin login attempts.");
        adminAttemptsLabel.setForeground(UiTheme.MUTED);
        setAdminLoginStatus(
                databaseAvailable ? "Database connected" : "Database unavailable",
                databaseAvailable ? UiTheme.SUCCESS : UiTheme.DANGER
        );
        adminLoginButton.setEnabled(databaseAvailable);
    }

    private void updateDashboardIdentity() {
        if (currentUser == null) {
            return;
        }
        welcomeLabel.setText("Hello, " + currentUser.getFullName());
        mobileLabel.setText(currentUser.getMobileNumber());
        balanceLabel.setText(balance.formatBalance(currentUser));
    }

    private void updateResponsiveLayout() {
        if (userSidebar == null) {
            return;
        }
        boolean compact = getWidth() < 1050;
        int width = compact ? 82 : 230;
        userSidebar.setPreferredSize(new Dimension(width, 0));
        userSidebar.setBorder(new EmptyBorder(30, compact ? 14 : 22, 28,
                compact ? 14 : 22));
        userSidebarBrand.setText(compact ? "J" : "JCASH");
        for (JButton button : dashboardControls) {
            Object fullText = button.getClientProperty("jcash.fullText");
            if (fullText instanceof String text) {
                button.setText(compact ? "" : text);
                button.setToolTipText(compact ? text : null);
                button.setHorizontalAlignment(compact
                        ? SwingConstants.CENTER : SwingConstants.LEFT);
            }
        }
        userSidebar.revalidate();
    }

    private <T> void runDashboardTask(
            String progressMessage,
            UiTask<T> task,
            Consumer<T> successHandler
    ) {
        setDashboardBusy(true);

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.execute();
            }

            @Override
            protected void done() {
                try {
                    T result = get();
                    successHandler.accept(result);
                    if (!progressMessage.startsWith("Loading")) {
                        refreshTransactionsSilently();
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showOperationFailure(exception);
                } catch (ExecutionException exception) {
                    showOperationFailure(exception.getCause());
                } finally {
                    setDashboardBusy(false);
                }
            }
        }.execute();
    }

    private void showOperationFailure(Throwable exception) {
        String message;
        if (exception instanceof IllegalArgumentException
                && exception.getMessage() != null) {
            message = exception.getMessage();
        } else {
            message = "The database operation could not be completed. "
                    + "Check MySQL and try again.";
        }
        showError("Unable to complete request", message);
    }

    private void setLoginBusy(boolean busy) {
        boolean locked = failedLoginAttempts >= MAX_LOGIN_ATTEMPTS;
        mobileNumberField.setEnabled(!busy && !locked);
        pinField.setEnabled(!busy && !locked);
        loginButton.setEnabled(
                !busy
                        && databaseAvailable
                        && !locked
        );
    }

    private void setAdminLoginBusy(boolean busy) {
        boolean locked = failedAdminLoginAttempts >= MAX_LOGIN_ATTEMPTS;
        adminUsernameField.setEnabled(!busy && !locked);
        adminPinField.setEnabled(!busy && !locked);
        adminLoginButton.setEnabled(
                !busy && databaseAvailable && !locked
        );
    }

    private void setRegistrationBusy(boolean busy) {
        registrationNameField.setEnabled(!busy);
        registrationMobileField.setEnabled(!busy);
        registrationPinField.setEnabled(!busy);
        registrationConfirmPinField.setEnabled(!busy);
        registrationSubmitButton.setEnabled(!busy && databaseAvailable);
        registrationRetryConnectionButton.setEnabled(!busy);
    }

    private void setDashboardBusy(boolean busy) {
        for (JButton control : dashboardControls) {
            control.setEnabled(!busy);
        }
    }

    private void setLoginStatus(String message, Color color) {
        loginStatusLabel.setText(message);
        loginStatusLabel.setForeground(color);
    }

    private void setAdminLoginStatus(String message, Color color) {
        adminLoginStatusLabel.setText(message);
        adminLoginStatusLabel.setForeground(color);
    }

    private void setRegistrationStatus(String message, Color color) {
        registrationStatusLabel.setText(message);
        registrationStatusLabel.setForeground(color);
    }

    private JPanel createDialogForm(
            String description,
            String fieldLabel,
            JTextField field
    ) {
        JPanel form = new JPanel();
        form.setBackground(UiTheme.SURFACE);
        form.setBorder(new EmptyBorder(8, 8, 8, 8));
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        addLeftAligned(form, UiTheme.label(
                description,
                UiTheme.MUTED,
                UiTheme.FONT_PLAIN
        ));
        form.add(Box.createVerticalStrut(16));
        addField(form, fieldLabel, field);
        return form;
    }

    private static void addField(
            JPanel panel,
            String labelText,
            JTextField field
    ) {
        JLabel label = UiTheme.label(
                labelText,
                UiTheme.TEXT,
                UiTheme.FONT_MEDIUM.deriveFont(12f)
        );
        addLeftAligned(panel, label);
        panel.add(Box.createVerticalStrut(7));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension preferred = field.getPreferredSize();
        field.setPreferredSize(new Dimension(preferred.width, 44));
        field.setMinimumSize(new Dimension(0, 44));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        panel.add(field);
    }

    private static void addLeftAligned(JPanel panel, JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(component);
    }

    private static JButton fullWidthButton(JButton button) {
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        return button;
    }

    private String receiptText(
            String amountLabel,
            BigDecimal amount,
            BigDecimal previousBalance,
            BigDecimal newBalance
    ) {
        return "Old balance: " + balance.formatAmount(previousBalance)
                + "\n" + amountLabel + ": "
                + balance.formatAmount(amount)
                + "\nNew balance: " + balance.formatAmount(newBalance);
    }

    private static BigDecimal parseAmount(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.trim();
        if (!normalized.matches("\\d+(\\.\\d{1,2})?")) {
            return null;
        }

        BigDecimal amount = new BigDecimal(normalized);
        return amount.compareTo(BigDecimal.ZERO) > 0 ? amount : null;
    }

    private static void installDigitsOnlyFilter(
            JTextField field,
            int maximumLength
    ) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(
                new DigitsOnlyFilter(maximumLength)
        );
    }

    private void hideAllPasswordFields() {
        UiTheme.hidePassword(pinField);
        UiTheme.hidePassword(adminPinField);
        UiTheme.hidePassword(registrationPinField);
        UiTheme.hidePassword(registrationConfirmPinField);
    }

    private void showError(String title, String message) {
        UiDialogs.message(this, title, message, true);
    }

    private void showSuccess(String title, String message) {
        UiDialogs.message(this, title, message, false);
        UiDialogs.toast(this, title);
    }

    @FunctionalInterface
    private interface UiTask<T> {

        T execute() throws Exception;
    }

    private static final class DigitsOnlyFilter extends DocumentFilter {

        private final int maximumLength;

        private DigitsOnlyFilter(int maximumLength) {
            this.maximumLength = maximumLength;
        }

        @Override
        public void insertString(
                FilterBypass bypass,
                int offset,
                String text,
                AttributeSet attributes
        ) throws BadLocationException {
            replace(bypass, offset, 0, text, attributes);
        }

        @Override
        public void replace(
                FilterBypass bypass,
                int offset,
                int length,
                String text,
                AttributeSet attributes
        ) throws BadLocationException {
            String replacement = text == null ? "" : text;
            String current = bypass.getDocument().getText(
                    0,
                    bypass.getDocument().getLength()
            );
            String candidate = current.substring(0, offset)
                    + replacement
                    + current.substring(offset + length);

            if (candidate.length() <= maximumLength
                    && candidate.matches("\\d*")) {
                bypass.replace(offset, length, replacement, attributes);
            }
        }
    }

    private static final class AmountCellRenderer
            extends DefaultTableCellRenderer {

        private AmountCellRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focused,
                int row,
                int column
        ) {
            Component component = super.getTableCellRendererComponent(
                    table,
                    value,
                    selected,
                    focused,
                    row,
                    column
            );

            if (!selected && value != null) {
                component.setForeground(
                        value.toString().startsWith("+")
                                ? UiTheme.SUCCESS : UiTheme.DANGER
                );
            }
            setFont(UiTheme.FONT_MEDIUM.deriveFont(13f));
            return component;
        }
    }
}
