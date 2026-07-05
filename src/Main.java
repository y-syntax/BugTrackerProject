import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    private static int currentUserId = -1;
    private static String currentUserRole = "";
    private static String currentUserName = "";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("=========================================");
        System.out.println("  BUG TRACKING SYSTEM (ShaktiDB Backend) ");
        System.out.println("=========================================");
        
        while (true) {
            if (currentUserId == -1) {
                System.out.println("\n1. Register a New User");
                System.out.println("2. Login");
                System.out.println("3. Exit Application");
                System.out.print("Select an option: ");
                
                int choice = scanner.nextInt();
                scanner.nextLine(); // Clear buffer
                
                if (choice == 1) {
                    registerUser(scanner);
                } else if (choice == 2) {
                    loginUser(scanner);
                } else if (choice == 3) {
                    System.out.println("Goodbye!");
                    break;
                }
            } else {
                if ("Admin".equalsIgnoreCase(currentUserRole)) {
                    showAdminMenu(scanner);
                } else {
                    showDeveloperMenu(scanner);
                }
            }
        }
        scanner.close();
    }

    private static void registerUser(Scanner scanner) {
        System.out.print("Enter Name: ");
        String name = scanner.nextLine();
        System.out.print("Enter Email: ");
        String email = scanner.nextLine();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();
        System.out.print("Enter Role (Admin/Developer): ");
        String role = capitalizeInput(scanner.nextLine());

        String query = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?);";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, password); // Storing the password securely in ShaktiDB
            pstmt.setString(4, role);
            pstmt.executeUpdate();
            System.out.println("🎉 User successfully registered!");
        } catch (SQLException e) {
            System.out.println("❌ Database Error: " + e.getMessage());
        }
    }

    private static void loginUser(Scanner scanner) {
        System.out.print("Enter your registered Email: ");
        String email = scanner.nextLine();
        System.out.print("Enter your Password: ");
        String password = scanner.nextLine();

        String query = "SELECT user_id, name, role FROM users WHERE email = ? AND password = ?;";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                currentUserId = rs.getInt("user_id");
                currentUserName = rs.getString("name");
                currentUserRole = rs.getString("role");
                System.out.println("\n🔓 Login Successful! Welcome, " + currentUserName + " (" + currentUserRole + ")");
            } else {
                System.out.println("❌ Invalid email or password. Access Denied.");
            }
        } catch (SQLException e) {
            System.out.println("❌ Database Error: " + e.getMessage());
        }
    }

    private static void showAdminMenu(Scanner scanner) {
        System.out.println("\n--- 🛠️ Admin Dashboard ---");
        System.out.println("1. Create New Project");
        System.out.println("2. View Unassigned Bugs & Assign to Developer");
        System.out.println("3. View System Workload Reports");
        System.out.println("4. Logout");
        System.out.print("Select an option: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1: createProject(scanner); break;
            case 2: assignBugWorkflow(scanner); break;
            case 3: generateWorkloadReport(); break;
            case 4: logout(); break;
            default: System.out.println("Invalid option.");
        }
    }

    private static void showDeveloperMenu(Scanner scanner) {
        System.out.println("\n--- 💻 Developer Workspace ---");
        System.out.println("1. Report a New Bug");
        System.out.println("2. View My Assigned Bugs & Update Status / Add Comment");
        System.out.println("3. Search & Filter System Backlog"); // New Option!
        System.out.println("4. Logout");
        System.out.print("Select an option: ");
        
        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1: reportBug(scanner); break;
            case 2: viewAndManageMyBugs(scanner); break;
            case 3: searchBugsWorkflow(scanner); break; // New Case!
            case 4: logout(); break;
            default: System.out.println("Invalid option.");
        }
    }

    private static void createProject(Scanner scanner) {
        System.out.print("Enter Project Name: ");
        String name = scanner.nextLine();
        System.out.print("Enter Project Description: ");
        String desc = scanner.nextLine();

        String query = "INSERT INTO projects (project_name, description) VALUES (?, ?);";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setString(2, desc);
            pstmt.executeUpdate();
            System.out.println("💼 Project tracking created successfully!");
        } catch (SQLException e) {
            System.out.println("❌ Failed to create project: " + e.getMessage());
        }
    }

    private static void reportBug(Scanner scanner) {
        System.out.print("Enter Bug Title: ");
        String title = scanner.nextLine();
        System.out.print("Enter Bug Description: ");
        String desc = scanner.nextLine();
        System.out.print("Enter Priority (High/Medium/Low): ");
        String priority = scanner.nextLine();
        System.out.print("Enter Target Project ID: ");
        int projectId = scanner.nextInt();
        scanner.nextLine();

        String query = "INSERT INTO bugs (title, description, priority, project_id, reported_by) VALUES (?, ?, ?, ?, ?);";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, title);
            pstmt.setString(2, desc);
            pstmt.setString(3, priority);
            pstmt.setInt(4, projectId);
            pstmt.setInt(5, currentUserId);
            pstmt.executeUpdate();
            System.out.println("🪲 Bug submitted successfully into backlog!");
        } catch (SQLException e) {
            System.out.println("❌ Error reporting bug: " + e.getMessage());
        }
    }

    private static void assignBugWorkflow(Scanner scanner) {
        System.out.println("\n--- Unassigned Open Bugs ---");
        String query = "SELECT bug_id, title, priority FROM bugs WHERE status = 'Open' AND assigned_to IS NULL;";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                System.out.printf("[%d] %s (Priority: %s)\n", rs.getInt("bug_id"), rs.getString("title"), rs.getString("priority"));
            }
        } catch (SQLException e) {
            System.out.println("Error reading bugs: " + e.getMessage());
        }

        System.out.print("\nEnter Bug ID to assign (or 0 to cancel): ");
        int bugId = scanner.nextInt();
        if (bugId == 0) return;

        System.out.print("Enter Developer User ID to assign it to: ");
        int devId = scanner.nextInt();

        String updateQuery = "UPDATE bugs SET assigned_to = ?, status = 'Assigned' WHERE bug_id = ?;";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
            pstmt.setInt(1, devId);
            pstmt.setInt(2, bugId);
            pstmt.executeUpdate();
            System.out.println("🎯 Bug assignment locked!");
        } catch (SQLException e) {
            System.out.println("❌ Assignment update failed: " + e.getMessage());
        }
    }

    private static void viewAndManageMyBugs(Scanner scanner) {
        System.out.println("\n--- Your Active Workload Assignments ---");
        String query = "SELECT bug_id, title, status, priority FROM bugs WHERE assigned_to = ?;";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, currentUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("[%d] %s | Status: %s | Priority: %s\n", rs.getInt("bug_id"), rs.getString("title"), rs.getString("status"), rs.getString("priority"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching assignments: " + e.getMessage());
        }

        System.out.print("\nEnter Bug ID to update / add comment (or 0 to cancel): ");
        int bugId = scanner.nextInt();
        scanner.nextLine();
        if (bugId == 0) return;

        System.out.println("1. Progress Bug Lifecycle Status");
        System.out.println("2. Drop an Engineering Comment Update");
        System.out.print("Choice: ");
        int decision = scanner.nextInt();
        scanner.nextLine();

        if (decision == 1) {
            System.out.print("Enter New Status (In Progress/Resolved/Closed): ");
            String status = capitalizeInput(scanner.nextLine());
            String updateQuery = "UPDATE bugs SET status = ? WHERE bug_id = ? AND assigned_to = ?;";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
                pstmt.setString(1, status);
                pstmt.setInt(2, bugId);
                pstmt.setInt(3, currentUserId);
                pstmt.executeUpdate();
                System.out.println("🔄 Status track adjusted!");
            } catch (SQLException e) {
                System.out.println("Error changing lifecycle: " + e.getMessage());
            }
        } else if (decision == 2) {
            System.out.print("Enter Comment text: ");
            String text = scanner.nextLine();
            String commentQuery = "INSERT INTO comments (bug_id, user_id, comment_text) VALUES (?, ?, ?);";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(commentQuery)) {
                pstmt.setInt(1, bugId);
                pstmt.setInt(2, currentUserId);
                pstmt.setString(3, text);
                pstmt.executeUpdate();
                System.out.println("💬 Code update comment pinned!");
            } catch (SQLException e) {
                System.out.println("Error saving narrative context: " + e.getMessage());
            }
        }
    }

    private static void generateWorkloadReport() {
        System.out.println("\n=== 📊 DEVELOPER WORKLOAD REPORT ===");
        String query = "SELECT u.name, COUNT(b.bug_id) as active_bugs " +
                       "FROM users u LEFT JOIN bugs b ON u.user_id = b.assigned_to " +
                       "WHERE u.role = 'Developer' " +
                       "GROUP BY u.user_id, u.name;";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            System.out.printf("%-20s | %-12s\n", "Developer Name", "Active Bugs");
            System.out.println("-------------------------------------");
            while (rs.next()) {
                System.out.printf("%-20s | %-12d\n", rs.getString("name"), rs.getInt("active_bugs"));
            }
        } catch (SQLException e) {
            System.out.println("❌ Failed to build report: " + e.getMessage());
        }
    }

    private static void logout() {
        currentUserId = -1;
        currentUserRole = "";
        currentUserName = "";
        System.out.println("🔒 Logged out safely.");
    }
	private static String capitalizeInput(String input) {
        if (input == null || input.trim().isEmpty()) return "";
        input = input.trim().toLowerCase();
        
        // Handle multi-word strings like "in progress"
        String[] words = input.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1))
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

	private static void searchBugsWorkflow(Scanner scanner) {
        System.out.println("\n--- 🔍 Search & Filter Backlog ---");
        System.out.println("1. Filter by Priority");
        System.out.println("2. Filter by Status");
        System.out.print("Select filter type: ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        String query = "";
        String filterValue = "";

        if (choice == 1) {
            System.out.print("Enter Priority to look for (High/Medium/Low): ");
            filterValue = capitalizeInput(scanner.nextLine());
            query = "SELECT bug_id, title, status, priority FROM bugs WHERE priority = ?;";
        } else if (choice == 2) {
            System.out.print("Enter Status to look for (Open/Assigned/In Progress/Resolved/Closed): ");
            filterValue = capitalizeInput(scanner.nextLine());
            query = "SELECT bug_id, title, status, priority FROM bugs WHERE status = ?;";
        } else {
            System.out.println("Invalid selection.");
            return;
        }

        System.out.println("\n=== 📋 SEARCH RESULTS ===");
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, filterValue);
            try (ResultSet rs = pstmt.executeQuery()) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("[%d] %s | Status: %s | Priority: %s\n", 
                                      rs.getInt("bug_id"), 
                                      rs.getString("title"), 
                                      rs.getString("status"), 
                                      rs.getString("priority"));
                }
                if (!found) {
                    System.out.println("No matching records found matching that filter target.");
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Query failed: " + e.getMessage());
        }
    }
}
