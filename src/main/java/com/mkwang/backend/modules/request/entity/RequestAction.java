package com.mkwang.backend.modules.request.entity;

/**
 * Enum representing actions that can be taken on a request.
 */
public enum RequestAction {
  APPROVE, // Duyệt yêu cầu
  REJECT, // Từ chối yêu cầu
  ESCALATE // Leo thang lên Admin
}
