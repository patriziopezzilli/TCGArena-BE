# TCG Arena Backend API Documentation

This document provides comprehensive API documentation for the TCG Arena backend, designed to facilitate Android app development. All endpoints are RESTful and return JSON responses.

## Base URL
```
http://your-backend-url/api
```

## Authentication
Most endpoints require authentication via JWT token in the Authorization header:
```
Authorization: Bearer <jwt_token>
```

## Controllers and Endpoints

### 1. Authentication Controller (`/api/auth`)

#### POST `/api/auth/login`
Login user with username/password.

**Request Body:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response (200):**
```json
{
  "token": "jwt_access_token",
  "refreshToken": "jwt_refresh_token",
  "user": {
    "id": 1,
    "username": "string",
    "email": "string",
    "displayName": "string",
    "isMerchant": false,
    "isAdmin": false,
    "points": 0,
    "favoriteTCGTypes": ["POKEMON", "MAGIC"],
    "profileImageUrl": "string",
    "dateJoined": "2023-01-01T00:00:00"
  }
}
```

#### POST `/api/auth/register`
Register a new user account.

**Request Body:**
```json
{
  "username": "string",
  "email": "string",
  "displayName": "string",
  "password": "string",
  "favoriteGames": ["POKEMON", "MAGIC"]
}
```

**Response (200):** Same as login response.

#### POST `/api/auth/refresh-token`
Refresh access token using refresh token.

**Request Body:**
```json
{
  "refreshToken": "string"
}
```

**Response (200):**
```json
{
  "token": "new_jwt_access_token",
  "refreshToken": "new_jwt_refresh_token"
}
```

#### POST `/api/auth/register-merchant`
Register a new merchant account with shop.

**Request Body:**
```json
{
  "username": "string",
  "email": "string",
  "displayName": "string",
  "password": "string",
  "shopName": "string",
  "description": "string",
  "address": "string",
  "city": "string",
  "zipCode": "string",
  "phone": "string"
}
```

**Response (200):**
```json
{
  "user": {...},
  "shop": {...},
  "token": "jwt_token"
}
```

### 2. User Controller (`/api/users`)

#### GET `/api/users`
Get all users with stats (Admin only).

**Response (200):**
```json
[
  {
    "id": 1,
    "username": "string",
    "displayName": "string",
    "totalCards": 150,
    "totalTournaments": 5,
    "wins": 3,
    "losses": 2,
    "winRate": 60.0
  }
]
```

#### GET `/api/users/{id}`
Get user by ID.

**Path Parameters:**
- `id` (Long): User ID

**Response (200):**
```json
{
  "id": 1,
  "username": "string",
  "email": "string",
  "displayName": "string",
  "isMerchant": false,
  "isAdmin": false,
  "points": 100,
  "favoriteTCGTypes": ["POKEMON"],
  "profileImageUrl": "string",
  "deviceToken": "string",
  "isPrivate": false,
  "dateJoined": "2023-01-01T00:00:00"
}
```

#### GET `/api/users/search`
Search users by query string.

**Query Parameters:**
- `query` (String): Search query

**Response (200):** Array of UserWithStatsDTO

#### GET `/api/users/leaderboard`
Get user leaderboard with stats.

**Response (200):** Array of UserWithStatsDTO

#### POST `/api/users`
Create a new user (Admin only).

**Request Body:** User object

**Response (200):** User object

#### PUT `/api/users/{id}`
Update existing user.

**Path Parameters:**
- `id` (Long): User ID

**Request Body:** User object

**Response (200):** Updated User object

#### DELETE `/api/users/{id}`
Delete user account.

**Path Parameters:**
- `id` (Long): User ID

**Response (204):** No content

#### PATCH `/api/users/{id}/profile`
Update user profile partially.

**Path Parameters:**
- `id` (Long): User ID

**Request Body:**
```json
{
  "displayName": "string",
  "bio": "string",
  "favoriteGame": "POKEMON"
}
```

**Response (200):** Updated User object

#### PUT `/api/users/{id}/profile-image`
Update user profile image.

**Path Parameters:**
- `id` (Long): User ID

**Request Body:** String (image URL)

**Response (200):** Updated User object

#### PUT `/api/users/{id}/device-token`
Update user device token for push notifications.

**Path Parameters:**
- `id` (Long): User ID

**Request Body:** String (device token)

**Response (200):** Updated User object

#### GET `/api/users/{id}/stats`
Get user statistics.

**Path Parameters:**
- `id` (Long): User ID

**Response (200):**
```json
{
  "id": 1,
  "userId": 1,
  "totalCardsCollected": 150,
  "totalTournamentsPlayed": 5,
  "tournamentsWon": 3,
  "tournamentsLost": 2,
  "totalPointsEarned": 500,
  "currentStreak": 2,
  "longestStreak": 5,
  "favoriteTCGType": "POKEMON",
  "lastActivityDate": "2023-12-01T00:00:00"
}
```

#### GET `/api/users/leaderboard/stats`
Get user leaderboard stats.

**Query Parameters:**
- `limit` (int, default: 50): Maximum results

**Response (200):** Array of UserStats

#### GET `/api/users/leaderboard/active`
Get active players leaderboard.

**Query Parameters:**
- `limit` (int, default: 50): Maximum results

**Response (200):** Array of UserStats

#### GET `/api/users/leaderboard/collection`
Get collection leaderboard.

**Query Parameters:**
- `limit` (int, default: 50): Maximum results

**Response (200):** Array of UserStats

#### GET `/api/users/leaderboard/tournaments`
Get tournament leaderboard.

**Query Parameters:**
- `limit` (int, default: 50): Maximum results

**Response (200):** Array of UserStats

#### GET `/api/users/{id}/favorite-tcgs`
Get user's favorite TCG types.

