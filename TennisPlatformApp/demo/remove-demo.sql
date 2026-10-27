-- Removes what load-demo.py created: its students, their bookings, waiting lists and messages, and
-- the lessons whose notes start with "Ejemplo". A message that reached any example student goes too:
-- before real students arrive there are no others. Run against the stack it was loaded into:
--   docker exec -i tennis-postgres psql -U tennis_platform -d tennis_platform < TennisPlatformApp/demo/remove-demo.sql
BEGIN;

CREATE TEMP TABLE demo_students ON COMMIT DROP AS
    SELECT id FROM users WHERE email LIKE '%@demo.oasis.local';
CREATE TEMP TABLE demo_lessons ON COMMIT DROP AS
    SELECT id FROM lessons WHERE notes LIKE 'Ejemplo:%';

DELETE FROM messages
    WHERE id IN (SELECT message_id FROM message_recipients WHERE student_user_id IN (SELECT id FROM demo_students));
DELETE FROM waiting_list_entries
    WHERE student_user_id IN (SELECT id FROM demo_students) OR lesson_id IN (SELECT id FROM demo_lessons);
DELETE FROM bookings
    WHERE student_user_id IN (SELECT id FROM demo_students) OR lesson_id IN (SELECT id FROM demo_lessons);
DELETE FROM lessons WHERE id IN (SELECT id FROM demo_lessons);
DELETE FROM student_photos WHERE user_id IN (SELECT id FROM demo_students);
DELETE FROM teacher_students WHERE student_user_id IN (SELECT id FROM demo_students);
DELETE FROM student_profiles WHERE user_id IN (SELECT id FROM demo_students);
DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM demo_students);
DELETE FROM email_verifications WHERE user_id IN (SELECT id FROM demo_students);
DELETE FROM password_reset_tokens WHERE user_id IN (SELECT id FROM demo_students);
DELETE FROM users WHERE id IN (SELECT id FROM demo_students);

COMMIT;
