-- Menu/RBAC hardening migration for MySQL 8.0.16+
-- Back up sys_menu and sys_role_menu before executing this one-time migration.
-- MySQL DDL commits implicitly; run this script during a maintenance window.

-- ----------------------------------------------------------------------
-- 1. Normalize values that have an unambiguous safe default.
-- ----------------------------------------------------------------------
UPDATE sys_menu SET parent_id = 0 WHERE parent_id IS NULL;
UPDATE sys_menu SET order_num = 0 WHERE order_num IS NULL;
UPDATE sys_menu SET status = '0' WHERE status IS NULL OR status = '';
UPDATE sys_menu SET visible = '0' WHERE visible IS NULL OR visible = '';
UPDATE sys_menu SET del_flag = '0' WHERE del_flag IS NULL OR del_flag = '';
UPDATE sys_menu SET path = '#' WHERE menu_type = 'F';
UPDATE sys_menu SET component = NULL WHERE menu_type = 'F';
UPDATE sys_menu SET component = 'Layout'
WHERE menu_type = 'M' AND (component IS NULL OR component = '');

-- ----------------------------------------------------------------------
-- 2. Abort on conflicts that require a business decision.
-- ----------------------------------------------------------------------
DELIMITER $$

DROP PROCEDURE IF EXISTS assert_sys_menu_migration_ready$$
CREATE PROCEDURE assert_sys_menu_migration_ready()
BEGIN
    DECLARE conflict_count BIGINT DEFAULT 0;

    SELECT COUNT(*) INTO conflict_count
    FROM (
        SELECT parent_id, menu_name
        FROM sys_menu
        WHERE del_flag <> '2'
        GROUP BY parent_id, menu_name
        HAVING COUNT(*) > 1
    ) conflicts;
    IF conflict_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'sys_menu contains duplicate active menu_name values under the same parent';
    END IF;

    SELECT COUNT(*) INTO conflict_count
    FROM (
        SELECT parent_id, path
        FROM sys_menu
        WHERE del_flag <> '2'
          AND menu_type <> 'F'
          AND path IS NOT NULL
          AND path <> ''
        GROUP BY parent_id, path
        HAVING COUNT(*) > 1
    ) conflicts;
    IF conflict_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'sys_menu contains duplicate active route paths under the same parent';
    END IF;

    SELECT COUNT(*) INTO conflict_count
    FROM sys_menu
    WHERE menu_type NOT IN ('M', 'C', 'F')
       OR status NOT IN ('0', '1')
       OR visible NOT IN ('0', '1')
       OR del_flag NOT IN ('0', '2')
       OR id = parent_id;
    IF conflict_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'sys_menu contains invalid type/status flags or a direct self-reference';
    END IF;

    SELECT COUNT(*) INTO conflict_count
    FROM sys_menu
    WHERE perms LIKE '%*%';
    IF conflict_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'sys_menu contains wildcard permissions; remove them before migration';
    END IF;

    SELECT COUNT(*) INTO conflict_count
    FROM sys_menu
    WHERE menu_type IN ('C', 'F')
      AND (perms IS NULL OR perms = '');
    IF conflict_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'page/button menu rows must have a permission identifier';
    END IF;

    SELECT COUNT(*) INTO conflict_count
    FROM sys_menu
    WHERE perms IS NOT NULL
      AND perms <> ''
      AND NOT REGEXP_LIKE(perms, '^[a-z][a-z0-9-]*(?::[a-z][a-z0-9-]*)*$');
    IF conflict_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'sys_menu contains malformed permission identifiers';
    END IF;

    SELECT COUNT(*) INTO conflict_count
    FROM sys_menu
    WHERE menu_type = 'C'
      AND (path IS NULL OR path = '' OR component IS NULL OR component = '');
    IF conflict_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'page menu rows must have both path and component';
    END IF;

    SELECT COUNT(*) INTO conflict_count
    FROM sys_menu child
    LEFT JOIN sys_menu parent ON parent.id = child.parent_id
    WHERE child.parent_id <> 0
      AND parent.id IS NULL;
    IF conflict_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'sys_menu contains rows whose parent_id does not exist';
    END IF;
END$$

CALL assert_sys_menu_migration_ready()$$
DROP PROCEDURE assert_sys_menu_migration_ready$$

DELIMITER ;

-- Diagnostic queries to run if the preflight procedure aborts:
-- SELECT parent_id, menu_name, GROUP_CONCAT(id) ids
-- FROM sys_menu WHERE del_flag <> '2'
-- GROUP BY parent_id, menu_name HAVING COUNT(*) > 1;
--
-- SELECT parent_id, path, GROUP_CONCAT(id) ids
-- FROM sys_menu
-- WHERE del_flag <> '2' AND menu_type <> 'F' AND path IS NOT NULL AND path <> ''
-- GROUP BY parent_id, path HAVING COUNT(*) > 1;

