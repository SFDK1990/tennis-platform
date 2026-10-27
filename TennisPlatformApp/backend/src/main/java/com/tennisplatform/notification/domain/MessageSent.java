package com.tennisplatform.notification.domain;

import java.util.UUID;

/** A message was saved with its recipients; once the transaction commits, they get it by email. */
public record MessageSent(UUID messageId) {
}
