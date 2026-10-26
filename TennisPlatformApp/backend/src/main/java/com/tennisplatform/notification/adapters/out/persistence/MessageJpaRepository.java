package com.tennisplatform.notification.adapters.out.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface MessageJpaRepository extends JpaRepository<MessageEntity, UUID> {

    Page<MessageEntity> findAllByTeacherUserIdOrderBySentAtDescIdDesc(UUID teacherUserId, Pageable pageable);

    @Query(value = """
            select m from MessageEntity m
            where m.id in (select r.id.messageId from MessageRecipientEntity r where r.id.studentUserId = :studentUserId)
            order by m.sentAt desc, m.id desc
            """, countQuery = """
            select count(r) from MessageRecipientEntity r where r.id.studentUserId = :studentUserId
            """)
    Page<MessageEntity> findReceivedBy(@Param("studentUserId") UUID studentUserId, Pageable pageable);
}