-- ----------------------------------------------------------------------
-- 3. Enforce safe defaults and state-machine checks.
-- ----------------------------------------------------------------------
ALTER TABLE sys_menu
    MODIFY parent_id BIGINT NOT NULL DEFAULT 0 COMMENT 'Parent menu ID',
    MODIFY order_num INT NOT NULL DEFAULT 0 COMMENT 'Display order',
    MODIFY menu_type CHAR(1) NOT NULL COMMENT 'M-directory C-page F-button',
    MODIFY visible CHAR(1) NOT NULL DEFAULT '0' COMMENT '0-visible 1-hidden',
    MODIFY status CHAR(1) NOT NULL DEFAULT '0' COMMENT '0-enabled 1-disabled',
    MODIFY del_flag CHAR(1) NOT NULL DEFAULT '0' COMMENT '0-active 2-deleted';

ALTER TABLE sys_menu
    ADD CONSTRAINT chk_sys_menu_type
        CHECK (menu_type IN ('M', 'C', 'F')),
    ADD CONSTRAINT chk_sys_menu_visible
        CHECK (visible IN ('0', '1')),
    ADD CONSTRAINT chk_sys_menu_status
        CHECK (status IN ('0', '1')),
    ADD CONSTRAINT chk_sys_menu_del_flag
        CHECK (del_flag IN ('0', '2')),
    ADD CONSTRAINT chk_sys_menu_not_self_parent
        CHECK (id <> parent_id),
    ADD CONSTRAINT chk_sys_menu_permission
        CHECK (
            perms IS NULL
            OR perms = ''
            OR (
                perms NOT LIKE '%*%'
                AND REGEXP_LIKE(perms, '^[a-z][a-z0-9-]*(?::[a-z][a-z0-9-]*)*$')
            )
        ),
    ADD CONSTRAINT chk_sys_menu_required_permission
        CHECK (menu_type = 'M' OR (perms IS NOT NULL AND perms <> '')),
    ADD CONSTRAINT chk_sys_menu_type_fields
        CHECK (
            (menu_type = 'M')
            OR (menu_type = 'C' AND path IS NOT NULL AND path <> ''
                AND component IS NOT NULL AND component <> '')
            OR (menu_type = 'F' AND path = '#' AND component IS NULL)
        );

-- ----------------------------------------------------------------------
-- 4. Conditional unique keys for active rows.
-- Deleted rows generate NULL and therefore do not block name/path reuse.
-- ----------------------------------------------------------------------
ALTER TABLE sys_menu
    ADD COLUMN active_menu_name VARCHAR(255)
        GENERATED ALWAYS AS (
            CASE WHEN del_flag <> '2' THEN menu_name ELSE NULL END
        ) STORED,
    ADD COLUMN active_route_path VARCHAR(255)
        GENERATED ALWAYS AS (
            CASE
                WHEN del_flag <> '2'
                 AND menu_type <> 'F'
                 AND path IS NOT NULL
                 AND path <> ''
                THEN path
                ELSE NULL
            END
        ) STORED,
    ADD UNIQUE KEY uk_sys_menu_parent_name_active (parent_id, active_menu_name),
    ADD UNIQUE KEY uk_sys_menu_parent_path_active (parent_id, active_route_path),
    ADD KEY idx_sys_menu_parent_order (parent_id, order_num, id),
    ADD KEY idx_sys_menu_state_type (del_flag, status, visible, menu_type),
    ADD KEY idx_sys_menu_perms (perms);

-- ----------------------------------------------------------------------
-- 5. Remove invalid/duplicate role-menu relations before constraints.
-- ----------------------------------------------------------------------
CREATE TEMPORARY TABLE tmp_valid_role_menu AS
SELECT DISTINCT rm.role_id, rm.menu_id
FROM sys_role_menu rm
INNER JOIN sys_role r ON r.id = rm.role_id
INNER JOIN sys_menu m ON m.id = rm.menu_id
WHERE r.del_flag = '0'
  AND m.del_flag <> '2';

DELETE FROM sys_role_menu;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role_id, menu_id FROM tmp_valid_role_menu;

DROP TEMPORARY TABLE tmp_valid_role_menu;

-- If sys_role_menu already has a primary key, replace the following PRIMARY
-- KEY statement with a UNIQUE KEY on the same columns.
ALTER TABLE sys_role_menu
    ADD PRIMARY KEY (role_id, menu_id),
    ADD KEY idx_sys_role_menu_menu_id (menu_id),
    ADD CONSTRAINT fk_sys_role_menu_role
        FOREIGN KEY (role_id) REFERENCES sys_role (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    ADD CONSTRAINT fk_sys_role_menu_menu
        FOREIGN KEY (menu_id) REFERENCES sys_menu (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT;

-- ----------------------------------------------------------------------
-- 6. Post-migration integrity checks. Each query should return zero rows.
-- ----------------------------------------------------------------------
SELECT rm.*
FROM sys_role_menu rm
LEFT JOIN sys_role r ON r.id = rm.role_id
LEFT JOIN sys_menu m ON m.id = rm.menu_id
WHERE r.id IS NULL OR m.id IS NULL;

SELECT parent_id, menu_name, COUNT(*) duplicate_count
FROM sys_menu
WHERE del_flag <> '2'
GROUP BY parent_id, menu_name
HAVING COUNT(*) > 1;

SELECT parent_id, path, COUNT(*) duplicate_count
FROM sys_menu
WHERE del_flag <> '2'
  AND menu_type <> 'F'
  AND path IS NOT NULL
  AND path <> ''
GROUP BY parent_id, path
HAVING COUNT(*) > 1;
