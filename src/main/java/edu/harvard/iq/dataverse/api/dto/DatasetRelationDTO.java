package edu.harvard.iq.dataverse.api.dto;

public class DatasetRelationDTO {
    private String datasetPid;
    private String relatedDatasetPid;
    private String externalIdentifier;
    private String identifierScheme;
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

    public String getExternalIdentifier() {
        return externalIdentifier;
    }

    public void setExternalIdentifier(String externalIdentifier) {
        this.externalIdentifier = externalIdentifier;
    }

    public String getIdentifierScheme() {
        return identifierScheme;
    }

    public void setIdentifierScheme(String identifierScheme) {
        this.identifierScheme = identifierScheme;
    }

    public String getRelationTypeName() {
        return relationTypeName;
    }

    public void setRelationTypeName(String relationTypeName) {
        this.relationTypeName = relationTypeName;
    }
}
