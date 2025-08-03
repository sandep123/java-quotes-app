import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Main {
    private static List<String> quotes;
    private static final String HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Inspirational Quotes</title>
                <style>
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
                        margin: 0;
                        padding: 0;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        color: #333;
                    }
                    .container {
                        max-width: 800px;
                        width: 90%;
                        margin: 0 auto;
                        text-align: center;
                    }
                    .quote-box {
                        background: white;
                        border-radius: 10px;
                        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
                        padding: 2rem;
                        margin: 2rem 0;
                        transition: all 0.3s ease;
                    }
                    .quote-box:hover {
                        transform: translateY(-5px);
                        box-shadow: 0 15px 35px rgba(0, 0, 0, 0.15);
                    }
                    .quote {
                        font-size: 1.5rem;
                        line-height: 1.6;
                        margin-bottom: 1rem;
                        font-style: italic;
                    }
                    .author {
                        font-size: 1.2rem;
                        color: #666;
                        font-weight: bold;
                    }
                    .btn {
                        background: #4a6fa5;
                        color: white;
                        border: none;
                        padding: 0.8rem 1.5rem;
                        font-size: 1rem;
                        border-radius: 5px;
                        cursor: pointer;
                        transition: background 0.3s;
                        margin: 0.5rem;
                    }
                    .btn:hover {
                        background: #3a5a80;
                    }
                    .footer {
                        margin-top: 2rem;
                        color: #666;
                        font-size: 0.9rem;
                    }
                    .api-info {
                        background: rgba(255, 255, 255, 0.8);
                        padding: 1rem;
                        border-radius: 5px;
                        margin-top: 2rem;
                        font-family: monospace;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>🌟 Inspirational Quotes 🌟</h1>
                    
                    <div class="quote-box">
                        <div class="quote">%QUOTE%</div>
                        <div class="author">%AUTHOR%</div>
                    </div>
                    
                    <div>
                        <button class="btn" onclick="window.location.reload()">Get Another Quote</button>
                        <button class="btn" onclick="copyToClipboard()">Copy Quote</button>
                    </div>
                    
                    <div class="api-info">
                        <h3>API Endpoint</h3>
                        <p>You can also access our quotes via API:</p>
                        <p><code>GET /api/quote</code> - Returns a random quote in JSON format</p>
                    </div>
                    
                    <div class="footer">
                        Made with ❤️ | %TOTAL_QUOTES% quotes available
                    </div>
                </div>
                
                <script>
                    function copyToClipboard() {
                        const quote = document.querySelector('.quote').textContent;
                        const author = document.querySelector('.author').textContent;
                        const text = `"${quote}" — ${author}`;
                        
                        navigator.clipboard.writeText(text).then(() => {
                            alert('Quote copied to clipboard!');
                        }).catch(err => {
                            console.error('Failed to copy: ', err);
                        });
                    }
                </script>
            </body>
            </html>
            """;

    public static void main(String[] args) throws IOException {
        // Load quotes from an external file
        quotes = loadQuotesFromFile("quotes.txt");
        
        if (quotes.isEmpty()) {
            System.err.println("No quotes found in the file. Please ensure 'quotes.txt' has content.");
            return;
        }
        
        // Create an HTTP server listening on port 8000
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        // Define a context for the root path ("/")
        server.createContext("/", exchange -> {
            try {
                String path = exchange.getRequestURI().getPath();
                
                if (path.equals("/") || path.equals("/index.html")) {
                    // Serve HTML page
                    String[] quoteParts = parseQuote(getRandomQuote());
                    String htmlResponse = HTML_TEMPLATE
                            .replace("%QUOTE%", quoteParts[0])
                            .replace("%AUTHOR%", quoteParts[1])
                            .replace("%TOTAL_QUOTES%", String.valueOf(quotes.size()));
                    
                    byte[] responseBytes = htmlResponse.getBytes(StandardCharsets.UTF_8);
                    
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }
                } else if (path.equals("/api/quote")) {
                    // Serve API response
                    String quote = getRandomQuote();
                    String jsonResponse = String.format("{\"quote\": \"%s\"}", quote.replace("\"", "\\\""));
                    
                    byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
                    
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }
                } else {
                    // Handle 404
                    String notFoundResponse = "<h1>404 Not Found</h1><p>The requested resource was not found.</p>";
                    byte[] responseBytes = notFoundResponse.getBytes(StandardCharsets.UTF_8);
                    
                    exchange.getResponseHeaders().set("Content-Type", "text/html");
                    exchange.sendResponseHeaders(404, responseBytes.length);
                    
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                exchange.sendResponseHeaders(500, 0);
                exchange.close();
            }
        });

        // Start the server
        server.start();
        System.out.println("Server is running on http://localhost:8000");
    }

    // Helper method to get a random quote
    private static String getRandomQuote() {
        Random random = new Random();
        return quotes.get(random.nextInt(quotes.size()));
    }

    // Helper method to parse quote into text and author
    private static String[] parseQuote(String fullQuote) {
        int lastDash = fullQuote.lastIndexOf("–");
        if (lastDash == -1) {
            return new String[]{fullQuote.trim(), "Unknown"};
        }
        return new String[]{
            fullQuote.substring(0, lastDash).trim(),
            fullQuote.substring(lastDash + 1).trim()
        };
    }

    // Helper method to load quotes from an external file
    private static List<String> loadQuotesFromFile(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            return reader.lines()
                    .filter(line -> !line.trim().isEmpty())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Error reading quotes file: " + e.getMessage());
            return List.of();
        }
    }
}
