MERGE INTO course (course_id, credits, prerequisites, tuition_fee) KEY(course_id)
VALUES ('CS101', 3, '', 3000000);

MERGE INTO course (course_id, credits, prerequisites, tuition_fee) KEY(course_id)
VALUES ('CS102', 4, 'CS101', 4000000);

MERGE INTO class_section (class_id, capacity, enrolled, schedule_slots, status, version, course_id) KEY(class_id)
VALUES ('CS101_01', 30, 10, 'T2_1-3', 'OPEN', 0, 'CS101');

MERGE INTO class_section (class_id, capacity, enrolled, schedule_slots, status, version, course_id) KEY(class_id)
VALUES ('CS102_01', 5, 4, 'T3_1-3,T5_4-6', 'OPEN', 0, 'CS102');

MERGE INTO student (
    student_id,
    academic_warning,
    completed_courses,
    current_credits,
    holds,
    max_credits_effective,
    registered_class_ids,
    registered_schedule_slots,
    tuition_debt,
    tuition_paid,
    version
) KEY(student_id)
VALUES (
    'SV001',
    FALSE,
    'CS101',
    0,
    '',
    15,
    '',
    '',
    0,
    TRUE,
    0
);

MERGE INTO student (
    student_id,
    academic_warning,
    completed_courses,
    current_credits,
    holds,
    max_credits_effective,
    registered_class_ids,
    registered_schedule_slots,
    tuition_debt,
    tuition_paid,
    version
) KEY(student_id)
VALUES (
    'SV002',
    FALSE,
    'CS101',
    0,
    '',
    15,
    '',
    '',
    0,
    FALSE,
    0
);
