package com.example.coffeenotes.feature.catalog.repository.media;

import com.example.coffeenotes.domain.catalog.media.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
    List<MediaAsset> findByOwner_IdOrderByCreatedAtDesc(UUID ownerId);

    void deleteByIdAndOwner_Id(UUID id, UUID ownerId);
}