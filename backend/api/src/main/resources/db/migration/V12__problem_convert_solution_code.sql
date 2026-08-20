-- 题意修改智能体：可选题解代码
ALTER TABLE problem_convert_record
    ADD COLUMN solution_code LONGTEXT NULL AFTER result_markdown;
