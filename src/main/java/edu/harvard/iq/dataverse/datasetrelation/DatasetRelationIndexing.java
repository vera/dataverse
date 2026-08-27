package edu.harvard.iq.dataverse.datasetrelation;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.engine.command.CommandContext;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for scheduling indexing tasks related to datasets and their relations.
 * The methods in this class are designed to handle the indexing of datasets whenever their relationships change and an
 * index update is necessary, i.e., to update the relatedDatasetCount.
 *
 * @author Vera Clemens (ZB MED)
 */
public final class DatasetRelationIndexing {

    private DatasetRelationIndexing() {
    }

    public static void schedule(CommandContext ctxt, Dataset definingDataset, Collection<DatasetRelation> relations) {
        Map<Long, Dataset> datasets = relatedDatasets(relations);
        datasets.put(definingDataset.getId(), definingDataset);
        ctxt.index().asyncIndexDatasetList(datasets.values().stream().toList(), true);
    }

    public static void scheduleChanges(CommandContext ctxt, Dataset definingDataset,
            Collection<DatasetRelation> previousRelations, Collection<DatasetRelation> currentRelations) {
        Map<Long, Dataset> previousRelatedDatasets = relatedDatasets(previousRelations);
        Map<Long, Dataset> currentRelatedDatasets = relatedDatasets(currentRelations);

        Map<Long, Dataset> changedRelatedDatasets = new LinkedHashMap<>();
        previousRelatedDatasets.forEach((id, dataset) -> {
            if (!currentRelatedDatasets.containsKey(id)) {
                changedRelatedDatasets.put(id, dataset);
            }
        });
        currentRelatedDatasets.forEach((id, dataset) -> {
            if (!previousRelatedDatasets.containsKey(id)) {
                changedRelatedDatasets.put(id, dataset);
            }
        });
        changedRelatedDatasets.put(definingDataset.getId(), definingDataset);
        ctxt.index().asyncIndexDatasetList(changedRelatedDatasets.values().stream().toList(), true);
    }

    private static Map<Long, Dataset> relatedDatasets(Collection<DatasetRelation> relations) {
        Map<Long, Dataset> datasets = new LinkedHashMap<>();
        for (DatasetRelation relation : relations) {
            if (relation instanceof InternalDatasetRelation internalRelation) {
                Dataset relatedDataset = internalRelation.getRelatedDataset();
                datasets.put(relatedDataset.getId(), relatedDataset);
            }
        }
        return datasets;
    }
}
