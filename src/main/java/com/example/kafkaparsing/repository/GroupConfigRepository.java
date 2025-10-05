package com.example.kafkaparsing.repository;

import com.example.kafkaparsing.entity.GroupConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface GroupConfigRepository extends JpaRepository<GroupConfig, BigDecimal> {

    // Find by group name
    List<GroupConfig> findByGroupName(String groupName);

    // Find by group type
    List<GroupConfig> findByGroupType(String groupType);

    // Find by group name and group type
    List<GroupConfig> findByGroupNameAndGroupType(String groupName, String groupType);

    // Find by group value
    List<GroupConfig> findByGroupValue(String groupValue);
}
