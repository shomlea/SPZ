# SPZ – Local database

The app keeps its data in a local SQLite database (`spz.db`), accessed through Room.
Imported book files are copied into the app's private storage (`filesDir/books/`); the database only stores their path.

## Schema

### books
| Column        | Type    | Notes                                     |
|---------------|---------|-------------------------------------------|
| id            | INTEGER | primary key, auto-generated               |
| title         | TEXT    | file name without extension when imported |
| author        | TEXT    | empty for imported books                  |
| mediaType     | TEXT    | `EBOOK` or `AUDIOBOOK`                    |
| filePath      | TEXT    | path of the copied file, empty for samples|
| fileSizeBytes | INTEGER | used to detect duplicates                 |
| position      | INTEGER | current page / position, starts at 0      |

### reflections
| Column | Type    | Notes                                         |
|--------|---------|-----------------------------------------------|
| id     | INTEGER | primary key, auto-generated                   |
| bookId | INTEGER | foreign key → `books.id`, `ON DELETE CASCADE` |
| text   | TEXT    | the user's takeaway                           |
| rating | INTEGER | 1 to 5                                        |

### daily_progress
| Column           | Type    | Notes                     |
|------------------|---------|---------------------------|
| date             | TEXT    | primary key, `yyyy-MM-dd` |
| readingMinutes   | INTEGER |                           |
| listeningMinutes | INTEGER |                           |

### Relations
- `books` 1 — N `reflections`: a book can have many reflections. Deleting a book deletes its reflections.
- `daily_progress` is independent: one row per day.

The two sample books are inserted once, when the database is first created.

## Data flow

```
 Compose screen  ──(button click)──►  ViewModel  ──►  AppRepository  ──►  DAO  ──►  Room / SQLite
       ▲                                  │                                         │
       └────────── StateFlow ◄────────────┴────────────── Flow ◄────────────────────┘
```

1. The user taps a button; the screen calls a ViewModel function.
2. The ViewModel launches a coroutine and calls the repository.
3. The repository calls a DAO, which runs the SQL query.
4. DAO queries return `Flow`, so when a table changes, Room sends the new data back up.
   The ViewModel turns it into a `StateFlow` and the screen redraws on its own.

### Importing a book

```
 "Add book" ──► system file picker ──► LibraryViewModel.importBook(uri)
                                              │
                                              ▼
                                 AppRepository.importBook(uri)
                                   1. copy file → filesDir/books/
                                   2. same title + size exists? → delete copy, DUPLICATE
                                   3. insert row into books (on error: delete copy, FAILED)
```

## Where each table is used
| Screen  | Reads                         | Writes                                  |
|---------|-------------------------------|-----------------------------------------|
| Home    | books (count), daily_progress | daily_progress (+5 reading minutes)     |
| Player  | books                         | books.position (Next page)              |
| Library | books, reflections            | books (import, delete), reflections     |