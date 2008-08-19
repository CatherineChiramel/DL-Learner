/**
 * Copyright (C) 2007-2008, Jens Lehmann
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
package org.dllearner.kb.aquisitors;

import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.dllearner.kb.extraction.Configuration;
import org.dllearner.utilities.datastructures.RDFNodeTuple;

/**
 * Can execute different queries.
 * 
 * @author Sebastian Hellmann
 * 
 */
public class LinkedDataTupelAquisitor extends TupelAquisitor {
	
	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(LinkedDataTupelAquisitor.class);
	@SuppressWarnings("unused")
	private Configuration configuration;
	

	public LinkedDataTupelAquisitor(Configuration configuration) {
		this.configuration = configuration;
	}

	// standard query get a tupels (p,o) for subject s
	@Override
	public SortedSet<RDFNodeTuple> retrieveTupel(String uri){
		throw new RuntimeException("Not Implemented yet");
	}
	@Override
	public SortedSet<RDFNodeTuple> retrieveClassesForInstances(String uri){
		throw new RuntimeException("Not Implemented yet");
	}
	@Override
	public SortedSet<RDFNodeTuple> retrieveTuplesForClassesOnly(String uri){
		throw new RuntimeException("Not Implemented yet");
	}
	
	
	


}
