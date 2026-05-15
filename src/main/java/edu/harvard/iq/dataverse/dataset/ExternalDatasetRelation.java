package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("external")
public class ExternalDatasetRelation extends DatasetRelation {

    private String externalIdentifier;
    private String identifierScheme;

    public ExternalDatasetRelation(Dataset dataset, String externalIdentifier, String identifierScheme, DatasetRelationType relationType, DatasetVersion definitionPoint) {
        super(dataset, relationType, definitionPoint);
        this.externalIdentifier = externalIdentifier;
        this.identifierScheme = identifierScheme;
    }

    protected ExternalDatasetRelation() {
        super();
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

    @Override
    public String toKey() {
        return getDataset().getId() + "|" + externalIdentifier + "|" + (identifierScheme != null ? identifierScheme : "") + "|" + (getRelationType() != null ? getRelationType().getId() : "") + "|" + getDefinitionPoint().getId();
    }
}