**Path Parameters:**
- `id` (Long): User ID

**Response (200):** Array of TCGType strings

#### PUT `/api/users/{id}/favorite-tcgs`
Update user's favorite TCG types.

**Path Parameters:**
- `id` (Long): User ID

**Request Body:** Array of TCGType strings

**Response (200):** Array of TCGType strings

#### PUT `/api/users/{id}/privacy`
Update user privacy setting.

**Path Parameters:**
- `id` (Long): User ID

**Request Body:**
```json
{
  "isPrivate": true
}
```

**Response (200):**
```json
{
  "isPrivate": true
}
```

### 3. Card Controller (`/api/cards/templates`)

#### GET `/api/cards/templates`
Get all card templates with pagination.

**Query Parameters:**
- `page` (int, default: 0): Page number
- `size` (int, default: 50): Items per page

**Response (200):** Page of CardTemplate objects

#### GET `/api/cards/templates/{id}`
Get card template by ID.

**Path Parameters:**
- `id` (Long): Card template ID

**Response (200):** CardTemplate object

#### POST `/api/cards/templates`
Create a new card template.

**Request Body:** CardTemplate object

**Response (200):** Created CardTemplate object

#### PUT `/api/cards/templates/{id}`
Update existing card template.

**Path Parameters:**
- `id` (Long): Card template ID

**Request Body:** CardTemplate object

**Response (200):** Updated CardTemplate object

#### DELETE `/api/cards/templates/{id}`
Delete card template.

**Path Parameters:**
- `id` (Long): Card template ID

**Response (204):** No content

#### GET `/api/cards/templates/search`
Search card templates.

**Query Parameters:**
- `q` (String): Search query (min 2 characters)

**Response (200):** Array of CardTemplate objects

#### GET `/api/cards/templates/search/advanced`
Advanced search with filters.

**Query Parameters:**
- `tcgType` (String): TCG type filter
- `expansionId` (Long): Expansion ID filter
- `setCode` (String): Set code filter
- `rarity` (String): Rarity filter
- `q` (String): Name search query
- `page` (int, default: 0): Page number
- `size` (int, default: 20): Items per page

**Response (200):** Page of CardTemplate objects

#### GET `/api/cards/templates/filters/tcg-types`
Get all TCG types.

**Response (200):** Array of TCGType enums

#### GET `/api/cards/templates/filters/rarities`
Get all rarities.

**Response (200):** Array of Rarity enums

#### GET `/api/cards/templates/filters/set-codes`
Get all set codes.

**Response (200):** Array of strings

#### GET `/api/cards/templates/market-price/{id}`
Get market price of card template.

**Path Parameters:**
- `id` (Long): Card template ID

**Response (200):** Double (price)

### 4. Deck Controller (`/api/decks`)

#### GET `/api/decks`
Get user's decks.

**Query Parameters:**
- `userId` (Long): User ID

**Response (200):** Array of Deck objects

#### GET `/api/decks/{id}`
Get deck by ID.

**Path Parameters:**
- `id` (Long): Deck ID

**Response (200):** Deck object

#### POST `/api/decks`
Create a new deck.

**Request Body:** Deck object

**Response (200):** Created Deck object

#### PUT `/api/decks/{id}`
Update existing deck.

**Path Parameters:**
- `id` (Long): Deck ID

**Request Body:** Deck object

**Response (200):** Updated Deck object

#### DELETE `/api/decks/{id}`
Delete deck.

**Path Parameters:**
- `id` (Long): Deck ID
- `userId` (Long): User ID performing action

**Response (204):** No content

#### POST `/api/decks/{id}/add-card`
Add card to deck.

**Path Parameters:**
- `id` (Long): Deck ID

**Query Parameters:**
- `cardId` (Long): Card ID
- `quantity` (int): Quantity to add
- `userId` (Long): User ID

**Response (200):** Updated Deck object

#### POST `/api/decks/{id}/add-card-template`
Add card template to deck.

**Path Parameters:**
- `id` (Long): Deck ID

**Query Parameters:**
- `templateId` (Long): Card template ID
- `userId` (Long): User ID

**Response (200):** Updated Deck object

#### DELETE `/api/decks/{id}/remove-card`
Remove card from deck.

**Path Parameters:**
- `id` (Long): Deck ID

**Query Parameters:**
- `cardId` (Long): Card ID
- `userId` (Long): User ID

**Response (204):** No content

#### PUT `/api/decks/{id}/cards/{deckCardId}/condition`
Update card condition in deck.

**Path Parameters:**
- `id` (Long): Deck ID
- `deckCardId` (Long): Deck card ID

**Query Parameters:**
- `condition` (String): New condition
- `userId` (Long): User ID

**Response (200):** No content

#### PUT `/api/decks/cards/{cardId}/condition`
Update card condition by card ID.

**Path Parameters:**
- `cardId` (Long): Card ID

**Query Parameters:**
- `condition` (String): New condition
- `userId` (Long): User ID

**Response (200):** No content

#### PUT `/api/decks/cards/{cardId}`
Update deck card by card ID.

**Path Parameters:**
- `cardId` (Long): Card ID

**Query Parameters:**
- `userId` (Long): User ID

**Request Body:** DeckCardUpdateDTO

**Response (200):** No content

#### DELETE `/api/decks/cards/{cardId}`
Remove card by card ID.

**Path Parameters:**
- `cardId` (Long): Card ID

**Query Parameters:**
- `userId` (Long): User ID

**Response (204):** No content

#### GET `/api/decks/collection`
Get user's collection deck.

**Query Parameters:**
- `userId` (Long): User ID

**Response (200):** Deck object

#### POST `/api/decks/create`
Create deck with parameters.

