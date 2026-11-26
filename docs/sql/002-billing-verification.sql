-- Read-only verification. Select the intended database in the mysql command.
-- No names, emails, passwords, card details or raw Stripe payloads are selected.

SELECT DATABASE() AS selected_database, VERSION() AS mysql_version;

SELECT 'users' AS table_name, COUNT(*) AS exact_count FROM users
UNION ALL SELECT 'categories', COUNT(*) FROM categories
UNION ALL SELECT 'items', COUNT(*) FROM items
UNION ALL SELECT 'tasks', COUNT(*) FROM tasks
UNION ALL SELECT 'establishments', COUNT(*) FROM establishments
UNION ALL SELECT 'billing_subscriptions', COUNT(*) FROM billing_subscriptions
UNION ALL SELECT 'stripe_webhook_events', COUNT(*) FROM stripe_webhook_events;

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('establishments', 'billing_subscriptions', 'stripe_webhook_events')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('establishments', 'billing_subscriptions', 'stripe_webhook_events')
ORDER BY TABLE_NAME, CONSTRAINT_TYPE, CONSTRAINT_NAME;

SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, COLUMN_NAME;

SELECT id, establishment_id, status,
       CAST(cancel_at_period_end AS UNSIGNED) AS cancel_at_period_end,
       stripe_subscription_id IS NOT NULL AS has_stripe_subscription,
       stripe_customer_id IS NOT NULL AS has_stripe_customer,
       checkout_session_id IS NOT NULL AS has_checkout_session,
       updated_at
FROM billing_subscriptions
ORDER BY id;

SELECT type, COUNT(*) AS processed_events
FROM stripe_webhook_events
GROUP BY type ORDER BY type;

SELECT 'establishments_without_owner' AS check_name, COUNT(*) AS anomalies
FROM establishments e LEFT JOIN users u ON u.id = e.owner_user_id WHERE u.id IS NULL
UNION ALL
SELECT 'subscriptions_without_establishment', COUNT(*)
FROM billing_subscriptions s LEFT JOIN establishments e ON e.id = s.establishment_id WHERE e.id IS NULL
UNION ALL
SELECT 'events_without_establishment', COUNT(*)
FROM stripe_webhook_events w LEFT JOIN establishments e ON e.id = w.establishment_id WHERE e.id IS NULL;

SELECT COUNT(*) AS duplicate_event_groups
FROM (SELECT id FROM stripe_webhook_events GROUP BY id HAVING COUNT(*) > 1) d;
