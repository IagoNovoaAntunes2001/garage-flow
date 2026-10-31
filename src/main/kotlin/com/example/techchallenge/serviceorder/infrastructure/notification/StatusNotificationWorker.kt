package com.example.techchallenge.serviceorder.infrastructure.notification

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = ["garage-flow.notifications.enabled"], havingValue = "true")
class StatusNotificationScheduling

data class PendingStatusNotification(val id: UUID, val leaseId: UUID, val orderId: UUID, val status: String, val recipient: String)

@Component
@ConditionalOnProperty(name = ["garage-flow.notifications.enabled"], havingValue = "true")
class StatusNotificationWorker(
    private val jdbc: JdbcTemplate,
    private val transactions: TransactionTemplate,
    private val mail: JavaMailSender,
    @param:Value("\${garage-flow.notifications.from}") private val sender: String,
) {
    @Scheduled(fixedDelayString = "\${garage-flow.notifications.poll-delay-ms:5000}")
    fun sendNext() {
        val leaseId = UUID.randomUUID()
        val notification = transactions.execute {
            jdbc.update("""UPDATE status_notifications SET state = 'FAILED', lease_id = NULL, lease_until = NULL,
                last_error = 'Lease expired after maximum attempts'
                WHERE state = 'PROCESSING' AND lease_until < now() AND attempts >= 5""")
            jdbc.query(
                """UPDATE status_notifications SET state = 'PROCESSING', lease_id = ?,
                    lease_until = now() + interval '2 minutes', attempts = attempts + 1
                    WHERE id = (SELECT id FROM status_notifications
                        WHERE (state = 'PENDING' AND next_attempt_at <= now())
                           OR (state = 'PROCESSING' AND lease_until < now())
                        ORDER BY created_at, id FOR UPDATE SKIP LOCKED LIMIT 1)
                    RETURNING id, service_order_id, status, recipient""",
                { row, _ -> PendingStatusNotification(row.getObject("id", UUID::class.java), leaseId,
                    row.getObject("service_order_id", UUID::class.java), row.getString("status"), row.getString("recipient")) },
                leaseId,
            ).firstOrNull()
        } ?: return
        try {
            val message = SimpleMailMessage().apply {
                setFrom(sender)
                setTo(notification.recipient)
                subject = "Atualização da ordem de serviço"
                text = "Ordem de serviço ${notification.orderId}: situação atual ${notification.status}."
            }
            mail.send(message)
            jdbc.update("""UPDATE status_notifications SET state = 'SENT', sent_at = now(), lease_id = NULL, lease_until = NULL
                WHERE id = ? AND lease_id = ?""", notification.id, notification.leaseId)
        } catch (exception: Exception) {
            jdbc.update("""UPDATE status_notifications SET state = CASE WHEN attempts >= 5 THEN 'FAILED' ELSE 'PENDING' END,
                next_attempt_at = now() + interval '1 minute', lease_id = NULL, lease_until = NULL, last_error = ?
                WHERE id = ? AND lease_id = ?""", exception.javaClass.simpleName.take(120), notification.id, notification.leaseId)
            logger.warn("Status notification delivery failed notificationId={} errorType={}", notification.id, exception.javaClass.simpleName)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StatusNotificationWorker::class.java)
    }
}
