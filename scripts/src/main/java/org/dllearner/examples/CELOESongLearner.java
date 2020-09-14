package org.dllearner.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

import org.dllearner.algorithms.celoe.CELOE;
import org.dllearner.core.ComponentInitException;
import org.dllearner.core.KnowledgeSource;
import org.dllearner.kb.OWLFile;
import org.dllearner.learningproblems.PosNegLP;
import org.dllearner.learningproblems.PosNegLPStandard;
import org.dllearner.learningproblems.PosOnlyLP;
import org.dllearner.reasoning.ClosedWorldReasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLIndividual;

import com.google.common.collect.Sets;

import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl;

/**
 * This class should exemplify how to run CELOE programmatically, i.e. by
 * creating all components explicitly in Java. This example runs the same
 * experiment as the examples/father.conf does.
 */
public class CELOESongLearner {
    static File familyExamplesDir = new File("./examples");
    static String uriPrefix = "http://upb.de/Music#";

    public static void main(String[] args) throws ComponentInitException {
        /* Define the knowledge source
         * > ks.type = "OWL File"
         * > ks.fileName = "father.owl"
         *
         */

        Random random = new Random(System.currentTimeMillis());
        OWLFile ks = new OWLFile();
        ks.setFileName(familyExamplesDir.getAbsolutePath() + "/music.owl");
        ks.init();

        /* Set up the reasoner
         * > reasoner.type = "closed world reasoner"
         * > reasoner.sources = { ks }
         */
        ClosedWorldReasoner reasoner = new ClosedWorldReasoner();

        // create { ks }, i.e. a set containing ks
        Set<KnowledgeSource> sources = new HashSet<>();
        sources.add(ks);

        reasoner.setSources(sources);
        reasoner.init();
        PosNegLPStandard lp = new PosNegLPStandard(reasoner);


        HashSet<OWLIndividual> playlistSongs = new HashSet<>();
        List<OWLIndividual> negativeExamples = new ArrayList<>();
        HashSet<OWLIndividual> sampledNegExamples = new HashSet<>();
        BufferedReader csvReader = null;

        try {
            csvReader = new BufferedReader(new FileReader(familyExamplesDir.getAbsolutePath() + "/preprocessedSongData2.csv"));
            String row = csvReader.readLine();
            while((row = csvReader.readLine()) != null) {
                String[] rowElements = row.split(",");
                if(rowElements[2].equals("00s Rock Anthems")) {
                    playlistSongs.add(new OWLNamedIndividualImpl(IRI.create(uriPrefix + rowElements[18])));
                }
                else {
                    negativeExamples.add(new OWLNamedIndividualImpl(IRI.create(uriPrefix + rowElements[18])));
                }
            }
            csvReader.close();


        } catch (Exception e) {
            e.printStackTrace();
        }
        // sampling songs from the dataset for negative examples
        int i=0;
        while(i != 1000) {
            int index = random.nextInt(negativeExamples.size());
            if(!sampledNegExamples.contains(negativeExamples.get(index)) ) {
                sampledNegExamples.add(negativeExamples.get(index));
                i++;
            }

        }
//        for( OWLIndividual s: negativeExamples) {
//            System.out.println(s);
//        }

        lp.setPositiveExamples(playlistSongs);
        lp.setNegativeExamples(sampledNegExamples);
        lp.init();

        /* Set up the learning algorithm
         * > alg.type = "celoe"
         * > alg.maxExecutionTimeInSeconds = 1
         */
        CELOE alg = new CELOE();
        alg.setMaxExecutionTimeInSeconds(3600);

        // This 'wiring' is not part of the configuration file since it is
        // done automatically when using bin/cli. However it has to be done explicitly,
        // here.
        alg.setLearningProblem(lp);
        alg.setReasoner(reasoner);

        alg.init();

        alg.start();
    }
}
