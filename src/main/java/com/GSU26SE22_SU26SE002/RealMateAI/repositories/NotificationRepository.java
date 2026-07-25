package com.GSU26SE22_SU26SE002.RealMateAI.repositories;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByAccount_AccountIdOrderByCreatedAtDesc(Integer accountId, Pageable pageable);

    long countByAccount_AccountIdAndIsReadFalse(Integer accountId);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.notificationId = :id AND n.account.accountId = :accountId
            """)
    Optional<Notification> findByIdAndAccountId(@Param("id") UUID id, @Param("accountId") Integer accountId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Notification n SET n.isRead = true
            WHERE n.account.accountId = :accountId AND n.isRead = false
            """)
    int markAllAsReadByAccountId(@Param("accountId") Integer accountId);
}