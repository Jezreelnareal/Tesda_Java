package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.RowFilter;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import model.Admin;
import model.AdminCreditTransaction;
import model.AdminDebitTransaction;
import model.BalanceChangeReceipt;
import model.CashInTransaction;
import model.SystemReport;
import model.Transaction;
import model.TransactionTotals;
import model.TransferTransaction;
import model.User;
import model.WithdrawalTransaction;
import service.AdminAccountService;
import service.Balance;

final class AdminDashboardPanel extends JPanel {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JFrame owner;
    private final AdminAccountService service;
    private final Balance balance;
    private final Runnable logoutAction;
    private final List<JButton> controls = new ArrayList<>();
    private final List<JButton> navigationButtons = new ArrayList<>();
    private final DefaultTableModel accountTableModel = new DefaultTableModel(
            new String[]{"Mobile/account number", "Full name", "Balance"},
            0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable accountTable = new JTable(accountTableModel);
    private final DefaultTableModel recentTableModel = new DefaultTableModel(
            new String[]{"Date", "Type", "Amount", "Participants"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable recentTable = new JTable(recentTableModel);
    private final JLabel identityLabel = UiTheme.label(
            "Administrator",
            UiTheme.NAVY,
            UiTheme.FONT_TITLE.deriveFont(22f)
    );
    private final JLabel usersMetric = UiTheme.label("0", UiTheme.NAVY,
            UiTheme.FONT_BALANCE.deriveFont(25f));
    private final JLabel balanceMetric = UiTheme.label("PHP 0.00", UiTheme.NAVY,
            UiTheme.FONT_BALANCE.deriveFont(25f));
    private final JLabel transactionsMetric = UiTheme.label("0", UiTheme.NAVY,
            UiTheme.FONT_BALANCE.deriveFont(25f));
    private final TableRowSorter<DefaultTableModel> accountSorter =
            new TableRowSorter<>(accountTableModel);
    private JPanel adminSidebar;
    private JLabel adminSidebarBrand;

    private Admin currentAdmin;

    AdminDashboardPanel(
            JFrame owner,
            AdminAccountService service,
            Balance balance,
            Runnable logoutAction
    ) {
        this.owner = owner;
        this.service = service;
        this.balance = balance;
        this.logoutAction = logoutAction;
        setLayout(new BorderLayout());
        setBackground(UiTheme.BACKGROUND);
        add(createSidebar(), BorderLayout.WEST);
        add(createBody(), BorderLayout.CENTER);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateResponsiveLayout();
            }
        });
    }

    void showFor(Admin admin) {
        currentAdmin = admin;
        identityLabel.setText("Admin: " + admin.username());
        loadDashboard();
    }

    private JPanel createSidebar() {
        adminSidebar = new JPanel();
        adminSidebar.setPreferredSize(new Dimension(230, 0));
        adminSidebar.setBackground(UiTheme.NAVY);
        adminSidebar.setBorder(new EmptyBorder(30, 22, 28, 22));
        adminSidebar.setLayout(new BoxLayout(adminSidebar, BoxLayout.Y_AXIS));
        adminSidebarBrand = UiTheme.label(
                "JCASH ADMIN",
                Color.WHITE,
                UiTheme.FONT_TITLE.deriveFont(22f)
        );
        adminSidebar.add(adminSidebarBrand);
        adminSidebar.add(Box.createVerticalStrut(32));

        JButton allAccounts = navigationButton("Accounts", UiIcon.Kind.HOME);
        JButton findAccount = navigationButton("Find account", UiIcon.Kind.SEARCH);
        JButton credit = navigationButton("Add funds", UiIcon.Kind.ADD);
        JButton debit = navigationButton("Deduct funds", UiIcon.Kind.REMOVE);
        JButton create = navigationButton("Create account", UiIcon.Kind.USER);
        JButton report = navigationButton("System report", UiIcon.Kind.REPORT);
        JButton logout = navigationButton("Log out", UiIcon.Kind.LOGOUT);
        navigationButtons.addAll(List.of(allAccounts, findAccount, credit,
                debit, create, report));
        allAccounts.addActionListener(event -> {
            UiTheme.setNavigationActive(allAccounts, navigationButtons);
            loadDashboard();
        });
        findAccount.addActionListener(event -> {
            UiTheme.setNavigationActive(findAccount, navigationButtons);
            findAccount();
        });
        credit.addActionListener(event -> {
            UiTheme.setNavigationActive(credit, navigationButtons);
            adjustAccount(true);
        });
        debit.addActionListener(event -> {
            UiTheme.setNavigationActive(debit, navigationButtons);
            adjustAccount(false);
        });
        create.addActionListener(event -> {
            UiTheme.setNavigationActive(create, navigationButtons);
            createAccount();
        });
        report.addActionListener(event -> {
            UiTheme.setNavigationActive(report, navigationButtons);
            generateReport();
        });
        logout.addActionListener(event -> {
            currentAdmin = null;
            accountTableModel.setRowCount(0);
            logoutAction.run();
        });

        for (JButton button : List.of(
                allAccounts,
                findAccount,
                credit,
                debit,
                create,
                report
        )) {
            controls.add(button);
            adminSidebar.add(button);
            adminSidebar.add(Box.createVerticalStrut(8));
        }
        controls.add(logout);
        adminSidebar.add(Box.createVerticalGlue());
        adminSidebar.add(logout);
        UiTheme.setNavigationActive(allAccounts, navigationButtons);
        return adminSidebar;
    }

