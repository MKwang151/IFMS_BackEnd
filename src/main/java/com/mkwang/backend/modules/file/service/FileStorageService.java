package com.mkwang.backend.modules.file.service;

import com.mkwang.backend.common.exception.ResourceNotFoundException;
import com.mkwang.backend.modules.file.dto.request.FileStorageRequest;
import com.mkwang.backend.modules.file.entity.FileStorage;
import com.mkwang.backend.modules.file.mapper.FileStorageMapper;
import com.mkwang.backend.modules.file.repository.FileStorageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageRepository fileStorageRepository;
    private final FileStorageMapper fileStorageMapper;
    private final CloudinaryService cloudinaryService;

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

    public FileStorage getFile(Long id) {
        return fileStorageRepository.findById(id)
                .orElseThrow( () -> new ResourceNotFoundException("File not found with id: " + id));
    }

    public List<FileStorage> getMutipleFiles(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return fileStorageRepository.findAllById(ids);
    }


    @Transactional
    public void deleteFile(Long id) {
        FileStorage file = fileStorageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        cloudinaryService.deleteFile(file.getCloudinaryPublicId());
        fileStorageRepository.delete(file);
    }
}
