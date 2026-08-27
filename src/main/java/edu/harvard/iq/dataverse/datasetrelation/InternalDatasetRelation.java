package edu.harvard.iq.dataverse.datasetrelation;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@DiscriminatorValue("internal")
public class InternalDatasetRelation extends DatasetRelation {

    @ManyToOne
    @JoinColumn(name = "relateddataset_id")
    private Dataset relatedDataset;

    public InternalDatasetRelation(Dataset datasetA, Dataset datasetB, DatasetRelationType relationType, DatasetVersion definitionPoint) {
        if (datasetA == null || datasetB == null || definitionPoint == null) {
            throw new IllegalArgumentException("Cannot create a relation for a null dataset or definition point");
        }
        if (!datasetA.equals(definitionPoint.getDataset())) {
            throw new IllegalArgumentException("The definition point must belong to the relation source dataset");
        }

        setDataset(datasetA);
        setRelatedDataset(datasetB);
        setRelationType(relationType);
        setDefinitionPoint(definitionPoint);
    }

    protected InternalDatasetRelation() {
        super();
    }

    public Dataset getRelatedDataset() {
        return relatedDataset;
    }

    public void setRelatedDataset(Dataset relatedDataset) {
        this.relatedDataset = relatedDataset;
    }

    public boolean isSelfRelation() {
        return this.getDataset().equals(this.getRelatedDataset());
    }

    @Override
    public String toKey() {
        return toVersionComparisonKey() + "|" + getDefinitionPoint().getId();
    }

    @Override
    public String toVersionComparisonKey() {
        return getDataset().getId() + "|" + relatedDataset.getId() + "|" + (getRelationType() != null ? getRelationType().getId() : "");
    }

    @Override
    public DatasetRelation copy(DatasetVersion newDefinitionPoint) {
        return new InternalDatasetRelation(getDataset(), relatedDataset, getRelationType(), newDefinitionPoint);
    }
}
