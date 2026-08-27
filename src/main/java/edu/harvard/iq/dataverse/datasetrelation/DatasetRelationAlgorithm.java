package edu.harvard.iq.dataverse.datasetrelation;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import java.util.List;

/**
 * Interface for algorithms that calculate or retrieve relations for a dataset.
 * This allows swapping the implementation (e.g. from simple direct relation-based to complex graph-based clustering).
 */
public interface DatasetRelationAlgorithm {
    
    /**
     * Retrieves relations for a given dataset.
     * 
     * @param dataset The dataset for which to find relations.
     * @param version Optional dataset version for version-specific filtering.
     * @param relationTypeNames Optional filter by relation type names.
     * @param datasetTypeNames Optional filter by dataset type names of the related dataset.
     * @param relationSources Optional filter by relation source (internal, external).
     * @param limit Maximum number of results.
     * @param offset Offset for pagination.
     * @return A list of DatasetRelation objects.
     */
    List<DatasetRelation> getRelations(Dataset dataset, DatasetVersion version, List<String> relationTypeNames, List<String> datasetTypeNames, List<String> relationSources, Integer limit, Integer offset);

    /**
     * Retrieves the counts of different types of relations for a dataset.
     * 
     * @param dataset The dataset.
     * @param version Optional dataset version for version-specific filtering.
     * @param groupBy The field to group by (e.g. "relationType", "datasetType").
     * @return A list of Object arrays, each containing grouping information and count.
     */
    List<Object[]> getRelationCounts(Dataset dataset, DatasetVersion version, String groupBy);

    /**
     * Retrieves the total number of relations returned for a dataset.
     * 
     * @param dataset The dataset.
     * @param version Optional dataset version for version-specific filtering.
     * @param relationTypeNames Optional filter by relation type names.
     * @param datasetTypeNames Optional filter by dataset type names of the related dataset.
     * @param relationSources Optional filter by relation source (internal, external).
     * @return Total number of relations returned by {@link #getRelations} with the same filters.
     */
    Long getTotalDatasetRelationCountFor(Dataset dataset, DatasetVersion version, List<String> relationTypeNames, List<String> datasetTypeNames, List<String> relationSources);
}
