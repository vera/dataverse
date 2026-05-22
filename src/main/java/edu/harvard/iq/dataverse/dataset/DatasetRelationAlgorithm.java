package edu.harvard.iq.dataverse.dataset;

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
     * @param relationTypeName Optional filter by relation type name.
     * @param datasetTypeNames Optional filter by dataset type names of the related dataset.
     * @param relationSources Optional filter by relation source (internal, external).
     * @param limit Maximum number of results.
     * @param offset Offset for pagination.
     * @return A list of DatasetRelation objects.
     */
    List<DatasetRelation> getRelations(Dataset dataset, DatasetVersion version, String relationTypeName, List<String> datasetTypeNames, List<String> relationSources, Integer limit, Integer offset);

    /**
     * Retrieves the counts of different types of relations for a dataset.
     * 
     * @param dataset The dataset.
     * @param version Optional dataset version for version-specific filtering.
     * @return A list of Object arrays, each containing relation type name and count.
     */
    List<Object[]> getRelationCounts(Dataset dataset, DatasetVersion version);

    /**
     * Retrieves the total count of related datasets for a dataset.
     * 
     * @param dataset The dataset.
     * @param version Optional dataset version for version-specific filtering.
     * @param relationTypeName Optional filter by relation type name.
     * @param datasetTypeNames Optional filter by dataset type names of the related dataset.
     * @param relationSources Optional filter by relation source (internal, external).
     * @return Total number of unique related datasets.
     */
    Long getTotalDatasetRelationCountFor(Dataset dataset, DatasetVersion version, String relationTypeName, List<String> datasetTypeNames, List<String> relationSources);
}
