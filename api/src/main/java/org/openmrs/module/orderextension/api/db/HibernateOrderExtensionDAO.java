/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.orderextension.api.db;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.module.orderextension.DrugRegimen;
import org.openmrs.module.orderextension.ExtendedDrugOrder;
import org.openmrs.module.orderextension.OrderGroup;
import org.openmrs.module.orderextension.OrderSet;
import org.openmrs.module.orderextension.OrderSetMember;

/**
 * Hibernate implementation of the OrderExtension Data Access Interface
 */
public class HibernateOrderExtensionDAO implements OrderExtensionDAO {
	
	protected final Log log = LogFactory.getLog(getClass());
    private SessionFactory sessionFactory;
    
	/**
	 * @see OrderExtensionDAO#getOrderSet(Integer)
	 */
	@Override
	public OrderSet getOrderSet(Integer id) {
		return (OrderSet) getCurrentSession().get(OrderSet.class, id);
	}

	/**
	 * @see OrderExtensionDAO#getOrderSetByUuid(String)
	 */
	@Override
	public OrderSet getOrderSetByUuid(String uuid) {
		String query = "FROM OrderSet s WHERE s.uuid = :uuid";
		return (OrderSet) getCurrentSession().createQuery(query).setString("uuid", uuid).uniqueResult();
	}

	/**
	 * @see OrderExtensionDAO#getAllOrderSets(boolean)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<OrderSet> getNamedOrderSets(String partialName, Concept indication, boolean includeRetired) {
		Criteria criteria = getCurrentSession().createCriteria(OrderSet.class);
		criteria.add(Restrictions.isNotNull("name"));
		if (!includeRetired) {
			criteria.add(Restrictions.eq("retired", false));
		}
		if (partialName != null) {
			criteria.add(Restrictions.ilike("name", partialName, MatchMode.ANYWHERE));
		}
		if (indication != null) {
			criteria.add(Restrictions.eq("indication", indication));
		}
		criteria.addOrder(Order.asc("name"));
		return criteria.list();
	}

	/**
	 * @see OrderExtensionDAO#saveOrderSet(OrderSet)
	 */
	@Override
	public OrderSet saveOrderSet(OrderSet orderSet) {
		getCurrentSession().saveOrUpdate(orderSet);
		return orderSet;
	}

	/**
	 * @see OrderExtensionDAO#purgeOrderSet(OrderSet)
	 */
	@Override
	public void purgeOrderSet(OrderSet orderSet) {
		getCurrentSession().delete(orderSet);
	}
	
	/**
	 * @see OrderExtensionDAO#getOrderSetMember(Integer)
	 */
	@Override
	public OrderSetMember getOrderSetMember(Integer id) {
		return (OrderSetMember) getCurrentSession().get(OrderSetMember.class, id);
	}

	/**
	 * @see OrderExtensionDAO#getParentOrderSets(OrderSet)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<OrderSet> getParentOrderSets(OrderSet orderSet) {
		String query = "select n.orderSet from NestedOrderSetMember n where n.nestedOrderSet = :nestedOrderSet";
		return getCurrentSession().createQuery(query).setEntity("nestedOrderSet", orderSet).list();
	}

	/**
	 * @see OrderExtensionDAO#saveOrderGroup(OrderGroup)
	 */
	@Override
	public <T extends OrderGroup> T saveOrderGroup(T orderGroup) {
		getCurrentSession().saveOrUpdate(orderGroup);
		return orderGroup;
	}
	
	/**
     * @see org.openmrs.module.orderextension.api.db.OrderExtensionDAO#getExtendedDrugOrdersForPatient(Patient patient)
     */
    @Override
    public List<ExtendedDrugOrder>  getExtendedDrugOrdersForPatient(Patient patient, Concept indication, Date startDateAfter, Date startDateBefore) {
    	Criteria criteria = getCurrentSession().createCriteria(ExtendedDrugOrder.class);
		
    	if(patient != null)
    	{
    		criteria.add(Restrictions.eq("patient", patient));
    	}
    	
    	if(indication != null)
    	{	
    		criteria.add(Restrictions.eq("indication", indication));
    	}
    	if(startDateAfter != null && startDateBefore != null) 
    	{
    		criteria.add(Restrictions.between("startDate", startDateAfter, startDateBefore));
    	}
    	else if(startDateAfter != null)
    	{
    		criteria.add(Restrictions.ge("startDate", startDateAfter));
    	}
    	else if(startDateBefore != null)
    	{
    		criteria.add(Restrictions.lt("startDate", startDateBefore));
    	}
		criteria.add(Restrictions.eq("voided", false));
		return criteria.list();
    }
	
	/**
	 * @see OrderExtensionDAO#getDrugRegimen(Integer)
	 */
	@Override
	public DrugRegimen getDrugRegimen(Integer id) {
		return (DrugRegimen) getCurrentSession().get(DrugRegimen.class, id);
	}
	
	/**
     * @see org.openmrs.module.orderextension.api.db.OrderExtensionDAO#getMaxNumberOfCyclesForRegimen(org.openmrs.module.orderextension.DrugRegimen)
     */
    @Override
    public Integer getMaxNumberOfCyclesForRegimen(Patient patient, DrugRegimen regimen) {
  
    	SQLQuery query = sessionFactory.getCurrentSession().createSQLQuery("select MAX(og.cycle_number) from orderextension_order_group og, orderextension_order er, orders o where og.id = er.group_id and er.order_id = o.order_id and o.voided = 0 and og.voided = 0 and og.order_set_id = :orderSetId and o.patient_id = :patientId and o.start_date >= :startDate");
		query.setInteger("patientId", patient.getId());
		query.setInteger("orderSetId", regimen.getOrderSet().getId());
		query.setDate("startDate", regimen.getFirstDrugOrderStartDate());
		
		return (Integer)query.uniqueResult();
    }

	/**
	 * @see OrderExtensionDAO#getOrderGroup(Integer)
	 */
	@Override
	public OrderGroup getOrderGroup(Integer id) {
		return (OrderGroup) getCurrentSession().get(OrderGroup.class, id);
	}
	
	/**
	 * @see OrderExtensionDAO#getExtendedOrders(Patient, Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends OrderGroup> List<T> getOrderGroups(Patient patient, Class<T> type) {
		Criteria criteria = getCurrentSession().createCriteria(type);
		// TODO: Need to actually restrict this by patient.  Might need to add Patient directly to OrderGroup
		criteria.add(Restrictions.eq("voided", false));
		return criteria.list();
	}
	
	/**
	 * @return the sessionFactory
	 */
	private Session getCurrentSession() {
		return sessionFactory.getCurrentSession();
	}

	/**
	 * @return the sessionFactory
	 */
	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	/**
	 * @param sessionFactory
	 */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
}