**Query Parameters:**
- `name` (String): Deck name
- `description` (String): Description
- `tcgType` (TCGType): TCG type
- `deckType` (DeckType): Deck type
- `userId` (Long): User ID

**Response (200):** Created Deck object

### 5. Tournament Controller (`/api/tournaments`)

#### GET `/api/tournaments`
Get all tournaments.

**Response (200):** Array of Tournament objects

#### GET `/api/tournaments/{id}`
Get tournament by ID.

**Path Parameters:**
- `id` (Long): Tournament ID

**Response (200):** Tournament object

#### GET `/api/tournaments/upcoming`
Get upcoming tournaments.

**Response (200):** Array of Tournament objects

#### GET `/api/tournaments/nearby`
Get nearby tournaments.

**Query Parameters:**
- `latitude` (double): Latitude
- `longitude` (double): Longitude
- `radiusKm` (double, default: 50): Search radius

**Response (200):** Array of Tournament objects

#### GET `/api/tournaments/past`
Get past tournaments.

**Response (200):** Array of Tournament objects

#### POST `/api/tournaments`
Create new tournament (Merchants only).

**Request Body:** Tournament object

**Response (200):** Created Tournament object

#### PUT `/api/tournaments/{id}`
Update tournament.

**Path Parameters:**
- `id` (Long): Tournament ID

**Request Body:** Tournament object

**Response (200):** Updated Tournament object

#### DELETE `/api/tournaments/{id}`
Delete tournament.

**Path Parameters:**
- `id` (Long): Tournament ID

**Response (204):** No content

#### POST `/api/tournaments/{tournamentId}/register`
Register for tournament.

**Path Parameters:**
- `tournamentId` (Long): Tournament ID

**Response (200):** TournamentParticipant object

#### DELETE `/api/tournaments/{tournamentId}/register`
Unregister from tournament.

**Path Parameters:**
- `tournamentId` (Long): Tournament ID

**Response (204):** No content

#### POST `/api/tournaments/{id}/participants/manual`
Register manual participant (Merchants only).

**Path Parameters:**
- `id` (Long): Tournament ID

**Request Body:** ManualRegistrationRequest

**Response (200):** TournamentParticipant object

#### POST `/api/tournaments/{id}/participants`
Add existing participant (Merchants only).

**Path Parameters:**
- `id` (Long): Tournament ID

**Request Body:**
```json
{
  "userIdentifier": "email_or_username"
}
```

**Response (200):** TournamentParticipant object

#### GET `/api/tournaments/{tournamentId}/participants`
Get tournament participants.

**Path Parameters:**
- `tournamentId` (Long): Tournament ID

**Response (200):** Array of TournamentParticipant objects

#### GET `/api/tournaments/{tournamentId}/participants/registered`
Get registered participants.

**Path Parameters:**
- `tournamentId` (Long): Tournament ID

**Response (200):** Array of TournamentParticipant objects

#### GET `/api/tournaments/{tournamentId}/participants/waiting`
Get waiting list.

**Path Parameters:**
- `tournamentId` (Long): Tournament ID

**Response (200):** Array of TournamentParticipant objects

#### POST `/api/tournaments/checkin`
Check-in participant using QR code.

**Query Parameters:**
- `code` (String): Check-in QR code

**Response (200):** TournamentParticipant object

#### POST `/api/tournaments/{tournamentId}/self-checkin`
Self check-in for tournament.

**Path Parameters:**
- `tournamentId` (Long): Tournament ID

**Response (200):** TournamentParticipant object

#### GET `/api/tournaments/{tournamentId}/participants/detailed`
Get participants with user details.

**Path Parameters:**
- `tournamentId` (Long): Tournament ID

**Response (200):** Array of TournamentParticipantDTO objects

#### POST `/api/tournaments/{tournamentId}/start`
Start tournament (Organizer only).

**Path Parameters:**
- `tournamentId` (Long): Tournament ID

**Response (200):** Started Tournament object

#### DELETE `/api/tournaments/{tournamentId}/participants/{participantId}`
Remove participant (Organizer only).

**Path Parameters:**
- `tournamentId` (Long): Tournament ID
- `participantId` (Long): Participant ID

**Response (204):** No content

#### POST `/api/tournaments/{tournamentId}/complete`
Complete tournament with placements (Organizer only).

**Path Parameters:**
- `tournamentId` (Long): Tournament ID

**Request Body:**
```json
{
  "placements": [
    {
      "participantId": 1,
      "placement": 1
    }
  ]
}
```

**Response (200):** Completed Tournament object

### 6. Shop Controller (`/api/shops`)

#### GET `/api/shops`
Get all shops.

**Response (200):** Array of Shop objects

#### GET `/api/shops/{id}`
Get shop by ID.

**Path Parameters:**
- `id` (Long): Shop ID

**Response (200):** Shop object

#### POST `/api/shops`
Create new shop.

**Request Body:** Shop object

**Response (200):** Created Shop object

#### PUT `/api/shops/{id}`
Update shop.

**Path Parameters:**
- `id` (Long): Shop ID

**Request Body:** Shop object

**Response (200):** Updated Shop object

#### DELETE `/api/shops/{id}`
Delete shop.

**Path Parameters:**
- `id` (Long): Shop ID

**Response (204):** No content

#### POST `/api/shops/{shopId}/subscribe`
Subscribe to shop notifications.

**Path Parameters:**
- `shopId` (Long): Shop ID

**Response (200):**
```json
{
  "message": "Successfully subscribed to shop",
  "subscriptionId": "123"
}
```

#### DELETE `/api/shops/{shopId}/subscribe`
Unsubscribe from shop.

**Path Parameters:**
- `shopId` (Long): Shop ID

**Response (200):**
```json
{
  "message": "Successfully unsubscribed from shop"
}
```

