package com.example.kafkaparsing.controller;

import com.example.kafkaparsing.entity.GroupConfig;
import com.example.kafkaparsing.repository.GroupConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/group-config")
@CrossOrigin(origins = "*")
public class GroupConfigController {

    @Autowired
    private GroupConfigRepository groupConfigRepository;

    // Get all group configurations
    @GetMapping("/all")
    public ResponseEntity<List<GroupConfig>> getAllGroupConfigs() {
        try {
            List<GroupConfig> configs = groupConfigRepository.findAll();
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get group configurations by group type
    @GetMapping("/type/{groupType}")
    public ResponseEntity<List<GroupConfig>> getGroupConfigsByType(@PathVariable String groupType) {
        try {
            List<GroupConfig> configs = groupConfigRepository.findByGroupType(groupType);
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get group configuration by ID
    @GetMapping("/{id}")
    public ResponseEntity<GroupConfig> getGroupConfigById(@PathVariable BigDecimal id) {
        try {
            Optional<GroupConfig> config = groupConfigRepository.findById(id);
            return config.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get group configurations by group name
    @GetMapping("/group/{groupName}")
    public ResponseEntity<List<GroupConfig>> getGroupConfigsByGroupName(@PathVariable String groupName) {
        try {
            List<GroupConfig> configs = groupConfigRepository.findByGroupName(groupName);
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get group configurations by group name and type
    @GetMapping("/group/{groupName}/type/{groupType}")
    public ResponseEntity<List<GroupConfig>> getGroupConfigsByNameAndType(@PathVariable String groupName, @PathVariable String groupType) {
        try {
            List<GroupConfig> configs = groupConfigRepository.findByGroupNameAndGroupType(groupName, groupType);
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Get group configurations by group value
    @GetMapping("/value/{groupValue}")
    public ResponseEntity<List<GroupConfig>> getGroupConfigsByValue(@PathVariable String groupValue) {
        try {
            List<GroupConfig> configs = groupConfigRepository.findByGroupValue(groupValue);
            return ResponseEntity.ok(configs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("GroupConfig API is running!");
    }
}
