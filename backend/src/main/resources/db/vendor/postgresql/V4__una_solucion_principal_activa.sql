-- Reject inconsistent existing data rather than silently choosing a recommended solution.
-- All service changes take the parent problem lock before changing recommendations.
CREATE UNIQUE INDEX uk_soluciones_principal_activa
    ON soluciones (problema_id)
    WHERE principal = TRUE AND activo = TRUE;
