package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.Dataset;
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
     * @param relationTypeName Optional filter by relation type name.
     * @param limit Maximum number of results.
     * @param offset Offset for pagination.
     * @return A list of DatasetRelation objects.
     */
    List<DatasetRelation> getRelations(Dataset dataset, String relationTypeName, Integer limit, Integer offset);

    /**
     * Retrieves the counts of different types of relations for a dataset.
     * 
     * @param dataset The dataset.
     * @return A list of Object arrays, each containing relation type name and count.
     */
    List<Object[]> getRelationCounts(Dataset dataset);

    /**
     * Retrieves the total count of related datasets for a dataset.
     * 
     * @param dataset The dataset.
     * @return Total number of unique related datasets.
     */
    Long getRelatedDatasetCount(Dataset dataset);
}
