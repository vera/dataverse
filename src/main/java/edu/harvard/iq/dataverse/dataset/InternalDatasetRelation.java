package edu.harvard.iq.dataverse.dataset;

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
        if (datasetA == null || datasetB == null) throw new IllegalArgumentException("Cannot create a relation for a null dataset");

        // We enforce canonical order to ensure uniqueness of relations
        if (datasetA.getId() < datasetB.getId()) {
            setDataset(datasetA);
            relatedDataset = datasetB;
            if (relationType != null) {
                setRelationType(relationType);
            }
        } else {
            setDataset(datasetB);
            relatedDataset = datasetA;
            if (relationType != null) {
                setRelationType(relationType.getInverse());
            }
        }
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

    @Override
    public String toKey() {
        return getDataset().getId() + "|" + relatedDataset.getId() + "|" + (getRelationType() != null ? getRelationType().getId() : "") + "|" + getDefinitionPoint().getId();
    }

    @Override
    public DatasetRelation copy(DatasetVersion newDefinitionPoint) {
        return new InternalDatasetRelation(getDataset(), relatedDataset, getRelationType(), newDefinitionPoint);
    }
}
