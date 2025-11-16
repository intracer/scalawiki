# ScalaWiki DuckDB Module

This module provides DuckDB database integration for ScalaWiki using Quill for type-safe, compile-time checked database queries.

## Features

- **ImageRepository**: Repository for reading and writing `org.scalawiki.dto.Image` instances
- **Quill Integration**: Uses Quill for type-safe database queries with compile-time verification
- **DuckDB Support**: Leverages DuckDB's embedded database capabilities for fast, serverless analytics
- **SQL Injection Protection**: All queries use parameterized statements via Quill's lift mechanism

## Usage

### Creating a Repository

```scala
import org.scalawiki.duckdb.ImageRepository

// Create an in-memory repository
val repository = ImageRepository.inMemory()

// Or create a file-based repository
val fileRepository = ImageRepository("jdbc:duckdb:/path/to/database.db")

// Initialize the database table
repository.createTable()
```

### CRUD Operations

```scala
import org.scalawiki.dto.Image

// Insert an image
val image = Image(
  title = "Example.jpg",
  url = Some("http://example.com/image.jpg"),
  author = Some("John Doe"),
  monumentIds = Seq("MON123")
)
repository.insert(image)

// Find by title
val found = repository.findByTitle("Example.jpg")

// Find by author
val byAuthor = repository.findByAuthor("John Doe")

// Find by monument ID
val byMonument = repository.findByMonumentId("MON123")

// Update
val updated = image.copy(author = Some("Jane Doe"))
repository.update(updated)

// Delete
repository.delete("Example.jpg")

// Get all images
val all = repository.findAll()

// Count images
val count = repository.count()
```

### Batch Operations

```scala
// Insert multiple images
val images = Seq(
  Image(title = "Image1.jpg"),
  Image(title = "Image2.jpg"),
  Image(title = "Image3.jpg")
)
repository.insertBatch(images)
```

## Dependencies

This module depends on:
- `io.getquill:quill-jdbc:4.8.6` - Quill JDBC support
- `org.duckdb:duckdb_jdbc:1.1.3` - DuckDB JDBC driver
- `scalawiki-core` - Core ScalaWiki DTOs

## Testing

Run tests with:
```
sbt "project scalawiki-duckdb" test
```

All tests use in-memory DuckDB databases for fast, isolated test execution.
