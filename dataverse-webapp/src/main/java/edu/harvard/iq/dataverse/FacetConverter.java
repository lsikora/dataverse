/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;

import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 * @author xyang
 */
@FacesConverter("facetConverter")
public class FacetConverter implements Converter {

    @EJB
    DatasetFieldServiceBean datasetFieldService;

    public Object getAsObject(FacesContext facesContext, UIComponent component, String submittedValue) {
        return datasetFieldService.find(new Long(submittedValue));
    }

    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        if (value == null || value.equals("")) {
            return "";
        } else {
            return ((DatasetFieldType) value).getId().toString();
        }
    }
}

