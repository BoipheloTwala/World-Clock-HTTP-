## World Clock

A browser-based world clock server: shows the current time in South Africa plus a clickable list of world cities, entirely via raw HTTP responses built by hand.

## How it works
ServerSocket parses the raw GET request line and query string itself (no servlet API).
Each city name is a real <a href="/?city=...> link - clicking one is a fresh GET request, per the assignment's requirement.
<meta http-equiv="refresh" content="1> makes the browser re-request the page every second, so the clock appears to "tick" with zero client-side JavaScript, the server is stateless and just answers "what time is it right now" on each request.
Time zone math uses java.util.TimeZone / Calendar (core JDK).
Styled with inline CSS: dark theme, glowing digital-clock cards, and a pill-button city picker with the selected city highlighted.

## Running
bash

javac WorldClockServer.java

java WorldClockServer

Open http://127.0.0.1:55555 in a browser.

## Notes
Connection: close is sent on every response so each per-second refresh opens a fresh connection, matching the server's per-request accept() loop design, no HTTP keep-alive handling needed.
