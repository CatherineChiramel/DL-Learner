/**
 * Copyright (C) 2007-2010, Jens Lehmann
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
 *
 */
package org.dllearner.algorithms.qtl.operations.lgg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import org.aksw.jena_sparql_api.cache.h2.CacheUtilsH2;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.core.SparqlServiceBuilder;
import org.dllearner.algorithms.qtl.QueryTreeUtils;
import org.dllearner.algorithms.qtl.datastructures.impl.RDFResourceTree;
import org.dllearner.algorithms.qtl.impl.QueryTreeFactory;
import org.dllearner.algorithms.qtl.impl.QueryTreeFactoryBase;
import org.dllearner.algorithms.qtl.util.StopURIsDBpedia;
import org.dllearner.algorithms.qtl.util.StopURIsOWL;
import org.dllearner.algorithms.qtl.util.StopURIsRDFS;
import org.dllearner.algorithms.qtl.util.StopURIsSKOS;
import org.dllearner.algorithms.qtl.util.filters.NamespaceDropStatementFilter;
import org.dllearner.algorithms.qtl.util.filters.ObjectDropStatementFilter;
import org.dllearner.algorithms.qtl.util.filters.PredicateDropStatementFilter;
import org.dllearner.core.AbstractReasonerComponent;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGenerator;
import org.dllearner.kb.sparql.ConciseBoundedDescriptionGeneratorImpl;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;
import uk.ac.manchester.cs.owlapi.dlsyntax.DLSyntaxObjectRenderer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

/**
 * 
 * @author Lorenz Bühmann
 *
 */
public class LGGGeneratorRDFS implements LGGGenerator2{
	
	private Logger logger = LoggerFactory.getLogger(LGGGeneratorRDFS.class);
	
	private Monitor mon = MonitorFactory.getTimeMonitor("lgg");
	
	private int subCalls;

	private AbstractReasonerComponent reasoner;
	
	public LGGGeneratorRDFS(AbstractReasonerComponent reasoner) {
		this.reasoner = reasoner;
	}
	
	@Override
	public RDFResourceTree getLGG(RDFResourceTree tree1, RDFResourceTree tree2) {
		return getLGG(tree1, tree2, false);
	}
	
	@Override
	public RDFResourceTree getLGG(RDFResourceTree tree1, RDFResourceTree tree2,
			boolean learnFilters) {
		
		reset();
		
		mon.start();
		RDFResourceTree lgg = computeLGG(tree1, tree2, learnFilters);
		mon.stop();
		
		addNumbering(lgg);
		
		return lgg;
	}

	@Override
	public RDFResourceTree getLGG(List<RDFResourceTree> trees) {
		return getLGG(trees, false);
	}
	
	@Override
	public RDFResourceTree getLGG(List<RDFResourceTree> trees, boolean learnFilters) {
		// if there is only 1 tree return it
		if(trees.size() == 1){
			return trees.get(0);
		}
		
		// lgg(t_1, t_n)
		mon.start();
		RDFResourceTree lgg = trees.get(0);
		for(int i = 1; i < trees.size(); i++) {
			lgg = getLGG(lgg, trees.get(i), learnFilters);
		}
		mon.stop();
		
		addNumbering(lgg);
		
		return lgg;
	}
	
	private void reset() {
		subCalls = 0;
	}
	
	private RDFResourceTree computeLGG(RDFResourceTree tree1, RDFResourceTree tree2, boolean learnFilters){
		subCalls++;
		
		// 1. compare the root node
		// if both root nodes have same URI or literal value, just return one of the two trees as LGG
		if((tree1.isResourceNode() || tree1.isLiteralValueNode()) && tree1.getData().equals(tree2.getData())){
			logger.trace("Early termination. Tree 1 {}  and tree 2 {} describe the same resource.", tree1, tree2);
			return tree1;
		}
		
		// handle literal nodes with same datatype
		if(tree1.isLiteralNode() && tree2.isLiteralNode()){
			RDFDatatype d1 = tree1.getData().getLiteralDatatype();
			RDFDatatype d2 = tree2.getData().getLiteralDatatype();

			if(d1 != null && d1.equals(d2)){
				return new RDFResourceTree(d1);
				// TODO collect literal values
			}
		}
		
		// else create new empty tree
		RDFResourceTree lgg = new RDFResourceTree();
		
		// 2. compare the edges
		// we only have to compare edges which are 
		// a) contained in both trees
		// b) related via subsumption, i.e. p1 ⊑ p2
		
		// get edges of tree 2 connected via subsumption
		Multimap<Node, Node> relatedEdges = getRelatedEdges(tree1, tree2);
		for (Entry<Node, Collection<Node>> entry : relatedEdges.asMap().entrySet()){
			Node edge1 = entry.getKey();
			Collection<Node> edges2 = entry.getValue();
			
			Set<RDFResourceTree> addedChildren = new HashSet<RDFResourceTree>();
		
			
			// loop over children of first tree
			for(RDFResourceTree child1 : tree1.getChildren(edge1)){
				// for all related edges of tree 2
				for (Node edge2 : edges2) {
					// loop over children of second tree
					for(RDFResourceTree child2 : tree2.getChildren(edge2)){
						// compute the LGG
						RDFResourceTree lggChild = computeLGG(child1, child2, learnFilters);
						
						// check if there was already a more specific child computed before
						// and if so don't add the current one
						boolean add = true;
						for(Iterator<RDFResourceTree> it = addedChildren.iterator(); it.hasNext();){
							RDFResourceTree addedChild = it.next();
							if(QueryTreeUtils.isSubsumedBy(addedChild, lggChild, reasoner, edge1.equals(RDF.type.asNode()))){
//								logger.trace("Skipped adding: Previously added child {} is subsumed by {}.",
//										addedChild.getStringRepresentation(),
//										lggChild.getStringRepresentation());
								add = false;
								break;
							} else if(QueryTreeUtils.isSubsumedBy(lggChild, addedChild, reasoner, edge1.equals(RDF.type.asNode()))){
//								logger.trace("Removing child node: {} is subsumed by previously added child {}.",
//										lggChild.getStringRepresentation(),
//										addedChild.getStringRepresentation());
								lgg.removeChild(addedChild, edge1);
								it.remove();
							} 
						}
						if(add){
							lgg.addChild(lggChild, edge1);
							addedChildren.add(lggChild);
//							logger.trace("Adding child {}", lggChild.getStringRepresentation());
						} 
					}
				}
			}
		}
		
		return lgg;
	}
	
