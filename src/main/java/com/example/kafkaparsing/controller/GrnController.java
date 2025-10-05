package com.example.kafkaparsing.controller;

import com.example.kafkaparsing.model.GrnCreateReceiptRequest;
import com.example.kafkaparsing.model.GrnCreateReceiptResponse;
import com.example.kafkaparsing.service.ApiAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/grn")
@CrossOrigin(origins = "*")
public class GrnController {

    private static final Logger logger = LoggerFactory.getLogger(GrnController.class);

    @Autowired
    private ApiAuditService apiAuditService;

    /**
     * GRN Create Receipt API - Supports both JSON and XML
     * 
     * JSON Example:
     * curl -X POST http://localhost:8080/api/grn/create-receipt \
     *   -H "Content-Type: application/json" \
     *   -d '{"grn_number": "GRN-2024-001", "supplier_code": "SUP001", ...}'
     * 
     * XML Example:
     * curl -X POST http://localhost:8080/api/grn/create-receipt \
     *   -H "Content-Type: application/xml" \
     *   -d '<GrnCreateReceiptRequest><grn_number>GRN-2024-001</grn_number>...</GrnCreateReceiptRequest>'
     */
    @PostMapping(value = "/create-receipt", consumes = {"application/json", "application/xml"}, produces = {"application/json", "application/xml"})
    public ResponseEntity<GrnCreateReceiptResponse> createReceipt(
            @RequestBody GrnCreateReceiptRequest request,
            HttpServletRequest httpRequest) {
        
        long startTime = System.currentTimeMillis();
        String requestId = apiAuditService.generateRequestId();
        String apiName = "GRN_CREATE_RECEIPT";
        
        logger.info("Processing GRN Create Receipt request with ID: {}", requestId);
        
        try {
            // Send request audit log to Kafka
            apiAuditService.sendRequestAuditLog(apiName, requestId, request, httpRequest);
            
            // Simulate processing time
            Thread.sleep(100);
            
            // Create dummy response
            GrnCreateReceiptResponse response = createDummyResponse(request);
            
            // Calculate processing time
            long processingTimeMs = System.currentTimeMillis() - startTime;
            
            // Send response audit log to Kafka
            apiAuditService.sendResponseAuditLog(apiName, requestId, response, httpRequest, 200, processingTimeMs);
            
            logger.info("GRN Create Receipt processed successfully for request ID: {}", requestId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing GRN Create Receipt request with ID: {}", requestId, e);
            
            // Create error response
            GrnCreateReceiptResponse errorResponse = new GrnCreateReceiptResponse(
                "ERROR",
                "Failed to process GRN receipt: " + e.getMessage(),
                null,
                request.getGrnNumber(),
                null,
                LocalDateTime.now(),
                0,
                0.0,
                request.getCurrency()
            );
            
            // Send error response audit log to Kafka
            long processingTimeMs = System.currentTimeMillis() - startTime;
            apiAuditService.sendResponseAuditLog(apiName, requestId, errorResponse, httpRequest, 500, processingTimeMs);
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("GRN API is running!");
    }

    /**
     * Create dummy response for the GRN request
     */
    private GrnCreateReceiptResponse createDummyResponse(GrnCreateReceiptRequest request) {
        return new GrnCreateReceiptResponse(
            "SUCCESS",
            "GRN receipt created successfully",
            UUID.randomUUID().toString(),
            request.getGrnNumber(),
            "RECEIPT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            LocalDateTime.now(),
            request.getItems() != null ? request.getItems().size() : 0,
            request.getTotalAmount(),
            request.getCurrency()
        );
    }
}
