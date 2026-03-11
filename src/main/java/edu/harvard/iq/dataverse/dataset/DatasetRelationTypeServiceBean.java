package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.api.AbstractApiBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;

import java.util.logging.Logger;

@Stateless
@Named
public class DatasetRelationTypeServiceBean {

    private static final Logger logger = Logger.getLogger(DatasetRelationTypeServiceBean.class.getName());

    @PersistenceContext
    EntityManager em;

    public DatasetRelationType findByName(String name) {
        return em.createNamedQuery("DatasetRelationType.getByName", DatasetRelationType.class)
                .setParameter("name", name)
                .getSingleResult();
    }

    public DatasetRelationType save(DatasetRelationType relationType) throws AbstractApiBean.WrappedResponse {
        if (relationType.getId() != null) {
            throw new AbstractApiBean.WrappedResponse(new IllegalArgumentException("There shouldn't be an ID already set"), null);
        }
        try {
            em.persist(relationType);
            em.flush();
        } catch (PersistenceException p) {
            if (p.getMessage().contains("duplicate key")) {
                throw new AbstractApiBean.WrappedResponse(new IllegalStateException("A relation type with the same name or inverse is already present.", p), null);
            } else {
                throw p;
            }
        }
        return relationType;
    }

}
