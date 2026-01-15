# Fix: Preserve Manual Date Changes During Batch Import

## Problem
When administrators manually change release dates for sets/expansions through the admin dashboard, the nightly batch import process would overwrite these changes with data from external APIs (TCGPlayer, etc.), reverting the dates back to their original values.

## Root Cause
The batch import process (`TCGApiClient.importCardsForTCG()`) was updating all set fields including `releaseDate` without checking if the date had been modified manually by administrators.

## Solution Implemented

### 1. Database Schema Changes
- Added `release_date_modified_manually` BOOLEAN column to `tcg_sets` table
- Default value: `FALSE`
- Migration script: `add_release_date_manual_flag.sql`

### 2. Entity Model Updates
- **TCGSet.java**: Added `releaseDateModifiedManually` field with getters/setters
- **TCGSetDTO.java**: Added corresponding field to DTO for API responses

### 3. Import Logic Protection
- **TCGApiClient.java**: Modified `getOrCreateTCGSet()` method to preserve manually modified release dates
- When importing sets, if `releaseDateModifiedManually` is `true`, the existing date is preserved
- Other fields (name, cardCount) can still be updated from API data

### 4. Admin Dashboard Integration
- **TCGSetService.java**: Modified `updateSet()` method to set `releaseDateModifiedManually = true` when release date is changed via admin dashboard
- Only sets flag when the date actually changes (compared to existing value)

## How It Works

1. **Manual Edit**: When admin changes a release date via dashboard, `releaseDateModifiedManually` is set to `true`
2. **Batch Import**: During nightly import, the system checks this flag:
   - If `true`: Preserves the manually set date
   - If `false`: Updates date from API data
3. **Logging**: Debug logs show when manual dates are preserved vs API dates are used

## Files Modified
- `TCGSet.java` - Added manual modification flag
- `TCGSetDTO.java` - Added field to DTO
- `TCGSetService.java` - Set flag on manual updates
- `TCGApiClient.java` - Preserve manual dates during import
- `add_release_date_manual_flag.sql` - Database migration

## Testing
1. Run the migration script to add the new column
2. Manually change a set's release date in admin dashboard
3. Run batch import - date should be preserved
4. Verify other fields (name, card count) still update from API

## Benefits
- ✅ Manual admin changes are preserved
- ✅ Automatic imports still work for unchanged data
- ✅ Clear audit trail of manual modifications
- ✅ No breaking changes to existing functionality