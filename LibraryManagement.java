// Your existing imports
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class LibraryManagement {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(LibraryFrame::new);
    }
}

class LibraryFrame extends JFrame implements ActionListener {
    CardLayout cardLayout = new CardLayout();
    JPanel cardPanel;
    JButton mainButton, libraryButton, aboutButton;
    Connection connection;
    JTextField nameField, regField, bookField, authorField;
    JTextField newBookField, newAuthorField;

    public LibraryFrame() {
        setTitle("Library Management System");
        setSize(950, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setIconImage(new ImageIcon("uniuyo_logo.png").getImage());
        initDB();

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        navPanel.setBackground(new Color(225, 225, 225));
        mainButton = new JButton("Borrow Book");
        libraryButton = new JButton("Library Page");
        aboutButton = new JButton("About");
        for (JButton btn : new JButton[]{mainButton, libraryButton, aboutButton}) {
            btn.setForeground(Color.WHITE);
            btn.setBackground(new Color(90, 100, 120));
            btn.setFocusPainted(false);
            btn.setFont(new Font("Arial", Font.BOLD, 14));
            navPanel.add(btn);
            btn.addActionListener(this);
        }

        cardPanel = new JPanel(cardLayout);
        cardPanel.add(mainPage(), "Main");
        cardPanel.add(libraryPage(), "Library");
        cardPanel.add(aboutPage(), "About");

        add(navPanel, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
        setVisible(true);
    }

    private void initDB() {
    try {
        Class.forName("org.postgresql.Driver");
        String url = "jdbc:postgresql://localhost:5432/library_db";
        String user = "postgres";
        String password = "Arua08141";
        connection = DriverManager.getConnection(url, user, password);
        connection.setAutoCommit(false);  // Enable transaction management

        Statement stmt = connection.createStatement();

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS books (" +
                "id SERIAL PRIMARY KEY," +  // Fixed typo here
                "title VARCHAR(100)," +
                "author VARCHAR(100)," +
                "available BOOLEAN DEFAULT TRUE)");

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS borrowers (" +
                "id SERIAL PRIMARY KEY," +
                "name VARCHAR(100)," +
                "regno VARCHAR(100)," +
                "book_title VARCHAR(100)," +
                "book_author VARCHAR(100))");

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS transactions (" +
                "id SERIAL PRIMARY KEY," +
                "borrower_name VARCHAR(100)," +
                "borrower_regno VARCHAR(100)," +
                "book_title VARCHAR(100)," +
                "book_author VARCHAR(100)," +
                "date_borrowed DATE," +
                "return_date DATE)");

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS deleted_books (" +
                "id SERIAL PRIMARY KEY," +
                "title VARCHAR(100)," +
                "author VARCHAR(100))");

        connection.commit();  // Ensure changes are persisted
    } catch (Exception e) {
        showError("Database setup failed: " + e.getMessage());
        try {
            if (connection != null) connection.rollback();
        } catch (SQLException ex) {
            showError("Rollback failed: " + ex.getMessage());
        }
        System.exit(1);
    }
}

    private JPanel mainPage() {
        JPanel panel = new GradientPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 350, 30, 350));
        Dimension fieldSize = new Dimension(180, 28);

        JLabel nameLabel = new JLabel("Borrower's Name:");
        nameField = new JTextField(); nameField.setMaximumSize(fieldSize); nameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel regLabel = new JLabel("Registration Number:");
        regField = new JTextField(); regField.setMaximumSize(fieldSize); regField.setAlignmentX(Component.CENTER_ALIGNMENT);
        regLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel bookLabel = new JLabel("Book Title:");
        bookField = new JTextField(); bookField.setMaximumSize(fieldSize); bookField.setAlignmentX(Component.CENTER_ALIGNMENT);
        bookLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel authorLabel = new JLabel("Author:");
        authorField = new JTextField(); authorField.setMaximumSize(fieldSize); authorField.setAlignmentX(Component.CENTER_ALIGNMENT);
        authorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(Box.createVerticalStrut(10));
        panel.add(nameLabel); panel.add(Box.createVerticalStrut(5)); panel.add(nameField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(regLabel); panel.add(Box.createVerticalStrut(5)); panel.add(regField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(bookLabel); panel.add(Box.createVerticalStrut(5)); panel.add(bookField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(authorLabel); panel.add(Box.createVerticalStrut(5)); panel.add(authorField);
        panel.add(Box.createVerticalStrut(15));

        JButton submitButton = new JButton("Submit Request");
        submitButton.setBackground(new Color(60, 130, 90));
        submitButton.setForeground(Color.WHITE);
        submitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        submitButton.setMaximumSize(new Dimension(180, 32));
        submitButton.addActionListener(e -> handleBorrowRequest());
        panel.add(submitButton);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private void handleBorrowRequest() {
        String name = nameField.getText().trim();
        String reg = regField.getText().trim();
        String title = bookField.getText().trim();
        String author = authorField.getText().trim();
        

        if (name.isEmpty() || reg.isEmpty() || title.isEmpty() || author.isEmpty()) {
            showError("Please fill all the fields.");
            return;
        }

        try {
            PreparedStatement findBook = connection.prepareStatement("SELECT id, available FROM books WHERE title = ? AND author = ?");
            findBook.setString(1, title);
            findBook.setString(2, author);
            ResultSet rsBook = findBook.executeQuery();

            if (!rsBook.next()) {
                showError("Book not found in the library.");
                return;
            }

            int bookId = rsBook.getInt("id");
            boolean available = rsBook.getBoolean("available");

            if (!available) {
                showError("Book is currently unavailable.");
                return;
            }

            PreparedStatement insertBorrower = connection.prepareStatement("INSERT INTO borrowers (name, regno, book_title, book_author) VALUES (?, ?, ?, ?) RETURNING id");
            insertBorrower.setString(1, name);
            insertBorrower.setString(2, reg);
            insertBorrower.setString(3, title);
            insertBorrower.setString(4, author);
            ResultSet rsBorrower = insertBorrower.executeQuery();
            rsBorrower.next();
            int borrowerId = rsBorrower.getInt("id");
            connection.commit();  // Commit the borrower insertion

            LocalDate today = LocalDate.now();
            LocalDate returnDate = today.plusDays(7);
            LocalTime now = LocalTime.now();


            PreparedStatement insertTransaction = connection.prepareStatement("INSERT INTO transactions (borrower_name, borrower_regno, book_title, book_author, date_borrowed, return_date) VALUES (?, ?, ?, ?, ?, ?)");
            insertTransaction.setString(1, name);
            insertTransaction.setString(2, reg);
            insertTransaction.setString(3, title);
            insertTransaction.setString(4, author);
            insertTransaction.setDate(5, Date.valueOf(today));
            insertTransaction.setDate(6, Date.valueOf(returnDate));
            insertTransaction.executeUpdate();
            connection.commit();  // Commit the transaction

            PreparedStatement updateBook = connection.prepareStatement("UPDATE books SET available = FALSE WHERE id = ?");
            updateBook.setInt(1, bookId);
            updateBook.executeUpdate();
            connection.commit();  // Commit the book update

            DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
            String message = "Borrower: " + name +
                    "\nReg No: " + reg +
                    "\nBook: " + title +
                    "\nAuthor: " + author +
                    "\nDate Borrowed: " + today +
                    "\nTime Borrowed: " + now.format(timeFormat) +
                    "\nReturn by: " + returnDate;
            JOptionPane.showMessageDialog(this, message, "Transaction Details", JOptionPane.INFORMATION_MESSAGE);

            nameField.setText(""); regField.setText(""); bookField.setText(""); authorField.setText("");
        } catch (SQLException e) {
            showError("Borrowing failed: " + e.getMessage());
        }
    }

    private JPanel libraryPage() {
        JPanel panel = new GradientPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 100, 30, 100));

        JPanel addBookPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        newBookField = new JTextField();
        newAuthorField = new JTextField();
        JButton addBookButton = new JButton("Add Book");
        addBookButton.setBackground(new Color(60, 130, 90));
        addBookButton.setForeground(Color.WHITE);
        addBookButton.addActionListener(e -> {
            String title = newBookField.getText().trim();
            String author = newAuthorField.getText().trim();
            if (title.isEmpty() || author.isEmpty()) {
                showError("Please enter both book title and author.");
                return;
            }
            try {
                PreparedStatement insertBook = connection.prepareStatement("INSERT INTO books (title, author, available) VALUES (?, ?, TRUE)");
                insertBook.setString(1, title);
                insertBook.setString(2, author);
                insertBook.executeUpdate();
                newBookField.setText("");
                newAuthorField.setText("");
                refreshLibraryTable();
            } catch (SQLException ex) {
                showError("Failed to add book: " + ex.getMessage());
            }
        });

        addBookPanel.add(new JLabel("Book Title:"));
        addBookPanel.add(newBookField);
        addBookPanel.add(new JLabel("Author:"));
        addBookPanel.add(newAuthorField);

        JPanel addBookContainer = new JPanel(new BorderLayout());
        addBookContainer.add(addBookPanel, BorderLayout.CENTER);
        addBookContainer.add(addBookButton, BorderLayout.EAST);

        String[] columnNames = {"Title", "Author"};
        JTable booksTable = new JTable(getBooksTableData(), columnNames);
        booksTable.setEnabled(false);
        JScrollPane tableScrollPane = new JScrollPane(booksTable);
        panel.putClientProperty("booksTable", booksTable);
        panel.putClientProperty("columnNames", columnNames);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JLabel("Library Books:"), BorderLayout.NORTH);
        topPanel.add(tableScrollPane, BorderLayout.CENTER);

        // Delete section
        JPanel deleteBookPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        JTextField deleteTitleField = new JTextField();
        JTextField deleteAuthorField = new JTextField();
        JButton deleteButton = new JButton("Delete Book");
        deleteButton.setBackground(new Color(180, 50, 50));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.addActionListener(e -> {
            String title = deleteTitleField.getText().trim();
            String author = deleteAuthorField.getText().trim();
            try {
                PreparedStatement checkBook = connection.prepareStatement("SELECT id, available FROM books WHERE title = ? AND author = ?");
                checkBook.setString(1, title);
                checkBook.setString(2, author);
                ResultSet rs = checkBook.executeQuery();
                if (!rs.next()) {
                    showError("Book not found.");
                    return;
                }
                int bookId = rs.getInt("id");
                boolean available = rs.getBoolean("available");
                if (available) {
                    showError("Book is still available. Only unavailable books can be deleted.");
                    return;
                }

                PreparedStatement deleteTransactions = connection.prepareStatement("DELETE FROM transactions WHERE book_id = ?");
                deleteTransactions.setInt(1, bookId);
                deleteTransactions.executeUpdate();
                connection.commit();

                PreparedStatement insertDeleted = connection.prepareStatement("INSERT INTO deleted_books (title, author) VALUES (?, ?)");
                insertDeleted.setString(1, title);
                insertDeleted.setString(2, author);
                insertDeleted.executeUpdate();
                connection.commit();

                PreparedStatement deleteBook = connection.prepareStatement("DELETE FROM books WHERE id = ?");
                deleteBook.setInt(1, bookId);
                deleteBook.executeUpdate();
                connection.commit();

                deleteTitleField.setText("");
                deleteAuthorField.setText("");
                refreshLibraryTable();
                JOptionPane.showMessageDialog(this, "Book deleted and archived successfully.");
                connection.commit();
            } catch (SQLException ex) {
                showError("Deletion failed: " + ex.getMessage());
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    showError("Rollback failed: " + rollbackEx.getMessage());
                }
            }
        });

        deleteBookPanel.add(new JLabel("Book Title:"));
        deleteBookPanel.add(deleteTitleField);
        deleteBookPanel.add(new JLabel("Author:"));
        deleteBookPanel.add(deleteAuthorField);

        JPanel deleteContainer = new JPanel(new BorderLayout());
        deleteContainer.add(deleteBookPanel, BorderLayout.CENTER);
        deleteContainer.add(deleteButton, BorderLayout.EAST);
        deleteContainer.setBorder(BorderFactory.createTitledBorder("Delete Unavailable Book"));

        panel.add(addBookContainer, BorderLayout.NORTH);
        panel.add(topPanel, BorderLayout.CENTER);
        panel.add(deleteContainer, BorderLayout.SOUTH);

        return panel;
    }

