package com.tennisplatform.notification.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface MessageRecipientJpaRepository extends JpaRepository<MessageRecipientEntity, MessageRecipientEntity.Key> {

    @Query("select r.id.studentUserId from MessageRecipientEntity r where r.id.messageId = :messageId")
    List<UUID> studentsOf(@Param("messageId") UUID messageId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from MessageRecipientEntity r where r.id.studentUserId = :studentUserId")
    int deleteAllOfStudent(@Param("studentUserId") UUID studentUserId);
}
