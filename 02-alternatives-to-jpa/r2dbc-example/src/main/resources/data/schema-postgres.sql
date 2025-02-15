CREATE TABLE IF NOT EXISTS posts
(
    id
    SERIAL
    PRIMARY
    KEY,
    title
    VARCHAR
(
    255
) NOT NULL,
    content VARCHAR
(
    1024
) NOT NULL
    );

CREATE TABLE IF NOT EXISTS comments
(
    id
    SERIAL
    PRIMARY
    KEY,
    post_id
    BIGINT,
    content
    VARCHAR
(
    1024
),
    CONSTRAINT fk_post_id FOREIGN KEY
(
    post_id
)
    REFERENCES posts
(
    id
) ON DELETE CASCADE
  ON UPDATE RESTRICT
    );