    private Object[][] getBooksTableData() {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT title, author FROM books ORDER BY title");
            List<Object[]> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new Object[]{rs.getString("title"), rs.getString("author")});
            }
            if (rows.isEmpty()) return new Object[][]{{"No books found", ""}};
            Object[][] data = new Object[rows.size()][2];
            for (int i = 0; i < rows.size(); i++) data[i] = rows.get(i);
            return data;
        } catch (SQLException e) {
            showError("Failed to load books: " + e.getMessage());
            return new Object[][]{{"Failed to load books", ""}};
        }
    }

    private void refreshLibraryTable() {
        for (Component comp : cardPanel.getComponents()) {
            if (comp instanceof JPanel panel) {
                JTable booksTable = (JTable) panel.getClientProperty("booksTable");
                String[] columnNames = (String[]) panel.getClientProperty("columnNames");
                if (booksTable != null && columnNames != null) {
                    booksTable.setModel(new DefaultTableModel(getBooksTableData(), columnNames));
                }
            }
        }
    }

    private JPanel aboutPage() {
        JPanel about = new GradientPanel();
        about.setLayout(new BoxLayout(about, BoxLayout.Y_AXIS));
        about.setBorder(BorderFactory.createEmptyBorder(40, 100, 40, 100));
        JLabel picLabel = new JLabel();
        ImageIcon picIcon = new ImageIcon("arua_image.jpg");
        Image img = picIcon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
        picLabel.setIcon(new ImageIcon(img));
        picLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        about.add(Box.createVerticalGlue());
        about.add(picLabel);
        about.add(Box.createVerticalStrut(20));
        JTextArea text = new JTextArea("Arua Sunday\nRegNo: 22/SC/CO/1164\nDepartment: Computer Science\nLevel: 200L\nUniversity: University of Uyo\nYear: 2025\nEmail: aruasamuel08141@gmail.com");
        text.setWrapStyleWord(true);
        text.setLineWrap(true);
        text.setEditable(false);
        text.setFont(new Font("Serif", Font.PLAIN, 16));
        text.setOpaque(false);
        text.setAlignmentX(Component.CENTER_ALIGNMENT);
        text.setMaximumSize(new Dimension(400, 150));
        about.add(text);
        about.add(Box.createVerticalGlue());
        return about;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == mainButton) cardLayout.show(cardPanel, "Main");
        else if (e.getSource() == libraryButton) {
            cardPanel.remove(1);
            cardPanel.add(libraryPage(), "Library");
            cardLayout.show(cardPanel, "Library");
        } else if (e.getSource() == aboutButton) {
            cardPanel.remove(2);
            cardPanel.add(aboutPage(), "About");
            cardLayout.show(cardPanel, "About");
        }
    }

    static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            int width = getWidth();
            int height = getHeight();
            Color color1 = Color.decode("#C9F0F0");
            Color color2 = new Color(160, 160, 160);
            GradientPaint gp = new GradientPaint(0, 0, color1, width, height, color2);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, width, height);
        }
    }
}
