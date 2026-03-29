package com.mkwang.backend.modules.file.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UploadFolder {
    POST("posts"),
    AVATAR("avatars"),
    DOCUMENT("documents"),
    PRODUCT("products"),
    REPORT("reports");

    private final String path;
}