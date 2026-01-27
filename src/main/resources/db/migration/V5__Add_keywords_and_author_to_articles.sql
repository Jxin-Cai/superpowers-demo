ALTER TABLE articles ADD COLUMN keywords VARCHAR(500) COMMENT '逗号分隔的关键词';
ALTER TABLE articles ADD COLUMN author_id BIGINT COMMENT '作者ID';
ALTER TABLE articles ADD CONSTRAINT fk_articles_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX idx_articles_keywords ON articles(keywords);
CREATE INDEX idx_articles_author ON articles(author_id);