#### GET `/api/shops/{shopId}/subscription`
Check subscription status.

**Path Parameters:**
- `shopId` (Long): Shop ID

**Response (200):**
```json
{
  "subscribed": true
}
```

#### GET `/api/shops/subscriptions`
Get user's subscriptions.

**Response (200):** Array of ShopSubscription objects

#### GET `/api/shops/{shopId}/subscribers`
Get shop subscribers (Merchant only).

**Path Parameters:**
- `shopId` (Long): Shop ID

**Response (200):** Array of User objects

#### GET `/api/shops/{shopId}/subscriber-count`
Get subscriber count.

**Path Parameters:**
- `shopId` (Long): Shop ID

**Response (200):**
```json
{
  "count": 25
}
```

#### GET `/api/shops/{shopId}/reservation-settings`
Get reservation settings.

**Path Parameters:**
- `shopId` (Long): Shop ID

**Response (200):**
```json
{
  "reservationDurationMinutes": 30,
  "defaultDurationMinutes": 30
}
```

#### PUT `/api/shops/{shopId}/reservation-settings`
Update reservation settings.

**Path Parameters:**
- `shopId` (Long): Shop ID

**Request Body:**
```json
{
  "reservationDurationMinutes": 45
}
```

**Response (200):**
```json
{
  "message": "Reservation settings updated successfully",
  "reservationDurationMinutes": 45
}
```

### 7. Inventory Card Controller (`/api/inventory`)

#### GET `/api/inventory`
Get inventory for shop with filters.

**Query Parameters:**
- `shopId` (String): Shop ID
- `tcgType` (String): TCG type filter
- `condition` (String): Condition filter
- `minPrice` (Double): Minimum price
- `maxPrice` (Double): Maximum price
- `searchQuery` (String): Search query
- `page` (int, default: 0): Page number
- `size` (int, default: 20): Items per page

**Response (200):** InventoryListResponse

#### GET `/api/inventory/template`
Download CSV template.

**Response (200):** CSV file

#### POST `/api/inventory/bulk-import`
Bulk import from CSV (Merchants only).

**Query Parameters:**
- `shopId` (String): Shop ID

**Request Body:** Multipart file

**Response (200):** BulkImportResponse

#### POST `/api/inventory/import-request`
Submit custom file for AI processing (Merchants only).

**Query Parameters:**
- `shopId` (String): Shop ID

**Request Body:** Multipart file + notes

**Response (201):** ImportRequestResponse

#### GET `/api/inventory/{id}`
Get single inventory card.

**Path Parameters:**
- `id` (String): Inventory card ID

**Response (200):** InventoryCard object

#### POST `/api/inventory`
Create inventory card (Merchants only).

**Request Body:** CreateInventoryCardRequest

**Response (201):** Created InventoryCard object

#### PUT `/api/inventory/{id}`
Update inventory card (Merchants only).

**Path Parameters:**
- `id` (String): Inventory card ID

**Request Body:** UpdateInventoryCardRequest

**Response (200):** Updated InventoryCard object

#### DELETE `/api/inventory/{id}`
Delete inventory card (Merchants only).

**Path Parameters:**
- `id` (String): Inventory card ID

**Response (204):** No content

#### GET `/api/inventory/stats`
Get inventory statistics (Merchants only).

**Query Parameters:**
- `shopId` (String): Shop ID

**Response (200):** InventoryStatsResponse

#### GET `/api/inventory/low-stock`
Get low stock items (Merchants only).

**Query Parameters:**
- `shopId` (String): Shop ID
- `threshold` (int, default: 5): Stock threshold

**Response (200):** Array of InventoryCard objects

#### POST `/api/inventory/bulk-add-by-set`
Bulk add by set code (Merchants only).

**Request Body:** BulkAddBySetRequest

**Response (200):** BulkImportResponse

#### POST `/api/inventory/bulk-add-by-expansion`
Bulk add by expansion (Merchants only).

**Request Body:** BulkAddByExpansionRequest

**Response (200):** BulkImportResponse

#### POST `/api/inventory/bulk-add-by-templates`
Bulk add by template IDs (Merchants only).

**Request Body:** BulkAddByTemplateIdsRequest

**Response (200):** BulkImportResponse

### 8. Reward Controller (`/api/rewards`)

#### GET `/api/rewards`
Get all active rewards.

**Response (200):** Array of Reward objects

#### GET `/api/rewards/partner/{partnerId}`
Get rewards by partner.

**Path Parameters:**
- `partnerId` (Long): Partner ID

**Response (200):** Array of Reward objects

#### GET `/api/rewards/{id}`
Get reward by ID.

**Path Parameters:**
- `id` (Long): Reward ID

**Response (200):** Reward object

#### POST `/api/rewards/{id}/redeem`
Redeem a reward.

**Path Parameters:**
- `id` (Long): Reward ID

**Response (200):**
```json
{
  "message": "Reward redeemed successfully"
}
```

#### GET `/api/rewards/points`
Get user points.

**Response (200):**
```json
{
  "points": 150
}
```

#### GET `/api/rewards/history`
Get transaction history.

**Response (200):** Array of RewardTransaction objects

#### POST `/api/rewards`
Create reward (Admin only).

**Request Body:** Reward object

**Response (200):** Created Reward object

### 9. Expansion Controller (`/api/expansions`)

#### GET `/api/expansions`
Get all expansions.

**Response (200):** Array of ExpansionDTO objects

#### GET `/api/expansions/{id}/cards`
Get cards for expansion.

**Path Parameters:**
- `id` (Long): Expansion ID

**Response (200):** Array of CardTemplate objects

#### GET `/api/expansions/recent`
Get recent expansions.

