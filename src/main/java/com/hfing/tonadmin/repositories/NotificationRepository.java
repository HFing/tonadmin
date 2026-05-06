package com.hfing.tonadmin.repositories;

import com.hfing.tonadmin.common.NotificationChannel;
import com.hfing.tonadmin.entities.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    @EntityGraph(attributePaths = {"branch"})
    @Query("""
        select n
        from Notification n
        where n.channel = :channel
          and (:branchId is null or n.branch.id = :branchId)
          and not exists (
              select r.id
              from NotificationRead r
              where r.notification = n
                and r.user.id = :userId
          )
        order by n.createdAt desc
        """)
    List<Notification> findUnreadVisible(
            @Param("channel") NotificationChannel channel,
            @Param("branchId") String branchId,
            @Param("userId") String userId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"branch"})
    @Query(
            value = """
                    select n
                    from Notification n
                    where n.channel = :channel
                      and (:branchId is null or n.branch.id = :branchId)
                    order by n.createdAt desc
                    """,
            countQuery = """
                    select count(n)
                    from Notification n
                    where n.channel = :channel
                      and (:branchId is null or n.branch.id = :branchId)
                    """
    )
    Page<Notification> findVisible(
            @Param("channel") NotificationChannel channel,
            @Param("branchId") String branchId,
            Pageable pageable
    );

    @Query("""
        select count(n)
        from Notification n
        where n.channel = :channel
          and n.createdAt >= :since
          and (:branchId is null or n.branch.id = :branchId)
          and not exists (
              select r.id
              from NotificationRead r
              where r.notification = n
                and r.user.id = :userId
          )
        """)
    long countRecentUnreadVisible(
            @Param("channel") NotificationChannel channel,
            @Param("branchId") String branchId,
            @Param("userId") String userId,
            @Param("since") Instant since
    );
}
