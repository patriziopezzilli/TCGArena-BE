# TCG Arena Backend

Backend for TCG Arena app, providing card data for Pokémon, Magic: The Gathering, and One Piece TCG.

## Features

- REST APIs to retrieve card information
- Batch processing to populate database with card data from external APIs
- Scheduled nightly updates

## Technologies

- Spring Boot
- Maven
- PostgreSQL
- Spring Data JPA
- Spring Batch
- Spring WebFlux (for API calls)
- Spring Cache with Caffeine (for performance optimization)
- Image compression utilities

## Trade Radar Feature
A GPS-based matching system for trading cards.
- **Documentation**: See [TRADE_RADAR_IMPLEMENTATION.md](TRADE_RADAR_IMPLEMENTATION.md) for full details on architecture, API, and testing.

## Setup

1. Install PostgreSQL and create a database named `tcg_arena`.
2. Update `src/main/resources/application.properties` with your database credentials.
3. **For production/API integration**: Set the Pokemon TCG API key as environment variable:
   ```bash
   export POKEMON_TCG_API_KEY=your_api_key_here
   ```
   Get your API key from: https://dev.pokemontcg.io/
4. Run `mvn spring-boot:run` to start the application.

## Production Configuration

For production deployment, use the `prod` profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

This enables:
- Batch job execution
- API integrations with external services
- Enhanced logging for batch operations

## Environment Variables

- `POKEMON_TCG_API_KEY`: Required for Pokemon TCG API integration (get from https://dev.pokemontcg.io/)

## APIs

- GET /api/cards - Get all cards
- GET /api/cards/game/{game} - Get cards by game (pokemon, magic, onepiece)
- POST /api/cards - Create a new card

## Batch Processing & API Integration

The application includes automated batch processing for fetching card data from external APIs:

### Scheduled Jobs
- **Nightly Import**: Runs every night at 2 AM to fetch and update Pokemon card data
- **Configurable**: Schedule can be modified in `CardImportScheduler.java`

### Supported APIs
- **Pokémon TCG API**: Primary integration for Pokemon cards
  - Fetches card details, images, rarities, and set information
  - Handles pagination and rate limiting
  - Updates existing cards and adds new ones
- **Scryfall API**: For Magic: The Gathering (planned)
- **One Piece TCG API**: For One Piece cards (planned)

### Data Flow
1. Scheduler triggers batch job at configured time
2. TCGApiClient fetches data from external APIs
3. Cards are parsed and validated
4. Existing cards are updated, new cards are inserted
5. Expansions/Sets are created as needed

### Manual Execution
To run the import job manually:
```bash
curl -X POST http://localhost:8080/actuator/batch/job/importCardsJob
```

## Performance Optimizations

The application includes several performance optimizations to ensure efficient operation:

### Caching Layer
- **Spring Cache with Caffeine**: In-memory caching for frequently accessed data
- **User Statistics**: Cached for 30 minutes to reduce database load
- **Leaderboards**: Cached with automatic invalidation on updates
- **Configuration**: 1000 max entries, 30-minute expiration

### Database Query Optimization
- **UserStats Queries**: Optimized with LIMIT clauses for leaderboard queries
- **Indexed Fields**: Proper indexing on frequently queried columns
- **Connection Pooling**: Configured for optimal database connections

### Image Compression
- **Automatic Compression**: Images are compressed before storage to reduce file sizes
- **Format Support**: JPEG, PNG, WebP compression
- **Quality Preservation**: Maintains visual quality while reducing storage requirements

### Monitoring
Cache hit rates and performance metrics can be monitored through Spring Boot Actuator endpoints.