-- Change password 'eatlog', OR if you see error message "Unknown system variable 'validate_password.policy'"
-- Try execute in mysql terminal:
-- for unix: install plugin validate_password soname 'validate_password.so';
-- for windows: install plugin validate_password soname 'validate_password.dll';
-- Info from https://stackoverflow.com/questions/55237257/mysql-validate-password-policy-unknown-system-variable
SET GLOBAL validate_password_policy=LOW;
SET GLOBAL validate_password_length=0;
DROP USER IF EXISTS `eatlog`@`%`;
CREATE USER `eatlog`@`%` IDENTIFIED WITH 'caching_sha2_password' by 'eatlog' REQUIRE NONE PASSWORD EXPIRE NEVER ACCOUNT UNLOCK PASSWORD HISTORY DEFAULT PASSWORD REUSE INTERVAL DEFAULT PASSWORD REQUIRE CURRENT DEFAULT;

GRANT ALL PRIVILEGES ON `eatlog`.* TO `eatlog`@`%`;
GRANT ALL PRIVILEGES ON `eatlog_maxmind_city`.* TO `eatlog`@`%`;

FLUSH PRIVILEGES;