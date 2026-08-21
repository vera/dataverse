package edu.harvard.iq.dataverse.dataset;

import edu.harvard.iq.dataverse.api.AbstractApiBean.WrappedResponse;
import edu.harvard.iq.dataverse.api.ApiConstants;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.json.NullSafeJsonBuilder;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.persistence.exceptions.DatabaseException;

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

    public DatasetRelationType save(DatasetRelationType relationType) throws IllegalArgumentException {
        try {
            em.persist(relationType);
            em.flush();
        } catch (PersistenceException p) {
            if (p.getMessage().contains("duplicate key")) {
                throw new IllegalArgumentException(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.error.create.duplicate"), p);
            } else if (p.getMessage().contains("violates not-null constraint")){
                throw new IllegalArgumentException(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.error.create.notNull"), p);
            } else {
                throw p;
            }
        }
        return relationType;
    }

    public void delete(DatasetRelationType doomed) throws IllegalArgumentException {
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
        } catch (PersistenceException p) {
            if (p.getMessage().contains("violates foreign key constraint")) {
                throw new IllegalArgumentException(BundleUtil.getStringFromBundle("datasets.api.datasetRelationType.error.delete.referenced"), p);
            } else {
                throw p;
            }
        }
    }

}
