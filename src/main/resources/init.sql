create table person (
    id text PRIMARY KEY,
    name text,
    timestamp timestamp,
    version integer,
    metadata jsonb
);
