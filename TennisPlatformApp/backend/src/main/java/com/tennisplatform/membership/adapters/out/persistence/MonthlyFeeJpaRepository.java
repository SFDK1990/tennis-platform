package com.tennisplatform.membership.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface MonthlyFeeJpaRepository extends JpaRepository<MonthlyFeeEntity, UUID> {

    List<MonthlyFeeEntity> findAllByMembershipIdInAndVoidedAtIsNull(Collection<UUID> membershipIds);

    @Query("""
            select f from MonthlyFeeEntity f
            where f.voidedAt is null
              and f.kind = com.tennisplatform.membership.domain.FeeKind.PAID
              and f.paidOn >= :from and f.paidOn < :until
              and f.membershipId in (
                  select m.id from GroupMembershipEntity m where m.teacherUserId = :teacherUserId)
            """)
    List<MonthlyFeeEntity> findPaidBetween(@Param("teacherUserId") UUID teacherUserId, @Param("from") LocalDate from,
                                           @Param("until") LocalDate until);
}