**Query Parameters:**
- `limit` (int, default: 5): Number of expansions

**Response (200):** Array of ExpansionDTO objects

#### GET `/api/expansions/stats`
Get TCG statistics.

**Response (200):** Array of TCGStatsDTO objects

#### POST `/api/expansions`
Create expansion.

**Request Body:** Expansion object

**Response (200):** ExpansionDTO object

#### PUT `/api/expansions/{id}`
Update expansion.

**Path Parameters:**
- `id` (Long): Expansion ID

**Request Body:** Expansion object

**Response (200):** ExpansionDTO object

#### DELETE `/api/expansions/{id}`
Delete expansion.

**Path Parameters:**
- `id` (Long): Expansion ID

**Response (204):** No content

### 10. TCG Set Controller (`/api/sets`)

#### GET `/api/sets`
Get all sets.

**Response (200):** Array of TCGSet objects

#### GET `/api/sets/{id}`
Get set by ID.

**Path Parameters:**
- `id` (Long): Set ID

**Response (200):** TCGSet object

#### GET `/api/sets/{id}/cards`
Get card templates by set ID.

**Path Parameters:**
- `id` (Long): Set ID

**Query Parameters:**
- `page` (int, default: 0): Page number
- `size` (int, default: 10): Items per page

**Response (200):** Page of CardTemplate objects

#### GET `/api/sets/code/{setCode}/cards`
Get cards by set code.

**Path Parameters:**
- `setCode` (String): Set code

**Response (200):** Array of Card objects

#### POST `/api/sets`
Create set.

**Request Body:** TCGSet object

**Response (200):** Created TCGSet object

#### PUT `/api/sets/{id}`
Update set.

**Path Parameters:**
- `id` (Long): Set ID

**Request Body:** TCGSet object

**Response (200):** Updated TCGSet object

#### DELETE `/api/sets/{id}`
Delete set.

**Path Parameters:**
- `id` (Long): Set ID

**Response (204):** No content

### 11. User Card Controller (`/api/cards`)

#### GET `/api/cards/collection`
Get user's card collection.

**Response (200):** Array of UserCard objects

#### POST `/api/cards/{cardTemplateId}/add-to-collection`
Add card template to collection.

**Path Parameters:**
- `cardTemplateId` (Long): Card template ID

**Query Parameters:**
- `condition` (String, default: "NEAR_MINT"): Card condition

**Response (200):** UserCard object

#### DELETE `/api/cards/collection/{userCardId}`
Remove card from collection.

**Path Parameters:**
- `userCardId` (Long): User card ID

**Response (204):** No content

#### PUT `/api/cards/collection/{userCardId}`
Update user card details.

**Path Parameters:**
- `userCardId` (Long): User card ID

**Request Body:** UserCardUpdateDto

**Response (200):** Updated UserCard object

### 12. Achievement Controller (`/api/achievements`)

#### GET `/api/achievements`
Get all active achievements.

**Response (200):** Array of Achievement objects

#### GET `/api/achievements/{id}`
Get achievement by ID.

**Path Parameters:**
- `id` (Long): Achievement ID

**Response (200):** Achievement object

#### GET `/api/achievements/user`
Get user achievements.

**Response (200):** Array of UserAchievement objects

#### POST `/api/achievements`
Create achievement (Admin only).

**Request Body:** Achievement object

**Response (200):** Created Achievement object

#### POST `/api/achievements/{id}/unlock`
Unlock achievement for user.

**Path Parameters:**
- `id` (Long): Achievement ID

**Response (200):**
```json
{
  "message": "Achievement unlocked successfully"
}
```

### 13. Admin Controller (`/api/admin`)

#### GET `/api/admin/shops`
Get all shops (Admin only).

**Response (200):** Array of Shop objects

#### GET `/api/admin/shops/pending`
Get pending shops (Admin only).

**Response (200):** Array of Shop objects

#### POST `/api/admin/shops/{id}/activate`
Activate shop (Admin only).

**Path Parameters:**
- `id` (Long): Shop ID

**Response (200):**
```json
{
  "success": true,
  "message": "Shop activated successfully",
  "shop": {...}
}
```

#### POST `/api/admin/shops/{id}/deactivate`
Deactivate shop (Admin only).

**Path Parameters:**
- `id` (Long): Shop ID

**Response (200):**
```json
{
  "success": true,
  "message": "Shop deactivated successfully",
  "shop": {...}
}
```

#### GET `/api/admin/shops/stats`
Get shop statistics (Admin only).

**Response (200):**
```json
{
  "total": 10,
  "active": 8,
  "pending": 2,
  "verified": 7
}
```

#### PUT `/api/admin/shops/{id}`
Update shop (Super Admin only).

**Path Parameters:**
- `id` (Long): Shop ID

**Request Body:** Shop object

**Response (200):**
```json
{
  "success": true,
  "message": "Shop updated successfully",
  "shop": {...}
}
```

#### POST `/api/admin/rewards`
Create reward (Admin only).

**Request Body:** Reward object

**Response (200):**
```json
{
  "success": true,
  "message": "Reward created successfully",
  "reward": {...}
}
```

#### PUT `/api/admin/rewards/{id}`
Update reward (Admin only).

**Path Parameters:**
- `id` (Long): Reward ID

**Request Body:** Reward object

**Response (200):**
```json
{
  "success": true,
  "message": "Reward updated successfully",
  "reward": {...}
}
```

#### DELETE `/api/admin/rewards/{id}`
Delete reward (Admin only).

**Path Parameters:**
- `id` (Long): Reward ID

**Response (200):**
```json
{
  "success": true,
  "message": "Reward deleted successfully"
}
```

#### POST `/api/admin/achievements`
Create achievement (Admin only).

**Request Body:** Achievement object

