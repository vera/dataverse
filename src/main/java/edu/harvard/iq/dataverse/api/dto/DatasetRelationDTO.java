package edu.harvard.iq.dataverse.api.dto;

import edu.harvard.iq.dataverse.dataset.DatasetRelationType;

public class DatasetRelationDTO {
    private String datasetPid;
    private String relatedDatasetPid;
    private String relationTypeName;

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

    public String getRelationTypeName() {
        return relationTypeName;
    }

    public void setRelationTypeName(String relationTypeName) {
        this.relationTypeName = relationTypeName;
    }
}
