/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.util.LruCache;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skraffmiller
 */
@Stateless
public class DataverseFieldTypeInputLevelServiceBean {

    //    private static final Logger logger = Logger.getLogger(DataverseFieldTypeInputLevelServiceBean.class.getCanonicalName());
    public static final LruCache<Long, List<DataverseFieldTypeInputLevel>> cache = new LruCache<>();

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    public List<DataverseFieldTypeInputLevel> findByDataverseId(Long dataverseId) {
        List<DataverseFieldTypeInputLevel> res = cache.get(dataverseId);

        if (res == null) {
            res = em.createNamedQuery("DataverseFieldTypeInputLevel.findByDataverseId", DataverseFieldTypeInputLevel.class)
                    .setParameter("dataverseId", dataverseId)
                    .getResultList();
            cache.put(dataverseId, res);
        }

        return res;
    }

    private void msg(String s) {
        //logger.fine(s);
    }

    /**
     * Find a list of DataverseFieldTypeInputLevel objects
     * Search criteria:
     * - Dataverse Id,
     * - list of DatasetField Ids
     *
     * @param dataverseId
     * @param datasetFieldIdList
     * @return List of DataverseFieldTypeInputLevel
     */
    public List<DataverseFieldTypeInputLevel> findByDataverseIdAndDatasetFieldTypeIdList(Long dataverseId, List<Long> datasetFieldIdList) {
        msg("---- findByDataverseIdAndDatasetFieldTypeIdList ----");
        Preconditions.checkNotNull(dataverseId);
        Preconditions.checkNotNull(datasetFieldIdList);
        
        if (datasetFieldIdList.isEmpty()) {
            new ArrayList<>();
        }

        try {
            return em.createNamedQuery("DataverseFieldTypeInputLevel.findByDataverseIdAndDatasetFieldTypeIdList", DataverseFieldTypeInputLevel.class)
                    .setParameter("datasetFieldIdList", datasetFieldIdList)
                    .setParameter("dataverseId", dataverseId)
                    .getResultList();
            /*List res = query.getResultList();
            msg("Number of results: " + res.size());
            return res;*/
        } catch (NoResultException nre) {
            return new ArrayList<>();
        }
    }
    //

    //    Query query = em.createQuery("select object(o) from MapLayerMetadata as o where o.dataset=:dataset");// order by o.name");
    //    query.setParameter("dataset", dataset);

    public DataverseFieldTypeInputLevel findByDataverseIdDatasetFieldTypeId(Long dataverseId, Long datasetFieldTypeId) {
        Query query = em.createNamedQuery("DataverseFieldTypeInputLevel.findByDataverseIdDatasetFieldTypeId", DataverseFieldTypeInputLevel.class);
        query.setParameter("dataverseId", dataverseId);
        query.setParameter("datasetFieldTypeId", datasetFieldTypeId);
        try {
            return (DataverseFieldTypeInputLevel) query.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    public void delete(DataverseFieldTypeInputLevel dataverseFieldTypeInputLevel) {
        em.remove(em.merge(dataverseFieldTypeInputLevel));
        cache.invalidate();
    }

    public void deleteFacetsFor(Dataverse d) {
        em.createNamedQuery("DataverseFieldTypeInputLevel.removeByOwnerId")
                .setParameter("ownerId", d.getId())
                .executeUpdate();
        cache.invalidate(d.getId());

    }

    public void create(DataverseFieldTypeInputLevel dataverseFieldTypeInputLevel) {

        em.persist(dataverseFieldTypeInputLevel);
    }

}
