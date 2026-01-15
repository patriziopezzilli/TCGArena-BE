# Tournament Reminder Notifications Implementation

## Overview
Implemented a scheduled batch job that sends push notifications to tournament participants when their registered tournaments are about to start (within 30 minutes).

## Technical Details

### New Components

#### 1. TournamentReminderScheduler
- **Location**: `src/main/java/com/tcg/arena/scheduler/TournamentReminderScheduler.java`
- **Schedule**: Every 10 minutes (`fixedRate = 600000`)
- **Logic**:
  - Finds tournaments starting within 30 minutes
  - Excludes tournaments already reminded in the last 2 hours
  - Sends personalized notifications to all registered participants
  - Updates `lastReminderSentAt` timestamp to prevent duplicates

#### 2. Database Changes
- **New Column**: `last_reminder_sent_at` in `tournaments` table
- **Migration**: `add_tournament_reminder_tracking.sql`
- **Purpose**: Prevents sending duplicate notifications for the same tournament

#### 3. Repository Methods
- **TournamentRepository**: `findTournamentsStartingWithinMinutes()` - finds tournaments needing reminders
- **TournamentParticipantRepository**: `findByTournamentIdsWithUserDetails()` - gets participants with user details

### Notification Logic

#### Timing
- **Check Frequency**: Every 10 minutes
- **Reminder Window**: 30 minutes before tournament start
- **Cooldown**: 2 hours between reminders for the same tournament

#### Message Personalization
- **5 minutes or less**: "⚡ Il torneo 'X' inizia tra Y minuti alle HH:mm! Preparati!"
- **5-15 minutes**: "⏰ Il torneo 'X' inizia tra Y minuti alle HH:mm. È ora di prepararsi!"
- **15-30 minutes**: "🏆 Il torneo 'X' a cui sei iscritto inizia tra Y minuti alle HH:mm."

#### Safety Features
- **Scheduler Lock**: Prevents duplicate job executions
- **Transactional**: Ensures data consistency
- **Error Handling**: Continues processing even if individual notifications fail
- **Rate Limiting**: 50ms delay between notifications to avoid overwhelming Firebase

### Database Schema Changes

```sql
ALTER TABLE tournaments ADD COLUMN last_reminder_sent_at TIMESTAMP NULL;
CREATE INDEX idx_tournaments_last_reminder_sent_at ON tournaments(last_reminder_sent_at);
```

### Configuration
- Uses existing `@EnableScheduling` configuration
- Integrates with existing `NotificationService` and `FirebaseMessagingService`
- Uses `SchedulerLockService` for distributed locking

## Benefits
- **User Experience**: Participants get timely reminders regardless of check-in status
- **Engagement**: Increases tournament attendance and participation
- **Scalability**: Batch processing handles multiple tournaments efficiently
- **Reliability**: Prevents notification spam with cooldown mechanisms