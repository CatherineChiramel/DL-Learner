/**
 * Copyright (C) 2007 - 2016, Jens Lehmann
 *
 * This file is part of DL-Learner.
 *
 * DL-Learner is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * DL-Learner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.dllearner.algorithms.schema;

import com.google.common.collect.Sets;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.dllearner.algorithms.properties.AxiomAlgorithms;
import org.dllearner.core.AbstractAxiomLearningAlgorithm;
import org.dllearner.core.AxiomLearningProgressMonitor;
import org.dllearner.core.SilentAxiomLearningProgressMonitor;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.reasoning.SPARQLReasoner;
import org.dllearner.utilities.OwlApiJenaUtils;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.profiles.OWL2DLProfile;
import org.semanticweb.owlapi.profiles.OWLProfile;
import org.semanticweb.owlapi.profiles.Profiles;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 
 * @author Lorenz Buehmann
 *
 */
public abstract class AbstractSchemaGenerator implements SchemaGenerator{
	
	protected QueryExecutionFactory qef;
	protected OWLProfile owlProfile = new OWL2DLProfile();
	protected SPARQLReasoner reasoner;
	
//	protected AxiomLearningProgressMonitor progressMonitor = new ConsoleAxiomLearningProgressMonitor();
	protected AxiomLearningProgressMonitor progressMonitor = new SilentAxiomLearningProgressMonitor();
	
	// the types of axioms to be processed
	protected Set<AxiomType<? extends OWLAxiom>> axiomTypes = AxiomAlgorithms.TBoxAndRBoxAxiomTypes;
	
	// the types of entities to be processed
	protected Set<EntityType<? extends OWLEntity>> entityTypes = Sets.<EntityType<? extends OWLEntity>>newHashSet(
			EntityType.CLASS, EntityType.OBJECT_PROPERTY, EntityType.DATA_PROPERTY);
	
	// the minimum accuracy threshold for generated axioms to be accepted
	private double accuracyThreshold = 0.3;
	
	// the entities which are processed
	protected SortedSet<OWLEntity> entities;
	
	// the knowledge base
	private OntModel model;
	
	// the profile used for rule based inferencing
	private OntModelSpec reasoningProfile = OntModelSpec.RDFS_MEM_RDFS_INF;
	
	public AbstractSchemaGenerator(QueryExecutionFactory qef) {
		this.qef = qef;
		this.reasoner = new SPARQLReasoner(qef);
		this.model = ModelFactory.createOntologyModel(reasoningProfile);
	}
	
	public AbstractSchemaGenerator(Model model) {
		// enable reasoning on model
		this.model = ModelFactory.createOntologyModel(reasoningProfile, model);
		this.qef = new QueryExecutionFactoryModel(this.model);
		this.reasoner = new SPARQLReasoner(qef);
	}
	
	/**
	 * Set the types of axioms that are generated.
	 * @param axiomTypes the axiom types to set
	 */
	public void setAxiomTypes(Set<AxiomType<? extends OWLAxiom>> axiomTypes) {
		this.axiomTypes = axiomTypes;
	}
	
	/**
	 * Set the types of axioms that are generated by giving an OWL profile.
	 * @param owlProfile the OWL profile
	 */
	public void setAxiomTypes(OWLProfile owlProfile) {
		if(owlProfile.equals(Profiles.OWL2_EL.getOWLProfile())){
			
		} else if(owlProfile.equals(Profiles.OWL2_RL.getOWLProfile())){

		} else if(owlProfile.equals(Profiles.OWL2_QL.getOWLProfile())){

		} else if(owlProfile.equals(Profiles.OWL2_DL.getOWLProfile())){
			
		} else {
			throw new IllegalArgumentException("OWL profile " + owlProfile.getName() + " not supported.");
		}
	}
	
	public void setEntityTypes(Set<EntityType<? extends OWLEntity>> entityTypes) {
		this.entityTypes = entityTypes;
	}
	
	/**
	 * @param entities the entities to set
	 */
	public void setEntities(SortedSet<OWLEntity> entities) {
		this.entities = entities;
	}
	
	/**
	 * Return the entities contained in the current knowledge base.
	 */
	protected SortedSet<OWLEntity> getEntities(){
		if(entities == null){
			entities = new TreeSet<>();
			for (EntityType<? extends OWLEntity> entityType : entityTypes) {
				if(entityType == EntityType.CLASS){
					entities.addAll(reasoner.getOWLClasses());
				} else if(entityType == EntityType.OBJECT_PROPERTY){
					entities.addAll(reasoner.getOWLObjectProperties());
				} else if(entityType == EntityType.DATA_PROPERTY){
					entities.addAll(reasoner.getOWLDataProperties());
				} else {
					throw new IllegalArgumentException("Entity type " + entityType.getName() + " not supported.");
				}
			}
		}
		return entities;
	}

	/**
	 * Return the entities contained in the current knowledge base for the given entity type.
	 *
	 * @param entityType the entity type
	 */
	protected <T extends OWLEntity> SortedSet<T> getEntities(EntityType<T> entityType) {
		SortedSet<T> entitiesForType = new TreeSet<>();
		for (OWLEntity entity : getEntities()) {
			if (entity.isType(entityType)) {
				entitiesForType.add((T) entity);
			}
		}
		return entitiesForType;
//		Stream<T> s = entities.stream().filter(e -> e.isType(entityType)).map(e -> (T) e);
//		return s.collect(Collectors.toList());
	}
	
	/**
	 * @param accuracyThreshold the accuracyThreshold to set
	 */
	public void setAccuracyThreshold(double accuracyThreshold) {
		this.accuracyThreshold = accuracyThreshold;
	}
	
	protected Set<OWLAxiom> applyLearningAlgorithm(OWLEntity entity, AxiomType<? extends OWLAxiom> axiomType) throws Exception{
		// get the algorithm class
		Class<? extends AbstractAxiomLearningAlgorithm<? extends OWLAxiom, ? extends OWLObject, ? extends OWLEntity>> algorithmClass = AxiomAlgorithms.getAlgorithmClass(axiomType);
		
		// create learning algorithm object
		AbstractAxiomLearningAlgorithm learner = null;
		try {
			learner = (AbstractAxiomLearningAlgorithm)algorithmClass.getConstructor(
					SparqlEndpointKS.class).newInstance(new SparqlEndpointKS(qef));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		learner.setEntityToDescribe(entity);
		learner.setUseSampling(false);
		learner.setProgressMonitor(progressMonitor);
		
		
		try {
			// initialize the learning algorithm
			learner.init();
			
			// run the learning algorithm
			learner.start();
			
			// return the result
			return new TreeSet<>(learner.getCurrentlyBestAxioms(accuracyThreshold));
		} catch (Exception e) {
			throw new Exception("Generation of " + axiomType.getName() + " axioms failed.", e);
		}
	}
	
	/**
	 * Add the axioms to the running knowledge base.
	 * @param axioms the axioms
	 */
	protected void addToKnowledgebase(Set<OWLAxiom> axioms) {
		Set<Statement> statements = OwlApiJenaUtils.asStatements(axioms);
		model.add(new ArrayList<>(statements));
	}

}
