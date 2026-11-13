CREATE INDEX idx_rag_texto ON rag_fragmentos USING GIN (to_tsvector('spanish', texto));
