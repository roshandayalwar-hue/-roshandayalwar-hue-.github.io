import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * Bank Account Management System
 * Demonstrates: Inheritance, Method Overriding, Abstract Class, Interface,
 *               Collections, Exception Handling and Swing.
 *
 * Compile : javac BankManagementSystem.java
 * Run     : java BankManagementSystem
 */

// ===================== CUSTOM EXCEPTIONS (Exception Handling) =====================
class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) { super(message); }
}

class InvalidAmountException extends Exception {
    public InvalidAmountException(String message) { super(message); }
}

class AccountNotFoundException extends Exception {
    public AccountNotFoundException(String message) { super(message); }
}

// ===================== INTERFACE =====================
interface Transactable {
    void deposit(double amount) throws InvalidAmountException;
    void withdraw(double amount) throws InvalidAmountException, InsufficientFundsException;
}

// ===================== ABSTRACT CLASS =====================
abstract class Account implements Transactable {
    private final String accountNumber;
    private final String holderName;
    protected double balance;
    private final List<String> history = new ArrayList<>();   // Collection

    public Account(String accountNumber, String holderName, double initialBalance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.balance = initialBalance;
        log("Opening", initialBalance);
    }

    public String getAccountNumber() { return accountNumber; }
    public String getHolderName()    { return holderName; }
    public double getBalance()       { return balance; }
    public List<String> getHistory() { return history; }

    // Concrete method shared by all accounts
    @Override
    public void deposit(double amount) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be greater than zero.");
        }
        balance += amount;
        log("Deposit", amount);
    }

    // Abstract methods - every subclass MUST override these
    @Override
    public abstract void withdraw(double amount)
            throws InvalidAmountException, InsufficientFundsException;

    public abstract double calculateInterest();

    public abstract String getAccountType();

    public void applyInterest() {
        double interest = calculateInterest();
        if (interest > 0) {
            balance += interest;
            log("Interest", interest);
        }
    }

    protected void log(String type, double amount) {
        history.add(String.format("%-10s %12.2f   | Balance: %.2f", type, amount, balance));
    }

    @Override
    public String toString() {
        return getAccountType() + " [" + accountNumber + ", " + holderName
                + ", Balance: " + String.format("%.2f", balance) + "]";
    }
}

// ===================== INHERITANCE + METHOD OVERRIDING =====================
class SavingsAccount extends Account {
    private static final double MIN_BALANCE = 500.0;
    private static final double INTEREST_RATE = 0.04;   // 4%

    public SavingsAccount(String accNo, String holder, double initial) {
        super(accNo, holder, initial);
    }

    @Override
    public void withdraw(double amount) throws InvalidAmountException, InsufficientFundsException {
        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be greater than zero.");
        }
        if (balance - amount < MIN_BALANCE) {
            throw new InsufficientFundsException(
                    "Savings account must keep a minimum balance of " + MIN_BALANCE
                            + ".\nAvailable to withdraw: " + String.format("%.2f", Math.max(0, balance - MIN_BALANCE)));
        }
        balance -= amount;
        log("Withdraw", amount);
    }

    @Override
    public double calculateInterest() { return balance * INTEREST_RATE; }

    @Override
    public String getAccountType() { return "Savings"; }

    @Override
    public String toString() { return super.toString() + " (Min Balance: " + MIN_BALANCE + ")"; }
}

class CurrentAccount extends Account {
    private static final double OVERDRAFT_LIMIT = 5000.0;

    public CurrentAccount(String accNo, String holder, double initial) {
        super(accNo, holder, initial);
    }

