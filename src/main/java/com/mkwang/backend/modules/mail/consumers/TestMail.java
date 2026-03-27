package com.mkwang.backend.modules.mail.consumers;


public record TestMail(
        String to,
        String subject,
        String content
) {

}
