package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.api.AbstractApiBean;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@Named
public class DatasetRelationTypeServiceBean {

    private static final Logger logger = Logger.getLogger(DatasetRelationTypeServiceBean.class.getName());

    @PersistenceContext
    EntityManager em;

    public List<DatasetRelationType> listAll() {
        return em.createNamedQuery("DatasetRelationType.findAll", DatasetRelationType.class).getResultList();
    }

    public DatasetRelationType findById(long id) {
        try {
            return em.createNamedQuery("DatasetRelationType.getById", DatasetRelationType.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            logger.log(Level.WARNING, "Couldn't find a dataset relation type with id " + id);
            return null;
        }
    }

    public DatasetRelationType findByName(String name) {
        if (name == null) {
            return null;
        }

        try {
            return em.createNamedQuery("DatasetRelationType.getByName", DatasetRelationType.class)
                    .setParameter("name", name)
                    .getSingleResult();
        } catch (NoResultException e) {
            logger.log(Level.WARNING, "Couldn't find a dataset relation type with name " + name);
            return null;
        }
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

    public int deleteById(long id) throws IllegalStateException, PersistenceException {
        DatasetRelationType doomed = findById(id);
        if (doomed == null) {
            return 0;
        }
        try {
            DatasetRelationType inverse = doomed.getInverse();
            if (inverse != null) {
                inverse.setInverse(null);
                doomed.setInverse(null);
                em.merge(inverse);
                em.merge(doomed);
            }
            em.remove(em.merge(doomed));
            em.flush();
            return 1;
        } catch (PersistenceException p) {
            if (p.getMessage().contains("violates foreign key constraint")) {
                throw new IllegalStateException("Dataset relation type with id " + id + " is referenced and cannot be deleted.", p);
            } else {
                throw p;
            }
        }
    }

}
