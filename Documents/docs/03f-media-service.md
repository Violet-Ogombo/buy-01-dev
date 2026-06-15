# Media Service Low-Level Deep Dive

The Media Service (`media-service`) is a dedicated microservice for uploading, serving, and deleting physical files (product images). It validates file headers to prevent MIME spoofing, generates randomized unique filenames, writes files directly to server disk storage, saves metadata inside MongoDB, and publishes creation events to Kafka.

---

## 1. Application Configuration (`application.yml`)
Path: [application.yml](file:///Users/aung.min/Desktop/github/buy-01-dev/media-service/src/main/resources/application.yml)

### Configurations:
*   **`server.port: 8083`**: Runs on port `8083`.
*   **`spring.servlet.multipart.max-file-size: 50MB`**: Sets maximum individual file upload size.
*   **`spring.servlet.multipart.max-request-size: 50MB`**: Sets maximum request size limits.
*   **`media.upload.dir`**: Sets path to the uploads directory (`/app/uploads/images`).
*   **`spring.data.mongodb.uri`**: Connects to the Mongo database `buy01` on host `mongodb:27017`.
*   **`spring.kafka.bootstrap-servers`**: Connects to Kafka brokers (`kafka:9092`).

---

## 2. Models & Validation

### `Media.java` (Model)
Path: [Media.java](file:///Users/aung.min/Desktop/github/buy-01-dev/media-service/src/main/java/com/example/mediaservice/model/Media.java)
Represents records stored inside the `media` MongoDB collection.
*   **Fields**: `id`, `filename`, `userId` (selling owner), `productId`, `contentType`, `size` (long), `imagePath` (absolute path on server disk), `createdAt`.

### `FileValidator.java`
Path: [FileValidator.java](file:///Users/aung.min/Desktop/github/buy-01-dev/media-service/src/main/java/com/example/mediaservice/service/FileValidator.java)
Checks validity of image uploads and handles filename sanitization to protect against directory traversal attacks.

#### Fields:
*   `ALLOWED_CONTENT_TYPES`: A static final list containing allowed HTTP MIME strings: `image/jpeg`, `image/jpg`, `image/png`, `image/gif`, and `image/webp`.

#### Methods:
*   `public void validate(MultipartFile file)`:
    *   **Logic**:
        1.  Throws `IllegalArgumentException("File is empty")` if file has no content.
        2.  Verifies MIME type content type string is within `ALLOWED_CONTENT_TYPES`.
        3.  Reads the first 12 bytes of the file (file magic numbers/signature bytes) to confirm they match standard headers for JPEG, PNG, GIF, or WEBP. This stops attackers from renaming executable files to `.jpg`.
*   `private boolean isAllowedImageHeader(byte[] header, int length)`:
    *   **Logic**: Uses byte-mask matching against standard formats:
        *   **JPEG**: Starts with `FF D8 FF`.
        *   **PNG**: Starts with `89 50 4E 47 0D 0A 1A 0A`.
        *   **GIF**: Starts with `GIF87a` or `GIF89a`.
        *   **WEBP**: Signature must start with `RIFF` and contain `WEBP` from offset 8.
*   `public String sanitizeAndGenerateNewFilename(String originalFilename)`:
    *   **Logic**:
        1.  Replaces backslash with forward slash.
        2.  Strips directories by locating the last slash and slicing.
        3.  Cleans parent directory traversal attempts by replacing `..` strings with empty space.
        4.  Extracts the original file extension (e.g. `png`, `jpg`).
        5.  Returns a randomized file identifier generated via `UUID.randomUUID().toString()` appended with the sanitized file extension.

---

## 3. Services

### `MediaService.java`
Path: [MediaService.java](file:///Users/aung.min/Desktop/github/buy-01-dev/media-service/src/main/java/com/example/mediaservice/service/MediaService.java)

#### Methods:
*   `public Media uploadImage(MultipartFile file, String userId, String productId)`:
    *   **Logic**:
        1.  Calls `fileValidator.validate(file)`.
        2.  Verifies that destination directory path `uploadDir` exists; creates directory if missing.
        3.  Calls `fileValidator.sanitizeAndGenerateNewFilename()`.
        4.  Writes incoming stream to disk file system using `Files.copy()`.
        5.  Saves metadata entity (path, size, filename) into MongoDB and triggers audit logging.
        6.  Serializes map structure `{"mediaId": "...", "productId": "...", "userId": "..."}` using `ObjectMapper` and publishes it to `"image-uploaded"` Kafka topic.
*   `public byte[] getImage(String id)`:
    *   **Logic**: Finds media metadata by ID. Asserts disk file exists. Reads and returns all bytes using `Files.readAllBytes()`.
*   `public Media getMediaById(String id)`:
    *   **Logic**: Returns database record. Throws `MediaNotFoundException` on missing keys.
*   `public void deleteImage(String id, String userId)`:
    *   **Logic**: Deletes file:
        1.  Finds media details.
        2.  Verifies requester ownership (`media.userId == userId`). If incorrect, throws `MediaAccessDeniedException`.
        3.  Deletes the file from disk using `Files.deleteIfExists()`.
        4.  Deletes database record and triggers audit logging.
*   `public List<Media> getMediaByProduct(String productId)`:
    *   **Logic**: Returns list of media items matching product ID.
*   `public Page<Media> getMediaByUser(String userId, int page, int size)`:
    *   **Logic**: Returns a paginated page of media uploaded by a specific user.

---

## 4. Controllers

### `MediaController.java`
Path: [MediaController.java](file:///Users/aung.min/Desktop/github/buy-01-dev/media-service/src/main/java/com/example/mediaservice/MediaController.java)

#### Methods:
*   `public ResponseEntity<Map<String, Object>> uploadImage(...)`:
    *   **Endpoint**: `POST /images`
    *   **Logic**: Extracts file, productId, and Authorization token. Attempts to extract user identity from JWT using helper method `extractUserIdFromJwt()`. If it fails, checks spring security authentication principal (forwarded by API gateway filter). Returns HTTP 403 on unauthorized uploads. Calls `mediaService.uploadImage()` and returns metadata.
*   `public ResponseEntity<byte[]> getImage(@PathVariable String id)`:
    *   **Endpoint**: `GET /images/{id}`
    *   **Logic**: Fetches image bytes. Sets Content-Type header dynamically based on original image format. Applies caching rules (`public, max-age=31536000` for 1 year) and adds ETag hash to avoid re-transmitting data.
*   `public ResponseEntity<Media> getImageMeta(@PathVariable String id)`:
    *   **Endpoint**: `GET /images/{id}/meta`
    *   **Logic**: Returns JSON details of the media record. Used internally by `product-service` to validate image owners.
*   `public ResponseEntity<Object> deleteImage(...)`:
    *   **Endpoint**: `DELETE /images/{id}` (Requires SELLER role)
    *   **Logic**: Calls `mediaService.deleteImage()`.
*   `public ResponseEntity<List<Media>> getProductImages(@PathVariable String productId)`:
    *   **Endpoint**: `GET /images/product/{productId}`
    *   **Logic**: Returns list of images matching product.
*   `public ResponseEntity<Map<String, Object>> getMyUploads(...)`:
    *   **Endpoint**: `GET /images/my-uploads` (Requires SELLER role)
    *   **Logic**: Returns paginated list of images uploaded by seller.

---

## 5. Test Summary

### Core Test Files:
1.  **`FileValidatorTest.java`**: Tests file validations. Asserts errors are thrown for empty files, invalid MIME formats, and spoofed signatures. Tests path sanitization and UUID file renaming logic.
2.  **`MediaServiceTest.java`**: Tests file uploads and disk writes using MockMultipartFile. Asserts database entries are persisted and event messaging gets emitted.
3.  **`MediaControllerTest.java`**: Simulates file upload endpoints, verifying HTTP responses, JWT extraction, and role authorization logic.
4.  **`MediaIntegrationTest.java`**: Validates end-to-end multi-layer file uploads, retrieval, cache headings, and deletion sequences.
