package com.mkwang.backend.modules.file.service;

import com.mkwang.backend.modules.file.dto.request.FileStorageRequest;
import com.mkwang.backend.modules.file.entity.FileStorage;
import com.mkwang.backend.modules.file.mapper.FileStorageMapper;
import com.mkwang.backend.modules.file.repository.FileStorageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageRepository fileStorageRepository;
    private final FileStorageMapper fileStorageMapper;

    public FileStorage save(FileStorageRequest request) {
        return fileStorageRepository.save(fileStorageMapper.toFileStorage(request));
    }

    public List<FileStorage> saveAll(List<FileStorageRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        List<FileStorage> files = requests.stream()
                .map(fileStorageMapper::toFileStorage)
                .toList();
        return fileStorageRepository.saveAll(files);
    }

    public Optional<FileStorage> findByPublicId(String publicId) {
        return fileStorageRepository.findByCloudinaryPublicId(publicId);
    }

    public List<FileStorage> findAllByPublicIds(List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return Collections.emptyList();
        }
        return fileStorageRepository.findAllByCloudinaryPublicIdIn(publicIds);
    }
}