	/*
	 * For each edge in tree 1 we compute the related edges in tree 2. 
	 */
	private Multimap<Node, Node> getRelatedEdges(RDFResourceTree tree1, RDFResourceTree tree2) {
		Multimap<Node, Node> relatedEdges = HashMultimap.create();
		
		for(Node edge1 : tree1.getEdges()) {
			// trivial
			if(tree2.getEdges().contains(edge1)) {
				relatedEdges.put(edge1, edge1);
			}
			// check if it's not a built-in properties
			if (!edge1.getNameSpace().equals(RDF.getURI()) 
					&& !edge1.getNameSpace().equals(RDFS.getURI())
					&& !edge1.getNameSpace().equals(OWL.getURI())) {
				// get related edges by subsumption
				OWLObjectPropertyImpl prop = new OWLObjectPropertyImpl(IRI.create(edge1.getURI()));
				for (OWLObjectProperty p : reasoner.getSuperProperties(prop)) {
					Node edge = NodeFactory.createURI(p.toStringID());
					if(tree2.getEdges().contains(edge)) {
						relatedEdges.put(edge1, edge);
					}
				}
				for (OWLObjectProperty p : reasoner.getSubProperties(prop)) {
					Node edge = NodeFactory.createURI(p.toStringID());
					if(tree2.getEdges().contains(edge)) {
						relatedEdges.put(edge1, edge);
					}
				}
			}
		}
		return relatedEdges;
	}
	
	private void addNumbering(RDFResourceTree tree){
//		tree.setId(nodeId++);
		for(RDFResourceTree child : tree.getChildren()){
			addNumbering(child);
		}
	}
	
	public static void main(String[] args) throws Exception {
		ToStringRenderer.getInstance().setRenderer(new DLSyntaxObjectRenderer());
		// knowledge base
		SparqlEndpoint endpoint = SparqlEndpoint.getEndpointDBpedia();
		QueryExecutionFactory qef = SparqlServiceBuilder
				.http(endpoint.getURL().toString(), endpoint.getDefaultGraphURIs())
				.withCache(CacheUtilsH2.createCacheFrontend("/tmp/cache", false, TimeUnit.DAYS.toMillis(60)))
				.withPagination(10000).withDelay(50, TimeUnit.MILLISECONDS).create();

		// tree generation
		ConciseBoundedDescriptionGenerator cbdGenerator = new ConciseBoundedDescriptionGeneratorImpl(qef);
		int maxDepth = 2;
		cbdGenerator.setRecursionDepth(maxDepth);

		QueryTreeFactory treeFactory = new QueryTreeFactoryBase();
		treeFactory.setMaxDepth(maxDepth);
		treeFactory.addDropFilters(
				new PredicateDropStatementFilter(StopURIsDBpedia.get()),
				new PredicateDropStatementFilter(StopURIsRDFS.get()),
				new PredicateDropStatementFilter(StopURIsOWL.get()),
				new ObjectDropStatementFilter(StopURIsOWL.get()),
				new PredicateDropStatementFilter(StopURIsSKOS.get()),
				new ObjectDropStatementFilter(StopURIsSKOS.get()),
				new NamespaceDropStatementFilter(Sets.newHashSet("http://dbpedia.org/property/",
						"http://purl.org/dc/terms/", "http://dbpedia.org/class/yago/",
						"http://www.w3.org/2003/01/geo/wgs84_pos#", "http://www.georss.org/georss/", FOAF.getURI())));
		List<RDFResourceTree> trees = new ArrayList<RDFResourceTree>();
		List<String> resources = Lists.newArrayList("http://dbpedia.org/resource/Leipzig",
				"http://dbpedia.org/resource/Dresden");
		for (String resource : resources) {
			try {
				System.out.println(resource);
				Model model = cbdGenerator.getConciseBoundedDescription(resource);
				RDFResourceTree tree = treeFactory.getQueryTree(ResourceFactory.createResource(resource), model);
				System.out.println(tree.getStringRepresentation());
				trees.add(tree);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// LGG computation
		LGGGenerator2 lggGen = new LGGGeneratorRDFS(new SPARQLReasoner(qef));
		RDFResourceTree lgg = lggGen.getLGG(trees);

		System.out.println("LGG");
		System.out.println(lgg.getStringRepresentation());
		System.out.println(QueryTreeUtils.toSPARQLQueryString(lgg));
		System.out.println(QueryTreeUtils.toOWLClassExpression(lgg));
	}

}
