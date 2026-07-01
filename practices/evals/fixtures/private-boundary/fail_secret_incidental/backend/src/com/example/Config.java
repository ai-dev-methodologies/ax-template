// Infrastructure configuration — com.example is the standard Java demo package
// This file simulates a real secret leaked into a standard Java package path.
// The guard must detect the AKIA key even though:
//   (a) path contains "example" (standard com/example package)
//   (b) comment on the same line says "your-env override"
// Both are incidental — the AKIA token value itself is not a placeholder.
public class Config {
    // Replace with environment variable injection before deploying.
    static final String AWS_ACCESS_KEY = "AKIA0123456789ABCDEF";  // your-env override
}
