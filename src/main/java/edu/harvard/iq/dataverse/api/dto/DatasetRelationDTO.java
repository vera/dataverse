package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.DatasetRelation;

public class DatasetRelationDTO {
    private String datasetPid;
    private String relatedDatasetPid;
    private DatasetRelation.DatasetRelationType relationType;

    public String getDatasetPid() {
        return datasetPid;
    }

    public void setDatasetPid(String datasetPid) {
        this.datasetPid = datasetPid;
    }

    public String getRelatedDatasetPid() {
        return relatedDatasetPid;
    }

    public void setRelatedDatasetPid(String relatedDatasetPid) {
        this.relatedDatasetPid = relatedDatasetPid;
    }

    public DatasetRelation.DatasetRelationType getRelationType() {
        return relationType;
    }

    public void setRelationType(DatasetRelation.DatasetRelationType relationType) {
        this.relationType = relationType;
    }
}