**Response (200):**
```json
{
  "success": true,
  "message": "Achievement created successfully",
  "achievement": {...}
}
```

#### PUT `/api/admin/achievements/{id}`
Update achievement (Admin only).

**Path Parameters:**
- `id` (Long): Achievement ID

**Request Body:** Achievement object

**Response (200):**
```json
{
  "success": true,
  "message": "Achievement updated successfully",
  "achievement": {...}
}
```

#### DELETE `/api/admin/achievements/{id}`
Delete achievement (Admin only).

**Path Parameters:**
- `id` (Long): Achievement ID

**Response (200):**
```json
{
  "success": true,
  "message": "Achievement deleted successfully"
}
```

#### POST `/api/admin/import/{tcgType}`
Trigger batch import (Admin only).

**Path Parameters:**
- `tcgType` (TCGType): TCG type

**Query Parameters:**
- `startIndex` (int, default: -99): Start index
- `endIndex` (int, default: -99): End index

**Response (200):** Success message

### 14. Batch Controller (`/api/batch`)

#### POST `/api/batch/import/{tcgType}`
Trigger legacy batch import.

**Path Parameters:**
- `tcgType` (String): TCG type

**Query Parameters:**
- `startIndex` (int, default: -99): Start index
- `endIndex` (int, default: -99): End index

**Response (200):**
```json
{
  "success": true,
  "message": "Batch import started for POKEMON",
  "tcgType": "POKEMON"
}
```

#### POST `/api/batch/justtcg/{tcgType}`
Trigger JustTCG import.

**Path Parameters:**
- `tcgType` (String): TCG type

**Response (200):**
```json
{
  "success": true,
  "message": "JustTCG import started for POKEMON",
  "tcgType": "POKEMON"
}
```

#### GET `/api/batch/justtcg/supported`
Get supported TCG types.

**Response (200):**
```json
{
  "supportedTypes": ["POKEMON", "MAGIC"],
  "count": 2
}
```

#### GET `/api/batch/justtcg/games`
Get available JustTCG games.

**Response (200):**
```json
{
  "success": true,
  "games": [...],
  "count": 5
}
```

### 15. Customer Request Controller (`/api/requests`)

#### POST `/api/requests`
Create new request.

**Request Body:** CreateRequestRequest

**Response (201):** CustomerRequestSummaryDTO

#### GET `/api/requests`
Get requests with filters.

**Query Parameters:**
- `shopId` (String): Shop ID
- `userId` (String): User ID
- `status` (String): Request status
- `type` (String): Request type
- `page` (int, default: 0): Page number
- `size` (int, default: 20): Items per page

**Response (200):** RequestListResponse

#### GET `/api/requests/{id}`
Get single request.

**Path Parameters:**
- `id` (String): Request ID

**Response (200):** CustomerRequestSummaryDTO

#### PUT `/api/requests/{id}/status`
Update request status (Merchant only).

**Path Parameters:**
- `id` (String): Request ID

**Query Parameters:**
- `shopId` (String): Shop ID

**Request Body:** UpdateRequestStatusRequest

**Response (200):** CustomerRequestSummaryDTO

#### POST `/api/requests/{id}/cancel`
Cancel request.

**Path Parameters:**
- `id` (String): Request ID

**Response (200):** CustomerRequestSummaryDTO

#### GET `/api/requests/{id}/messages`
Get messages for request.

**Path Parameters:**
- `id` (String): Request ID

**Response (200):** MessageListResponse

#### POST `/api/requests/{id}/messages`
Send message as user.

**Path Parameters:**
- `id` (String): Request ID

**Request Body:** SendMessageRequest

**Response (201):** RequestMessage

#### POST `/api/requests/{id}/messages/merchant`
Send message as merchant.

**Path Parameters:**
- `id` (String): Request ID

**Query Parameters:**
- `shopId` (String): Shop ID

**Request Body:** SendMessageRequest

**Response (201):** RequestMessage

#### POST `/api/requests/{id}/read`
Mark request as read.

**Path Parameters:**
- `id` (String): Request ID

**Response (200):** No content

#### GET `/api/requests/stats`
Get request statistics (Merchant only).

**Query Parameters:**
- `shopId` (String): Shop ID

**Response (200):** RequestStatsResponse

### 16. Health Controller (`/health`)

#### GET `/health`
Health check endpoint.

**Response (200):**
```json
{
  "status": "UP",
  "timestamp": "2023-12-01T10:00:00",
  "service": "TCG Arena Backend"
}
```

### 17. Image Controller (`/api/images`)

#### POST `/api/images/upload`
Upload image.

**Request Body:** Multipart file + entity info

**Response (200):** Image object

#### GET `/api/images/entity/{entityType}/{entityId}`
Get images by entity.

**Path Parameters:**
- `entityType` (String): Entity type
- `entityId` (Long): Entity ID

**Response (200):** Array of Image objects

#### GET `/api/images/user`
Get user's images.

**Response (200):** Array of Image objects

#### DELETE `/api/images/{id}`
Delete image.

**Path Parameters:**
- `id` (Long): Image ID

**Response (200):**
```json
{
  "message": "Image deleted successfully"
}
```

### 18. Merchant Backoffice Controller (`/api/merchant`)

#### GET `/api/merchant/shop/status`
Get merchant shop status.

**Response (200):**
```json
{
  "shop": {...},
  "active": true,
  "verified": true,
  "user": {...}
}
```

#### GET `/api/merchant/dashboard/stats`
Get dashboard statistics.

**Response (200):** MerchantDashboardStatsDTO

#### GET `/api/merchant/profile`
Get merchant profile.

**Response (200):** User object

#### PUT `/api/merchant/shop`
Update shop information.

**Request Body:** Shop object