    @Override
    public void withdraw(double amount) throws InvalidAmountException, InsufficientFundsException {
        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be greater than zero.");
        }
        if (balance + OVERDRAFT_LIMIT < amount) {
            throw new InsufficientFundsException(
                    "Overdraft limit of " + OVERDRAFT_LIMIT + " exceeded.");
        }
        balance -= amount;
        log("Withdraw", amount);
    }

    @Override
    public double calculateInterest() { return 0; }   // Current accounts earn no interest

    @Override
    public String getAccountType() { return "Current"; }

    @Override
    public String toString() { return super.toString() + " (Overdraft: " + OVERDRAFT_LIMIT + ")"; }
}

// ===================== BANK (Collections) =====================
class Bank {
    private final Map<String, Account> accounts = new LinkedHashMap<>();   // Collection
    private int nextNumber = 1001;

    public Account createAccount(String type, String holder, double initial)
            throws InvalidAmountException {
        if (holder == null || holder.trim().isEmpty()) {
            throw new InvalidAmountException("Account holder name cannot be empty.");
        }
        if (initial < 0) {
            throw new InvalidAmountException("Initial deposit cannot be negative.");
        }
        String accNo = "ACC" + nextNumber++;
        Account acc;
        if (type.equals("Savings")) {
            if (initial < 500) {
                nextNumber--;
                throw new InvalidAmountException("Savings account needs at least 500 to open.");
            }
            acc = new SavingsAccount(accNo, holder.trim(), initial);
        } else {
            acc = new CurrentAccount(accNo, holder.trim(), initial);
        }
        accounts.put(accNo, acc);
        return acc;
    }

    public Account findAccount(String accNo) throws AccountNotFoundException {
        Account acc = accounts.get(accNo.trim().toUpperCase());
        if (acc == null) {
            throw new AccountNotFoundException("No account found with number: " + accNo);
        }
        return acc;
    }

    public void transfer(String fromNo, String toNo, double amount)
            throws AccountNotFoundException, InvalidAmountException, InsufficientFundsException {
        Account from = findAccount(fromNo);
        Account to = findAccount(toNo);
        if (from == to) {
            throw new InvalidAmountException("Cannot transfer to the same account.");
        }
        from.withdraw(amount);
        to.deposit(amount);
    }

    public java.util.Collection<Account> getAllAccounts() { return accounts.values(); }
}

// ===================== SWING GUI =====================
public class BankManagementSystem extends JFrame {
    private final Bank bank = new Bank();

