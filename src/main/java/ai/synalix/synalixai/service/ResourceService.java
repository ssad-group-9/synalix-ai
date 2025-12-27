package ai.synalix.synalixai.service;

import ai.synalix.synalixai.dto.gpu.ResourceDetail;
import ai.synalix.synalixai.dto.gpu.GpuStatusResponse;
import ai.synalix.synalixai.entity.Resource;
import ai.synalix.synalixai.enums.ApiErrorCode;
import ai.synalix.synalixai.exception.ApiException;
import ai.synalix.synalixai.repository.ResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Resource management service
 */
@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final RestTemplate restTemplate;

    /**
     * Backend base URL, from env BACKEND or default http://123.249.124.73:8080
     */
    @Value("${app.backend-base-url}")
    private String backendBaseUrl;

    @Autowired
    public ResourceService(ResourceRepository resourceRepository, RestTemplate restTemplate) {
        this.resourceRepository = resourceRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Get all resources
     */
    public List<ResourceDetail> getAllResources() {
        return getGpuStatus().getGpuDetails();
    }

    /**
     * Get resource by ID
     */
    public Resource getResourceById(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND));
    }

    /**
     * Fetch GPU status from external backend: {$BACKEND}/api/gpu/status
     *
     * @return GPU status response
     * @throws ApiException when external call fails
     */
    public GpuStatusResponse getGpuStatus() {
        var url = backendBaseUrl.endsWith("/")
                ? backendBaseUrl + "api/gpu/status"
                : backendBaseUrl + "/api/gpu/status";
        try {
            var response = restTemplate.getForObject(url, GpuStatusResponse.class);
            if (response != null && response.getGpuDetails() != null) {
                var resources = response.getGpuDetails().stream()
                        .map(detail -> {
                            var r = new Resource();
                            r.setId(detail.getId());
                            r.setName(detail.getName());
                            r.setStatus(detail.getStatus());
                            r.setMemoryTotal(detail.getMemoryTotal());
                            r.setMemoryUsed(detail.getMemoryUsed());
                            return r;
                        })
                        .toList();
                resourceRepository.deleteAll();
                resourceRepository.saveAll(resources);
            }
            return response;
        } catch (RestClientException ex) {
            throw new ApiException(ApiErrorCode.INTERNAL_SERVER_ERROR, "Failed to fetch GPU status");
        }
    }
}
