import java.io.*;
import java.net.*;
import java.util.*;

public class WorldClockServer {

    private static final int PORT = 55555;
    private static final boolean DEBUG = true; // flip to false before demo

    // City name -> Java TimeZone ID
    private static final String[][] CITIES = {
        { "Johannesburg", "Africa/Johannesburg" },
        { "London",       "Europe/London" },
        { "New York",     "America/New_York" },
        { "Los Angeles",  "America/Los_Angeles" },
        { "Tokyo",        "Asia/Tokyo" },
        { "Sydney",       "Australia/Sydney" },
        { "Moscow",       "Europe/Moscow" },
        { "Dubai",        "Asia/Dubai" },
        { "Mumbai",       "Asia/Kolkata" },
        { "Beijing",      "Asia/Shanghai" }
    };

    private static final String SA_TIMEZONE = "Africa/Johannesburg";

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            if (DEBUG) System.out.println("World clock server listening on port " + PORT);
            while (true) {
                Socket client = serverSocket.accept();
                Thread t = new Thread(() -> handleClient(client));
                t.setDaemon(true);
                t.start();
            }
        }
    }

    private static void handleClient(Socket client) {
        try (Socket s = client;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(s.getInputStream()));
             OutputStream rawOut = s.getOutputStream()) {

            String requestLine = in.readLine(); // e.g. "GET /?city=Tokyo HTTP/1.1"
            if (requestLine == null || requestLine.isEmpty()) return;
            if (DEBUG) System.out.println("Request: " + requestLine);

            // We don't need the rest of the headers, but must read them off
            // the socket so the connection behaves; stop at the blank line.
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                // discard headers
            }

            String path = extractPath(requestLine);       // "/?city=Tokyo"
            String selectedCity = extractCityParam(path);  // "Tokyo" or null

            String html = buildPage(selectedCity);
            sendHttpResponse(rawOut, html);

        } catch (IOException e) {
            if (DEBUG) e.printStackTrace();
        }
    }

    // ---- Minimal HTTP request-line parsing ----

    private static String extractPath(String requestLine) {
        // "GET /?city=Tokyo HTTP/1.1" -> "/?city=Tokyo"
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) return "/";
        return parts[1];
    }

    private static String extractCityParam(String path) {
        int q = path.indexOf('?');
        if (q < 0) return null;
        String query = path.substring(q + 1);
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            String key = pair.substring(0, eq);
            String value = pair.substring(eq + 1);
            if (key.equals("city")) {
                return urlDecode(value);
            }
        }
        return null;
    }

    // Tiny URL decoder for %20 and + in city names, no java.net.URLDecoder needed
    private static String urlDecode(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '+') {
                sb.append(' ');
            } else if (c == '%' && i + 2 < s.length()) {
                String hex = s.substring(i + 1, i + 3);
                sb.append((char) Integer.parseInt(hex, 16));
                i += 2;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ---- HTML building ----

    private static String buildPage(String selectedCity) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta http-equiv=\"refresh\" content=\"1\">");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<title>World Clock</title>");
        html.append(STYLE);
        html.append("</head><body>");

        html.append("<div class=\"wrap\">");

        html.append("<h1>🌍 World Clock</h1>");

        // South Africa clock card
        html.append("<div class=\"card home\">");
        html.append("<div class=\"label\">South Africa</div>");
        html.append("<div class=\"time\">").append(currentTimeString(SA_TIMEZONE, true)).append("</div>");
        html.append("<div class=\"date\">").append(currentTimeString(SA_TIMEZONE, false)).append("</div>");
        html.append("</div>");

        // Selected city clock card (only if a city was clicked)
        if (selectedCity != null) {
            String tzId = lookupTimeZone(selectedCity);
            html.append("<div class=\"card selected\">");
            if (tzId != null) {
                html.append("<div class=\"label\">").append(selectedCity).append("</div>");
                html.append("<div class=\"time\">").append(currentTimeString(tzId, true)).append("</div>");
                html.append("<div class=\"date\">").append(currentTimeString(tzId, false)).append("</div>");
            } else {
                html.append("<div class=\"label\">Unknown city: ").append(selectedCity).append("</div>");
            }
            html.append("</div>");
        }

        // City picker grid
        html.append("<h2>Choose a city</h2>");
        html.append("<div class=\"grid\">");
        for (String[] city : CITIES) {
            String name = city[0];
            String cssClass = name.equals(selectedCity) ? "city active" : "city";
            html.append("<a class=\"").append(cssClass).append("\" href=\"/?city=")
                .append(urlEncode(name)).append("\">").append(name).append("</a>");
        }
        html.append("</div>");

        html.append("<div class=\"footer\">Updates automatically every second</div>");
        html.append("</div>"); // .wrap
        html.append("</body></html>");
        return html.toString();
    }

    private static final String STYLE =
        "<style>" +
        "  * { box-sizing: border-box; }" +
        "  body {" +
        "    margin: 0; padding: 40px 20px;" +
        "    background: linear-gradient(135deg, #1e1e2f, #2d2d44);" +
        "    font-family: 'Segoe UI', Arial, sans-serif;" +
        "    color: #eee; text-align: center;" +
        "  }" +
        "  .wrap { max-width: 720px; margin: 0 auto; }" +
        "  h1 { font-size: 2.2em; margin-bottom: 30px; letter-spacing: 1px; }" +
        "  h2 { font-weight: normal; color: #bbb; margin-top: 40px; }" +
        "  .card {" +
        "    display: inline-block; min-width: 260px;" +
        "    background: #12121c; border-radius: 16px;" +
        "    padding: 25px 35px; margin: 10px;" +
        "    box-shadow: 0 8px 24px rgba(0,0,0,0.4);" +
        "    border: 1px solid #333;" +
        "  }" +
        "  .card.selected { border: 1px solid #4fd1c5; }" +
        "  .label { font-size: 1.1em; color: #9aa5b1; text-transform: uppercase; letter-spacing: 2px; margin-bottom: 8px; }" +
        "  .time {" +
        "    font-family: 'Consolas', 'Courier New', monospace;" +
        "    font-size: 2.6em; font-weight: bold;" +
        "    color: #4fd1c5; letter-spacing: 2px;" +
        "  }" +
        "  .card.selected .time { color: #f6ad55; }" +
        "  .date { color: #888; margin-top: 6px; font-size: 0.95em; }" +
        "  .grid {" +
        "    display: flex; flex-wrap: wrap; justify-content: center; gap: 10px;" +
        "    margin-top: 15px;" +
        "  }" +
        "  .city {" +
        "    display: inline-block; padding: 10px 18px;" +
        "    background: #23233a; color: #ddd; text-decoration: none;" +
        "    border-radius: 999px; border: 1px solid #3a3a55;" +
        "    transition: background 0.15s ease;" +
        "    font-size: 0.95em;" +
        "  }" +
        "  .city:hover { background: #33334f; }" +
        "  .city.active { background: #4fd1c5; color: #12121c; font-weight: bold; border-color: #4fd1c5; }" +
        "  .footer { margin-top: 40px; color: #666; font-size: 0.85em; }" +
        "</style>";

    private static String urlEncode(String s) {
        // simple: only spaces need encoding for our city names
        return s.replace(" ", "%20");
    }

    private static String lookupTimeZone(String cityName) {
        for (String[] city : CITIES) {
            if (city[0].equalsIgnoreCase(cityName)) return city[1];
        }
        return null;
    }

    // withTime=true -> "HH:MM:SS", withTime=false -> "YYYY-MM-DD"
    private static String currentTimeString(String timeZoneId, boolean withTime) {
        TimeZone tz = TimeZone.getTimeZone(timeZoneId);
        Calendar cal = Calendar.getInstance(tz);

        if (withTime) {
            int hour   = cal.get(Calendar.HOUR_OF_DAY);
            int minute = cal.get(Calendar.MINUTE);
            int second = cal.get(Calendar.SECOND);
            return String.format("%02d:%02d:%02d", hour, minute, second);
        } else {
            int day    = cal.get(Calendar.DAY_OF_MONTH);
            int month  = cal.get(Calendar.MONTH) + 1;
            int year   = cal.get(Calendar.YEAR);
            return String.format("%04d-%02d-%02d", year, month, day);
        }
    }

    // ---- Raw HTTP response ----

    private static void sendHttpResponse(OutputStream out, String html) throws IOException {
        byte[] body = html.getBytes("UTF-8");
        PrintWriter headerWriter = new PrintWriter(
                new OutputStreamWriter(out, "UTF-8"), false);

        headerWriter.print("HTTP/1.1 200 OK\r\n");
        headerWriter.print("Content-Type: text/html; charset=UTF-8\r\n");
        headerWriter.print("Content-Length: " + body.length + "\r\n");
        headerWriter.print("Connection: close\r\n");
        headerWriter.print("\r\n");
        headerWriter.flush();

        out.write(body);
        out.flush();
    }
}