# TeleDrive Feature Test Status

Use this file as the shared test log. When you test a feature, send me the result and I will update the status, notes, reproduction steps, and fix priority here.

Status values:
- `Untested`
- `Works`
- `Does not work`
- `Partially works`
- `Blocked`

## Current Test Matrix

| Area | Feature | Status | Notes | Priority |
| --- | --- | --- | --- | --- |
| App startup | Launch app from launcher | Untested |  |  |
| App startup | App stays open without crash | Untested |  |  |
| Authentication | Enter Telegram API ID and hash | Untested |  |  |
| Authentication | Phone number login | Untested |  |  |
| Authentication | Verification code login | Untested |  |  |
| Authentication | Telegram 2FA password login | Untested |  |  |
| Security | Create master password | Does not work | Latest error: `Serializer for class KeyEntry is not found`; `KeyEntry` needs kotlinx serialization support. | P0 |
| Security | Unlock with master password | Untested |  |  |
| Security | Change master password | Untested |  |  |
| Security | Biometric unlock | Untested |  |  |
| Security | Auto-lock after inactivity | Untested |  |  |
| Security | Clear stored keys or reset security | Untested |  |  |
| Drive | Load root drive / Saved Messages | Untested |  |  |
| Drive | Refresh file list | Untested |  |  |
| Drive | Create folder / private channel | Untested |  |  |
| Drive | Open folder | Untested |  |  |
| Drive | Delete folder | Untested |  |  |
| Files | Upload document | Does not work | WorkManager reports it is not initialized because the manifest disables the default initializer. | P0 |
| Files | Upload image | Does not work | Same upload path as documents; blocked by WorkManager initialization. | P0 |
| Files | Upload video | Does not work | Same upload path as documents; blocked by WorkManager initialization. | P0 |
| Files | Upload large file | Does not work | Same upload path as documents; blocked by WorkManager initialization. | P0 |
| Files | Download file | Untested |  |  |
| Files | Open downloaded file | Untested |  |  |
| Files | Delete file | Untested |  |  |
| Files | Move file to another folder | Untested |  |  |
| Files | Multi-select files | Untested |  |  |
| Search | Search current folder | Untested |  |  |
| Search | Search across folders | Untested |  |  |
| Transfers | Upload progress display | Untested |  |  |
| Transfers | Download progress display | Untested |  |  |
| Transfers | Multiple transfer queue | Untested |  |  |
| Transfers | Retry failed transfer | Untested |  |  |
| Preview | File metadata preview | Untested |  |  |
| Preview | Image thumbnail preview | Untested | Known pending/limited in README. |  |
| Preview | Video/audio preview | Untested | Known pending in README. |  |
| Preview | PDF preview | Untested | Known pending in README. |  |
| Backup | Manual or scheduled backup worker | Untested |  |  |
| Backup | Backup manifest creation | Untested |  |  |
| Backup | Backup detects changed files | Untested |  |  |
| Theme | Light mode | Untested |  |  |
| Theme | Dark mode | Untested |  |  |
| Theme | Dynamic Material You colors | Untested | Android 12+ only. |  |
| Permissions | Storage/file picker permission flow | Untested |  |  |
| Permissions | Notification permission flow | Untested | Android 13+ only. |  |
| Stability | Rotate screen / configuration change | Untested |  |  |
| Stability | Background and reopen app | Untested |  |  |
| Stability | Logout or relaunch after force stop | Untested |  |  |
| Drive | Background auto-refresh | Does not work | Refresh runs every 15 seconds but visibly flashes/pulls the app UI. | P1 |

## Issues To Fix

Add broken or partial items here as we discover them.

| ID | Feature | Status | Device / Android | Steps to reproduce | Expected | Actual | Evidence | Fix status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| ISSUE-001 | File upload | Does not work | Not provided | Pick a file to upload. | Upload starts in WorkManager and shows transfer progress. | App shows WorkManager is not initialized because the manifest explicitly disabled the initializer. | User report, 2026-05-09 | Fixed in code; needs retest |
| ISSUE-002 | Background auto-refresh | Does not work | Not provided | Stay on drive screen and wait about 15 seconds. | File list refreshes quietly in the background. | UI flashes briefly like a full refresh/pull refresh. | User report, 2026-05-09 | Fixed in code; needs retest |
| ISSUE-003 | Create master password | Does not work | CPH2421 / Android not provided | Settings -> Security Settings -> Set Master Password -> enter matching password -> Set password. | Password is saved and Security screen shows active master password. | App first exited to homescreen due to JNA; after that fix, showed missing serializer for `KeyEntry`. | User report and logcat. | Serializer fix in code; needs retest |

## Working Features

Add confirmed working items here.

| Feature | Device / Android | Notes | Confirmed on |
| --- | --- | --- | --- |

## Test Session Notes

- 2026-05-09: Created tracker before manual feature testing.
- 2026-05-09: User reported upload blocked by WorkManager initialization and visible flashing during 15-second auto-refresh.
- 2026-05-09: User reported master password exits to homescreen and does not save. Root cause confirmed through logcat as missing JNA native dispatch library in LazySodium path.
- 2026-05-09: User reported follow-up master password error: `Serializer for class KeyEntry is not found`.
