package ai.synalix.synalixai.controller;

import ai.synalix.synalixai.dto.resource.ResourceResponse;
import ai.synalix.synalixai.entity.Resource;
import ai.synalix.synalixai.service.ResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Resource management REST controller
 */
@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    @Autowired
    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * Get all resources
     */
    @GetMapping
    public ResponseEntity<List<ResourceResponse>> getAllResources() {
        var resources = resourceService.getAllResources();
        var responses = resources.stream()
                .map(this::convertToResourceResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    private ResourceResponse convertToResourceResponse(Resource resource) {
        var response = new ResourceResponse();
        response.setId(resource.getId());
        response.setName(resource.getName());
        response.setStatus(resource.getStatus());
        response.setMemoryTotal(resource.getMemoryTotal());
        response.setMemoryUsed(resource.getMemoryUsed());
        return response;
    }
}
