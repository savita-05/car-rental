import java.sql.*;
import java.util.Scanner;

public class CarRentalConsoleApp {

    // ===== Global Scanner (No warning, reused everywhere) =====
    static Scanner sc = new Scanner(System.in);

    // ===== DB Utility Class ======
    static class DBUtil {
        private static final String URL = "jdbc:mysql://127.0.0.1:3306/carrental";
        private static final String USER = "root";
        private static final String PASS = "xxxxxx";

        static Connection getConnection() {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                return DriverManager.getConnection(URL, USER, PASS);
            } catch (ClassNotFoundException e) {
                System.out.println("MySQL JDBC Driver not found.");
            } catch (SQLException e) {
                System.out.println("DB Error: " + e.getMessage());
            }
            return null;
        }
    }

    // ===== User Class ======
    static class User {
        int id;
        String name, email, password, role;

        public User(int id, String name, String email, String password, String role) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.password = password;
            this.role = role;
        }
    }

    // ===== UserDAO ======
    static class UserDAO {
        static boolean register(String name, String email, String password) {
            try (Connection conn = DBUtil.getConnection()) {
                String sql = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, 'customer')";
                PreparedStatement ps = conn.prepareStatement(sql);

                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, password);

                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.out.println("Register Error: " + e.getMessage());
            }
            return false;
        }

        static User login(String email, String password) {
            try (Connection conn = DBUtil.getConnection()) {

                String sql = "SELECT * FROM users WHERE email=? AND password=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, email);
                ps.setString(2, password);

                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("email"),
                            rs.getString("password"),
                            rs.getString("role")
                    );
                }

            } catch (SQLException e) {
                System.out.println("Login Error: " + e.getMessage());
            }
            return null;
        }
    }

    // ===== Car DAO ======
    static class CarDAO {

        static void addCar(String name, String color, String plate, double price, String location) {
            try (Connection conn = DBUtil.getConnection()) {

                String sql = "INSERT INTO cars (name, color, plate, price_per_day, location) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(sql);

                ps.setString(1, name);
                ps.setString(2, color);
                ps.setString(3, plate);
                ps.setDouble(4, price);
                ps.setString(5, location);

                ps.executeUpdate();
                System.out.println("\nCar added successfully!");

            } catch (SQLException e) {
                System.out.println("Add Car Error: " + e.getMessage());
            }
        }

        static void viewCars() {
            try (Connection conn = DBUtil.getConnection()) {
                String sql = "SELECT * FROM cars";
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();

                System.out.println("\n---- CAR LIST ----");
                while (rs.next()) {
                    System.out.println(rs.getInt("id") + " | " +
                            rs.getString("name") + " | " +
                            rs.getString("color") + " | " +
                            rs.getString("plate") + " | Rs." +
                            rs.getDouble("price_per_day") + "/day | " +
                            rs.getString("location"));
                }

            } catch (SQLException e) {
                System.out.println("View Cars Error: " + e.getMessage());
            }
        }

        static void deleteCar(int id) {
            try (Connection conn = DBUtil.getConnection()) {
                String sql = "DELETE FROM cars WHERE id=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, id);

                ps.executeUpdate();
                System.out.println("Car deleted successfully!");

            } catch (SQLException e) {
                System.out.println("Delete Car Error: " + e.getMessage());
            }
        }
    }

    // ===== Booking DAO ======
    static class BookingDAO {

        static void bookCar(int carId, int customerId, String pickup, String from, String to) {
            try (Connection conn = DBUtil.getConnection()) {

                java.sql.Date fromDate = java.sql.Date.valueOf(from);
                java.sql.Date toDate = java.sql.Date.valueOf(to);

                long diff = (toDate.getTime() - fromDate.getTime()) / (1000 * 60 * 60 * 24);
                if (diff <= 0) {
                    System.out.println("Invalid date range!");
                    return;
                }

                PreparedStatement pricePs = conn.prepareStatement(
                        "SELECT price_per_day FROM cars WHERE id=?");
                pricePs.setInt(1, carId);
                ResultSet rs = pricePs.executeQuery();
                double price = 0;

                if (rs.next()) {
                    price = rs.getDouble("price_per_day");
                }

                double totalAmount = price * diff;

                String sql = "INSERT INTO bookings (car_id, customer_id, pickup_location, from_date, to_date, total_amount) "
                        + "VALUES (?, ?, ?, ?, ?, ?)";

                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, carId);
                ps.setInt(2, customerId);
                ps.setString(3, pickup);
                ps.setDate(4, fromDate);
                ps.setDate(5, toDate);
                ps.setDouble(6, totalAmount);

                ps.executeUpdate();

                System.out.println("\nBooking successful! Total Amount: Rs." + totalAmount);

            } catch (SQLException e) {
                System.out.println("Booking Error: " + e.getMessage());
            }
        }

        static void viewStatus(int customerId) {
            try (Connection conn = DBUtil.getConnection()) {
                String sql = "SELECT * FROM bookings WHERE customer_id=?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, customerId);

                ResultSet rs = ps.executeQuery();

                System.out.println("\n---- Booking Status ----");
                while (rs.next()) {
                    System.out.println(
                            "Booking ID: " + rs.getInt("id") +
                            " | Car ID: " + rs.getInt("car_id") +
                            " | From: " + rs.getDate("from_date") +
                            " | To: " + rs.getDate("to_date") +
                            " | Amount: Rs." + rs.getDouble("total_amount") +
                            " | Status: " + rs.getString("status")
                    );
                }

            } catch (SQLException e) {
                System.out.println("Status Error: " + e.getMessage());
            }
            
        }
        static void viewBookings() {
            try (Connection conn = DBUtil.getConnection()) {
                String sql = "SELECT b.id AS booking_id, u.name AS customer_name, c.name AS car_name, c.plate, b.pickup_location, b.from_date, b.to_date, b.total_amount, b.status "
                           + "FROM bookings b "
                           + "JOIN users u ON b.customer_id = u.id "
                           + "JOIN cars c ON b.car_id = c.id";
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery();
        
                System.out.println("\n---- ALL BOOKINGS ----");
                while (rs.next()) {
                    System.out.println(
                        "Booking ID: " + rs.getInt("booking_id") +
                        " | Customer: " + rs.getString("customer_name") +
                        " | Car: " + rs.getString("car_name") +
                        " | Plate: " + rs.getString("plate") +
                        " | Pickup: " + rs.getString("pickup_location") +
                        " | From: " + rs.getDate("from_date") +
                        " | To: " + rs.getDate("to_date") +
                        " | Amount: Rs." + rs.getDouble("total_amount") +
                        " | Status: " + rs.getString("status")
                    );
                }
        
            } catch (SQLException e) {
                System.out.println("View Bookings Error: " + e.getMessage());
            }
        }
        
    }

    // ======== MAIN MENU ==========
    public static void main(String[] args) {

        while (true) {
            System.out.println("\n===== CAR RENTAL SYSTEM =====");
            System.out.println("1. Admin Login");
            System.out.println("2. Customer Register");
            System.out.println("3. Customer Login");
            System.out.println("4. Exit");
            System.out.print("Choose: ");

            int choice = sc.nextInt();

            switch (choice) {

                case 1: {
                    System.out.println("\n-- Admin Login --");
                    System.out.print("Email: ");
                    String email = sc.next();
                    System.out.print("Password: ");
                    String pass = sc.next();

                    User admin = UserDAO.login(email, pass);

                    if (admin != null && admin.role.equals("admin")) {
                        adminMenu();
                    } else {
                        System.out.println("Invalid admin credentials.");
                    }
                }
                    break;

                case 2: {
                    System.out.println("\n-- Register Customer --");
                    System.out.print("Name: ");
                    String name = sc.next();
                    System.out.print("Email: ");
                    String email = sc.next();
                    System.out.print("Password: ");
                    String pass = sc.next();

                    if (UserDAO.register(name, email, pass))
                        System.out.println("Registered successfully!");
                    else
                        System.out.println("Registration failed!");
                }
                    break;

                case 3: {
                    System.out.println("\n-- Customer Login --");
                    System.out.print("Email: ");
                    String email = sc.next();
                    System.out.print("Password: ");
                    String pass = sc.next();

                    User customer = UserDAO.login(email, pass);

                    if (customer != null && customer.role.equals("customer")) {
                        customerMenu(customer.id);
                    } else {
                        System.out.println("Invalid credentials.");
                    }
                }
                    break;

                case 4:
                    System.out.println("Exiting...");
                    System.exit(0);

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    // =============== ADMIN MENU ===============
    static void adminMenu() {

        while (true) {
            System.out.println("\n===== ADMIN MENU =====");
            System.out.println("1. Add Car");
            System.out.println("2. View Cars");
            System.out.println("3. Delete Car");
            System.out.println("4. View Bookings");
            System.out.println("5. Logout");
            System.out.print("Choose: ");

            int c = sc.nextInt();

            switch (c) {
                case 1:
                    System.out.print("Name: ");
                    String name = sc.next();
                    System.out.print("Color: ");
                    String color = sc.next();
                    System.out.print("Plate: ");
                    String plate = sc.next();
                    System.out.print("Price/day: ");
                    double price = sc.nextDouble();
                    System.out.print("Location: ");
                    String loc = sc.next();

                    CarDAO.addCar(name, color, plate, price, loc);
                    break;

                case 2:
                    CarDAO.viewCars();
                    break;

                case 3:
                    System.out.print("Enter Car ID to delete: ");
                    int id = sc.nextInt();
                    CarDAO.deleteCar(id);
                    break;

                case 4:
                    BookingDAO.viewBookings(); // <-- Show all bookings
                    break;

                case 5:
                    return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    // =============== CUSTOMER MENU ===============
    static void customerMenu(int customerId) {

        while (true) {
            System.out.println("\n===== CUSTOMER MENU =====");
            System.out.println("1. View Cars");
            System.out.println("2. Book Car");
            System.out.println("3. View Booking Status");
            System.out.println("4. Logout");
            System.out.print("Choose: ");

            int c = sc.nextInt();

            switch (c) {
                case 1:
                    CarDAO.viewCars();
                    break;

                case 2:
                    System.out.print("Enter Car ID: ");
                    int cid = sc.nextInt();

                    System.out.print("Pickup Location: ");
                    String pickup = sc.next();

                    System.out.print("From (YYYY-MM-DD): ");
                    String from = sc.next();

                    System.out.print("To (YYYY-MM-DD): ");
                    String to = sc.next();

                    BookingDAO.bookCar(cid, customerId, pickup, from, to);
                    break;

                case 3:
                    BookingDAO.viewStatus(customerId);
                    break;

                case 4:
                    return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }
}
