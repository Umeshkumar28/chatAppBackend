-- Run this SQL command in MySQL to fix the content column size
-- Connect to MySQL: mysql -u root -p -P 3307
-- Then: USE chatapp;

ALTER TABLE messages MODIFY COLUMN content TEXT NOT NULL;