**Response (200):**
```json
{
  "success": true,
  "message": "Shop updated successfully",
  "shop": {...}
}
```

#### POST `/api/merchant/shop/photo`
Upload shop photo.

**Request Body:**
```json
{
  "photoBase64": "data:image/jpeg;base64,..."
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Photo uploaded successfully"
}
```

### 19. Notification Controller (`/api/notifications`)

#### GET `/api/notifications`
Get user notifications.

**Response (200):** Array of Notification objects

#### GET `/api/notifications/unread`
Get unread notifications.

**Response (200):** Array of Notification objects

#### PUT `/api/notifications/{id}/read`
Mark notification as read.

**Path Parameters:**
- `id` (Long): Notification ID

**Response (200):**
```json
{
  "message": "Notification marked as read"
}
```

#### POST `/api/notifications/device-token`
Register device token.

**Request Body:**
```json
{
  "token": "device_token",
  "platform": "ios"
}
```

**Response (200):**
```json
{
  "message": "Device token registered"
}
```

#### DELETE `/api/notifications/device-token`
Unregister device token.

**Query Parameters:**
- `token` (String): Device token

**Response (200):**
```json
{
  "message": "Device token unregistered"
}
```

#### POST `/api/notifications/test-push`
Send test push notification.

**Response (200):**
```json
{
  "message": "Test notification sent"
}
```

#### POST `/api/notifications/shop/{shopId}/broadcast`
Send notification to shop subscribers (Merchant only).

**Path Parameters:**
- `shopId` (Long): Shop ID

**Request Body:**
```json
{
  "title": "Shop Update",
  "message": "New cards arrived!"
}
```

**Response (200):**
```json
{
  "message": "Notification sent to shop subscribers"
}
```

### 20. Partner Controller (`/api/partners`)

#### GET `/api/partners`
Get all active partners.

**Response (200):** Array of Partner objects

#### GET `/api/partners/all`
Get all partners (Admin only).

**Response (200):** Array of Partner objects

#### GET `/api/partners/{id}`
Get partner by ID.

**Path Parameters:**
- `id` (Long): Partner ID

**Response (200):** Partner object

#### POST `/api/partners`
Create/update partner (Admin only).

**Request Body:** Partner object

**Response (200):** Partner object

#### DELETE `/api/partners/{id}`
Delete partner (Admin only).

**Path Parameters:**
- `id` (Long): Partner ID

**Response (200):** No content

### 21. Pro Deck Controller (`/api/pro-decks`)

#### GET `/api/pro-decks`
Get all pro decks.

**Response (200):** Array of ProDeck objects

#### GET `/api/pro-decks/{id}`
Get pro deck by ID.

**Path Parameters:**
- `id` (Long): Pro deck ID

**Response (200):** ProDeck object

#### GET `/api/pro-decks/recent`
Get recent pro decks.

**Response (200):** Array of ProDeck objects

### 22. Reservation Controller (`/api/reservations`)

#### GET `/api/reservations`
Get reservations by card ID.

**Query Parameters:**
- `cardId` (String): Card ID
- `merchantId` (String): Merchant ID
- `page` (int, default: 0): Page number
- `size` (int, default: 50): Items per page

**Response (200):** ReservationListResponse

#### POST `/api/reservations`
Create reservation.

**Request Body:** CreateReservationRequest

**Response (201):** ReservationResponse

#### GET `/api/reservations/my`
Get user's reservations.

**Query Parameters:**
- `shopId` (Long): Shop ID filter
- `page` (int, default: 0): Page number
- `size` (int, default: 20): Items per page

**Response (200):** ReservationListResponse

#### GET `/api/reservations/user`
Get user's reservations.

**Query Parameters:**
- `page` (int, default: 0): Page number
- `size` (int, default: 20): Items per page

**Response (200):** ReservationListResponse

#### GET `/api/reservations/merchant`
Get merchant's reservations.

**Query Parameters:**
- `shopId` (Long): Shop ID
- `status` (ReservationStatus, default: PENDING): Status filter
- `page` (int, default: 0): Page number
- `size` (int, default: 20): Items per page

**Response (200):** ReservationListResponse

#### POST `/api/reservations/validate`
Validate reservation (Merchant only).

**Query Parameters:**
- `shopId` (Long): Shop ID

**Request Body:** ValidateReservationRequest

**Response (200):** ReservationResponse

#### PUT `/api/reservations/{id}/validate`
Validate reservation by ID.

**Path Parameters:**
- `id` (String): Reservation ID

**Query Parameters:**
- `shopId` (Long): Shop ID

**Response (200):** ReservationResponse

#### PUT `/api/reservations/{id}/complete`
Complete pickup.

**Path Parameters:**
- `id` (String): Reservation ID

**Query Parameters:**
- `shopId` (Long): Shop ID

**Response (200):** ReservationResponse

#### PUT `/api/reservations/{id}/cancel`
Cancel reservation.

**Path Parameters:**
- `id` (String): Reservation ID

**Response (200):** ReservationResponse

### 23. Shop News Controller (`/api`)

#### GET `/api/shops/{shopId}/news`
Get active news for shop (Public).

**Path Parameters:**
- `shopId` (Long): Shop ID

**Response (200):** Array of ShopNewsDTO objects

#### GET `/api/merchant/shops/{shopId}/news`
Get all news for merchant's shop.

**Path Parameters:**
- `shopId` (Long): Shop ID

**Response (200):** Array of ShopNewsDTO objects

#### GET `/api/merchant/shops/{shopId}/news/active`
Get active news for merchant's shop.

**Path Parameters:**
- `shopId` (Long): Shop ID

**Response (200):** Array of ShopNewsDTO objects

#### GET `/api/merchant/shops/{shopId}/news/future`
Get future news for merchant's shop.

