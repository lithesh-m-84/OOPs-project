import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class GroceryManagementSystem extends JFrame {
    // JDBC Connection variables
    private Connection connection;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    // Login Panel Components
    private JTextField usernameField;
    private JPasswordField passwordField;

    // Registration Panel Components
    private JTextField regUsernameField;
    private JPasswordField regPasswordField;

    // Product Panel Components (Vendor side)
    private JTextField nameField, mrpField, discountField, stockField, expiryField;
    private JTable productTable;

    // Customer Panel Components
    private JTable customerTable;
    private JTextField productIdField, quantityField;

    // Restock Panel Components (Vendor)
    private JTextField restockProductIdField, restockQuantityField;

    public static void main(String[] args) {
        // Setup the frame
        GroceryManagementSystem frame = new GroceryManagementSystem();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setVisible(true);
    }

    public GroceryManagementSystem() {
        try {
            // Initialize JDBC connection
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/grocerydb", "root", "#mysql123");

            // Setup CardLayout and main panel
            cardLayout = new CardLayout();
            mainPanel = new JPanel(cardLayout);
            add(mainPanel);

            // Add Login Panel
            mainPanel.add(createLoginPanel(), "Login");

            // Add Registration Panel
            mainPanel.add(createRegistrationPanel(), "Registration");

            // Add Product Display Panel (Vendor)
            mainPanel.add(createProductDisplayPanel(), "ProductDisplay");

            // Add Customer Panel
            mainPanel.add(createCustomerPanel(), "Customer");

            // Add Add Product Panel (Vendor)
            mainPanel.add(createAddProductPanel(), "AddProduct");

            // Add Restock Panel (Vendor)
            mainPanel.add(createRestockProductPanel(), "RestockProduct");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database connection failed", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Login Panel
    private JPanel createLoginPanel() {
        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new GridLayout(3, 2));

        // Username and Password fields
        loginPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        loginPanel.add(usernameField);

        loginPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        loginPanel.add(passwordField);

        // Login Button
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                if (validateLogin(username, password)) {
                    cardLayout.show(mainPanel, "ProductDisplay");
                } else {
                    JOptionPane.showMessageDialog(loginPanel, "Invalid credentials", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        loginPanel.add(loginButton);

        // Register Button
        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "Registration");
            }
        });
        loginPanel.add(registerButton);

        return loginPanel;
    }

    // Registration Panel
    private JPanel createRegistrationPanel() {
        JPanel registrationPanel = new JPanel();
        registrationPanel.setLayout(new GridLayout(3, 2));

        // Registration Fields
        registrationPanel.add(new JLabel("New Username:"));
        regUsernameField = new JTextField();
        registrationPanel.add(regUsernameField);

        registrationPanel.add(new JLabel("New Password:"));
        regPasswordField = new JPasswordField();
        registrationPanel.add(regPasswordField);

        // Register Button
        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String username = regUsernameField.getText();
                String password = new String(regPasswordField.getPassword());

                // Register new user in the database
                if (registerUser(username, password)) {
                    JOptionPane.showMessageDialog(registrationPanel, "Registration successful! Please log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    cardLayout.show(mainPanel, "Login");
                } else {
                    JOptionPane.showMessageDialog(registrationPanel, "Username already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        registrationPanel.add(registerButton);

        return registrationPanel;
    }

    // Validate login credentials
    private boolean validateLogin(String username, String password) {
        try {
            String query = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();
            return rs.next();  // Returns true if user exists with given credentials
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Register new user in the database
    private boolean registerUser(String username, String password) {
        try {
            // Check if username already exists
            String checkQuery = "SELECT * FROM users WHERE username = ?";
            PreparedStatement checkPs = connection.prepareStatement(checkQuery);
            checkPs.setString(1, username);
            ResultSet rs = checkPs.executeQuery();

            if (rs.next()) {
                // Username already exists
                return false;
            }

            // Insert new user into the database
            String insertQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
            PreparedStatement ps = connection.prepareStatement(insertQuery);
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();

            return true;  // Registration successful
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Product Display Panel (Vendor View)
    private JPanel createProductDisplayPanel() {
        JPanel productDisplayPanel = new JPanel();
        productDisplayPanel.setLayout(new BorderLayout());

        // Product Table for Vendor
        productTable = new JTable(new DefaultTableModel(new Object[]{"ID", "Product Name", "MRP", "Discount", "Stock", "Expiry Date"}, 0));
        JScrollPane scrollPane = new JScrollPane(productTable);
        productDisplayPanel.add(scrollPane, BorderLayout.CENTER);

        // Add New Product Button
        JButton addProductButton = new JButton("Add New Product");
        addProductButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "AddProduct");
            }
        });
        productDisplayPanel.add(addProductButton, BorderLayout.SOUTH);

        // Restock Product Button
        JButton restockButton = new JButton("Restock Product");
        restockButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "RestockProduct");
            }
        });
        productDisplayPanel.add(restockButton, BorderLayout.NORTH);

        // Refresh table on panel load
        refreshProductTable(productTable);

        // Button to switch to customer view
        JButton customerButton = new JButton("Customer View");
        customerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "Customer");
            }
        });
        productDisplayPanel.add(customerButton, BorderLayout.NORTH);

        return productDisplayPanel;
    }

    // Customer Panel (View Products and Place Orders)
    private JPanel createCustomerPanel() {
        JPanel customerPanel = new JPanel(new BorderLayout());

        // Product Table for Customer
        customerTable = new JTable(new DefaultTableModel(
                new Object[]{"ID", "Product Name", "MRP", "Discount", "Stock"}, 0));
        refreshProductTable(customerTable);  // Refresh table with product data
        JScrollPane scrollPane = new JScrollPane(customerTable);
        customerPanel.add(scrollPane, BorderLayout.CENTER);

        // Cart Panel for Customer to enter Product ID and Quantity
        JPanel cartPanel = new JPanel(new GridLayout(2, 2));
        productIdField = new JTextField();
        quantityField = new JTextField();

        cartPanel.add(new JLabel("Product ID:"));
        cartPanel.add(productIdField);
        cartPanel.add(new JLabel("Quantity:"));
        cartPanel.add(quantityField);

        customerPanel.add(cartPanel, BorderLayout.NORTH);

        // Place Order Button
        JButton placeOrderButton = new JButton("Place Order");
        placeOrderButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    int productId = Integer.parseInt(productIdField.getText());
                    int quantity = Integer.parseInt(quantityField.getText());

                    if (placeOrder(productId, quantity)) {
                        JOptionPane.showMessageDialog(customerPanel, "Order placed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        refreshProductTable(customerTable);  // Refresh the customer table
                    } else {
                        JOptionPane.showMessageDialog(customerPanel, "Insufficient stock or invalid product", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(customerPanel, "Please enter valid product ID and quantity", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        customerPanel.add(placeOrderButton, BorderLayout.SOUTH);
        return customerPanel;
    }

    // Add Product Panel (Vendor Add Product)
    private JPanel createAddProductPanel() {
        JPanel addProductPanel = new JPanel();
        addProductPanel.setLayout(new GridLayout(6, 2));

        // Add Product Fields
        addProductPanel.add(new JLabel("Product Name:"));
        nameField = new JTextField();
        addProductPanel.add(nameField);

        addProductPanel.add(new JLabel("MRP:"));
        mrpField = new JTextField();
        addProductPanel.add(mrpField);

        addProductPanel.add(new JLabel("Discount:"));
        discountField = new JTextField();
        addProductPanel.add(discountField);

        addProductPanel.add(new JLabel("Stock:"));
        stockField = new JTextField();
        addProductPanel.add(stockField);

        addProductPanel.add(new JLabel("Expiry Date (yyyy-mm-dd):"));
        expiryField = new JTextField();
        addProductPanel.add(expiryField);

        // Add Product Button
        JButton addProductButton = new JButton("Add Product");
        addProductButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    String name = nameField.getText();
                    double mrp = Double.parseDouble(mrpField.getText());
                    double discount = Double.parseDouble(discountField.getText());
                    int stock = Integer.parseInt(stockField.getText());
                    String expiryDate = expiryField.getText();

                    addProduct(name, mrp, discount, stock, expiryDate);
                    JOptionPane.showMessageDialog(addProductPanel, "Product added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    cardLayout.show(mainPanel, "ProductDisplay");
                    refreshProductTable(productTable); // Refresh product list
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(addProductPanel, "Please enter valid data", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        addProductPanel.add(addProductButton);

        return addProductPanel;
    }

    // Restock Product Panel (Vendor Restock Products)
    private JPanel createRestockProductPanel() {
        JPanel restockProductPanel = new JPanel();
        restockProductPanel.setLayout(new GridLayout(2, 2));

        restockProductPanel.add(new JLabel("Product ID:"));
        restockProductIdField = new JTextField();
        restockProductPanel.add(restockProductIdField);

        restockProductPanel.add(new JLabel("Quantity to Restock:"));
        restockQuantityField = new JTextField();
        restockProductPanel.add(restockQuantityField);

        // Restock Button
        JButton restockButton = new JButton("Restock");
        restockButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    int productId = Integer.parseInt(restockProductIdField.getText());
                    int quantity = Integer.parseInt(restockQuantityField.getText());

                    if (restockProduct(productId, quantity)) {
                        JOptionPane.showMessageDialog(restockProductPanel, "Product restocked successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        cardLayout.show(mainPanel, "ProductDisplay");
                        refreshProductTable(productTable); // Refresh product list
                    } else {
                        JOptionPane.showMessageDialog(restockProductPanel, "Invalid product or restock quantity", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(restockProductPanel, "Please enter valid product ID and quantity", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        restockProductPanel.add(restockButton);

        return restockProductPanel;
    }

    // Helper method to refresh product table
    private void refreshProductTable(JTable table) {
        try {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);  // Clear existing data

            String query = "SELECT * FROM products";
            PreparedStatement ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("mrp"),
                        rs.getDouble("discount"),
                        rs.getInt("stock"),
                        rs.getDate("expiry_date")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Place an order
    private boolean placeOrder(int productId, int quantity) {
        try {
            String query = "SELECT stock FROM products WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int stock = rs.getInt("stock");

                if (stock >= quantity) {
                    // Update stock
                    String updateQuery = "UPDATE products SET stock = stock - ? WHERE id = ?";
                    PreparedStatement updatePs = connection.prepareStatement(updateQuery);
                    updatePs.setInt(1, quantity);
                    updatePs.setInt(2, productId);
                    updatePs.executeUpdate();

                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Add new product to the database
    private void addProduct(String name, double mrp, double discount, int stock, String expiryDate) {
        try {
            String query = "INSERT INTO products (name, mrp, discount, stock, expiry_date) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, name);
            ps.setDouble(2, mrp);
            ps.setDouble(3, discount);
            ps.setInt(4, stock);
            ps.setString(5, expiryDate);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Restock product in the database
    private boolean restockProduct(int productId, int quantity) {
        try {
            String query = "UPDATE products SET stock = stock + ? WHERE id = ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, quantity);
            ps.setInt(2, productId);
            int rowsAffected = ps.executeUpdate();

            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
