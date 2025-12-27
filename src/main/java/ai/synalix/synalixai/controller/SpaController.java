package ai.synalix.synalixai.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Controller for serving SPA static resources for all non-API routes.
 */
@Controller
@RequestMapping
public class SpaController {

    private static final String INDEX_FILE_NAME = "index.html";

    private final Path staticDirectory;

    public SpaController(@Value("${app.static-directory:./static}") String staticDirectoryPath) {
        this.staticDirectory = Paths.get(staticDirectoryPath).toAbsolutePath().normalize();
    }

    /**
     * Serve static resources or fall back to index.html for client-side routing when the request path is not under /api.
     *
     * @param request current HTTP request
     * @return static resource response, SPA index fallback, or 404 when not found
     * @throws IOException if resource content type resolution fails
     */
    @GetMapping(value = {"/", "/{path:^(?!api$).*$}", "/{path:^(?!api$).*$}/**"})
    public ResponseEntity<Resource> serveSpa(HttpServletRequest request) throws IOException {
        final var requestUri = request.getRequestURI();
        if (requestUri.startsWith("/api")) {
            return ResponseEntity.notFound().build();
        }

        final var relativePath = extractRelativePath(requestUri);
        final var isAssetRequest = relativePath.contains(".");
        final var candidatePath = resolveStaticPath(relativePath);

        if (candidatePath != null && Files.exists(candidatePath) && Files.isRegularFile(candidatePath)) {
            return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .contentType(resolveMediaType(candidatePath))
                .body(new FileSystemResource(candidatePath));
        }

        final var indexPath = staticDirectory.resolve(INDEX_FILE_NAME);
        if (!isAssetRequest && Files.exists(indexPath) && Files.isRegularFile(indexPath)) {
            return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.TEXT_HTML)
                .body(new FileSystemResource(indexPath));
        }

        return ResponseEntity.notFound().build();
    }

    private String extractRelativePath(String requestUri) {
        if (requestUri == null || requestUri.isBlank() || "/".equals(requestUri)) {
            return INDEX_FILE_NAME;
        }

        var trimmed = requestUri.startsWith("/") ? requestUri.substring(1) : requestUri;
        return trimmed.isBlank() ? INDEX_FILE_NAME : trimmed;
    }

    private Path resolveStaticPath(String relativePath) {
        final var targetPath = staticDirectory.resolve(relativePath).normalize();
        if (!targetPath.startsWith(staticDirectory)) {
            return null;
        }
        return targetPath;
    }

    private MediaType resolveMediaType(Path path) throws IOException {
        final var detectedType = Files.probeContentType(path);
        if (detectedType == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(detectedType);
    }
}
