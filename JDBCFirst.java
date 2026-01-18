import java.sql.*;
import java.util.Scanner;

public class JDBCFirst {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/simple_logistics_plus";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "EGOKaushik833";
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Connected to logistics database!");
            
            while (true) {
                printMenu();
                int choice = getIntInput("Enter your choice: ");
                
                switch (choice) {
                    case 1:
                        placeOrder(conn);
                        break;
                    case 2:
                        trackShipment(conn);
                        break;
                    case 3:
                        viewInventory(conn);
                        break;
                    case 4:
                        updateShipmentStatus(conn);
                        break;
                    case 5:
                        processReturn(conn);
                        break;
                    case 6:
                        System.out.println("Exiting...");
                        return;
                    default:
                        System.out.println("Invalid choice!");
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private static void printMenu() {
        System.out.println("\n=== Logistics System ===");
        System.out.println("1. Place New Order");
        System.out.println("2. Track Shipment");
        System.out.println("3. View Inventory");
        System.out.println("4. Update Shipment Status");
        System.out.println("5. Process Return");
        System.out.println("6. Exit");
    }

    private static int getIntInput(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number!");
            }
        }
    }

    private static void placeOrder(Connection conn) throws SQLException {
        System.out.println("\n--- Place New Order ---");
        int customerId = getIntInput("Customer ID: ");
        int productId = getIntInput("Product ID: ");
        int quantity = getIntInput("Quantity: ");
        int warehouseId = getIntInput("Warehouse ID: ");

        String sql = "INSERT INTO Orders (customer_id, total_price, tracking_id) " +
                     "VALUES (?, (SELECT unit_price * ? FROM Products WHERE product_id = ?), UUID())";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, customerId);
            pstmt.setInt(2, quantity);
            pstmt.setInt(3, productId);
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int orderId = rs.getInt(1);
                        createShipment(conn, orderId, warehouseId);
                        System.out.println("Order placed successfully! Order ID: " + orderId);
                    }
                }
            }
        }
    }

    private static void createShipment(Connection conn, int orderId, int warehouseId) throws SQLException {
        String sql = "INSERT INTO Shipments (order_id, warehouse_id, status) VALUES (?, ?, 'Preparing')";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            pstmt.setInt(2, warehouseId);
            pstmt.executeUpdate();
        }
    }

    private static void trackShipment(Connection conn) throws SQLException {
        System.out.print("\nEnter Tracking ID: ");
        String trackingId = scanner.nextLine();
        
        String sql = "SELECT s.*, o.status AS order_status " +
                     "FROM Shipments s " +
                     "JOIN Orders o USING(order_id) " +
                     "WHERE o.tracking_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, trackingId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("\nShipment Details:");
                    System.out.println("Status: " + rs.getString("status"));
                    System.out.println("Carrier: " + rs.getString("carrier"));
                    System.out.println("Estimated Delivery: " + rs.getDate("estimated_delivery"));
                    System.out.println("Order Status: " + rs.getString("order_status"));
                } else {
                    System.out.println("No shipment found with that tracking ID!");
                }
            }
        }
    }

    private static void viewInventory(Connection conn) throws SQLException {
        String sql = "SELECT p.name, w.location, i.quantity " +
                     "FROM Inventory i " +
                     "JOIN Products p USING(product_id) " +
                     "JOIN Warehouses w USING(warehouse_id)";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            System.out.println("\n--- Inventory Overview ---");
            while (rs.next()) {
                System.out.printf("%-20s | %-15s | Qty: %d%n",
                        rs.getString("name"),
                        rs.getString("location"),
                        rs.getInt("quantity"));
            }
        }
    }

    private static void updateShipmentStatus(Connection conn) throws SQLException {
        int orderId = getIntInput("\nEnter Order ID: ");
        System.out.print("New Status (Preparing/In Transit/Delivered): ");
        String newStatus = scanner.nextLine();
        
        String sql = "UPDATE Shipments SET status = ? WHERE order_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, orderId);
            int rowsUpdated = pstmt.executeUpdate();
            System.out.println(rowsUpdated + " shipment(s) updated");
        }
    }

    private static void processReturn(Connection conn) throws SQLException {
        int orderId = getIntInput("\nEnter Order ID: ");
        int productId = getIntInput("Product ID: ");
        int quantity = getIntInput("Return Quantity: ");
        System.out.print("Reason: ");
        String reason = scanner.nextLine();
        
        String sql = "INSERT INTO Returns (order_id, product_id, quantity, reason, status) " +
                     "VALUES (?, ?, ?, ?, 'Pending')";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            pstmt.setInt(2, productId);
            pstmt.setInt(3, quantity);
            pstmt.setString(4, reason);
            int rowsInserted = pstmt.executeUpdate();
            System.out.println(rowsInserted + " return request created");
        }
    }
}