    private final JTextField nameField = new JTextField(12);
    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{"Savings", "Current"});
    private final JTextField initialField = new JTextField(8);

    private final JTextField accNoField = new JTextField(8);
    private final JTextField amountField = new JTextField(8);
    private final JTextField toAccField = new JTextField(8);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Account No", "Holder", "Type", "Balance"}, 0) {
        @Override
        public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable table = new JTable(tableModel);

    public BankManagementSystem() {
        super("Bank Account Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        add(buildCreatePanel(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildTransactionPanel(), BorderLayout.SOUTH);

        // Clicking a row fills the account number field
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) accNoField.setText((String) tableModel.getValueAt(row, 0));
        });

        setSize(780, 480);
        setLocationRelativeTo(null);
    }

    private JPanel buildCreatePanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        p.setBorder(BorderFactory.createTitledBorder("Create New Account"));
        p.add(new JLabel("Holder Name:"));
        p.add(nameField);
        p.add(new JLabel("Type:"));
        p.add(typeBox);
        p.add(new JLabel("Initial Deposit:"));
        p.add(initialField);
        JButton create = new JButton("Create Account");
        create.addActionListener(e -> createAccount());
        p.add(create);
        return p;
    }

    private JPanel buildTransactionPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        p.setBorder(BorderFactory.createTitledBorder("Transactions"));
        p.add(new JLabel("Account No:"));
        p.add(accNoField);
        p.add(new JLabel("Amount:"));
        p.add(amountField);

        JButton deposit = new JButton("Deposit");
        JButton withdraw = new JButton("Withdraw");
        JButton interest = new JButton("Apply Interest");
        JButton history = new JButton("History");
        deposit.addActionListener(e -> deposit());
        withdraw.addActionListener(e -> withdraw());
        interest.addActionListener(e -> applyInterest());
        history.addActionListener(e -> showHistory());
        p.add(deposit);
        p.add(withdraw);
        p.add(interest);
        p.add(history);

        p.add(new JLabel("  Transfer To:"));
        p.add(toAccField);
        JButton transfer = new JButton("Transfer");
        transfer.addActionListener(e -> transfer());
        p.add(transfer);
        return p;
    }

    // ---------- Action handlers (all use try/catch - Exception Handling) ----------
    private void createAccount() {
        try {
            double initial = Double.parseDouble(initialField.getText().trim());
            Account acc = bank.createAccount((String) typeBox.getSelectedItem(),
                    nameField.getText(), initial);
            refreshTable();
            info("Account created successfully!\nAccount Number: " + acc.getAccountNumber());
            nameField.setText("");
            initialField.setText("");
        } catch (NumberFormatException ex) {
            error("Please enter a valid number for the initial deposit.");
        } catch (InvalidAmountException ex) {
            error(ex.getMessage());
        }
    }

    private void deposit() {
        try {
            Account acc = bank.findAccount(accNoField.getText());
            acc.deposit(parseAmount());
            refreshTable();
            info("Deposit successful.\nNew balance: " + String.format("%.2f", acc.getBalance()));
        } catch (NumberFormatException ex) {
            error("Please enter a valid amount.");
        } catch (AccountNotFoundException | InvalidAmountException ex) {
            error(ex.getMessage());
        }
    }

    private void withdraw() {
        try {
            Account acc = bank.findAccount(accNoField.getText());
            acc.withdraw(parseAmount());          // polymorphism: runtime type decides rules
            refreshTable();
            info("Withdrawal successful.\nNew balance: " + String.format("%.2f", acc.getBalance()));
        } catch (NumberFormatException ex) {
            error("Please enter a valid amount.");
        } catch (AccountNotFoundException | InvalidAmountException | InsufficientFundsException ex) {
            error(ex.getMessage());
        }
    }

    private void transfer() {
        try {
            bank.transfer(accNoField.getText(), toAccField.getText(), parseAmount());
            refreshTable();
            info("Transfer successful.");
        } catch (NumberFormatException ex) {
            error("Please enter a valid amount.");
        } catch (AccountNotFoundException | InvalidAmountException | InsufficientFundsException ex) {
            error(ex.getMessage());
        }
    }

    private void applyInterest() {
        try {
            Account acc = bank.findAccount(accNoField.getText());
            double before = acc.getBalance();
            acc.applyInterest();                  // calls the overridden calculateInterest()
            refreshTable();
            info(acc.getAccountType() + " account interest added: "
                    + String.format("%.2f", acc.getBalance() - before));
        } catch (AccountNotFoundException ex) {
            error(ex.getMessage());
        }
    }

    private void showHistory() {
        try {
            Account acc = bank.findAccount(accNoField.getText());
            StringBuilder sb = new StringBuilder(acc.toString()).append("\n\n");
            for (String line : acc.getHistory()) sb.append(line).append("\n");
            JTextArea area = new JTextArea(sb.toString(), 14, 45);
            area.setEditable(false);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            JOptionPane.showMessageDialog(this, new JScrollPane(area),
                    "Transaction History", JOptionPane.PLAIN_MESSAGE);
        } catch (AccountNotFoundException ex) {
            error(ex.getMessage());
        }
    }

    // ---------- Helpers ----------
    private double parseAmount() {
        return Double.parseDouble(amountField.getText().trim());
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Account a : bank.getAllAccounts()) {
            tableModel.addRow(new Object[]{a.getAccountNumber(), a.getHolderName(),
                    a.getAccountType(), String.format("%.2f", a.getBalance())});
        }
    }

    private void info(String msg)  { JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BankManagementSystem().setVisible(true));
    }
}