    private JPanel createBody() {
        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setBackground(UiTheme.BACKGROUND);
        body.setBorder(new EmptyBorder(24, 30, 28, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(identityLabel, BorderLayout.WEST);
        header.add(UiTheme.themeButton(owner), BorderLayout.EAST);

        JPanel metrics = new JPanel(new GridLayout(1, 3, 14, 0));
        metrics.setOpaque(false);
        metrics.add(metricCard("CUSTOMERS", usersMetric));
        metrics.add(metricCard("COMBINED BALANCE", balanceMetric));
        metrics.add(metricCard("TRANSACTIONS", transactionsMetric));

        accountTable.setFont(UiTheme.FONT_PLAIN.deriveFont(13f));
        accountTable.setRowHeight(36);
        accountTable.setAutoCreateRowSorter(true);
        accountTable.setRowSorter(accountSorter);
        accountTable.setFillsViewportHeight(true);
        accountTable.setForeground(UiTheme.TEXT);
        accountTable.setBackground(UiTheme.SURFACE);
        accountTable.setGridColor(UiTheme.BORDER);
        accountTable.setShowVerticalLines(false);
        accountTable.getTableHeader().setFont(UiTheme.FONT_MEDIUM);
        accountTable.getTableHeader().setForeground(UiTheme.NAVY);
        centerTableText(accountTable);
        JScrollPane scrollPane = new JScrollPane(accountTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scrollPane.getViewport().setBackground(UiTheme.SURFACE);

        JPanel tablePanel = new JPanel(new BorderLayout(0, 12));
        tablePanel.setOpaque(false);
        JPanel tableHeading = new JPanel(new BorderLayout(12, 0));
        tableHeading.setOpaque(false);
        tableHeading.add(UiTheme.label(
                "Customer accounts",
                UiTheme.NAVY,
                UiTheme.FONT_TITLE.deriveFont(21f)
        ), BorderLayout.WEST);
        JTextField search = UiTheme.styleField(new JTextField(16));
        search.putClientProperty(com.formdev.flatlaf.FlatClientProperties.PLACEHOLDER_TEXT,
                "Search accounts");
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { filterAccounts(search.getText()); }
            public void removeUpdate(DocumentEvent event) { filterAccounts(search.getText()); }
            public void changedUpdate(DocumentEvent event) { filterAccounts(search.getText()); }
        });
        tableHeading.add(search, BorderLayout.EAST);
        tablePanel.add(tableHeading, BorderLayout.NORTH);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        recentTable.setFont(UiTheme.FONT_PLAIN.deriveFont(12f));
        recentTable.setRowHeight(28);
        recentTable.setFillsViewportHeight(true);
        recentTable.setForeground(UiTheme.TEXT);
        recentTable.setBackground(UiTheme.SURFACE);
        recentTable.setGridColor(UiTheme.BORDER);
        recentTable.setShowVerticalLines(false);
        recentTable.getTableHeader().setFont(UiTheme.FONT_MEDIUM);
        recentTable.getTableHeader().setForeground(UiTheme.NAVY);
        centerTableText(recentTable);
        JPanel recentPanel = new JPanel(new BorderLayout(0, 8));
        recentPanel.setOpaque(false);
        recentPanel.setPreferredSize(new Dimension(0, 190));
        recentPanel.add(UiTheme.label("Recent system activity", UiTheme.NAVY,
                UiTheme.FONT_TITLE.deriveFont(18f)), BorderLayout.NORTH);
        JScrollPane recentScrollPane = new JScrollPane(recentTable);
        recentScrollPane.setBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER)
        );
        recentScrollPane.getViewport().setBackground(UiTheme.SURFACE);
        recentPanel.add(recentScrollPane, BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        center.add(metrics, BorderLayout.NORTH);
        center.add(tablePanel, BorderLayout.CENTER);
        center.add(recentPanel, BorderLayout.SOUTH);
        body.add(header, BorderLayout.NORTH);
        body.add(center, BorderLayout.CENTER);
        return body;
    }

