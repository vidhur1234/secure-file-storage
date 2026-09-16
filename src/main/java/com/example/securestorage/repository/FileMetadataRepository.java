package com.example.securestorage.repository;

import com.example.securestorage.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    List<FileMetadata> findByOwnerId(Long ownerId);

    List<FileMetadata> findByTargetRoleIdIn(Set<Long> roleIds);

    @Query("SELECT f FROM FileMetadata f WHERE f.targetRole.id IN :roleIds OR f.owner.id = :userId")
    List<FileMetadata> findAccessibleFiles(@Param("roleIds") Set<Long> roleIds, @Param("userId") Long userId);

    @Query("SELECT f FROM FileMetadata f WHERE (f.targetRole.id IN :roleIds OR f.owner.id = :userId) AND LOWER(f.fileName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<FileMetadata> searchAccessibleFiles(@Param("roleIds") Set<Long> roleIds, @Param("userId") Long userId, @Param("keyword") String keyword);

    @Query("SELECT f FROM FileMetadata f WHERE LOWER(f.fileName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<FileMetadata> searchAllFiles(@Param("keyword") String keyword);
}
