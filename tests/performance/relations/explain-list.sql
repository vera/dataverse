\set ON_ERROR_STOP on

CREATE TEMP TABLE relation_benchmark_target AS
SELECT d.id AS dataset_id,
       (
           SELECT dv.id
           FROM datasetversion dv
           WHERE dv.dataset_id = d.id
             AND dv.versionstate = 'RELEASED'
           ORDER BY dv.id DESC
           LIMIT 1
       ) AS version_id
FROM dataset d
JOIN dvobject o ON o.id = d.id
WHERE o.protocol || ':' || o.authority || o.separator || o.identifier = :'persistent_id';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM relation_benchmark_target WHERE version_id IS NOT NULL) THEN
        RAISE EXCEPTION 'No released dataset found for the supplied persistent ID';
    END IF;
END;
$$;

SELECT dataset_id AS target_dataset_id, version_id AS target_version_id
FROM relation_benchmark_target
\gset

\echo Capturing list-query plan for dataset :target_dataset_id and version :target_version_id
EXPLAIN (ANALYZE, BUFFERS)
SELECT dr.*
FROM datasetrelation dr
JOIN datasetversion dv_def ON dr.definitionpoint_id = dv_def.id
WHERE (
    dr.definitionpoint_id = :target_version_id
    OR (
        dv_def.dataset_id != :target_dataset_id
        AND (dr.dataset_id = :target_dataset_id OR dr.relateddataset_id = :target_dataset_id)
        AND dr.definitionpoint_id = (
            SELECT dv.id
            FROM datasetversion dv
            WHERE dv.dataset_id = dv_def.dataset_id
              AND dv.id = (
                  SELECT MAX(dv2.id)
                  FROM datasetversion dv2
                  WHERE dv2.dataset_id = dv.dataset_id
                    AND dv2.versionstate = 'RELEASED'
              )
        )
    )
)
AND dr.id = (
    SELECT dr2.id
    FROM datasetrelation dr2
    JOIN datasetversion dv_def2 ON dr2.definitionpoint_id = dv_def2.id
    WHERE (
        dr2.definitionpoint_id = :target_version_id
        OR (
            (dr2.dataset_id = :target_dataset_id OR (dr2.relation_source = 'internal' AND dr2.relateddataset_id = :target_dataset_id))
            AND dv_def2.dataset_id != :target_dataset_id
            AND dr2.definitionpoint_id = (
                SELECT dv2.id
                FROM datasetversion dv2
                WHERE dv2.dataset_id = dv_def2.dataset_id
                  AND dv2.id = (
                      SELECT MAX(dv3.id)
                      FROM datasetversion dv3
                      WHERE dv3.dataset_id = dv2.dataset_id
                        AND dv3.versionstate = 'RELEASED'
                  )
            )
        )
    )
    AND (
        CASE WHEN dr.dataset_id = :target_dataset_id THEN dr.relationtype_id
             ELSE (SELECT rt.inverse_id FROM datasetrelationtype rt WHERE rt.id = dr.relationtype_id)
        END
        =
        CASE WHEN dr2.dataset_id = :target_dataset_id THEN dr2.relationtype_id
             ELSE (SELECT rt2.inverse_id FROM datasetrelationtype rt2 WHERE rt2.id = dr2.relationtype_id)
        END
    )
    AND (
        CASE WHEN dr.relation_source = 'internal' THEN
            CASE WHEN dr.dataset_id = :target_dataset_id THEN CAST(dr.relateddataset_id AS VARCHAR) ELSE CAST(dr.dataset_id AS VARCHAR) END
        ELSE dr.externalidentifier
        END
        =
        CASE WHEN dr2.relation_source = 'internal' THEN
            CASE WHEN dr2.dataset_id = :target_dataset_id THEN CAST(dr2.relateddataset_id AS VARCHAR) ELSE CAST(dr2.dataset_id AS VARCHAR) END
        ELSE dr2.externalidentifier
        END
    )
    ORDER BY (CASE WHEN dr2.definitionpoint_id = :target_version_id THEN 0 ELSE 1 END) ASC, dr2.id ASC
    LIMIT 1
)
ORDER BY (CASE WHEN dv_def.dataset_id = :target_dataset_id THEN 0 ELSE 1 END) ASC, dr.id ASC
LIMIT 10 OFFSET 0;

\echo Capturing total-count-query plan for dataset :target_dataset_id and version :target_version_id
EXPLAIN (ANALYZE, BUFFERS)
SELECT COUNT(DISTINCT
    CASE
        WHEN dr.relation_source = 'internal' THEN
            CASE WHEN dr.dataset_id = :target_dataset_id THEN CAST(dr.relateddataset_id AS VARCHAR) ELSE CAST(dr.dataset_id AS VARCHAR) END
        ELSE dr.externalidentifier
    END
)
FROM datasetrelation dr
JOIN datasetversion dv_def ON dr.definitionpoint_id = dv_def.id
WHERE (
    dr.definitionpoint_id = :target_version_id
    OR (
        dv_def.dataset_id != :target_dataset_id
        AND (dr.dataset_id = :target_dataset_id OR dr.relateddataset_id = :target_dataset_id)
        AND dr.definitionpoint_id = (
            SELECT dv.id
            FROM datasetversion dv
            WHERE dv.dataset_id = dv_def.dataset_id
              AND dv.id = (
                  SELECT MAX(dv2.id)
                  FROM datasetversion dv2
                  WHERE dv2.dataset_id = dv.dataset_id
                    AND dv2.versionstate = 'RELEASED'
              )
        )
    )
);