    private JPanel metricCard(String title, JLabel value) {
        UiTheme.RoundedPanel card = new UiTheme.RoundedPanel(UiTheme.SURFACE, 18);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(UiTheme.label(title, UiTheme.MUTED,
                UiTheme.FONT_MEDIUM.deriveFont(11f)));
        card.add(Box.createVerticalStrut(6));
        card.add(value);
        return card;
    }

    private JButton navigationButton(String text, UiIcon.Kind icon) {
        JButton button = UiTheme.darkButton(text);
        button.putClientProperty("jcash.fullText", text);
        button.setIcon(new UiIcon(icon));
        button.setIconTextGap(12);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        return button;
    }

    private void filterAccounts(String query) {
        String trimmed = query.trim();
        accountSorter.setRowFilter(trimmed.isEmpty() ? null
                : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(trimmed)));
    }

    private void loadDashboard() {
        runTask("Loading dashboard...", () -> new Object[]{
                service.listAccounts(), service.generateSystemReport()
        }, values -> {
            @SuppressWarnings("unchecked")
            List<User> users = (List<User>) values[0];
            populateAccounts(users);
            populateMetrics((SystemReport) values[1]);
        });
    }

    private void populateMetrics(SystemReport report) {
        usersMetric.setText(Integer.toString(report.userCount()));
        balanceMetric.setText(balance.formatAmount(report.combinedBalance()));
        long total = report.totalsByType().values().stream()
                .mapToLong(TransactionTotals::count).sum();
        transactionsMetric.setText(Long.toString(total));
        recentTableModel.setRowCount(0);
        report.recentTransactions().stream().limit(10).forEach(transaction ->
                recentTableModel.addRow(new Object[]{
                        transaction.getDateTime().format(DATE_TIME_FORMAT),
                        transaction.getType(),
                        balance.formatAmount(transaction.getAmount()),
                        participants(transaction)
                }));
    }

    private void populateAccounts(List<User> users) {
        accountTableModel.setRowCount(0);
        for (User user : users) {
            accountTableModel.addRow(new Object[]{
                user.getMobileNumber(),
                user.getFullName(),
                balance.formatBalance(user)
            });
        }
    }

    private void findAccount() {
        JTextField mobileField = UiTheme.styleField(new JTextField());
        JPanel form = verticalForm();
        addField(form, "11-digit mobile/account number", mobileField);
        if (!UiDialogs.form(owner, "Find account", form, "Find")) {
            return;
        }
        String mobile = mobileField.getText().trim();
        runTask("Finding account...", () -> service.findAccount(mobile),
                user -> {
                    if (user == null) {
                        showError("Account not found", "No matching account exists.");
                        return;
                    }
                    showAccount(user);
                });
    }

    private void showAccount(User user) {
        UiDialogs.message(owner, "Account details",
                "Full name: " + user.getFullName()
                        + "\nAccount/mobile number: "
                        + user.getMobileNumber()
                        + "\nBalance: " + balance.formatBalance(user), false);
    }

    private void adjustAccount(boolean credit) {
        JTextField mobileField = UiTheme.styleField(new JTextField());
        JTextField amountField = UiTheme.styleField(new JTextField());
        JPanel form = verticalForm();
        addField(form, "Mobile/account number", mobileField);
        form.add(Box.createVerticalStrut(12));
        addField(form, "Amount", amountField);
        String title = credit ? "Add funds" : "Deduct funds";
        if (!UiDialogs.form(owner, title, form, "Continue")) {
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
        String mobile = mobileField.getText().trim();
        runTask(
                credit ? "Adding funds..." : "Deducting funds...",
                () -> credit
                        ? service.creditAccount(currentAdmin, mobile, amount)
                        : service.debitAccount(currentAdmin, mobile, amount),
                receipt -> {
                    showBalanceReceipt(
                            credit ? "Funds added" : "Funds deducted",
                            credit ? "Amount added" : "Amount deducted",
                            receipt
                    );
                    loadDashboard();
                }
        );
    }

    private void showBalanceReceipt(
            String title,
            String amountLabel,
            BalanceChangeReceipt receipt
    ) {
        UiDialogs.message(owner, title,
                "Account: " + receipt.user().getFullName()
                        + " (" + receipt.user().getMobileNumber() + ")\n"
                        + "Old balance: "
                        + balance.formatAmount(receipt.previousBalance())
                        + "\n" + amountLabel + ": "
                        + balance.formatAmount(
                                receipt.transaction().getAmount()
                        )
                        + "\nNew balance: "
                        + balance.formatBalance(receipt.user()), false);
        UiDialogs.toast(owner, title);
    }

    private void createAccount() {
        JTextField nameField = UiTheme.styleField(new JTextField());
        JTextField mobileField = UiTheme.styleField(new JTextField());
        JPasswordField pinField = new JPasswordField();
        JPasswordField confirmPinField = new JPasswordField();
        UiTheme.styleField(pinField);
        UiTheme.styleField(confirmPinField);
        UiTheme.installPasswordVisibilityToggle(pinField);
        UiTheme.installPasswordVisibilityToggle(confirmPinField);
        JPanel form = verticalForm();
        addField(form, "Full name", nameField);
        form.add(Box.createVerticalStrut(10));
        addField(form, "Mobile/account number", mobileField);
        form.add(Box.createVerticalStrut(10));
        addField(form, "4-digit PIN", pinField);
        form.add(Box.createVerticalStrut(10));
        addField(form, "Confirm PIN", confirmPinField);
        if (!UiDialogs.form(owner, "Create new account", form,
                "Create account")) {
            return;
        }

        String pin = new String(pinField.getPassword()).trim();
        String confirmation = new String(
                confirmPinField.getPassword()
        ).trim();
        if (!pin.equals(confirmation)) {
            showError("PINs do not match", "Enter the same PIN twice.");
            return;
        }
        runTask(
                "Creating account...",
                () -> service.createAccount(
                        nameField.getText().trim(),
                        mobileField.getText().trim(),
                        pin
                ),
                user -> {
                    showAccount(user);
                    UiDialogs.toast(owner, "Account created");
                    loadDashboard();
                }
        );
    }

    private void generateReport() {
        runTask(
                "Generating report...",
                service::generateSystemReport,
                this::showReport
        );
    }

    private void showReport(SystemReport report) {
        StringBuilder summary = new StringBuilder();
        summary.append("Total users: ").append(report.userCount())
                .append("\nCombined balances: ")
                .append(balance.formatAmount(report.combinedBalance()))
                .append("\n\nTransactions by type:\n");
        for (model.TransactionType type : model.TransactionType.values()) {
            TransactionTotals totals = report.totalsByType().getOrDefault(
                    type,
                    new TransactionTotals(0, BigDecimal.ZERO)
            );
            summary.append(type).append(": ")
                    .append(totals.count()).append(" | ")
                    .append(balance.formatAmount(totals.amount()))
                    .append('\n');
        }

        JTextArea summaryArea = new JTextArea(summary.toString());
        summaryArea.setEditable(false);
        summaryArea.setFont(UiTheme.FONT_PLAIN);
        summaryArea.setBackground(UiTheme.BACKGROUND);
        DefaultTableModel model = new DefaultTableModel(
                new String[]{"Date", "Type", "Amount", "Participants"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (Transaction transaction : report.recentTransactions()) {
            model.addRow(new Object[]{
                transaction.getDateTime().format(DATE_TIME_FORMAT),
                transaction.getType(),
                balance.formatAmount(transaction.getAmount()),
                participants(transaction)
            });
        }
        JTable table = new JTable(model);
        table.setRowHeight(28);
        centerTableText(table);
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setPreferredSize(new Dimension(760, 520));
        panel.add(summaryArea, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        UiDialogs.componentMessage(owner, "JCash system report", panel);
    }

    private static void centerTableText(JTable table) {
        DefaultTableCellRenderer cellRenderer =
                new DefaultTableCellRenderer();
        cellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int column = 0; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column)
                    .setCellRenderer(cellRenderer);
        }

        TableCellRenderer headerRenderer =
                table.getTableHeader().getDefaultRenderer();
        table.getTableHeader().setDefaultRenderer(
                (target, value, selected, focused, row, column) -> {
                    Component component = headerRenderer
                            .getTableCellRendererComponent(
                                    target,
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
                }
        );
    }

    private static String participants(Transaction transaction) {
        if (transaction instanceof CashInTransaction cashIn) {
            return "To " + cashIn.getUserMobileNumber();
        }
        if (transaction instanceof WithdrawalTransaction withdrawal) {
            return "From " + withdrawal.getUserMobileNumber();
        }
        if (transaction instanceof TransferTransaction transfer) {
            return transfer.getSenderMobileNumber() + " -> "
                    + transfer.getReceiverMobileNumber();
        }
        if (transaction instanceof AdminCreditTransaction credit) {
            return credit.getAdminUsername() + " -> "
                    + credit.getUserMobileNumber();
        }
        if (transaction instanceof AdminDebitTransaction debit) {
            return debit.getUserMobileNumber() + " -> "
                    + debit.getAdminUsername();
        }
        return "-";
    }

    private <T> void runTask(
            String progress,
            AdminTask<T> task,
            Consumer<T> success
    ) {
        setBusy(true);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.execute();
            }

            @Override
            protected void done() {
                try {
                    success.accept(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showFailure(exception);
                } catch (ExecutionException exception) {
                    showFailure(exception.getCause());
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void showFailure(Throwable exception) {
        String message = exception instanceof IllegalArgumentException
                && exception.getMessage() != null
                ? exception.getMessage()
                : "The database operation could not be completed.";
        showError("Unable to complete request", message);
    }

    private void setBusy(boolean busy) {
        for (JButton button : controls) {
            button.setEnabled(!busy);
        }
    }

    private void updateResponsiveLayout() {
        if (adminSidebar == null) {
            return;
        }
        boolean compact = getWidth() < 1050;
        adminSidebar.setPreferredSize(new Dimension(compact ? 82 : 230, 0));
        adminSidebar.setBorder(new EmptyBorder(30, compact ? 14 : 22, 28,
                compact ? 14 : 22));
        adminSidebarBrand.setText(compact ? "A" : "JCASH ADMIN");
        for (JButton button : controls) {
            Object fullText = button.getClientProperty("jcash.fullText");
            if (fullText instanceof String text) {
                button.setText(compact ? "" : text);
                button.setToolTipText(compact ? text : null);
                button.setHorizontalAlignment(compact
                        ? SwingConstants.CENTER : SwingConstants.LEFT);
            }
        }
        adminSidebar.revalidate();
    }

    private void showError(String title, String message) {
        UiDialogs.message(owner, title, message, true);
    }

    private static JPanel verticalForm() {
        JPanel panel = new JPanel();
        panel.setBackground(UiTheme.SURFACE);
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
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
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(6));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension preferred = field.getPreferredSize();
        field.setPreferredSize(new Dimension(preferred.width, 44));
        field.setMinimumSize(new Dimension(0, 44));
        field.setMaximumSize(new Dimension(360, 44));
        panel.add(field);
    }

    private static BigDecimal parseAmount(String input) {
        if (input == null || !input.trim().matches("\\d+(\\.\\d{1,2})?")) {
            return null;
        }
        BigDecimal amount = new BigDecimal(input.trim());
        return amount.compareTo(BigDecimal.ZERO) > 0 ? amount : null;
    }

    @FunctionalInterface
    private interface AdminTask<T> {

        T execute() throws Exception;
    }
}