**Path Parameters:**
- `shopId` (Long): Shop ID

**Response (200):** Array of ShopNewsDTO objects

#### GET `/api/merchant/shops/{shopId}/news/expired`
Get expired news for merchant's shop.

**Path Parameters:**
- `shopId` (Long): Shop ID

**Response (200):** Array of ShopNewsDTO objects

#### POST `/api/merchant/shops/{shopId}/news`
Create news item.

**Path Parameters:**
- `shopId` (Long): Shop ID

**Request Body:** ShopNewsDTO

**Response (201):** ShopNewsDTO

#### PUT `/api/merchant/shops/{shopId}/news/{newsId}`
Update news item.

**Path Parameters:**
- `shopId` (Long): Shop ID
- `newsId` (Long): News ID

**Request Body:** ShopNewsDTO

**Response (200):** ShopNewsDTO

#### DELETE `/api/merchant/shops/{shopId}/news/{newsId}`
Delete news item.

**Path Parameters:**
- `shopId` (Long): Shop ID
- `newsId` (Long): News ID

**Response (200):**
```json
{
  "message": "Notizia eliminata con successo"
}
```

### 24. User Activity Controller (`/api/user-activities`)

#### GET `/api/user-activities`
Get current user's activities.

**Query Parameters:**
- `limit` (int, default: 20): Number of activities

**Response (200):** Array of UserActivityDTO objects

#### GET `/api/user-activities/{userId}`
Get user activities.

**Path Parameters:**
- `userId` (Long): User ID

**Query Parameters:**
- `limit` (int, default: 20): Number of activities

**Response (200):** Array of UserActivityDTO objects

#### GET `/api/user-activities/recent/global`
Get recent global activities.

**Query Parameters:**
- `limit` (int, default: 50): Number of activities

**Response (200):** Array of UserActivityDTO objects

### 25. Waiting List Controller (`/api/waiting-list`)

#### POST `/api/waiting-list/join`
Join waiting list.

**Request Body:** WaitingListRequestDTO

**Response (200):** WaitingListResponseDTO

#### GET `/api/waiting-list/all`
Get all entries (Admin only).

**Response (200):** Array of WaitingListEntry objects

#### GET `/api/waiting-list/uncontacted`
Get uncontacted entries (Admin only).

**Response (200):** Array of WaitingListEntry objects

#### PUT `/api/waiting-list/{id}/contacted`
Mark as contacted (Admin only).

**Path Parameters:**
- `id` (Long): Entry ID

**Response (200):** No content

## Error Responses

All endpoints may return the following error responses:

**400 Bad Request:**
```json
{
  "error": "Error message"
}
```

**401 Unauthorized:**
```json
{
  "error": "Authentication required"
}
```

**403 Forbidden:**
```json
{
  "error": "Insufficient permissions"
}
```

**404 Not Found:**
```json
{
  "error": "Resource not found"
}
```

**500 Internal Server Error:**
```json
{
  "error": "Internal server error"
}
```

## Data Models

### User
```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "displayName": "John Doe",
  "isMerchant": false,
  "isAdmin": false,
  "points": 100,
  "favoriteTCGTypes": ["POKEMON", "MAGIC"],
  "profileImageUrl": "https://example.com/image.jpg",
  "deviceToken": "device_token_string",
  "isPrivate": false,
  "dateJoined": "2023-01-01T00:00:00"
}
```

### CardTemplate
```json
{
  "id": 1,
  "name": "Charizard",
  "tcgType": "POKEMON",
  "rarity": "RARE",
  "setCode": "BASE1",
  "cardNumber": "4/102",
  "marketPrice": 25.99,
  "imageUrl": "https://example.com/card.jpg"
}
```

### Deck
```json
{
  "id": 1,
  "name": "My Pokemon Deck",
  "description": "A powerful fire deck",
  "tcgType": "POKEMON",
  "deckType": "DECK",
  "ownerId": 1,
  "cards": [...]
}
```

### Tournament
```json
{
  "id": 1,
  "name": "Regional Championship",
  "description": "Monthly regional tournament",
  "tcgType": "POKEMON",
  "maxParticipants": 64,
  "entryFee": 10.00,
  "prizePool": 500.00,
  "startDate": "2023-12-15T14:00:00",
  "location": "Convention Center",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "organizerId": 2,
  "status": "UPCOMING",
  "isRanked": false
}
```

### Shop
```json
{
  "id": 1,
  "name": "Game Store Pro",
  "description": "Best TCG cards in town",
  "address": "123 Main St, City, State 12345",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "phoneNumber": "+1-555-0123",
  "email": "contact@gamestore.com",
  "websiteUrl": "https://gamestore.com",
  "openingHours": "Mon-Fri 9AM-9PM, Sat-Sun 10AM-8PM",
  "openingDays": "Monday,Tuesday,Wednesday,Thursday,Friday,Saturday,Sunday",
  "instagramUrl": "https://instagram.com/gamestore",
  "facebookUrl": "https://facebook.com/gamestore",
  "twitterUrl": "https://twitter.com/gamestore",
  "type": "LOCAL_STORE",
  "tcgTypes": ["POKEMON", "MAGIC"],
  "services": ["CARD_TRADING", "DECK_BUILDING"],
  "photoBase64": "data:image/jpeg;base64,...",
  "active": true,
  "isVerified": true,
  "ownerId": 2,
  "reservationDurationMinutes": 30
}
```

This documentation covers all 194+ endpoints across 25 controllers in the TCG Arena backend. Use this as a comprehensive reference for developing the Android application.</content>
<parameter name="filePath">/Users/patriziopezzilli/Documents/Sviluppo/TCGArena-BE/TCG_Arena_Backend_API_Documentation.md