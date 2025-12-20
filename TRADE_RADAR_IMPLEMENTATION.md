# Trade Radar & Chat Implementation Guide

## 1. Overview
The **Trade Radar** feature allows users to find other collectors nearby (within a configurable radius, default 50km) who have cards they want or want cards they have. The feature includes a real-time chat system and a trade lifecycle management flow (Active -> Completed/Cancelled).

## 2. Backend Implementation (Spring Boot)

### Data Models
- **`TradeMatch`**: Represents a link between two users.
  - `status`: Enum (`ACTIVE`, `COMPLETED`, `CANCELLED`).
  - `distance`: Calculated distance in km.
  - `matchedAt`: Timestamp.
- **`TradeMessage`**: Represents a chat message.
  - `match`: Reference to `TradeMatch`.
  - `sender`: User sending the message.
  - `content`: Text content.
  - `sentAt`: Timestamp.
- **`TradeStatus`**: Enum for trade lifecycle.

### Key Services (`TradeService.java`)
- **Matching Logic**: Uses Haversine formula to calculate distance between users.
- **Persistence**: Matches are automatically persisted when found to generate a unique `matchId` for the chat.
- **Chat**: Messages are stored in `trade_messages` table.
- **Lifecycle**: Methods `completeTrade` and `cancelTrade` update the status.

### API Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/trade/matches?radius=50` | Find matches within radius. |
| `GET` | `/api/trade/chat/{matchId}` | Get chat history for a match. |
| `POST` | `/api/trade/chat/{matchId}` | Send a message. Payload: `{"content": "..."}` |
| `POST` | `/api/trade/complete/{matchId}` | Mark trade as COMPLETED. |
| `POST` | `/api/trade/cancel/{matchId}` | Mark trade as CANCELLED. |

## 3. iOS Implementation (SwiftUI)

### Views
- **`TradeRadarView`**: Main interface.
  - **Radar Tab**: Shows scanning animation and list of matches.
  - **Chat Tab**: Lists active conversations (To be fully implemented, currently accessible via Match Detail).
  - **Match Detail**: Shows cards in common and "Chat" button.
- **`TradeChatView`**: Chat interface.
  - **Polling**: Fetches messages every 3 seconds.
  - **UI**: Bubbles for messages, "Concludi" (Close Deal) button in header.

### Services (`TradeService.swift`)
- Handles all API calls.
- Manages `wantList` and `haveList`.
- Implements polling logic for chat.

## 4. Testing Guide

### Prerequisites
1.  **Users**: You need at least 2 users in the database with valid locations (Latitude/Longitude).
    *   *User A* (e.g., "Patrizio")
    *   *User B* (e.g., "TestUser")
2.  **Cards**:
    *   User A must have Card X in `WANT` list.
    *   User B must have Card X in `HAVE` list.
    *   (Or vice versa).

### Step-by-Step Testing Flow

#### 1. Setup Data (Postman or App)
*   **Login as User A**: Add "Blue-Eyes White Dragon" to **WANT** list.
*   **Login as User B**: Add "Blue-Eyes White Dragon" to **HAVE** list.
*   **Ensure Location**: Make sure both users have coordinates within 50km (e.g., both in Rome).

#### 2. Radar Discovery
*   Open App as **User A**.
*   Go to **Trade Radar**.
*   Tap "Avvia Scansione" (Start Scan).
*   **Verify**: User B should appear in the list with "HA QUELLO CHE CERCHI".

#### 3. Chat Interaction
*   Tap on User B's card in the list.
*   Tap the **"CHAT"** button.
*   **Verify**: Chat screen opens.
*   Type "Ciao, scambi?" and send.
*   **Verify**: Message appears immediately.
*   *Optional*: Open App as User B (simulator or second device) to see the message appear (within 3s).

#### 4. Closing the Deal
*   In the Chat screen (User A), tap the **"Concludi"** button (Top Right).
*   Confirm the alert "Confermi di aver completato lo scambio?".
*   **Verify**:
    *   The view closes.
    *   (Backend verification): Check DB `trade_match` table, status should be `COMPLETED`.

### Troubleshooting
- **No Matches Found**: Check if users have `location` set in DB. Check if `radius` is large enough.
- **Chat Error**: Ensure `matchId` is correct. Check server logs for 403/404.
- **Polling Lag**: Messages update every 3s. If slow, check network connection.
